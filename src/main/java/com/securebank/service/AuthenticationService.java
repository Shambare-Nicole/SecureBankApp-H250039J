package com.securebank.service;

import com.securebank.config.AppConfig;
import com.securebank.exception.AccountLockedException;
import com.securebank.exception.AuthenticationException;
import com.securebank.exception.ValidationException;
import com.securebank.model.User;
import com.securebank.security.PasswordHasher;
import com.securebank.storage.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.security.SecureRandom;

public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Integer> failedLoginAttempts = new HashMap<>();
    private final Map<String, Instant> loginLockouts = new HashMap<>();
    private final Random random = new SecureRandom();

    public AuthenticationService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        users.putAll(userRepository.loadAll());
    }

    public void registerUser(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validateUsername(normalizedUsername);
        validatePassword(password);
        if (users.containsKey(normalizedUsername)) {
            throw new ValidationException("Username already exists.");
        }

        byte[] salt = new byte[AppConfig.SALT_BYTES];
        random.nextBytes(salt);
        String passwordHash = passwordHasher.hash(password, salt);
        User user = new User(normalizedUsername, passwordHash, Base64.getEncoder().encodeToString(salt));
        users.put(normalizedUsername, user);
        userRepository.saveAll(users);
    }

    public boolean login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        Instant lockoutExpiry = loginLockouts.get(normalizedUsername);
        Instant now = Instant.now();
        if (lockoutExpiry != null) {
            if (now.isBefore(lockoutExpiry)) {
                throw new AccountLockedException("Account is locked. Try again later.");
            }
            loginLockouts.remove(normalizedUsername);
            failedLoginAttempts.remove(normalizedUsername);
        }

        User user = users.get(normalizedUsername);
        if (user != null && password != null && passwordHasher.verify(password, user.getPasswordHash())) {
            failedLoginAttempts.remove(normalizedUsername);
            return true;
        }

        int attempts = failedLoginAttempts.getOrDefault(normalizedUsername, 0) + 1;
        if (attempts >= AppConfig.MAX_FAILED_LOGINS) {
            failedLoginAttempts.remove(normalizedUsername);
            loginLockouts.put(normalizedUsername, now.plus(AppConfig.LOCKOUT_DURATION));
            throw new AccountLockedException("Account is locked due to too many failed login attempts.");
        }
        failedLoginAttempts.put(normalizedUsername, attempts);
        throw new AuthenticationException("Login failed. Please check your details.");
    }

    public User getUser(String username) {
        return users.get(normalizeUsername(username));
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new ValidationException("Username cannot be empty.");
        }
        return username.trim();
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank() || username.length() < AppConfig.USERNAME_MIN_LENGTH
                || username.length() > AppConfig.USERNAME_MAX_LENGTH) {
            throw new ValidationException("Username length is invalid.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < AppConfig.PASSWORD_MIN_LENGTH
                || password.length() > AppConfig.PASSWORD_MAX_LENGTH) {
            throw new ValidationException("Password length is invalid.");
        }
    }
}