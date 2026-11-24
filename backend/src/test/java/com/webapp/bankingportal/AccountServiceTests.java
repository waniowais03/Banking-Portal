package com.webapp.bankingportal;

import java.math.BigDecimal;

import jakarta.validation.ConstraintViolationException;
import lombok.val;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.InvalidPinException;
import com.webapp.bankingportal.exception.NotFoundException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.repository.AccountRepository;

public class AccountServiceTests extends BaseTest {

    @Autowired
    AccountRepository accountRepository;

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private void assertBalance(BigDecimal expected, BigDecimal actual) {
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void test_create_account_with_valid_user() {
        val user = createUser();
        userRepository.save(user);

        val account = accountService.createAccount(user);

        Assertions.assertNotNull(account);
        Assertions.assertNotNull(account.getAccountNumber());
        Assertions.assertTrue(
                account.getAccountNumber().matches("[A-Z0-9]{12}"),
                "Account number must contain exactly 12 uppercase alphanumeric characters"
        );
        Assertions.assertEquals(user, account.getUser());
        assertBalance(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    public void test_create_account_with_null_user() {
        Assertions.assertThrows(
                ConstraintViolationException.class,
                () -> accountService.createAccount(null)
        );
    }

    @Test
    public void test_create_pin_with_valid_details() {
        val accountDetails = createAccount();
        val pin = getRandomPin();

        accountService.createPin(
                accountDetails.get("accountNumber"),
                accountDetails.get("password"),
                pin
        );

        val account = accountRepository.findByAccountNumber(
                accountDetails.get("accountNumber")
        );

        Assertions.assertTrue(passwordEncoder.matches(pin, account.getPin()));
    }

    @Test
    public void test_create_pin_with_invalid_account_number() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.createPin(
                    getRandomAccountNumber(),
                    getRandomPassword(),
                    getRandomPin()
            );
        });
    }

