package com.securebank.storage;

import com.securebank.model.Transaction;
import java.util.List;

public interface TransactionRepository {
    List<Transaction> loadAll();

    void saveAll(List<Transaction> transactions);
}