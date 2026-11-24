
package com.webapp.bankingportal;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.entity.TransactionType;
import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.repository.IdempotencyRecordRepository;
import com.webapp.bankingportal.repository.TransactionRepository;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.service.AccountService;
import com.webapp.bankingportal.service.IdempotencyService;

@SpringBootTest
@ActiveProfiles("mysql-test")
class MySQLTransactionIntegrationTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Test
    void concurrentWithdrawalsMustNotOverdrawAccount() throws Exception {

        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setName("MySQL Concurrency Test");
        user.setPassword(passwordEncoder.encode("TestPassword123!"));
        user.setEmail("mysql-" + unique + "@example.com");
        user.setCountryCode("IN");
        user.setPhoneNumber("+91" + System.nanoTime());
        user.setAddress("MySQL Test Address");

        userRepository.saveAndFlush(user);

        Account account = accountService.createAccount(user);

        String accountNumber = account.getAccountNumber();
        String pin = "1234";

        accountService.createPin(
                accountNumber,
                "TestPassword123!",
                pin
        );

        accountService.cashDeposit(
                accountNumber,
                pin,
                new BigDecimal("1000.00")
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            java.util.concurrent.Callable<Boolean> withdrawal = () -> {

                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start signal timeout");
                }

                try {
                    accountService.cashWithdrawal(
                            accountNumber,
                            pin,
                            new BigDecimal("700.00")
                    );

                    return true;

                } catch (InsufficientBalanceException exception) {
                    return false;
                }
            };

            Future<Boolean> first = executor.submit(withdrawal);
            Future<Boolean> second = executor.submit(withdrawal);

            assertTrue(ready.await(10, TimeUnit.SECONDS));

            start.countDown();

            boolean firstResult = first.get(30, TimeUnit.SECONDS);
            boolean secondResult = second.get(30, TimeUnit.SECONDS);

            assertNotEquals(
                    firstResult,
                    secondResult,
                    "Exactly one withdrawal must succeed"
            );

            Account savedAccount =
                    accountRepository.findByAccountNumber(accountNumber);

            assertNotNull(savedAccount);

            assertEquals(
                    0,
                    new BigDecimal("300.00")
                            .compareTo(savedAccount.getBalance()),
                    "Account must not be overdrawn"
            );

            var transactions =
                    transactionRepository
                            .findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(
                                    accountNumber,
                                    accountNumber
                            );

            assertEquals(
                    2,
                    transactions.size(),
                    "Only deposit and one successful withdrawal should exist"
            );

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentTransfersMustNotOverdrawSourceAccount() throws Exception {

        Account sourceAccount = createTestAccount("Transfer Source");
        Account targetAccountA = createTestAccount("Transfer Target A");
        Account targetAccountB = createTestAccount("Transfer Target B");

        String sourceNumber = sourceAccount.getAccountNumber();
        String targetNumberA = targetAccountA.getAccountNumber();
        String targetNumberB = targetAccountB.getAccountNumber();

        String pin = "1234";

        accountService.createPin(
                sourceNumber,
                "TestPassword123!",
                pin
        );

        accountService.cashDeposit(
                sourceNumber,
                pin,
                new BigDecimal("1000.00")
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            java.util.concurrent.Callable<Boolean> transferA = () -> {

                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start signal timeout");
                }

                try {
                    accountService.fundTransfer(
                            sourceNumber,
                            targetNumberA,
                            pin,
                            new BigDecimal("700.00")
                    );

                    return true;

                } catch (InsufficientBalanceException exception) {
                    return false;
                }
            };

            java.util.concurrent.Callable<Boolean> transferB = () -> {

                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start signal timeout");
                }

                try {
                    accountService.fundTransfer(
                            sourceNumber,
                            targetNumberB,
                            pin,
                            new BigDecimal("700.00")
                    );

                    return true;

                } catch (InsufficientBalanceException exception) {
                    return false;
                }
            };

            Future<Boolean> first = executor.submit(transferA);
            Future<Boolean> second = executor.submit(transferB);

            assertTrue(ready.await(10, TimeUnit.SECONDS));

            start.countDown();

            boolean firstResult = first.get(30, TimeUnit.SECONDS);
            boolean secondResult = second.get(30, TimeUnit.SECONDS);

            Account savedSource =
                    accountRepository.findByAccountNumber(sourceNumber);

            Account savedTargetA =
                    accountRepository.findByAccountNumber(targetNumberA);

            Account savedTargetB =
                    accountRepository.findByAccountNumber(targetNumberB);

            assertNotNull(savedSource);
            assertNotNull(savedTargetA);
            assertNotNull(savedTargetB);

            assertNotEquals(
                    firstResult,
                    secondResult,
                    "Exactly one transfer must succeed"
            );

            assertEquals(
                    0,
                    new BigDecimal("300.00")
                            .compareTo(savedSource.getBalance()),
                    "Source account must retain exactly 300.00"
            );

            boolean targetAReceivedTransfer =
                    savedTargetA.getBalance()
                            .compareTo(new BigDecimal("700.00")) == 0;

            boolean targetBReceivedTransfer =
                    savedTargetB.getBalance()
                            .compareTo(new BigDecimal("700.00")) == 0;

            assertNotEquals(
                    targetAReceivedTransfer,
                    targetBReceivedTransfer,
                    "Exactly one target account must receive the transfer"
            );

            var transactions =
                    transactionRepository
                            .findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(
                                    sourceNumber,
                                    sourceNumber
                            );

            long transferCount = transactions.stream()
                    .filter(transaction ->
                            transaction.getTransactionType()
                                    == TransactionType.CASH_TRANSFER)
                    .count();

            assertEquals(
                    1,
                    transferCount,
                    "Exactly one successful transfer record must exist"
            );

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedTransactionInsertMustRollbackWithdrawal() {

        Account account = createTestAccount("Rollback Test");

        String accountNumber = account.getAccountNumber();
        String pin = "1234";

        accountService.createPin(
                accountNumber,
                "TestPassword123!",
                pin
        );

        accountService.cashDeposit(
                accountNumber,
                pin,
                new BigDecimal("1000.00")
        );

        long transactionCountBefore =
                transactionRepository
                        .findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(
                                accountNumber,
                                accountNumber
                        )
                        .size();

        jdbcTemplate.execute(
                "DROP TRIGGER IF EXISTS fail_transaction_insert"
        );

        try {
            jdbcTemplate.execute("""
                    CREATE TRIGGER fail_transaction_insert
                    BEFORE INSERT ON `transaction`
                    FOR EACH ROW
                    SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Simulated database failure'
                    """);

            assertThrows(
                    RuntimeException.class,
                    () -> accountService.cashWithdrawal(
                            accountNumber,
                            pin,
                            new BigDecimal("200.00")
                    ),
                    "Withdrawal must fail when transaction insertion fails"
            );

        } finally {
            jdbcTemplate.execute(
                    "DROP TRIGGER IF EXISTS fail_transaction_insert"
            );
        }

        Account savedAccount =
                accountRepository.findByAccountNumber(accountNumber);

        assertNotNull(savedAccount);

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(savedAccount.getBalance()),
                "Account balance must remain unchanged after rollback"
        );

        long transactionCountAfter =
                transactionRepository
                        .findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(
                                accountNumber,
                                accountNumber
                        )
                        .size();

        assertEquals(
                transactionCountBefore,
                transactionCountAfter,
                "No transaction record should be inserted after failure"
        );
    }

    @Test
    void concurrentDuplicateIdempotencyRequestsMustExecuteOnlyOnce()
            throws Exception {

        String accountNumber = "MYSQL-" + UUID.randomUUID();
        String endpoint = "/api/account/deposit";
        String idempotencyKey = UUID.randomUUID().toString();
        String payload = "amount=500.00";

        AtomicInteger executionCount = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            java.util.concurrent.Callable<ResponseEntity<String>> request =
                    () -> {

                        ready.countDown();

                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "Start signal timeout"
                            );
                        }

                        return idempotencyService.execute(
                                accountNumber,
                                endpoint,
                                idempotencyKey,
                                payload,
                                () -> {
                                    executionCount.incrementAndGet();

                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException exception) {
                                        Thread.currentThread().interrupt();
                                        throw new IllegalStateException(exception);
                                    }

                                    return ResponseEntity.ok(
                                            "Deposit successful"
                                    );
                                }
                        );
                    };

            Future<ResponseEntity<String>> first =
                    executor.submit(request);

            Future<ResponseEntity<String>> second =
                    executor.submit(request);

            assertTrue(ready.await(10, TimeUnit.SECONDS));

            start.countDown();

            ResponseEntity<String> firstResponse =
                    first.get(30, TimeUnit.SECONDS);

            ResponseEntity<String> secondResponse =
                    second.get(30, TimeUnit.SECONDS);

            assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
            assertEquals(HttpStatus.OK, secondResponse.getStatusCode());

            assertEquals(
                    "Deposit successful",
                    firstResponse.getBody()
            );

            assertEquals(
                    "Deposit successful",
                    secondResponse.getBody()
            );

            assertEquals(
                    1,
                    executionCount.get(),
                    "Concurrent duplicate requests must execute only once"
            );

            var savedRecord =
                    idempotencyRecordRepository
                            .findByAccountNumberAndEndpointAndIdempotencyKey(
                                    accountNumber,
                                    endpoint,
                                    idempotencyKey
                            );

            assertTrue(
                    savedRecord.isPresent(),
                    "Idempotency record must be stored in MySQL"
            );

            assertEquals(
                    "Deposit successful",
                    savedRecord.get().getResponseBody()
            );

            assertEquals(
                    HttpStatus.OK.value(),
                    savedRecord.get().getStatusCode()
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Account createTestAccount(String label) {

        String unique = UUID.randomUUID().toString();

        User user = new User();
        user.setName(label);
        user.setPassword(passwordEncoder.encode("TestPassword123!"));
        user.setEmail("mysql-" + unique + "@example.com");
        user.setCountryCode("IN");
        user.setPhoneNumber("+91" + System.nanoTime());
        user.setAddress("MySQL Test Address");

        userRepository.saveAndFlush(user);

        return accountService.createAccount(user);
    }
}
