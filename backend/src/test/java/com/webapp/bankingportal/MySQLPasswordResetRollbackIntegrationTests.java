package com.webapp.bankingportal;

import com.webapp.bankingportal.dto.ResetPasswordRequest;
import com.webapp.bankingportal.entity.PasswordResetToken;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.repository.PasswordResetTokenRepository;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("mysql-test")
class MySQLPasswordResetRollbackIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void passwordUpdateFailureMustRollbackTokenDeletion() {
        String unique = UUID.randomUUID().toString().replace("-", "");
        String email = "rollback" + unique.substring(0, 12) + "@test.com";
        String phone = String.valueOf(System.nanoTime()).substring(0, 10);
        String resetToken = "rollback-" + unique;

        User user = new User();
        user.setName("Rollback Test User");
        user.setEmail(email);
        user.setPassword("OriginalPassword123");
        user.setCountryCode("+91");
        user.setPhoneNumber(phone);
        user.setAddress("Test Address");

        user = userRepository.saveAndFlush(user);

        String originalPassword = user.getPassword();

        tokenRepository.saveAndFlush(
                new PasswordResetToken(
                        resetToken,
                        user,
                        LocalDateTime.now().plusHours(1)
                )
        );

        String triggerName = "fail_password_reset_test";

        jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + triggerName);

        try {
            jdbcTemplate.execute(
                    "CREATE TRIGGER " + triggerName +
                    " BEFORE UPDATE ON `user` " +
                    " FOR EACH ROW " +
                    " SIGNAL SQLSTATE '45000' " +
                    " SET MESSAGE_TEXT = 'Simulated password update failure'"
            );

            ResetPasswordRequest request = new ResetPasswordRequest(
                    email,
                    resetToken,
                    "NewPassword123"
            );

            assertThrows(
                    RuntimeException.class,
                    () -> authService.resetPassword(request)
            );

        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + triggerName);
        }

        assertTrue(
                tokenRepository.findByToken(resetToken).isPresent(),
                "Reset token must remain available after transaction rollback"
        );

        User refreshedUser = userRepository.findByEmail(email).orElseThrow();

        assertEquals(
                originalPassword,
                refreshedUser.getPassword(),
                "Original password must remain unchanged"
        );

        tokenRepository.findByToken(resetToken).ifPresent(tokenRepository::delete);
        userRepository.deleteById(user.getId());
    }
}