    @Test
    public void test_create_pin_with_invalid_password() {
        val accountNumber = createAccount().get("accountNumber");

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.createPin(
                    accountNumber,
                    getRandomPassword(),
                    getRandomPin()
            );
        });
    }

    @Test
    public void test_create_pin_with_existing_pin() {
        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.createPin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("password"),
                    getRandomPin()
            );
        });
    }

    @Test
    public void test_create_pin_with_missing_or_empty_pin() {
        val accountDetails = createAccount();

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("password"),
                    null
            );
        });

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("password"),
                    ""
            );
        });
    }

    @Test
    public void test_create_pin_with_invalid_format() {
        val accountDetails = createAccount();

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("password"),
                    faker.number().digits(3)
            );
        });

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("password"),
                    faker.number().digits(5)
            );
        });

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("password"),
                    getRandomPassword().substring(0, 4)
            );
        });
    }

    @Test
    public void test_update_pin_with_valid_details() {
        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        val newPin = getRandomPin();

        accountService.updatePin(
                accountDetails.get("accountNumber"),
                accountDetails.get("pin"),
                accountDetails.get("password"),
                newPin
        );

        val account = accountRepository.findByAccountNumber(
                accountDetails.get("accountNumber")
        );

        Assertions.assertTrue(passwordEncoder.matches(newPin, account.getPin()));
    }

    @Test
    public void test_update_pin_with_invalid_account_number() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.updatePin(
                    getRandomAccountNumber(),
                    getRandomPin(),
                    getRandomPassword(),
                    getRandomPin()
            );
        });
    }

    @Test
    public void test_update_pin_with_incorrect_password() {
        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    getRandomPassword(),
                    getRandomPin()
            );
        });
    }

    @Test
    public void test_update_pin_with_missing_or_empty_password() {
        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    null,
                    getRandomPin()
            );
        });

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    "",
                    getRandomPin()
            );
        });
    }

    @Test
    public void test_update_pin_with_incorrect_pin() {
        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(
                    accountDetails.get("accountNumber"),
                    getRandomPin(),
                    accountDetails.get("password"),
                    getRandomPin()
            );
        });
    }

    @Test
    public void test_update_pin_for_account_with_no_pin() {
        val accountDetails = createAccount();

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(
                    accountDetails.get("accountNumber"),
                    getRandomPin(),
                    accountDetails.get("password"),
                    getRandomPin()
            );
        });
    }

    @Test
    public void test_update_pin_with_missing_or_empty_old_pin() {
        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(
                    accountDetails.get("accountNumber"),
                    null,
                    accountDetails.get("password"),
                    getRandomPin()
            );
        });

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(
                    accountDetails.get("accountNumber"),
                    "",
                    accountDetails.get("password"),
                    getRandomPin()
            );
        });
    }

    @Test
    public void test_update_pin_with_missing_or_empty_new_pin() {
        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.updatePin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    accountDetails.get("password"),
                    null
            );
        });

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.updatePin(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    accountDetails.get("password"),
                    ""
            );
        });
    }

    @Test
    public void test_deposit_cash_with_valid_details() {
        BigDecimal balance = amount("1000.00");

        val accountDetails = createAccountWithInitialBalance(balance);

        val account = accountRepository.findByAccountNumber(
                accountDetails.get("accountNumber")
        );

        assertBalance(balance, account.getBalance());
    }

    @Test
    public void test_deposit_cash_with_invalid_account_number() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.cashDeposit(
                    getRandomAccountNumber(),
                    getRandomPin(),
                    amount("50.00")
            );
        });
    }

    @Test
    public void test_deposit_cash_with_invalid_pin() {
        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.cashDeposit(
                    accountDetails.get("accountNumber"),
                    getRandomPin(),
                    amount("50.00")
            );
        });
    }

    @Test
    public void test_deposit_invalid_amount() {
        val accountDetails = createAccountWithPin(
                passwordEncoder,
                userRepository,
                accountService
        );

        Assertions.assertThrows(InvalidAmountException.class, () -> {
            accountService.cashDeposit(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    amount("-50.00")
            );
        });

        Assertions.assertThrows(InvalidAmountException.class, () -> {
            accountService.cashDeposit(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    amount("0.00")
            );
        });

        Assertions.assertThrows(InvalidAmountException.class, () -> {
            accountService.cashDeposit(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    amount("50.00")
            );
        });

        Assertions.assertThrows(InvalidAmountException.class, () -> {
            accountService.cashDeposit(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    amount("100001.00")
            );
        });
    }

    @Test
    public void test_withdraw_cash_with_valid_details() {
        BigDecimal balance = amount("1000.00");
        val accountDetails = createAccountWithInitialBalance(balance);

        BigDecimal withdrawalAmount = amount("500.00");

        accountService.cashWithdrawal(
                accountDetails.get("accountNumber"),
                accountDetails.get("pin"),
                withdrawalAmount
        );

        val account = accountRepository.findByAccountNumber(
                accountDetails.get("accountNumber")
        );

        assertBalance(
                balance.subtract(withdrawalAmount),
                account.getBalance()
        );
    }

    @Test
    public void test_withdraw_insufficient_balance() {
        val accountDetails = createAccountWithInitialBalance(
                amount("500.00")
        );

        Assertions.assertThrows(InsufficientBalanceException.class, () -> {
            accountService.cashWithdrawal(
                    accountDetails.get("accountNumber"),
                    accountDetails.get("pin"),
                    amount("1000.00")
            );
        });
    }

    @Test
    public void test_transfer_funds_with_valid_accounts() {
        BigDecimal sourceAccountBalance = amount("1000.00");
        val sourceAccountDetails = createAccountWithInitialBalance(
                sourceAccountBalance
        );

        BigDecimal targetAccountBalance = amount("500.00");
        val targetAccountDetails = createAccountWithInitialBalance(
                targetAccountBalance
        );

        BigDecimal transferAmount = amount("200.00");

        accountService.fundTransfer(
                sourceAccountDetails.get("accountNumber"),
                targetAccountDetails.get("accountNumber"),
                sourceAccountDetails.get("pin"),
                transferAmount
        );

        val sourceAccount = accountRepository.findByAccountNumber(
                sourceAccountDetails.get("accountNumber")
        );

        val targetAccount = accountRepository.findByAccountNumber(
                targetAccountDetails.get("accountNumber")
        );

        assertBalance(
                sourceAccountBalance.subtract(transferAmount),
                sourceAccount.getBalance()
        );

        assertBalance(
                targetAccountBalance.add(transferAmount),
                targetAccount.getBalance()
        );
    }

    @Test
    public void test_transfer_non_existent_target_account() {
        val accountDetails = createAccountWithInitialBalance(
                amount("500.00")
        );

        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.fundTransfer(
                    accountDetails.get("accountNumber"),
                    getRandomAccountNumber(),
                    accountDetails.get("pin"),
                    amount("1000.00")
            );
        });
    }

    @Test
    public void test_transfer_funds_insufficient_balance() {
        val sourceAccountDetails = createAccountWithInitialBalance(
                amount("500.00")
        );

        val targetAccountDetails = createAccount();

        Assertions.assertThrows(InsufficientBalanceException.class, () -> {
            accountService.fundTransfer(
                    sourceAccountDetails.get("accountNumber"),
                    targetAccountDetails.get("accountNumber"),
                    sourceAccountDetails.get("pin"),
                    amount("1000.00")
            );
        });
    }

    @Test
    public void test_generate_unique_account_numbers_for_multiple_accounts() {
        java.util.Set<String> accountNumbers = new java.util.HashSet<>();

        for (int i = 0; i < 100; i++) {
            val user = createUser();
            user.setCountryCode("US");
            user.setPhoneNumber("+1555000" + String.format("%04d", i));
            user.setEmail("account-number-test-" + i + "@example.com");
            userRepository.save(user);

            val account = accountService.createAccount(user);
            String accountNumber = account.getAccountNumber();

            Assertions.assertTrue(
                    accountNumber.matches("[A-Z0-9]{12}")
            );
            Assertions.assertTrue(
                    accountNumbers.add(accountNumber),
                    "Duplicate account number generated: " + accountNumber
            );
        }

        Assertions.assertEquals(100, accountNumbers.size());
    }

}