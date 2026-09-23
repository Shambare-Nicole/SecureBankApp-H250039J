package com.securebank.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void minimumTransactionIsOneCent() {
        assertEquals(0, new BigDecimal("0.01").compareTo(AppConfig.MIN_TRANSACTION));
    }

    @Test
    void limitsAreConsistent() {
        assertTrue(AppConfig.MAX_TRANSACTION.compareTo(AppConfig.MIN_TRANSACTION) > 0);
        assertTrue(AppConfig.MAX_BALANCE.compareTo(AppConfig.MAX_TRANSACTION) >= 0);
        assertTrue(AppConfig.PASSWORD_MIN_LENGTH <= AppConfig.PASSWORD_MAX_LENGTH);
    }
}
