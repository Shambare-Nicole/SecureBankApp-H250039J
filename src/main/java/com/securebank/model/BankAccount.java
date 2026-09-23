package com.securebank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BankAccount {
    private final String username;
    private final String accountNumber;
    private final String accountName;
    private BigDecimal balance;

    public BankAccount(String username, String accountNumber, String accountName, BigDecimal balance) {
        this.username = username;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.balance = balance.setScale(2, RoundingMode.HALF_EVEN);
    }

    public String getUsername() {
        return username;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public BigDecimal getBalance() {
        return balance.setScale(2, RoundingMode.HALF_EVEN);
    }

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }
        balance = balance.add(amount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds.");
        }
        balance = balance.subtract(amount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public String toFileString() {
        return username + "|" + accountNumber + "|" + accountName + "|" + balance.toPlainString();
    }

    public static BankAccount fromFileString(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid account record: " + line);
        }
        return new BankAccount(parts[0], parts[1], parts[2], new BigDecimal(parts[3]));
    }
}
