package com.securebank.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Central place for every rule and limit in the application.
 * Only constants live here (no mutable state), so nothing can change them at runtime.
 * Nothing in this class is secret: passwords and keys are never stored in source code.
 */
public final class AppConfig {

    private AppConfig() {
        // Utility class: no instances.
    }

    // ---- Money rules ----
    public static final int MONEY_SCALE = 2;
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_EVEN;
    public static final BigDecimal MIN_TRANSACTION = new BigDecimal("0.01");
    public static final BigDecimal MAX_TRANSACTION = new BigDecimal("1000000.00");
    public static final BigDecimal MAX_BALANCE = new BigDecimal("100000000.00");

    // ---- Password hashing (PBKDF2WithHmacSHA256) ----
    public static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int HASH_ITERATIONS = 210_000;
    public static final int SALT_BYTES = 16;
    public static final int HASH_BITS = 256;

    // ---- Login protection ----
    public static final int MAX_FAILED_LOGINS = 5;
    public static final Duration LOCKOUT_DURATION = Duration.ofMinutes(5);

    // ---- Input rules ----
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 20;
    public static final int PASSWORD_MIN_LENGTH = 10;
    public static final int PASSWORD_MAX_LENGTH = 128;
    public static final int ACCOUNT_NAME_MAX_LENGTH = 40;
    public static final int MAX_INPUT_LENGTH = 200;
    public static final int MAX_INVALID_INPUTS_IN_A_ROW = 5;
    public static final int ACCOUNT_NUMBER_LENGTH = 10;

    // ---- Storage ----
    public static final Path DATA_DIRECTORY = Path.of("data");
    public static final String USERS_FILE = "users.txt";
    public static final String ACCOUNTS_FILE = "accounts.txt";
    public static final String TRANSACTIONS_FILE = "transactions.txt";
    public static final String FIELD_SEPARATOR = "|";
    public static final int MAX_MALFORMED_LINES = 5;
}
