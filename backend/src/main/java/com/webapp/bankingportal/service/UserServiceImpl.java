
package com.webapp.bankingportal.service;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import com.webapp.bankingportal.dto.LoginRequest;
import com.webapp.bankingportal.dto.OtpRequest;
import com.webapp.bankingportal.dto.OtpVerificationRequest;
import com.webapp.bankingportal.dto.UserResponse;
import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.exception.AccountNumberCollisionException;
import com.webapp.bankingportal.exception.InvalidTokenException;
import com.webapp.bankingportal.exception.PasswordResetException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.exception.UserInvalidException;
import com.webapp.bankingportal.mapper.UserMapper;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.util.JsonUtil;
import com.webapp.bankingportal.util.LoggedinUser;
import com.webapp.bankingportal.util.ValidationUtil;
import com.webapp.bankingportal.util.ApiMessages;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AccountService accountService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final GeolocationService geolocationService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final ValidationUtil validationUtil;
    private final PlatformTransactionManager transactionManager;

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 10;

    // REGISTER USER
    @Override
    public ResponseEntity<String> registerUser(User user) {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        for (int attempt = 1;
             attempt <= MAX_ACCOUNT_NUMBER_ATTEMPTS;
             attempt++) {

            User registrationUser = copyRegistrationUser(user);

            try {
                User savedUser = transactionTemplate.execute(status -> {

                    validationUtil.validateNewUser(registrationUser);
                    encodePassword(registrationUser);

                    return saveUserWithAccount(registrationUser);
                });

                return ResponseEntity.ok(
                        JsonUtil.toJson(new UserResponse(savedUser))
                );

            } catch (AccountNumberCollisionException exception) {

                if (attempt == MAX_ACCOUNT_NUMBER_ATTEMPTS) {
                    throw exception;
                }

                log.warn(
                        "Account number collision. Retrying registration, attempt {}/{}",
                        attempt + 1,
                        MAX_ACCOUNT_NUMBER_ATTEMPTS
                );
            }
        }

        throw new IllegalStateException(
                "User registration failed after maximum account number attempts"
        );
    }

    private User copyRegistrationUser(User source) {

        User copy = new User();
        copy.setName(source.getName());
        copy.setPassword(source.getPassword());
        copy.setEmail(source.getEmail());
        copy.setCountryCode(source.getCountryCode());
        copy.setPhoneNumber(source.getPhoneNumber());
        copy.setAddress(source.getAddress());

        return copy;
    }

    // LOGIN
    @Override
    public ResponseEntity<String> login(
            LoginRequest loginRequest,
            HttpServletRequest request) throws InvalidTokenException {

        User user = authenticateUser(loginRequest);

        sendLoginNotification(user, request.getRemoteAddr());

        String token = generateAndSaveToken(
                user.getAccount().getAccountNumber()
        );

        return ResponseEntity.ok(
                String.format(ApiMessages.TOKEN_ISSUED_SUCCESS.getMessage(), token)
        );
    }

    // GENERATE OTP
    @Override
    public ResponseEntity<String> generateOtp(OtpRequest otpRequest) {

        User user = getUserByIdentifier(otpRequest.identifier());

        String otp = otpService.generateOTP(
                user.getAccount().getAccountNumber()
        );

        return sendOtpEmail(user, otp);
    }

    // VERIFY OTP AND LOGIN
    @Override
    public ResponseEntity<String> verifyOtpAndLogin(
            OtpVerificationRequest otpVerificationRequest)
            throws InvalidTokenException {

        validateOtpRequest(otpVerificationRequest);

        User user = getUserByIdentifier(
                otpVerificationRequest.identifier()
        );

        validateOtp(user, otpVerificationRequest.otp());

        String token = generateAndSaveToken(
                user.getAccount().getAccountNumber()
        );

        return ResponseEntity.ok(
                String.format(ApiMessages.TOKEN_ISSUED_SUCCESS.getMessage(), token)
        );
    }

    // UPDATE USER
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<String> updateUser(User updatedUser) {

        String accountNumber = LoggedinUser.getAccountNumber();

        authenticateUser(accountNumber, updatedUser.getPassword());

        User existingUser = getUserByAccountNumber(accountNumber);

        updateUserDetails(existingUser, updatedUser);

        User savedUser = saveUser(existingUser);

        return ResponseEntity.ok(
                JsonUtil.toJson(new UserResponse(savedUser))
        );
    }

    // RESET PASSWORD
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(User user, String newPassword) {

        try {
            user.setPassword(passwordEncoder.encode(newPassword));

            userRepository.save(user);

            return true;

        } catch (Exception e) {

            throw new PasswordResetException(
                    ApiMessages.PASSWORD_RESET_FAILURE.getMessage(),
                    e
            );
        }
    }

    // LOGOUT
    @Override
    public ModelAndView logout(String token) throws InvalidTokenException {

        token = token.substring(7);

        tokenService.validateToken(token);

        tokenService.invalidateToken(token);

        log.info(
                "User logged out successfully {}",
                tokenService.getUsernameFromToken(token)
        );

        return new ModelAndView("redirect:/logout");
    }

    // SAVE USER
    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // GET USER BY IDENTIFIER
    @Override
    public User getUserByIdentifier(String identifier) {

        User user;

        if (validationUtil.doesEmailExist(identifier)) {

            user = getUserByEmail(identifier);

        } else if (validationUtil.doesAccountExist(identifier)) {

            user = getUserByAccountNumber(identifier);

        } else {

            throw new UserInvalidException(
                    String.format(
                            ApiMessages.USER_NOT_FOUND_BY_IDENTIFIER.getMessage(),
                            identifier
                    )
            );
        }

        return user;
    }

    // GET USER BY ACCOUNT NUMBER
    @Override
    public User getUserByAccountNumber(String accountNo) {

        return userRepository.findByAccountAccountNumber(accountNo)
                .orElseThrow(
                        () -> new UserInvalidException(
                                String.format(
                                        ApiMessages.USER_NOT_FOUND_BY_ACCOUNT.getMessage(),
                                        accountNo
                                )
                        )
                );
    }

    // GET USER BY EMAIL
    @Override
    public User getUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new UserInvalidException(
                                String.format(
                                        ApiMessages.USER_NOT_FOUND_BY_EMAIL.getMessage(),
                                        email
                                )
                        )
                );
    }

    // ENCODE PASSWORD
    private void encodePassword(User user) {

        user.setCountryCode(user.getCountryCode().toUpperCase());

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );
    }

    // SAVE USER WITH ACCOUNT
    private User saveUserWithAccount(User user) {

        // First save user
        User savedUser = saveUser(user);

        // Create and save account
        Account account = accountService.createAccount(savedUser);

        if (account == null) {
            throw new IllegalStateException(
                    "Account creation failed during user registration"
            );
        }

        // Maintain both sides of the relationship
        account.setUser(savedUser);
        savedUser.setAccount(account);

        // Save updated user
        return saveUser(savedUser);
    }

    // AUTHENTICATE USER
    private User authenticateUser(LoginRequest loginRequest) {

        User user = getUserByIdentifier(
                loginRequest.identifier()
        );

        String accountNumber = user.getAccount().getAccountNumber();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        accountNumber,
                        loginRequest.password()
                )
        );

        return user;
    }

    private void authenticateUser(String accountNumber, String password) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        accountNumber,
                        password
                )
        );
    }

    // GENERATE AND SAVE TOKEN
    private String generateAndSaveToken(String accountNumber)
            throws InvalidTokenException {

        var userDetails = userDetailsService.loadUserByUsername(
                accountNumber
        );

        String token = tokenService.generateToken(userDetails);

        tokenService.saveToken(token);

        return token;
    }

    // SEND OTP EMAIL
    private ResponseEntity<String> sendOtpEmail(User user, String otp) {

        CompletableFuture<Void> emailSendingFuture =
                otpService.sendOTPByEmail(
                        user.getEmail(),
                        user.getName(),
                        user.getAccount().getAccountNumber(),
                        otp
                );

        ResponseEntity<String> successResponse =
                ResponseEntity.ok(
                        String.format(
                                ApiMessages.OTP_SENT_SUCCESS.getMessage(),
                                user.getEmail()
                        )
                );

        ResponseEntity<String> failureResponse =
                ResponseEntity.internalServerError()
                        .body(
                                String.format(
                                        ApiMessages.OTP_SENT_FAILURE.getMessage(),
                                        user.getEmail()
                                )
                        );

        return emailSendingFuture
                .thenApply(result -> successResponse)
                .exceptionally(e -> failureResponse)
                .join();
    }

    // VALIDATE OTP REQUEST
    private void validateOtpRequest(OtpVerificationRequest request) {

        if (request.identifier() == null ||
                request.identifier().isEmpty()) {

            throw new IllegalArgumentException(
                    ApiMessages.IDENTIFIER_MISSING_ERROR.getMessage()
            );
        }

        if (request.otp() == null ||
                request.otp().isEmpty()) {

            throw new IllegalArgumentException(
                    ApiMessages.OTP_MISSING_ERROR.getMessage()
            );
        }
    }

    // VALIDATE OTP
    private void validateOtp(User user, String otp) {

        if (!otpService.validateOTP(
                user.getAccount().getAccountNumber(),
                otp
        )) {

            throw new UnauthorizedException(
                    ApiMessages.OTP_INVALID_ERROR.getMessage()
            );
        }
    }

    // UPDATE USER DETAILS
    private void updateUserDetails(
            User existingUser,
            User updatedUser) {

        ValidationUtil.validateUserDetails(updatedUser);

        updatedUser.setPassword(
                existingUser.getPassword()
        );

        userMapper.updateUser(
                updatedUser,
                existingUser
        );
    }

    // SEND LOGIN NOTIFICATION
    private CompletableFuture<Boolean> sendLoginNotification(
            User user,
            String ip) {

        String loginTime = new Timestamp(
                System.currentTimeMillis()
        ).toString();

        return geolocationService.getGeolocation(ip)

                .thenComposeAsync(geolocationResponse -> {

                    String loginLocation = String.format(
                            "%s, %s",
                            geolocationResponse.getCity().getNames().get("en"),
                            geolocationResponse.getCountry().getNames().get("en")
                    );

                    return sendLoginEmail(
                            user,
                            loginTime,
                            loginLocation
                    );
                })

                .exceptionallyComposeAsync(throwable ->
                        sendLoginEmail(
                                user,
                                loginTime,
                                "Unknown"
                        )
                );
    }

    // SEND LOGIN EMAIL
    private CompletableFuture<Boolean> sendLoginEmail(
            User user,
            String loginTime,
            String loginLocation) {

        String emailText = emailService.getLoginEmailTemplate(
                user.getName(),
                loginTime,
                loginLocation
        );

        return emailService.sendEmail(
                user.getEmail(),
                ApiMessages.EMAIL_SUBJECT_LOGIN.getMessage(),
                emailText
        )
        .thenApplyAsync(result -> true)
        .exceptionally(ex -> false);
    }
}
