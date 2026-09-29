package com.securebank.service;

import com.securebank.config.AppConfig;
import com.securebank.model.BankAccount;
import com.securebank.model.Transaction;
import com.securebank.model.User;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class BankingService {
    private static final Logger LOGGER = Logger.getLogger(BankingService.class.getName());
    private final Path dataDirectory;
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, BankAccount> accounts = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<String, Integer> failedLoginAttempts = new HashMap<>();
    private final Map<String, Instant> loginLockouts = new HashMap<>();
    private final Random random = new SecureRandom();

    public BankingService() {
        this(AppConfig.DATA_DIRECTORY.toString());
    }

    public BankingService(String dataDirectoryPath) {
        this.dataDirectory = Path.of(dataDirectoryPath);
        ensureDirectoryExists();
        loadUsers();
        loadAccounts();
        loadTransactions();
    }

    public void registerUser(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validateUsername(normalizedUsername);
        validatePassword(password);
        if (users.containsKey(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists.");
        }

        byte[] salt = new byte[AppConfig.SALT_BYTES];
        random.nextBytes(salt);
        String passwordHash = hashPassword(password, salt);
        User user = new User(normalizedUsername, passwordHash, Base64.getEncoder().encodeToString(salt));
        users.put(normalizedUsername, user);
        persistUsers();
    }

    public boolean login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        Instant lockoutExpiry = loginLockouts.get(normalizedUsername);
        Instant now = Instant.now();
        if (lockoutExpiry != null) {
            if (now.isBefore(lockoutExpiry)) {
                throw new IllegalStateException("Account is locked. Try again later.");
            }
            loginLockouts.remove(normalizedUsername);
            failedLoginAttempts.remove(normalizedUsername);
        }

        User user = users.get(normalizedUsername);
        if (user != null && password != null && isValidPassword(password, user)) {
            failedLoginAttempts.remove(normalizedUsername);
            return true;
        }

        int attempts = failedLoginAttempts.getOrDefault(normalizedUsername, 0) + 1;
        if (attempts >= AppConfig.MAX_FAILED_LOGINS) {
            failedLoginAttempts.remove(normalizedUsername);
            loginLockouts.put(normalizedUsername, now.plus(AppConfig.LOCKOUT_DURATION));
            throw new IllegalStateException("Account is locked due to too many failed login attempts.");
        }
        failedLoginAttempts.put(normalizedUsername, attempts);
        return false;
    }

    public User getUser(String username) {
        return users.get(normalizeUsername(username));
    }

    public static BigDecimal parseAmount(String input) {
        return new BigDecimal(input);
    }

    public String createAccount(String username, String accountName) {
        User user = users.get(normalizeUsername(username));
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        String trimmedName = accountName == null ? "" : accountName.trim();
        if (trimmedName.isEmpty() || trimmedName.length() > AppConfig.ACCOUNT_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Account name is invalid.");
        }

        String accountNumber = generateAccountNumber();
        BankAccount account = new BankAccount(user.getUsername(), accountNumber, trimmedName,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        accounts.put(accountNumber, account);
        persistAccounts();
        return accountNumber;
    }

    public BigDecimal getBalance(String username, String accountNumber) {
        BankAccount bankAccount = getOwnedAccount(username, accountNumber);
        return bankAccount.getBalance();
    }

    public void deposit(String username, String accountNumber, BigDecimal amount) {
        BankAccount bankAccount = getOwnedAccount(username, accountNumber);
        validateAmount(amount, "Deposit");
        bankAccount.deposit(amount);
        transactions.add(new Transaction(accountNumber, "DEPOSIT", amount, bankAccount.getBalance()));
        persistAccounts();
        persistTransactions();
    }

    public void withdraw(String username, String accountNumber, BigDecimal amount) {
        BankAccount bankAccount = getOwnedAccount(username, accountNumber);
        validateAmount(amount, "Withdrawal");
        if (bankAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds.");
        }
        bankAccount.withdraw(amount);
        transactions.add(new Transaction(accountNumber, "WITHDRAW", amount, bankAccount.getBalance()));
        persistAccounts();
        persistTransactions();
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

    public List<Transaction> getTransactionsForAccount(String username, String accountNumber) {
        getOwnedAccount(username, accountNumber);
        String normalizedNumber = normalizeAccountNumber(accountNumber);
        List<Transaction> result = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getAccountNumber().equals(normalizedNumber)) {
                result.add(transaction);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public List<BankAccount> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create data directory: " + dataDirectory, e);
        }
    }

    private void loadUsers() {
        Path usersFile = dataDirectory.resolve(AppConfig.USERS_FILE);
        if (!Files.exists(usersFile)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(usersFile, StandardCharsets.UTF_8);
            int malformedLines = 0;
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                if (line == null || line.isBlank()) {
                    continue;
                }
                try {
                    User user = User.fromFileString(line);
                    users.put(normalizeUsername(user.getUsername()), user);
                } catch (RuntimeException e) {
                    malformedLines = recordMalformedLine(usersFile, index + 1, malformedLines);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load users from disk.", e);
        }
    }

    private void loadAccounts() {
        Path accountsFile = dataDirectory.resolve(AppConfig.ACCOUNTS_FILE);
        if (!Files.exists(accountsFile)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(accountsFile, StandardCharsets.UTF_8);
            int malformedLines = 0;
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                if (line == null || line.isBlank()) {
                    continue;
                }
                try {
                    BankAccount account = BankAccount.fromFileString(line);
                    accounts.put(account.getAccountNumber(), account);
                } catch (RuntimeException e) {
                    malformedLines = recordMalformedLine(accountsFile, index + 1, malformedLines);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load accounts from disk.", e);
        }
    }

    private void loadTransactions() {
        Path transactionsFile = dataDirectory.resolve(AppConfig.TRANSACTIONS_FILE);
        if (!Files.exists(transactionsFile)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(transactionsFile, StandardCharsets.UTF_8);
            int malformedLines = 0;
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                if (line == null || line.isBlank()) {
                    continue;
                }
                try {
                    transactions.add(Transaction.fromFileString(line));
                } catch (RuntimeException e) {
                    malformedLines = recordMalformedLine(transactionsFile, index + 1, malformedLines);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load transaction history from disk.", e);
        }
    }

    private void persistUsers() {
        Path usersFile = dataDirectory.resolve(AppConfig.USERS_FILE);
        List<String> lines = new ArrayList<>();
        for (User user : users.values()) {
            lines.add(user.toFileString());
        }
        writeFile(usersFile, lines);
    }

    private void persistAccounts() {
        Path accountsFile = dataDirectory.resolve(AppConfig.ACCOUNTS_FILE);
        List<String> lines = new ArrayList<>();
        for (BankAccount account : accounts.values()) {
            lines.add(account.toFileString());
        }
        writeFile(accountsFile, lines);
    }

    private void persistTransactions() {
        Path transactionsFile = dataDirectory.resolve(AppConfig.TRANSACTIONS_FILE);
        List<String> lines = new ArrayList<>();
        for (Transaction transaction : transactions) {
            lines.add(transaction.toFileString());
        }
        writeFile(transactionsFile, lines);
    }

    private void writeFile(Path file, List<String> lines) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write file: " + file, e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to clean up temporary data file.", e);
                }
            }
        }
    }

    private BankAccount getOwnedAccount(String username, String accountNumber) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedNumber = normalizeAccountNumber(accountNumber);
        BankAccount bankAccount = accounts.get(normalizedNumber);
        if (bankAccount == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        if (!bankAccount.getUsername().equals(normalizedUsername)) {
            throw new IllegalArgumentException("You are not authorized to access this account.");
        }
        return bankAccount;
    }

    private int recordMalformedLine(Path file, int lineNumber, int malformedLines) {
        LOGGER.warning("Skipping malformed record in " + file.getFileName() + " at line " + lineNumber + ".");
        int totalMalformedLines = malformedLines + 1;
        if (totalMalformedLines > AppConfig.MAX_MALFORMED_LINES) {
            throw new IllegalStateException("Too many malformed lines in " + file.getFileName()
                    + "; refusing to load data safely.");
        }
        return totalMalformedLines;
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        return username.trim();
    }

    private String normalizeAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            throw new IllegalArgumentException("Account number is required.");
        }
        return accountNumber.trim();
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank() || username.length() < AppConfig.USERNAME_MIN_LENGTH
                || username.length() > AppConfig.USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Username length is invalid.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < AppConfig.PASSWORD_MIN_LENGTH
                || password.length() > AppConfig.PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("Password length is invalid.");
        }
    }

    private void validateAmount(BigDecimal amount, String operation) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(operation + " amount must be greater than zero.");
        }
        if (amount.compareTo(AppConfig.MIN_TRANSACTION) < 0) {
            throw new IllegalArgumentException(operation + " amount is below the minimum allowed amount.");
        }
        if (amount.compareTo(AppConfig.MAX_TRANSACTION) > 0) {
            throw new IllegalArgumentException(operation + " amount exceeds the system limit.");
        }
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

    private String hashPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, AppConfig.HASH_ITERATIONS,
                    AppConfig.HASH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(AppConfig.HASH_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return AppConfig.HASH_ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to hash password securely.", e);
        }
    }

    private boolean isValidPassword(String password, User user) {
        String[] parts = user.getPasswordHash().split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, AppConfig.HASH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(AppConfig.HASH_ALGORITHM);
            byte[] actualHash = factory.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
