
package com.webapp.bankingportal;

import static org.springframework.security.core.userdetails.User.withUsername;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import com.github.javafaker.Faker;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.jayway.jsonpath.JsonPath;

import com.webapp.bankingportal.dto.AmountRequest;
import com.webapp.bankingportal.dto.GeolocationResponse;
import com.webapp.bankingportal.dto.LoginRequest;
import com.webapp.bankingportal.dto.PinRequest;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.service.AccountService;
import com.webapp.bankingportal.service.GeolocationService;
import com.webapp.bankingportal.service.TokenService;
import com.webapp.bankingportal.util.ApiMessages;
import com.webapp.bankingportal.util.JsonUtil;

import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import lombok.val;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@AutoConfigureMockMvc
public abstract class BaseTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected AccountService accountService;

    @Autowired
    protected TokenService tokenService;

    @MockBean
    protected GeolocationService geolocationService;

    protected static final int MIN_PASSWORD_LENGTH = 8;
    protected static final int MAX_PASSWORD_LENGTH = 127;

    protected static final Faker faker = new Faker();

    protected static final PhoneNumberUtil phoneNumberUtil =
            PhoneNumberUtil.getInstance();

    // Configure mock geolocation response before every test.
    @BeforeEach
    protected void setUpGeolocationMock() {

        GeolocationResponse.City city =
                new GeolocationResponse.City();

        city.setNames(Map.of("en", "Test City"));

        GeolocationResponse.Country country =
                new GeolocationResponse.Country();

        country.setNames(Map.of("en", "Test Country"));

        GeolocationResponse response =
                new GeolocationResponse();

        response.setCity(city);
        response.setCountry(country);

        when(geolocationService.getGeolocation(anyString()))
                .thenReturn(
                        CompletableFuture.completedFuture(response)
                );
    }

    // Creates a POST request with a unique idempotency key.
    protected MockHttpServletRequestBuilder postWithIdempotencyKey(
            String url) {

        return MockMvcRequestBuilders
                .post(url)
                .header(
                        "Idempotency-Key",
                        UUID.randomUUID().toString()
                );
    }

    protected static String getRandomAccountNumber() {
        return faker.lorem().characters(6, false, true);
    }

    protected static String getRandomPassword() {
        return "!" + faker.internet().password(
                MAX_PASSWORD_LENGTH - 2,
                MAX_PASSWORD_LENGTH - 1,
                true,
                true
        );
    }

    protected static String getRandomCountryCode() {

        val supportedRegions =
                phoneNumberUtil.getSupportedRegions().toArray();

        val index = faker.number().numberBetween(
                0,
                supportedRegions.length - 1
        );

        return supportedRegions[index].toString();
    }

    protected static String getRandomPhoneNumber(String region) {

        val exampleNumber =
                phoneNumberUtil.getExampleNumber(region);

        for (int i = 0; i < 100; ++i) {

            val nationalNumber =
                    String.valueOf(exampleNumber.getNationalNumber());

            val randomPhoneNumber =
                    faker.number().digits(nationalNumber.length());

            try {

                val phoneNumber =
                        phoneNumberUtil.parse(randomPhoneNumber, region);

                if (phoneNumberUtil.isValidNumber(phoneNumber)) {

                    return phoneNumberUtil.format(
                            phoneNumber,
                            PhoneNumberUtil.PhoneNumberFormat.E164
                    );
                }

            } catch (NumberParseException e) {
                // Continue to next attempt if parsing fails.
            }
        }

        return phoneNumberUtil.format(
                exampleNumber,
                PhoneNumberUtil.PhoneNumberFormat.E164
        );
    }

    protected static String getRandomOtp() {
        return faker.number().digits(6);
    }

    protected static String getRandomPin() {
        return faker.number().digits(4);
    }

    protected String generateToken(
            String username,
            String password) {

        return tokenService.generateToken(
                withUsername(username)
                        .password(password)
                        .build()
        );
    }

    protected String generateToken(
            String username,
            String password,
            Date expiry) {

        return tokenService.generateToken(
                withUsername(username)
                        .password(password)
                        .build(),
                expiry
        );
    }

    protected static User createUser() {

        val countryCode = getRandomCountryCode();
        val phoneNumber = getRandomPhoneNumber(countryCode);

        val user = new User();

        user.setName(faker.name().fullName());
        user.setPassword(getRandomPassword());
        user.setEmail(faker.internet().safeEmailAddress());
        user.setAddress(faker.address().fullAddress());
        user.setCountryCode(countryCode);
        user.setPhoneNumber(phoneNumber);

        return user;
    }

    protected User createAndRegisterUser() throws Exception {

        val user = createUser();

        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(user))
        )
        .andExpect(MockMvcResultMatchers.status().isOk());

        return user;
    }

    protected HashMap<String, String> createAndLoginUser()
            throws Exception {

        val user = createAndRegisterUser();

        val accountNumber = userRepository
                .findByEmail(user.getEmail())
                .get()
                .getAccount()
                .getAccountNumber();

        val loginRequest =
                new LoginRequest(
                        accountNumber,
                        user.getPassword()
                );

        val loginResult = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(loginRequest))
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();

        val responseBody =
                loginResult.getResponse().getContentAsString();

        String token = JsonPath.read(responseBody, "$.token");

        val userDetails = new HashMap<String, String>();

        userDetails.put("name", user.getName());
        userDetails.put("email", user.getEmail());
        userDetails.put("countryCode", user.getCountryCode());
        userDetails.put("phoneNumber", user.getPhoneNumber());
        userDetails.put("address", user.getAddress());
        userDetails.put("accountNumber", accountNumber);
        userDetails.put("password", user.getPassword());
        userDetails.put("token", token);

        return userDetails;
    }

    protected HashMap<String, String> createAndLoginUserWithPin()
            throws Exception {

        val userDetails = createAndLoginUser();

        val accountNumber = userDetails.get("accountNumber");
        val password = userDetails.get("password");

        val pinRequest = new PinRequest(
                accountNumber,
                getRandomPin(),
                password
        );

        mockMvc.perform(
                postWithIdempotencyKey("/api/account/pin/create")
                        .header(
                                "Authorization",
                                "Bearer " + userDetails.get("token")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(pinRequest))
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
                MockMvcResultMatchers.content()
                        .string(
                                ApiMessages.PIN_CREATION_SUCCESS.getMessage()
                        )
        );

        userDetails.put("pin", pinRequest.pin());

        return userDetails;
    }

    protected HashMap<String, String> createAndLoginUserWithInitialBalance(
            BigDecimal amount) throws Exception {

        val userDetails = createAndLoginUserWithPin();

        val amountRequest = new AmountRequest(
                userDetails.get("accountNumber"),
                userDetails.get("pin"),
                amount
        );

        mockMvc.perform(
                postWithIdempotencyKey("/api/account/deposit")
                        .header(
                                "Authorization",
                                "Bearer " + userDetails.get("token")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(amountRequest))
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(
                MockMvcResultMatchers.content()
                        .string(
                                ApiMessages.CASH_DEPOSIT_SUCCESS.getMessage()
                        )
        );

        return userDetails;
    }

    protected HashMap<String, String> createAccount() {

        val accountDetails = new HashMap<String, String>();

        val user = createUser();

        accountDetails.put("password", user.getPassword());

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        userRepository.save(user);

        val account = accountService.createAccount(user);

        accountDetails.put(
                "accountNumber",
                account.getAccountNumber()
        );

        return accountDetails;
    }

    protected HashMap<String, String> createAccountWithPin(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            AccountService accountService) {

        val accountDetails = createAccount();

        accountDetails.put("pin", getRandomPin());

        accountService.createPin(
                accountDetails.get("accountNumber"),
                accountDetails.get("password"),
                accountDetails.get("pin")
        );

        return accountDetails;
    }

    protected HashMap<String, String> createAccountWithInitialBalance(
            BigDecimal amount) {

        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        accountService.cashDeposit(
                accountDetails.get("accountNumber"),
                accountDetails.get("pin"),
                amount
        );

        return accountDetails;
    }

    protected static String getTextFromPart(Part part)
            throws MessagingException, IOException {

        Object body = part.getContent();

        if (body instanceof String) {
            return (String) body;
        }

        if (body instanceof MimeMultipart) {

            MimeMultipart multipart = (MimeMultipart) body;

            StringBuilder result = new StringBuilder();

            for (int i = 0; i < multipart.getCount(); i++) {

                Part bodyPart = multipart.getBodyPart(i);

                String partText = getTextFromPart(bodyPart);

                if (bodyPart.isMimeType("text/html")) {
                    return partText;
                }

                if (!partText.isBlank()) {
                    result.append(partText).append('\n');
                }
            }

            return result.toString();
        }

        return "";
    }

    protected static String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart)
            throws MessagingException, IOException {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < mimeMultipart.getCount(); i++) {

            Part bodyPart = mimeMultipart.getBodyPart(i);

            String partText = getTextFromPart(bodyPart);

            if (bodyPart.isMimeType("text/html")) {
                return partText;
            }

            if (!partText.isBlank()) {
                result.append(partText).append('\n');
            }
        }

        return result.toString();
    }

    // Extracts the six-digit OTP from an email message.
    protected static String getOtpFromEmail(MimeMessage message)
            throws MessagingException, IOException {

        String emailBody = getTextFromPart(message);

        Matcher matcher = Pattern.compile("\\b\\d{6}\\b")
                .matcher(emailBody);

        if (matcher.find()) {
            return matcher.group();
        }

        throw new IllegalStateException(
                "Six-digit OTP not found in email body"
        );
    }
}
