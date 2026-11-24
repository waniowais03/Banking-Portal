package com.webapp.bankingportal.exception;

public class AccountNumberCollisionException extends RuntimeException {

    public AccountNumberCollisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
