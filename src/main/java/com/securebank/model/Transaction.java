package com.securebank.model;

import com.securebank.exception.StorageException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String accountNumber;
    private final String type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String timestamp;

    public Transaction(String accountNumber, String type, BigDecimal amount, BigDecimal balanceAfter) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = LocalDateTime.now().format(FORMATTER);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String toFileString() {
        return timestamp + "|" + accountNumber + "|" + type + "|" + amount.toPlainString() + "|"
                + balanceAfter.toPlainString();
    }

    public static Transaction fromFileString(String line) {
        String[] parts = line.split("\\|", 5);
        if (parts.length != 5) {
            throw new StorageException("Invalid transaction record.");
        }
        try {
            return new Transaction(parts[1], parts[2], new BigDecimal(parts[3]), new BigDecimal(parts[4]));
        } catch (NumberFormatException e) {
            throw new StorageException("Invalid transaction record.", e);
        }
    }
}
