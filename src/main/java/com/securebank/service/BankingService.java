package com.securebank.service;

import com.securebank.config.AppConfig;
import com.securebank.exception.AuthenticationException;
import com.securebank.model.BankAccount;
import com.securebank.model.Transaction;
import com.securebank.model.User;
import com.securebank.security.PasswordHasher;
import com.securebank.security.Pbkdf2PasswordHasher;
import com.securebank.storage.FileAccountRepository;
import com.securebank.storage.FileTransactionRepository;
import com.securebank.storage.FileUserRepository;
import com.securebank.storage.UserRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

public class BankingService {
    private final AuthenticationService authenticationService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public BankingService() {
        this(AppConfig.DATA_DIRECTORY.toString());
    }

    public BankingService(String dataDirectoryPath) {
        Path dataDirectory = Path.of(dataDirectoryPath);
        UserRepository userRepository = new FileUserRepository(dataDirectory);
        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        authenticationService = new AuthenticationService(userRepository, passwordHasher);
        accountService = new AccountService(authenticationService, new FileAccountRepository(dataDirectory));
        transactionService = new TransactionService(accountService, new FileTransactionRepository(dataDirectory));
    }

    public void registerUser(String username, String password) {
        authenticationService.registerUser(username, password);
    }

    public boolean login(String username, String password) {
        try {
            return authenticationService.login(username, password);
        } catch (AuthenticationException e) {
            return false;
        }
    }

    public User getUser(String username) {
        return authenticationService.getUser(username);
    }

    public static BigDecimal parseAmount(String input) {
        return new BigDecimal(input);
    }

    public String createAccount(String username, String accountName) {
        return accountService.createAccount(username, accountName);
    }

    public BigDecimal getBalance(String username, String accountNumber) {
        return accountService.getBalance(username, accountNumber);
    }

    public void deposit(String username, String accountNumber, BigDecimal amount) {
        transactionService.deposit(username, accountNumber, amount);
    }

    public void withdraw(String username, String accountNumber, BigDecimal amount) {
        transactionService.withdraw(username, accountNumber, amount);
    }

    public List<String> listAccountNumbersForUser(String username) {
        return accountService.listAccountNumbersForUser(username);
    }

    public List<Transaction> getTransactionsForAccount(String username, String accountNumber) {
        return transactionService.getTransactionsForAccount(username, accountNumber);
    }

    public List<User> getAllUsers() {
        return authenticationService.getAllUsers();
    }

    public List<BankAccount> getAllAccounts() {
        return accountService.getAllAccounts();
    }
}
