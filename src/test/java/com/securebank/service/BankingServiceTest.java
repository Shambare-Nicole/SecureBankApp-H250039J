package com.securebank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.securebank.config.AppConfig;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankingServiceTest {

    private BankingService service;

    @BeforeEach
    void setUp() throws Exception {
        Path tempDir = Files.createTempDirectory("securebank-tests");
        service = new BankingService(tempDir.toString());
        service.registerUser("alice", "Password123!");
    }

    @Test
    void depositAndWithdrawUpdateBalance() {
        String accountNumber = service.createAccount("alice", "Savings");

        service.deposit(accountNumber, new BigDecimal("100.00"));
        assertEquals(0, new BigDecimal("100.00").compareTo(service.getBalance(accountNumber)));

        service.withdraw(accountNumber, new BigDecimal("25.50"));
        assertEquals(0, new BigDecimal("74.50").compareTo(service.getBalance(accountNumber)));
    }

    @Test
    void invalidWithdrawRejected() {
        String accountNumber = service.createAccount("alice", "Savings");
        service.deposit(accountNumber, new BigDecimal("50.00"));

        assertThrows(IllegalArgumentException.class,
                () -> service.withdraw(accountNumber, new BigDecimal("1000.00")));
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
}
