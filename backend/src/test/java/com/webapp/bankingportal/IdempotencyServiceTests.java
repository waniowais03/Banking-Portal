
package com.webapp.bankingportal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import com.webapp.bankingportal.entity.IdempotencyRecord;
import com.webapp.bankingportal.exception.IdempotencyConflictException;
import com.webapp.bankingportal.repository.IdempotencyRecordRepository;
import com.webapp.bankingportal.service.IdempotencyService;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class IdempotencyServiceTests {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRecordRepository repository;

    private final String account = "TEST123";
    private final String endpoint = "/api/account/deposit";
    private final String key = "test-key-001";
    private final String payload = "amount=100";

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void sameKeyAndSameRequestShouldReturnCachedResponse() {

        AtomicInteger executionCount = new AtomicInteger();

        var firstResponse = idempotencyService.execute(
                account, endpoint, key, payload,
                () -> {
                    executionCount.incrementAndGet();
                    return ResponseEntity.ok("Deposit successful");
                }
        );

        var secondResponse = idempotencyService.execute(
                account, endpoint, key, payload,
                () -> {
                    executionCount.incrementAndGet();
                    return ResponseEntity.ok("Deposit successful");
                }
        );

        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        assertEquals("Deposit successful", secondResponse.getBody());
        assertEquals(1, executionCount.get());
    }

    @Test
    void sameKeyAndDifferentRequestShouldReturnConflict() {

        idempotencyService.execute(
                account, endpoint, key, payload,
                () -> ResponseEntity.ok("Deposit successful")
        );

        assertThrows(
                IdempotencyConflictException.class,
                () -> idempotencyService.execute(
                        account, endpoint, key, "amount=200",
                        () -> ResponseEntity.ok("Another deposit")
                )
        );
    }

    @Test
    void differentKeysShouldExecuteSeparately() {

        AtomicInteger executionCount = new AtomicInteger();

        idempotencyService.execute(
                account, endpoint, "key-one", payload,
                () -> {
                    executionCount.incrementAndGet();
                    return ResponseEntity.ok("First");
                }
        );

        idempotencyService.execute(
                account, endpoint, "key-two", payload,
                () -> {
                    executionCount.incrementAndGet();
                    return ResponseEntity.ok("Second");
                }
        );

        assertEquals(2, executionCount.get());
    }

    @Test
    void failedOperationShouldNotPersistIdempotencyRecord() {

        assertThrows(
                RuntimeException.class,
                () -> idempotencyService.execute(
                        account, endpoint, key, payload,
                        () -> {
                            throw new RuntimeException("Simulated failure");
                        }
                )
        );

        Optional<IdempotencyRecord> record =
                repository.findByAccountNumberAndEndpointAndIdempotencyKey(
                        account, endpoint, key
                );

        assertTrue(record.isEmpty());
    }

    @Test
    void concurrentDuplicateRequestsShouldExecuteOperationOnlyOnce()
            throws Exception {

        AtomicInteger executionCount = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<ResponseEntity<String>> request = () -> {

                ready.countDown();
                start.await();

                return idempotencyService.execute(
                        account, endpoint, "concurrent-key", payload,
                        () -> {
                            executionCount.incrementAndGet();

                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(e);
                            }

                            return ResponseEntity.ok("Deposit successful");
                        }
                );
            };

            Future<ResponseEntity<String>> first =
                    executor.submit(request);

            Future<ResponseEntity<String>> second =
                    executor.submit(request);

            assertTrue(ready.await(5, TimeUnit.SECONDS));

            start.countDown();

            ResponseEntity<String> firstResponse =
                    first.get(15, TimeUnit.SECONDS);

            ResponseEntity<String> secondResponse =
                    second.get(15, TimeUnit.SECONDS);

            assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
            assertEquals(HttpStatus.OK, secondResponse.getStatusCode());

            assertEquals("Deposit successful", firstResponse.getBody());
            assertEquals("Deposit successful", secondResponse.getBody());

            assertEquals(
                    1,
                    executionCount.get(),
                    "Duplicate requests must execute the operation only once"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
