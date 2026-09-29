package com.securebank.storage;

import com.securebank.model.BankAccount;
import java.util.Map;

public interface AccountRepository {
    Map<String, BankAccount> loadAll();

    void saveAll(Map<String, BankAccount> accounts);
}