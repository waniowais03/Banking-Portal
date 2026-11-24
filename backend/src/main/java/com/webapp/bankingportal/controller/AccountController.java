
package com.webapp.bankingportal.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.webapp.bankingportal.dto.AmountRequest;
import com.webapp.bankingportal.dto.FundTransferRequest;
import com.webapp.bankingportal.dto.PinRequest;
import com.webapp.bankingportal.dto.PinUpdateRequest;
import com.webapp.bankingportal.dto.OtpRequest;
import com.webapp.bankingportal.service.AccountService;
import com.webapp.bankingportal.service.TransactionService;
import com.webapp.bankingportal.service.OtpService;
import com.webapp.bankingportal.service.UserService;
import com.webapp.bankingportal.service.IdempotencyService;
import com.webapp.bankingportal.util.ApiMessages;
import com.webapp.bankingportal.util.JsonUtil;
import com.webapp.bankingportal.util.LoggedinUser;

import lombok.RequiredArgsConstructor;
import lombok.val;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final OtpService otpService;
    private final UserService userService;
    private final IdempotencyService idempotencyService;

    @GetMapping("/pin/check")
    public ResponseEntity<String> checkAccountPIN() {
        val isPINValid = accountService.isPinCreated(
                LoggedinUser.getAccountNumber());

        val response = isPINValid
                ? ApiMessages.PIN_CREATED.getMessage()
                : ApiMessages.PIN_NOT_CREATED.getMessage();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/pin/create")
    public ResponseEntity<String> createPIN(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PinRequest pinRequest) {

        String accountNumber = LoggedinUser.getAccountNumber();

        String payload = canonicalPayload(
                pinRequest.password(),
                pinRequest.pin()
        );

        return idempotencyService.execute(
                accountNumber,
                "/api/account/pin/create",
                idempotencyKey,
                payload,
                () -> {
                    accountService.createPin(
                            accountNumber,
                            pinRequest.password(),
                            pinRequest.pin()
                    );

                    return ResponseEntity.ok(
                            ApiMessages.PIN_CREATION_SUCCESS.getMessage());
                }
        );
    }

    @PostMapping("/pin/update")
    public ResponseEntity<String> updatePIN(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PinUpdateRequest pinUpdateRequest) {

        String accountNumber = LoggedinUser.getAccountNumber();

        String payload = canonicalPayload(
                pinUpdateRequest.oldPin(),
                pinUpdateRequest.newPin(),
                pinUpdateRequest.password()
        );

        return idempotencyService.execute(
                accountNumber,
                "/api/account/pin/update",
                idempotencyKey,
                payload,
                () -> {
                    accountService.updatePin(
                            accountNumber,
                            pinUpdateRequest.oldPin(),
                            pinUpdateRequest.password(),
                            pinUpdateRequest.newPin()
                    );

                    return ResponseEntity.ok(
                            ApiMessages.PIN_UPDATE_SUCCESS.getMessage());
                }
        );
    }

    @PostMapping("/pin/forgot/send-otp")
    public ResponseEntity<String> sendOtpForPinReset(
            @RequestBody OtpRequest otpRequest) {

        val user = userService.getUserByIdentifier(
                otpRequest.identifier());

        val accountNumber = user.getAccount().getAccountNumber();

        val generatedOtp = otpService.generateOTP(accountNumber);

        val emailFuture = otpService.sendOTPByEmail(
                user.getEmail(),
                user.getName(),
                accountNumber,
                generatedOtp
        );

        emailFuture.join();

        return ResponseEntity.ok(
                String.format(
                        "OTP sent successfully to %s",
                        user.getEmail()
                )
        );
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> cashDeposit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody AmountRequest amountRequest) {

        String accountNumber = LoggedinUser.getAccountNumber();

        String payload = canonicalPayload(
                amountRequest.pin(),
                normalizeAmount(amountRequest.amount())
        );

        return idempotencyService.execute(
                accountNumber,
                "/api/account/deposit",
                idempotencyKey,
                payload,
                () -> {
                    accountService.cashDeposit(
                            accountNumber,
                            amountRequest.pin(),
                            amountRequest.amount()
                    );

                    return ResponseEntity.ok(
                            ApiMessages.CASH_DEPOSIT_SUCCESS.getMessage());
                }
        );
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> cashWithdrawal(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody AmountRequest amountRequest) {

        String accountNumber = LoggedinUser.getAccountNumber();

        String payload = canonicalPayload(
                amountRequest.pin(),
                normalizeAmount(amountRequest.amount())
        );

        return idempotencyService.execute(
                accountNumber,
                "/api/account/withdraw",
                idempotencyKey,
                payload,
                () -> {
                    accountService.cashWithdrawal(
                            accountNumber,
                            amountRequest.pin(),
                            amountRequest.amount()
                    );

                    return ResponseEntity.ok(
                            ApiMessages.CASH_WITHDRAWAL_SUCCESS.getMessage());
                }
        );
    }

    @PostMapping("/fund-transfer")
    public ResponseEntity<String> fundTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody FundTransferRequest fundTransferRequest) {

        String accountNumber = LoggedinUser.getAccountNumber();

        String payload = canonicalPayload(
                fundTransferRequest.targetAccountNumber(),
                fundTransferRequest.pin(),
                normalizeAmount(fundTransferRequest.amount())
        );

        return idempotencyService.execute(
                accountNumber,
                "/api/account/fund-transfer",
                idempotencyKey,
                payload,
                () -> {
                    accountService.fundTransfer(
                            accountNumber,
                            fundTransferRequest.targetAccountNumber(),
                            fundTransferRequest.pin(),
                            fundTransferRequest.amount()
                    );

                    return ResponseEntity.ok(
                            ApiMessages.CASH_TRANSFER_SUCCESS.getMessage());
                }
        );
    }

    @GetMapping("/transactions")
    public ResponseEntity<String> getAllTransactionsByAccountNumber() {

        val transactions = transactionService
                .getAllTransactionsByAccountNumber(
                        LoggedinUser.getAccountNumber());

        return ResponseEntity.ok(JsonUtil.toJson(transactions));
    }

    @GetMapping("/send-statement")
    public ResponseEntity<String> sendBankStatement() {

        String accountNumber = LoggedinUser.getAccountNumber();

        transactionService.sendBankStatementByEmail(accountNumber);

        return ResponseEntity.ok(
                "{\"message\": \"Bank statement sent to your email.\"}");
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount == null
                ? null
                : amount.stripTrailingZeros().toPlainString();
    }

    private String canonicalPayload(String... values) {

        StringBuilder payload = new StringBuilder();

        for (String value : values) {

            if (value == null) {
                payload.append("-1:");
            } else {
                payload.append(value.length())
                        .append(":")
                        .append(value);
            }
        }

        return payload.toString();
    }
}
