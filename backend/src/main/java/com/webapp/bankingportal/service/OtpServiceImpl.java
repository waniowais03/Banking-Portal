
package com.webapp.bankingportal.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.webapp.bankingportal.entity.OtpInfo;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.exception.AccountDoesNotExistException;
import com.webapp.bankingportal.exception.OtpRetryLimitExceededException;
import com.webapp.bankingportal.repository.OtpInfoRepository;
import com.webapp.bankingportal.util.ApiMessages;
import com.webapp.bankingportal.util.ValidationUtil;

import lombok.RequiredArgsConstructor;
import lombok.val;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    public static final int OTP_ATTEMPTS_LIMIT = 3;
    public static final int OTP_EXPIRY_MINUTES = 5;
    public static final int OTP_RESET_WAITING_TIME_MINUTES = 10;
    public static final int OTP_RETRY_LIMIT_WINDOW_MINUTES = 15;

    private static final int OTP_LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final EmailService emailService;
    private final OtpInfoRepository otpInfoRepository;
    private final AccountRepository accountRepository;
    private final ValidationUtil validationUtil;

    @Value("${otp.hmac.secret}")
    private String otpHmacSecret;

    private String hashOtp(String accountNumber, String otp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            SecretKeySpec key = new SecretKeySpec(
                    otpHmacSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );

            mac.init(key);

            byte[] hash = mac.doFinal(
                    (accountNumber + ":" + otp).getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash OTP", e);
        }
    }

    private boolean matchesOtp(String accountNumber, String otp, String storedHash) {
        if (otp == null || storedHash == null) {
            return false;
        }

        byte[] expected = hashOtp(accountNumber, otp)
                .getBytes(StandardCharsets.UTF_8);

        byte[] actual = storedHash.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, actual);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = OtpRetryLimitExceededException.class)
    public String generateOTP(String accountNumber) {

        if (!validationUtil.doesAccountExist(accountNumber)) {
            throw new AccountDoesNotExistException(
                    ApiMessages.ACCOUNT_NOT_FOUND.getMessage()
            );
        }

        accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountDoesNotExistException(
                        ApiMessages.ACCOUNT_NOT_FOUND.getMessage()
                ));

        LocalDateTime now = LocalDateTime.now();

        OtpInfo otpInfo = otpInfoRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElse(null);

        if (otpInfo == null) {
            otpInfo = new OtpInfo(
                    accountNumber,
                    "",
                    now
            );
        }

        if (otpInfo.getGenerationBlockedUntil() != null
                && now.isBefore(otpInfo.getGenerationBlockedUntil())) {

            long waitingMinutes = Math.max(
                    1,
                    java.time.Duration.between(
                            now,
                            otpInfo.getGenerationBlockedUntil()
                    ).toMinutes() + 1
            );

            throw new OtpRetryLimitExceededException(
                    String.format(
                            ApiMessages.OTP_GENERATION_LIMIT_EXCEEDED.getMessage(),
                            waitingMinutes
                    )
            );
        }

        if (otpInfo.getGenerationWindowStartedAt() == null
                || !otpInfo.getGenerationWindowStartedAt()
                        .isAfter(now.minusMinutes(OTP_RETRY_LIMIT_WINDOW_MINUTES))) {

            otpInfo.setGenerationWindowStartedAt(now);
            otpInfo.setGenerationAttempts(0);
            otpInfo.setGenerationBlockedUntil(null);
        }

        if (otpInfo.getGenerationAttempts() >= OTP_ATTEMPTS_LIMIT) {

            otpInfo.setGenerationBlockedUntil(
                    now.plusMinutes(OTP_RESET_WAITING_TIME_MINUTES)
            );

            otpInfoRepository.saveAndFlush(otpInfo);

            throw new OtpRetryLimitExceededException(
                    String.format(
                            ApiMessages.OTP_GENERATION_LIMIT_EXCEEDED.getMessage(),
                            OTP_RESET_WAITING_TIME_MINUTES
                    )
            );
        }

        int otpValue = 100_000 + SECURE_RANDOM.nextInt(900_000);
        String otp = String.valueOf(otpValue);

        otpInfo.setOtp(hashOtp(accountNumber, otp));
        otpInfo.setGeneratedAt(now);
        otpInfo.setFailedAttempts(0);
        otpInfo.setConsumed(false);

        otpInfo.setGenerationAttempts(
                otpInfo.getGenerationAttempts() + 1
        );

        otpInfoRepository.saveAndFlush(otpInfo);

        return otp;
    }

    @Override
    public CompletableFuture<Void> sendOTPByEmail(
            String email,
            String name,
            String accountNumber,
            String otp
    ) {

        String maskedAccount = accountNumber.length() > 3
                ? "xxx" + accountNumber.substring(3)
                : "xxx";

        String emailText = emailService.getOtpLoginEmailTemplate(
                name,
                maskedAccount,
                otp
        );

        return emailService.sendEmail(
                email,
                ApiMessages.EMAIL_SUBJECT_OTP.getMessage(),
                emailText
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = OtpRetryLimitExceededException.class)
    public boolean validateOTP(String accountNumber, String otp) {

        if (accountNumber == null || otp == null
                || !otp.matches("\\d{6}")) {
            return false;
        }

        OtpInfo otpInfo = otpInfoRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElse(null);

        if (otpInfo == null || otpInfo.isConsumed()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (otpInfo.getGeneratedAt() == null
                || !now.isBefore(
                        otpInfo.getGeneratedAt().plusMinutes(OTP_EXPIRY_MINUTES)
                )) {

            otpInfo.setConsumed(true);
            otpInfoRepository.save(otpInfo);

            return false;
        }

        if (otpInfo.getFailedAttempts() >= OTP_ATTEMPTS_LIMIT) {
            otpInfo.setConsumed(true);
            otpInfoRepository.save(otpInfo);

            return false;
        }

        if (!matchesOtp(accountNumber, otp, otpInfo.getOtp())) {

            otpInfo.setFailedAttempts(
                    otpInfo.getFailedAttempts() + 1
            );

            if (otpInfo.getFailedAttempts() >= OTP_ATTEMPTS_LIMIT) {
                otpInfo.setConsumed(true);
            }

            otpInfoRepository.save(otpInfo);

            return false;
        }

        otpInfo.setConsumed(true);
        otpInfoRepository.save(otpInfo);

        return true;
    }
}
