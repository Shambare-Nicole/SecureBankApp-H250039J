package com.securebank.storage;

import com.securebank.config.AppConfig;
import com.securebank.model.Transaction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileTransactionRepository implements TransactionRepository {
    private final Path dataDirectory;

    public FileTransactionRepository(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        FileStorageSupport.ensureDirectoryExists(dataDirectory);
    }

    @Override
    public List<Transaction> loadAll() {
        Path transactionsFile = dataDirectory.resolve(AppConfig.TRANSACTIONS_FILE);
        List<Transaction> transactions = new ArrayList<>();
        if (!Files.exists(transactionsFile)) {
            return transactions;
        }

        List<String> lines = FileStorageSupport.readLines(transactionsFile,
                "Unable to load transaction history from disk.");
        int malformedLines = 0;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.isBlank()) {
                continue;
            }
            try {
                transactions.add(Transaction.fromFileString(line));
            } catch (RuntimeException e) {
                malformedLines = FileStorageSupport.recordMalformedLine(transactionsFile, index + 1, malformedLines);
            }
        }
        return transactions;
    }

    @Override
    public void saveAll(List<Transaction> transactions) {
        List<String> lines = new ArrayList<>();
        for (Transaction transaction : transactions) {
            lines.add(transaction.toFileString());
        }
        FileStorageSupport.writeFile(dataDirectory.resolve(AppConfig.TRANSACTIONS_FILE), lines);
    }
}