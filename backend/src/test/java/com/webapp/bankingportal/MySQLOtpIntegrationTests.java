
package com.webapp.bankingportal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.OtpInfo;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.repository.OtpInfoRepository;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.service.AccountService;
import com.webapp.bankingportal.service.OtpService;

@SpringBootTest
@ActiveProfiles("mysql-test")
class MySQLOtpIntegrationTests {

    @Autowired
    private OtpService otpService;

    @Autowired
    private OtpInfoRepository otpInfoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Account createTestAccount() {

        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setName("MySQL OTP Test");
        user.setPassword(passwordEncoder.encode("TestPassword123!"));
        user.setEmail("otp-mysql-" + unique + "@example.com");
        user.setCountryCode("IN");
        user.setPhoneNumber("+91" + System.nanoTime());
        user.setAddress("MySQL OTP Test Address");

        userRepository.saveAndFlush(user);

        return accountService.createAccount(user);
    }

    @Test
    void concurrentOtpVerificationMustSucceedOnlyOnce()
            throws Exception {

        Account account = createTestAccount();
        String accountNumber = account.getAccountNumber();

        String otp = otpService.generateOTP(accountNumber);

        assertNotNull(otp);
        assertTrue(otp.matches("\\d{6}"));

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {

            Future<Boolean> first = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return otpService.validateOTP(accountNumber, otp);
            });

            Future<Boolean> second = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return otpService.validateOTP(accountNumber, otp);
            });

            assertTrue(ready.await(10, TimeUnit.SECONDS));

            start.countDown();

            boolean firstResult = first.get(30, TimeUnit.SECONDS);
            boolean secondResult = second.get(30, TimeUnit.SECONDS);

            assertNotEquals(
                    firstResult,
                    secondResult,
                    "Exactly one concurrent OTP verification must succeed"
            );

            OtpInfo savedInfo = otpInfoRepository
                    .findByAccountNumber(accountNumber)
                    .orElseThrow();

            assertTrue(savedInfo.isConsumed());

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentWrongOtpAttemptsMustRespectRetryLimit()
            throws Exception {

        Account account = createTestAccount();
        String accountNumber = account.getAccountNumber();

        String validOtp = otpService.generateOTP(accountNumber);

        String wrongOtp = validOtp.equals("111111")
                ? "222222"
                : "111111";

        int requestCount = 5;

        ExecutorService executor =
                Executors.newFixedThreadPool(requestCount);

        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try {

            @SuppressWarnings("unchecked")
            Future<Boolean>[] results = new Future[requestCount];

            for (int i = 0; i < requestCount; i++) {

                results[i] = executor.submit(() -> {

                    ready.countDown();

                    assertTrue(start.await(10, TimeUnit.SECONDS));

                    return otpService.validateOTP(
                            accountNumber,
                            wrongOtp
                    );
                });
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));

            start.countDown();

            for (Future<Boolean> result : results) {

                assertFalse(
                        result.get(30, TimeUnit.SECONDS),
                        "Incorrect OTP must never be accepted"
                );
            }

            OtpInfo savedInfo = otpInfoRepository
                    .findByAccountNumber(accountNumber)
                    .orElseThrow();

            assertEquals(
                    3,
                    savedInfo.getFailedAttempts(),
                    "Failed attempts must not exceed the configured limit"
            );

            assertTrue(
                    savedInfo.isConsumed(),
                    "OTP must be consumed after three incorrect attempts"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
