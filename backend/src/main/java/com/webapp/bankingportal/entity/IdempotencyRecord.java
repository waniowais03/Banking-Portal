package com.webapp.bankingportal.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Entity
@Table(
    name = "idempotency_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_account_endpoint_key",
        columnNames = {"account_number", "endpoint", "idempotency_key"}
    )
)
@Data
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "endpoint", nullable = false, length = 100)
    private String endpoint;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_body", length = 2000)
    private String responseBody;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
