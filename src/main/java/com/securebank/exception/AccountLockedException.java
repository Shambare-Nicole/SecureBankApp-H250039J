package com.securebank.exception;

public class AccountLockedException extends BankException {
    private static final long serialVersionUID = 1L;

    public AccountLockedException(String message) {
        super(message);
    }
}