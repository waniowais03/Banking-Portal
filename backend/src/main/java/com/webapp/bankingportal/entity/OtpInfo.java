
package com.webapp.bankingportal.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class OtpInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private int failedAttempts = 0;

    @Column(nullable = false)
    private int generationAttempts = 0;

    private LocalDateTime generationWindowStartedAt;

    private LocalDateTime generationBlockedUntil;

    @Column(nullable = false)
    private boolean consumed = false;

    public OtpInfo(String accountNumber, String otp, LocalDateTime generatedAt) {
        this.accountNumber = accountNumber;
        this.otp = otp;
        this.generatedAt = generatedAt;
        this.failedAttempts = 0;
        this.generationAttempts = 0;
        this.generationWindowStartedAt = generatedAt;
        this.generationBlockedUntil = null;
        this.consumed = false;
    }
}
