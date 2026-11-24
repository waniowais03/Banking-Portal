package com.webapp.bankingportal;

import com.webapp.bankingportal.entity.PasswordResetToken;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.repository.PasswordResetTokenRepository;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("mysql-test")
class MySQLPasswordResetTokenIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    private User createTestUser() {
        User user = new User();
        user.setName("Password Reset Test");
        user.setEmail("reset" + System.nanoTime() + "@test.com");
        user.setPassword("TestPassword123");
        user.setCountryCode("+91");
        user.setPhoneNumber(String.valueOf(System.nanoTime()).substring(0, 10));
        user.setAddress("Test Address");
        return userRepository.saveAndFlush(user);
    }

    private PasswordResetToken createTestToken(
            User user, String token, LocalDateTime expiry) {
        return tokenRepository.saveAndFlush(
                new PasswordResetToken(token, user, expiry));
    }

    private void cleanup(String... tokens) {
        for (String token : tokens) {
            tokenRepository.deleteByToken(token);
        }
    }

    @Test
    @Transactional
    void validTokenMustBeConsumedSuccessfully() {
        User user = createTestUser();
        String token = "valid-" + System.nanoTime();

        createTestToken(user, token, LocalDateTime.now().plusHours(1));

        assertTrue(authService.verifyPasswordResetToken(token, user));
        assertFalse(tokenRepository.findByToken(token).isPresent());
    }

    @Test
    @Transactional
    void expiredTokenMustNotBeConsumed() {
        User user = createTestUser();
        String token = "expired-" + System.nanoTime();

        createTestToken(user, token, LocalDateTime.now().minusMinutes(1));

        assertFalse(authService.verifyPasswordResetToken(token, user));
        assertTrue(tokenRepository.findByToken(token).isPresent());

        cleanup(token);
    }

    @Test
    @Transactional
    void wrongUserMustNotConsumeValidToken() {
        User owner = createTestUser();
        User anotherUser = createTestUser();
        String token = "wrong-user-" + System.nanoTime();

        createTestToken(owner, token, LocalDateTime.now().plusHours(1));

        assertFalse(authService.verifyPasswordResetToken(token, anotherUser));
        assertTrue(tokenRepository.findByToken(token).isPresent());

        cleanup(token);
    }

    @Test
    @Transactional
    void consumedTokenMustNotBeUsedAgain() {
        User user = createTestUser();
        String token = "reuse-" + System.nanoTime();

        createTestToken(user, token, LocalDateTime.now().plusHours(1));

        assertTrue(authService.verifyPasswordResetToken(token, user));
        assertFalse(authService.verifyPasswordResetToken(token, user));
    }

    @Test
    void concurrentVerificationMustConsumeTokenOnlyOnce() throws Exception {
        User user = createTestUser();
        String token = "concurrent-" + System.nanoTime();

        createTestToken(user, token, LocalDateTime.now().plusHours(1));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> {
                start.await();
                return authService.verifyPasswordResetToken(token, user);
            });

            Future<Boolean> second = executor.submit(() -> {
                start.await();
                return authService.verifyPasswordResetToken(token, user);
            });

            start.countDown();

            boolean firstResult = first.get();
            boolean secondResult = second.get();

            assertNotEquals(firstResult, secondResult);
            assertFalse(tokenRepository.findByToken(token).isPresent());
        } finally {
            executor.shutdownNow();
            tokenRepository.deleteByToken(token);
        }
    }

    @Test
    @Transactional
    void multiplePasswordResetTokensMustHaveUniqueGeneratedIds() {
        User firstUser = createTestUser();
        User secondUser = createTestUser();
        User thirdUser = createTestUser();

        PasswordResetToken first = createTestToken(
                firstUser, "id-one-" + System.nanoTime(),
                LocalDateTime.now().plusHours(1));

        PasswordResetToken second = createTestToken(
                secondUser, "id-two-" + System.nanoTime(),
                LocalDateTime.now().plusHours(1));

        PasswordResetToken third = createTestToken(
                thirdUser, "id-three-" + System.nanoTime(),
                LocalDateTime.now().plusHours(1));

        Set<Long> ids = new HashSet<>();
        ids.add(first.getId());
        ids.add(second.getId());
        ids.add(third.getId());

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotNull(third.getId());
        assertEquals(3, ids.size());
    }
}
