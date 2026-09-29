package com.securebank.storage;

import com.securebank.config.AppConfig;
import com.securebank.model.BankAccount;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileAccountRepository implements AccountRepository {
    private final Path dataDirectory;

    public FileAccountRepository(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        FileStorageSupport.ensureDirectoryExists(dataDirectory);
    }

    @Override
    public Map<String, BankAccount> loadAll() {
        Path accountsFile = dataDirectory.resolve(AppConfig.ACCOUNTS_FILE);
        Map<String, BankAccount> accounts = new HashMap<>();
        if (!Files.exists(accountsFile)) {
            return accounts;
        }

        List<String> lines = FileStorageSupport.readLines(accountsFile, "Unable to load accounts from disk.");
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
                malformedLines = FileStorageSupport.recordMalformedLine(accountsFile, index + 1, malformedLines);
            }
        }
        return accounts;
    }

    @Override
    public void saveAll(Map<String, BankAccount> accounts) {
        List<String> lines = new ArrayList<>();
        for (BankAccount account : accounts.values()) {
            lines.add(account.toFileString());
        }
        FileStorageSupport.writeFile(dataDirectory.resolve(AppConfig.ACCOUNTS_FILE), lines);
    }
}