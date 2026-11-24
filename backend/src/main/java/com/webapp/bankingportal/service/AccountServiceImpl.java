
package com.webapp.bankingportal.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Date;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.Transaction;
import com.webapp.bankingportal.entity.TransactionType;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.exception.AccountNumberCollisionException;
import com.webapp.bankingportal.exception.FundTransferException;
import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.InvalidPinException;
import com.webapp.bankingportal.exception.NotFoundException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.repository.TransactionRepository;
import com.webapp.bankingportal.util.ApiMessages;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom ACCOUNT_NUMBER_RANDOM =
            new SecureRandom();

    private static final String ACCOUNT_NUMBER_CHARACTERS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 10;

    private static final String ACCOUNT_NUMBER_CONSTRAINT =
            "UK66gkcp94endmotfwb8r4ocxm9";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionRepository transactionRepository;
    private final OtpService otpService;
    private final EntityManager entityManager;

    @Override
    public Account createAccount(User user) {

        val account = new Account();

        account.setAccountNumber(generateUniqueAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);

        try {
            return accountRepository.saveAndFlush(account);

        } catch (DataIntegrityViolationException exception) {

            Throwable cause = exception;

            while (cause != null) {

                if (cause instanceof ConstraintViolationException violation
                        && ACCOUNT_NUMBER_CONSTRAINT.equalsIgnoreCase(
                                violation.getConstraintName())) {

                    throw new AccountNumberCollisionException(
                            "Account number collision detected",
                            exception
                    );
                }

                cause = cause.getCause();
            }

            throw exception;
        }
    }

    @Override
    public boolean isPinCreated(String accountNumber) {

        val account = accountRepository.findByAccountNumber(accountNumber);

        if (account == null) {
            throw new NotFoundException(
                    ApiMessages.ACCOUNT_NOT_FOUND.getMessage()
            );
        }

        return account.getPin() != null;
    }

    private String generateUniqueAccountNumber() {

        for (int attempt = 1;
             attempt <= MAX_ACCOUNT_NUMBER_ATTEMPTS;
             attempt++) {

            StringBuilder accountNumber = new StringBuilder(12);

            for (int i = 0; i < 12; i++) {

                int index = ACCOUNT_NUMBER_RANDOM.nextInt(
                        ACCOUNT_NUMBER_CHARACTERS.length()
                );

                accountNumber.append(
                        ACCOUNT_NUMBER_CHARACTERS.charAt(index)
                );
            }

            String candidate = accountNumber.toString();

            if (accountRepository.findByAccountNumber(candidate) == null) {
                return candidate;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique account number after "
                        + MAX_ACCOUNT_NUMBER_ATTEMPTS + " attempts"
        );
    }

    private void validatePin(Account account, String pin) {

        if (account.getPin() == null) {
            throw new UnauthorizedException(
                    ApiMessages.PIN_NOT_CREATED.getMessage()
            );
        }

        if (pin == null || pin.isEmpty()) {
            throw new UnauthorizedException(
                    ApiMessages.PIN_EMPTY_ERROR.getMessage()
            );
        }

        if (!passwordEncoder.matches(pin, account.getPin())) {
            throw new UnauthorizedException(
                    ApiMessages.PIN_INVALID_ERROR.getMessage()
            );
        }
    }

    private void validatePassword(
            String accountNumber,
            String password) {

        val account =
                accountRepository.findByAccountNumber(accountNumber);

        if (account == null) {
            throw new NotFoundException(
                    ApiMessages.ACCOUNT_NOT_FOUND.getMessage()
            );
        }

        if (password == null || password.isEmpty()) {
            throw new UnauthorizedException(
                    ApiMessages.PASSWORD_EMPTY_ERROR.getMessage()
            );
        }

        if (!passwordEncoder.matches(
                password,
                account.getUser().getPassword())) {

            throw new UnauthorizedException(
                    ApiMessages.PASSWORD_INVALID_ERROR.getMessage()
            );
        }
    }

    @Override
    public void createPin(
            String accountNumber,
            String password,
            String pin) {

        validatePassword(accountNumber, password);

        val account =
                accountRepository.findByAccountNumber(accountNumber);

        if (account.getPin() != null) {
            throw new UnauthorizedException(
                    ApiMessages.PIN_ALREADY_EXISTS.getMessage()
            );
        }

        if (pin == null || pin.isEmpty()) {
            throw new InvalidPinException(
                    ApiMessages.PIN_EMPTY_ERROR.getMessage()
            );
        }

        if (!pin.matches("[0-9]{4}")) {
            throw new InvalidPinException(
                    ApiMessages.PIN_FORMAT_INVALID_ERROR.getMessage()
            );
        }

        account.setPin(passwordEncoder.encode(pin));

        accountRepository.save(account);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePin(
            String accountNumber,
            String oldPin,
            String password,
            String newPin) {

        log.info("PIN update process initiated");

        validatePassword(accountNumber, password);

        Account account = getAccountForUpdate(accountNumber);

        validatePin(account, oldPin);

        if (newPin == null || newPin.isEmpty()) {
            throw new InvalidPinException(
                    ApiMessages.PIN_EMPTY_ERROR.getMessage()
            );
        }

        if (!newPin.matches("[0-9]{4}")) {
            throw new InvalidPinException(
                    ApiMessages.PIN_FORMAT_INVALID_ERROR.getMessage()
            );
        }

        account.setPin(passwordEncoder.encode(newPin));

        accountRepository.save(account);
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidAmountException(
                    ApiMessages.AMOUNT_NEGATIVE_ERROR.getMessage()
            );
        }

        if (amount.remainder(BigDecimal.valueOf(100))
                .compareTo(BigDecimal.ZERO) != 0) {

            throw new InvalidAmountException(
                    ApiMessages.AMOUNT_NOT_MULTIPLE_OF_100_ERROR.getMessage()
            );
        }

        if (amount.compareTo(BigDecimal.valueOf(100000)) > 0) {

            throw new InvalidAmountException(
                    ApiMessages.AMOUNT_EXCEED_100_000_ERROR.getMessage()
            );
        }
    }

    private Account getAccountForUpdate(String accountNumber) {

        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new NotFoundException(
                        ApiMessages.ACCOUNT_NOT_FOUND.getMessage()
                ));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cashDeposit(
            String accountNumber,
            String pin,
            BigDecimal amount) {

        Account account = getAccountForUpdate(accountNumber);

        validatePin(account, pin);
        validateAmount(amount);

        account.setBalance(
                account.getBalance().add(amount)
        );

        accountRepository.save(account);

        Transaction transaction = new Transaction();

        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.CASH_DEPOSIT);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(account);

        transactionRepository.save(transaction);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cashWithdrawal(
            String accountNumber,
            String pin,
            BigDecimal amount) {

        validateAmount(amount);

        Account account = getAccountForUpdate(accountNumber);

        validatePin(account, pin);

        BigDecimal currentBalance = account.getBalance();

        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    ApiMessages.BALANCE_INSUFFICIENT_ERROR.getMessage()
            );
        }

        account.setBalance(
                currentBalance.subtract(amount)
        );

        accountRepository.save(account);

        Transaction transaction = new Transaction();

        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.CASH_WITHDRAWAL);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(account);

        transactionRepository.save(transaction);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void fundTransfer(
            String sourceAccountNumber,
            String targetAccountNumber,
            String pin,
            BigDecimal amount) {

        // 1. Validate source account number.
        if (sourceAccountNumber == null ||
                sourceAccountNumber.isBlank()) {

            throw new NotFoundException(
                    ApiMessages.ACCOUNT_NOT_FOUND.getMessage()
            );
        }

        // 2. Validate target account number.
        if (targetAccountNumber == null ||
                targetAccountNumber.isBlank()) {

            throw new NotFoundException(
                    ApiMessages.ACCOUNT_NOT_FOUND.getMessage()
            );
        }

        // 3. Prevent transfer to the same account.
        if (sourceAccountNumber.equals(targetAccountNumber)) {

            throw new FundTransferException(
                    ApiMessages.CASH_TRANSFER_SAME_ACCOUNT_ERROR.getMessage()
            );
        }

        // 4. Validate transfer amount.
        validateAmount(amount);

        // 5. Lock both accounts in consistent alphabetical order.
        Account firstLockedAccount;
        Account secondLockedAccount;

        if (sourceAccountNumber.compareTo(targetAccountNumber) < 0) {

            firstLockedAccount =
                    getAccountForUpdate(sourceAccountNumber);

            secondLockedAccount =
                    getAccountForUpdate(targetAccountNumber);

        } else {

            firstLockedAccount =
                    getAccountForUpdate(targetAccountNumber);

            secondLockedAccount =
                    getAccountForUpdate(sourceAccountNumber);
        }

        // 6. Refresh both locked entities from the database.
        entityManager.refresh(
                firstLockedAccount,
                LockModeType.PESSIMISTIC_WRITE
        );

        entityManager.refresh(
                secondLockedAccount,
                LockModeType.PESSIMISTIC_WRITE
        );

        // 7. Identify source and target accounts.
        Account sourceAccount =
                sourceAccountNumber.compareTo(targetAccountNumber) < 0
                        ? firstLockedAccount
                        : secondLockedAccount;

        Account targetAccount =
                targetAccountNumber.compareTo(sourceAccountNumber) < 0
                        ? firstLockedAccount
                        : secondLockedAccount;

        // 8. Validate PIN using the locked source account.
        validatePin(sourceAccount, pin);

        // 9. Read source balance.
        BigDecimal sourceBalance = sourceAccount.getBalance();

        log.info(
                "Transfer validation initiated | Thread: {}",
                Thread.currentThread().getName()
        );

        // 10. Check sufficient balance.
        if (sourceBalance.compareTo(amount) < 0) {

            throw new InsufficientBalanceException(
                    ApiMessages.BALANCE_INSUFFICIENT_ERROR.getMessage()
            );
        }

        // 11. Calculate new balances.
        BigDecimal newSourceBalance =
                sourceBalance.subtract(amount);

        BigDecimal newTargetBalance =
                targetAccount.getBalance().add(amount);

        // 12. Update both accounts.
        sourceAccount.setBalance(newSourceBalance);
        targetAccount.setBalance(newTargetBalance);

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // 13. Create transaction record.
        Transaction transaction = new Transaction();

        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.CASH_TRANSFER);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(sourceAccount);
        transaction.setTargetAccount(targetAccount);

        transactionRepository.save(transaction);

        log.info("Transfer completed successfully");
    }
}
