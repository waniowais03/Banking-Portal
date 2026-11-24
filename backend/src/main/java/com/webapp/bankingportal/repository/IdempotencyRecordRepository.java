package com.webapp.bankingportal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.webapp.bankingportal.entity.IdempotencyRecord;

@Repository
public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord>
    findByAccountNumberAndEndpointAndIdempotencyKey(
            String accountNumber,
            String endpoint,
            String idempotencyKey
    );
}
