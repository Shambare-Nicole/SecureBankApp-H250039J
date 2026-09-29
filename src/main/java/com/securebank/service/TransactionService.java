package com.securebank.service;

import com.securebank.config.AppConfig;
import com.securebank.exception.InsufficientFundsException;
import com.securebank.exception.ValidationException;
import com.securebank.model.BankAccount;
import com.securebank.model.Transaction;
import com.securebank.storage.TransactionRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionService {
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final List<Transaction> transactions = new ArrayList<>();

    public TransactionService(AccountService accountService, TransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        transactions.addAll(transactionRepository.loadAll());
    }

    public void deposit(String username, String accountNumber, BigDecimal amount) {
        BankAccount bankAccount = accountService.getOwnedAccount(username, accountNumber);
        validateAmount(amount, "Deposit");
        bankAccount.deposit(amount);
        transactions.add(new Transaction(accountNumber, "DEPOSIT", amount, bankAccount.getBalance()));
        accountService.persistAccounts();
        transactionRepository.saveAll(transactions);
    }

    public void withdraw(String username, String accountNumber, BigDecimal amount) {
        BankAccount bankAccount = accountService.getOwnedAccount(username, accountNumber);
        validateAmount(amount, "Withdrawal");
        if (bankAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds.");
        }
        bankAccount.withdraw(amount);
        transactions.add(new Transaction(accountNumber, "WITHDRAW", amount, bankAccount.getBalance()));
        accountService.persistAccounts();
        transactionRepository.saveAll(transactions);
    }

    public List<Transaction> getTransactionsForAccount(String username, String accountNumber) {
        BankAccount bankAccount = accountService.getOwnedAccount(username, accountNumber);
        String normalizedNumber = bankAccount.getAccountNumber();
        List<Transaction> result = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getAccountNumber().equals(normalizedNumber)) {
                result.add(transaction);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private void validateAmount(BigDecimal amount, String operation) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException(operation + " amount must be greater than zero.");
        }
        if (amount.compareTo(AppConfig.MIN_TRANSACTION) < 0) {
            throw new ValidationException(operation + " amount is below the minimum allowed amount.");
        }
        if (amount.compareTo(AppConfig.MAX_TRANSACTION) > 0) {
            throw new ValidationException(operation + " amount exceeds the system limit.");
        }
    }
}