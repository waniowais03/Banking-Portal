package com.webapp.bankingportal.service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.webapp.bankingportal.entity.IdempotencyRecord;
import com.webapp.bankingportal.exception.IdempotencyConflictException;
import com.webapp.bankingportal.repository.IdempotencyRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final PlatformTransactionManager transactionManager;

    @Value("${idempotency.hmac.secret}")
    private String hmacSecret;

    public ResponseEntity<String> execute(
            String accountNumber,
            String endpoint,
            String idempotencyKey,
            String canonicalPayload,
            Supplier<ResponseEntity<String>> operation) {

        validateKey(idempotencyKey);

        String requestHash = createRequestHash(
                accountNumber,
                endpoint,
                canonicalPayload
        );

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        try {
            return transactionTemplate.execute(status -> {

                Optional<IdempotencyRecord> existing =
                        repository.findByAccountNumberAndEndpointAndIdempotencyKey(
                                accountNumber,
                                endpoint,
                                idempotencyKey
                        );

                if (existing.isPresent()) {
                    return replayOrReject(existing.get(), requestHash);
                }

                IdempotencyRecord record = new IdempotencyRecord();
                record.setAccountNumber(accountNumber);
                record.setEndpoint(endpoint);
                record.setIdempotencyKey(idempotencyKey);
                record.setRequestHash(requestHash);

                // Claim the key before executing the banking operation.
                repository.saveAndFlush(record);

                ResponseEntity<String> response = operation.get();

                record.setResponseBody(response.getBody());
                record.setStatusCode(response.getStatusCode().value());

                repository.save(record);

                return response;
            });

        } catch (DataIntegrityViolationException exception) {

            // A concurrent request may have claimed the same key.
            Optional<IdempotencyRecord> existing =
                    repository.findByAccountNumberAndEndpointAndIdempotencyKey(
                            accountNumber,
                            endpoint,
                            idempotencyKey
                    );

            if (existing.isPresent()) {
                return replayOrReject(existing.get(), requestHash);
            }

            throw exception;
        }
    }

    private ResponseEntity<String> replayOrReject(
            IdempotencyRecord record,
            String requestHash) {

        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency key already used with a different request."
            );
        }

        if (record.getResponseBody() == null ||
                record.getStatusCode() == null) {
            throw new IdempotencyConflictException(
                    "Previous request has no completed response."
            );
        }

        return ResponseEntity
                .status(record.getStatusCode())
                .body(record.getResponseBody());
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid Idempotency-Key header is required."
            );
        }
    }

    private String createRequestHash(
            String accountNumber,
            String endpoint,
            String payload) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(secretKey);

            String canonicalData =
                    accountNumber + "\n" +
                    endpoint + "\n" +
                    payload;

            byte[] hash = mac.doFinal(
                    canonicalData.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder();

            for (byte value : hash) {
                hex.append(String.format("%02x", value & 0xff));
            }

            return hex.toString();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to generate request fingerprint.",
                    exception
            );
        }
    }
}
