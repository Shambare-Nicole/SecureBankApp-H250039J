package com.securebank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.securebank.config.AppConfig;
import com.securebank.exception.AccountLockedException;
import com.securebank.exception.InsufficientFundsException;
import com.securebank.exception.StorageException;
import com.securebank.exception.UnauthorizedAccessException;
import com.securebank.exception.ValidationException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankingServiceTest {

    private BankingService service;
    private Path dataDirectory;

    @BeforeEach
    void setUp() throws Exception {
        dataDirectory = Files.createTempDirectory("securebank-tests");
        service = new BankingService(dataDirectory.toString());
        service.registerUser("alice", "Password123!");
    }

    @Test
    void depositAndWithdrawUpdateBalance() {
        String accountNumber = service.createAccount("alice", "Savings");

        service.deposit("alice", accountNumber, new BigDecimal("100.00"));
        assertEquals(0, new BigDecimal("100.00").compareTo(service.getBalance("alice", accountNumber)));

        service.withdraw("alice", accountNumber, new BigDecimal("25.50"));
        assertEquals(0, new BigDecimal("74.50").compareTo(service.getBalance("alice", accountNumber)));
    }

    @Test
    void invalidWithdrawRejected() {
        String accountNumber = service.createAccount("alice", "Savings");
        service.deposit("alice", accountNumber, new BigDecimal("50.00"));

        assertThrows(InsufficientFundsException.class,
                () -> service.withdraw("alice", accountNumber, new BigDecimal("1000.00")));
    }

    @Test
    void passwordsAreHashedAndVerified() {
        service.registerUser("bob", "StrongPass123!");

        assertTrue(service.login("bob", "StrongPass123!"));
        assertTrue(service.getUser("bob") != null);
        assertTrue(service.getUser("bob").getPasswordHash() != null);
        assertTrue(service.getUser("bob").getPasswordHash().length() > 20);
        assertTrue(service.getUser("bob").getSalt().length() > 10);
        assertTrue(service.getUser("bob").getPasswordHash()
                .startsWith("" + AppConfig.HASH_ITERATIONS));
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownUsername() {
        assertFalse(service.login("alice", "WrongPassword!"));
        assertFalse(service.login("unknown", "WrongPassword!"));
    }

    @Test
    void registeringDuplicateUsernameIsRejected() {
        assertThrows(ValidationException.class,
                () -> service.registerUser("alice", "AnotherPassword123!"));
    }

    @Test
    void depositRejectsZeroAndNegativeAmounts() {
        String accountNumber = service.createAccount("alice", "Savings");

        assertThrows(ValidationException.class,
                () -> service.deposit("alice", accountNumber, BigDecimal.ZERO));
        assertThrows(ValidationException.class,
                () -> service.deposit("alice", accountNumber, new BigDecimal("-1.00")));
    }

    @Test
    void malformedDepositAmountIsRejectedAsInvalidInput() {
        String accountNumber = service.createAccount("alice", "Savings");

        assertThrows(NumberFormatException.class,
                () -> service.deposit("alice", accountNumber,
                        BankingService.parseAmount("not-a-number")));
        assertEquals(0, BigDecimal.ZERO.compareTo(service.getBalance("alice", accountNumber)));
    }

    @Test
    void withdrawalRejectsZeroAndNegativeAmounts() {
        String accountNumber = service.createAccount("alice", "Savings");

        assertThrows(ValidationException.class,
                () -> service.withdraw("alice", accountNumber, BigDecimal.ZERO));
        assertThrows(ValidationException.class,
                () -> service.withdraw("alice", accountNumber, new BigDecimal("-1.00")));
    }

    @Test
    void usersCannotAccessAnotherUsersAccount() {
        service.registerUser("bob", "StrongPass123!");
        String accountNumber = service.createAccount("alice", "Savings");

        assertThrows(UnauthorizedAccessException.class, () -> service.getBalance("bob", accountNumber));
        assertThrows(UnauthorizedAccessException.class,
                () -> service.deposit("bob", accountNumber, new BigDecimal("10.00")));
        assertThrows(UnauthorizedAccessException.class,
                () -> service.withdraw("bob", accountNumber, new BigDecimal("10.00")));
        assertThrows(UnauthorizedAccessException.class,
                () -> service.getTransactionsForAccount("bob", accountNumber));
    }

    @Test
    void persistedDataIsAvailableAfterServiceReload() {
        String accountNumber = service.createAccount("alice", "Savings");
        service.deposit("alice", accountNumber, new BigDecimal("42.50"));

        BankingService reloadedService = new BankingService(dataDirectory.toString());

        assertTrue(reloadedService.login("alice", "Password123!"));
        assertEquals(0, new BigDecimal("42.50").compareTo(reloadedService.getBalance("alice", accountNumber)));
        assertEquals(1, reloadedService.getTransactionsForAccount("alice", accountNumber).size());
    }

    @Test
    void loginLocksUsernameAfterMaximumFailedAttempts() {
        for (int attempt = 1; attempt < AppConfig.MAX_FAILED_LOGINS; attempt++) {
            assertFalse(service.login("unknown", "WrongPassword!"));
        }

        assertThrows(AccountLockedException.class,
                () -> service.login("unknown", "WrongPassword!"));
        assertThrows(AccountLockedException.class,
                () -> service.login("unknown", "WrongPassword!"));
    }

    @Test
    void malformedUserRecordsAreSkippedWhenBelowLimit() throws Exception {
        Path usersFile = dataDirectory.resolve(AppConfig.USERS_FILE);
        Files.writeString(usersFile, "malformed-record\n", StandardOpenOption.APPEND);

        BankingService reloadedService = new BankingService(dataDirectory.toString());

        assertTrue(reloadedService.login("alice", "Password123!"));
    }

    @Test
    void tooManyMalformedUserRecordsFailSafely() throws Exception {
        Path usersFile = dataDirectory.resolve(AppConfig.USERS_FILE);
        Files.write(usersFile, List.of("bad-1", "bad-2", "bad-3", "bad-4", "bad-5", "bad-6"));

        StorageException error = assertThrows(StorageException.class,
                () -> new BankingService(dataDirectory.toString()));

        assertTrue(error.getMessage().contains("Too many malformed lines"));
    }
}
