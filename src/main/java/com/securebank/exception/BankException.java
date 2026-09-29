package com.securebank.exception;

public class BankException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BankException(String message) {
        super(message);
    }

    public BankException(String message, Throwable cause) {
        super(message, cause);
    }
}