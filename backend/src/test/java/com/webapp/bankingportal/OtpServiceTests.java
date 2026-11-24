package com.webapp.bankingportal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.OtpInfo;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.exception.OtpRetryLimitExceededException;
import com.webapp.bankingportal.repository.OtpInfoRepository;
import com.webapp.bankingportal.service.EmailService;
import com.webapp.bankingportal.service.OtpServiceImpl;
import com.webapp.bankingportal.util.ValidationUtil;

@ExtendWith(MockitoExtension.class)
class OtpServiceTests {

    private static final String ACCOUNT = "ACC123456";
    private static final String SECRET =
            "test-only-otp-hmac-secret-for-unit-tests-2026";

    @Mock
    private EmailService emailService;

    @Mock
    private OtpInfoRepository otpInfoRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ValidationUtil validationUtil;

    @InjectMocks
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                otpService, "otpHmacSecret", SECRET
        );

        Account account = new Account();
        account.setAccountNumber(ACCOUNT);

        lenient().when(accountRepository.findByAccountNumberForUpdate(ACCOUNT))
                .thenReturn(Optional.of(account));
    }

    @Test
    void testGenerateOTPReturnsSixDigitCodeAndStoresHash() {
        when(validationUtil.doesAccountExist(ACCOUNT)).thenReturn(true);
        when(otpInfoRepository.findByAccountNumberForUpdate(ACCOUNT))
                .thenReturn(Optional.empty());

        AtomicReference<OtpInfo> savedOtp = new AtomicReference<>();

        when(otpInfoRepository.saveAndFlush(any(OtpInfo.class)))
                .thenAnswer(invocation -> {
                    OtpInfo info = invocation.getArgument(0);
                    savedOtp.set(info);
                    return info;
                });

        String otp = otpService.generateOTP(ACCOUNT);

        assertNotNull(otp);
        assertTrue(otp.matches("\\d{6}"));
        assertNotNull(savedOtp.get());
        assertNotEquals(otp, savedOtp.get().getOtp());
        assertEquals(1, savedOtp.get().getGenerationAttempts());
        assertEquals(0, savedOtp.get().getFailedAttempts());
        assertFalse(savedOtp.get().isConsumed());
    }

    @Test
    void testCorrectOTPCanOnlyBeUsedOnce() {
        when(validationUtil.doesAccountExist(ACCOUNT)).thenReturn(true);

        OtpInfo[] savedOtp = new OtpInfo[1];

        when(otpInfoRepository.findByAccountNumberForUpdate(ACCOUNT))
                .thenAnswer(invocation ->
                        Optional.ofNullable(savedOtp[0]));

        when(otpInfoRepository.saveAndFlush(any(OtpInfo.class)))
                .thenAnswer(invocation -> {
                    savedOtp[0] = invocation.getArgument(0);
                    return savedOtp[0];
                });

        when(otpInfoRepository.save(any(OtpInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String otp = otpService.generateOTP(ACCOUNT);

        assertTrue(otpService.validateOTP(ACCOUNT, otp));
        assertTrue(savedOtp[0].isConsumed());
        assertFalse(otpService.validateOTP(ACCOUNT, otp));
    }

    @Test
    void testWrongOTPIsRejected() {
        OtpInfo info = new OtpInfo(
                ACCOUNT, "some-hash", LocalDateTime.now()
        );

        when(otpInfoRepository.findByAccountNumberForUpdate(ACCOUNT))
                .thenReturn(Optional.of(info));

        when(otpInfoRepository.save(any(OtpInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(otpService.validateOTP(ACCOUNT, "123456"));
        assertEquals(1, info.getFailedAttempts());
        assertFalse(info.isConsumed());
    }

    @Test
    void testOTPIsConsumedAfterThreeWrongAttempts() {
        OtpInfo info = new OtpInfo(
                ACCOUNT, "some-hash", LocalDateTime.now()
        );

        when(otpInfoRepository.findByAccountNumberForUpdate(ACCOUNT))
                .thenReturn(Optional.of(info));

        when(otpInfoRepository.save(any(OtpInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(otpService.validateOTP(ACCOUNT, "111111"));
        assertFalse(otpService.validateOTP(ACCOUNT, "222222"));
        assertFalse(otpService.validateOTP(ACCOUNT, "333333"));

        assertEquals(3, info.getFailedAttempts());
        assertTrue(info.isConsumed());
        assertFalse(otpService.validateOTP(ACCOUNT, "123456"));
    }

    @Test
    void testExpiredOTPIsRejected() {
        OtpInfo info = new OtpInfo(
                ACCOUNT,
                "some-hash",
                LocalDateTime.now().minusMinutes(6)
        );

        when(otpInfoRepository.findByAccountNumberForUpdate(ACCOUNT))
                .thenReturn(Optional.of(info));

        when(otpInfoRepository.save(any(OtpInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertFalse(otpService.validateOTP(ACCOUNT, "123456"));
        assertTrue(info.isConsumed());
    }

    @Test
    void testInvalidOTPFormatIsRejected() {
        assertFalse(otpService.validateOTP(ACCOUNT, "123"));
        assertFalse(otpService.validateOTP(ACCOUNT, "abcdef"));
        assertFalse(otpService.validateOTP(ACCOUNT, null));

        verifyNoInteractions(otpInfoRepository);
    }

    @Test
    void testOTPGenerationLimitIsEnforced() {
        OtpInfo info = new OtpInfo(
                ACCOUNT, "", LocalDateTime.now()
        );

        info.setGenerationAttempts(3);

        when(validationUtil.doesAccountExist(ACCOUNT)).thenReturn(true);
        when(otpInfoRepository.findByAccountNumberForUpdate(ACCOUNT))
                .thenReturn(Optional.of(info));

        when(otpInfoRepository.saveAndFlush(any(OtpInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(
                OtpRetryLimitExceededException.class,
                () -> otpService.generateOTP(ACCOUNT)
        );

        assertNotNull(info.getGenerationBlockedUntil());
        verify(otpInfoRepository).saveAndFlush(info);
    }
}
