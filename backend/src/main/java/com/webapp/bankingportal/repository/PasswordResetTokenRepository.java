
package com.webapp.bankingportal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.webapp.bankingportal.entity.PasswordResetToken;
import com.webapp.bankingportal.entity.User;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PasswordResetToken p WHERE p.token = :token")
    Optional<PasswordResetToken> findByTokenForUpdate(
            @Param("token") String token
    );

    PasswordResetToken findByUser(User user);

    void deleteByToken(String token);
}
