package com.securebank.service;

import com.securebank.config.AppConfig;
import com.securebank.exception.UnauthorizedAccessException;
import com.securebank.exception.ValidationException;
import com.securebank.model.BankAccount;
import com.securebank.model.User;
import com.securebank.storage.AccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AccountService {
    private final AuthenticationService authenticationService;
    private final AccountRepository accountRepository;
    private final Map<String, BankAccount> accounts = new HashMap<>();
    private final Random random = new SecureRandom();

    public AccountService(AuthenticationService authenticationService, AccountRepository accountRepository) {
        this.authenticationService = authenticationService;
        this.accountRepository = accountRepository;
        accounts.putAll(accountRepository.loadAll());
    }

    public String createAccount(String username, String accountName) {
        User user = authenticationService.getUser(username);
        if (user == null) {
            throw new ValidationException("User not found.");
        }

        String trimmedName = accountName == null ? "" : accountName.trim();
        if (trimmedName.isEmpty() || trimmedName.length() > AppConfig.ACCOUNT_NAME_MAX_LENGTH) {
            throw new ValidationException("Account name is invalid.");
        }

        String accountNumber = generateAccountNumber();
        BankAccount account = new BankAccount(user.getUsername(), accountNumber, trimmedName,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        accounts.put(accountNumber, account);
        persistAccounts();
        return accountNumber;
    }

    public BigDecimal getBalance(String username, String accountNumber) {
        return getOwnedAccount(username, accountNumber).getBalance();
    }

    public List<String> listAccountNumbersForUser(String username) {
        String normalizedUsername = normalizeUsername(username);
        List<String> result = new ArrayList<>();
        for (BankAccount account : accounts.values()) {
            if (account.getUsername().equals(normalizedUsername)) {
                result.add(account.getAccountNumber());
            }
        }
        return result;
    }

    public List<BankAccount> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    BankAccount getOwnedAccount(String username, String accountNumber) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedNumber = normalizeAccountNumber(accountNumber);
        BankAccount bankAccount = accounts.get(normalizedNumber);
        if (bankAccount == null) {
            throw new ValidationException("Account not found.");
        }
        if (!bankAccount.getUsername().equals(normalizedUsername)) {
            throw new UnauthorizedAccessException("You are not authorized to access this account.");
        }
        return bankAccount;
    }

    void persistAccounts() {
        accountRepository.saveAll(accounts);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new ValidationException("Username cannot be empty.");
        }
        return username.trim();
    }

    private String normalizeAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            throw new ValidationException("Account number is required.");
        }
        return accountNumber.trim();
    }

    private String generateAccountNumber() {
        StringBuilder accountNumber = new StringBuilder();
        for (int i = 0; i < AppConfig.ACCOUNT_NUMBER_LENGTH; i++) {
            accountNumber.append(random.nextInt(10));
        }
        String candidate = accountNumber.toString();
        if (accounts.containsKey(candidate)) {
            return generateAccountNumber();
        }
        return candidate;
    }
}