
package com.webapp.bankingportal.controller;

import java.util.Map;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.webapp.bankingportal.exception.AccountDoesNotExistException;
import com.webapp.bankingportal.exception.FundTransferException;
import com.webapp.bankingportal.exception.GeolocationException;
import com.webapp.bankingportal.exception.IdempotencyConflictException;
import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.InvalidOtpException;
import com.webapp.bankingportal.exception.InvalidPinException;
import com.webapp.bankingportal.exception.InvalidTokenException;
import com.webapp.bankingportal.exception.NotFoundException;
import com.webapp.bankingportal.exception.OtpRetryLimitExceededException;
import com.webapp.bankingportal.exception.PasswordResetException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.exception.UserInvalidException;
import com.webapp.bankingportal.util.ApiMessages;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountDoesNotExistException.class)
    public ResponseEntity<String> handleAccountDoesNotExistException(
            AccountDoesNotExistException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentialsException(
            BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ex.getMessage());
    }

    @ExceptionHandler(FundTransferException.class)
    public ResponseEntity<String> handleFundTransferException(
            FundTransferException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(GeolocationException.class)
    public ResponseEntity<String> handleGeolocationException(
            GeolocationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<String> handleIdempotencyConflictException(
            IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<String> handleInsufficientBalanceException(
            InsufficientBalanceException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<String> handleInvalidAmountException(
            InvalidAmountException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<String> handleInvalidOtpException(
            InvalidOtpException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ex.getMessage());
    }

    @ExceptionHandler(InvalidPinException.class)
    public ResponseEntity<String> handleInvalidPinException(
            InvalidPinException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<String> handleInvalidTokenException(
            InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ex.getMessage());
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<String> handleJsonProcessingException(
            JsonProcessingException ex) {
        log.error("JSON processing failed", ex);
        return ResponseEntity.internalServerError()
                .body("Unable to process the request.");
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(
            NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(OtpRetryLimitExceededException.class)
    public ResponseEntity<String> handleOtpRetryLimitExceededException(
            OtpRetryLimitExceededException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<Map<String, String>> handlePasswordResetException(
            PasswordResetException ex, WebRequest request) {
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "Unable to process the password reset request."));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<String> handleRestClientException(
            RestClientException ex) {
        log.error("External service request failed", ex);
        return ResponseEntity.internalServerError()
                .body("An external service is currently unavailable.");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> handleUnauthorizedException(
            UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ex.getMessage());
    }

    @ExceptionHandler(UserInvalidException.class)
    public ResponseEntity<String> handleUserInvalidException(
            UserInvalidException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<Map<String, String>> handleRedisConnectionFailure(
            RedisConnectionFailureException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "message",
                        ApiMessages.REDIS_CONNECTION_FAILURE.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(
            MethodArgumentNotValidException ex) {

        var fieldErrors = ex.getBindingResult().getFieldErrors();

        if (fieldErrors.isEmpty()) {
            return ResponseEntity.badRequest().body("Validation failed.");
        }

        var error = fieldErrors.get(0);

        String field = error.getField();
        String message;

        switch (field) {
            case "name":
                message = ApiMessages.USER_NAME_EMPTY_ERROR.getMessage();
                break;

            case "email":
                if ("Email".equals(error.getCode())) {
                    message = "Please provide a valid email address.";
                } else {
                    message = ApiMessages.USER_EMAIL_EMPTY_ERROR.getMessage();
                }
                break;

            case "countryCode":
                message = ApiMessages.USER_COUNTRY_CODE_EMPTY_ERROR.getMessage();
                break;

            case "phoneNumber":
                message = ApiMessages.USER_PHONE_NUMBER_EMPTY_ERROR.getMessage();
                break;

            case "address":
                message = ApiMessages.USER_ADDRESS_EMPTY_ERROR.getMessage();
                break;

            case "password":
                message = ApiMessages.PASSWORD_EMPTY_ERROR.getMessage();
                break;

            default:
                message = error.getDefaultMessage();
        }

        return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Unexpected application error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An unexpected error occurred."));
    }
}
