package com.webapp.bankingportal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.OtpInfo;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.exception.OtpRetryLimitExceededException;
import com.webapp.bankingportal.repository.OtpInfoRepository;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.service.OtpService;
import com.webapp.bankingportal.util.ValidationUtil;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class OtpServiceIntegrationTests {

    private static final String ACCOUNT = "OTPTEST12345";

    @Autowired
    private OtpService otpService;

    @Autowired
    private OtpInfoRepository otpInfoRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private ValidationUtil validationUtil;

    @BeforeEach
    void setUp() {

        otpInfoRepository.deleteAll();

        userRepository.findByAccountAccountNumber(ACCOUNT)
                .ifPresent(userRepository::delete);

        User user = new User();
        user.setName("OTP Integration Test");
        user.setPassword("TestPassword123!");
        user.setEmail("otp-" + UUID.randomUUID() + "@example.com");
        user.setCountryCode("IN");
        user.setPhoneNumber("+91" + String.valueOf(
                System.nanoTime()).substring(0, 10));
        user.setAddress("Test Address");

        Account account = new Account();
        account.setAccountNumber(ACCOUNT);
        account.setUser(user);

        user.setAccount(account);

        userRepository.saveAndFlush(user);

        when(validationUtil.doesAccountExist(anyString()))
                .thenReturn(true);
    }

    @Test
    void testCooldownPersistsAfterGenerationLimitException() {

        for (int i = 0; i < 3; i++) {
            String otp = otpService.generateOTP(ACCOUNT);

            assertNotNull(otp);
            assertTrue(otp.matches("\\d{6}"));
        }

        assertThrows(
                OtpRetryLimitExceededException.class,
                () -> otpService.generateOTP(ACCOUNT)
        );

        OtpInfo savedInfo = otpInfoRepository
                .findByAccountNumber(ACCOUNT)
                .orElseThrow();

        assertNotNull(savedInfo.getGenerationBlockedUntil());

        assertTrue(
                savedInfo.getGenerationBlockedUntil()
                        .isAfter(LocalDateTime.now())
        );

        LocalDateTime originalCooldown =
                savedInfo.getGenerationBlockedUntil();

        assertThrows(
                OtpRetryLimitExceededException.class,
                () -> otpService.generateOTP(ACCOUNT)
        );

        OtpInfo verifiedInfo = otpInfoRepository
                .findByAccountNumber(ACCOUNT)
                .orElseThrow();

        assertEquals(
                originalCooldown,
                verifiedInfo.getGenerationBlockedUntil()
        );
    }

    @Test
    void testConcurrentFirstTimeOtpGenerationRespectsLimit()
            throws Exception {

        int requestCount = 6;

        ExecutorService executor =
                Executors.newFixedThreadPool(requestCount);

        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();

        try {

            for (int i = 0; i < requestCount; i++) {

                results.add(executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {
                        String otp = otpService.generateOTP(ACCOUNT);

                        assertTrue(otp.matches("\\d{6}"));
                        return true;

                    } catch (OtpRetryLimitExceededException e) {
                        return false;
                    }
                }));
            }

            assertTrue(ready.await(
                    10,
                    java.util.concurrent.TimeUnit.SECONDS
            ));

            start.countDown();

            int successfulRequests = 0;
            int rejectedRequests = 0;

            for (Future<Boolean> result : results) {

                if (result.get()) {
                    successfulRequests++;
                } else {
                    rejectedRequests++;
                }
            }

            assertEquals(3, successfulRequests);
            assertEquals(3, rejectedRequests);

            OtpInfo savedInfo = otpInfoRepository
                    .findByAccountNumber(ACCOUNT)
                    .orElseThrow();

            assertEquals(3, savedInfo.getGenerationAttempts());

            assertNotNull(savedInfo.getGenerationBlockedUntil());

        } finally {
            executor.shutdownNow();
        }
    }
}
