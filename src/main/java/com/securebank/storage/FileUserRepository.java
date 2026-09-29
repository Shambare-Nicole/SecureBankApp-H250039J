package com.securebank.storage;

import com.securebank.config.AppConfig;
import com.securebank.model.User;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUserRepository implements UserRepository {
    private final Path dataDirectory;

    public FileUserRepository(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        FileStorageSupport.ensureDirectoryExists(dataDirectory);
    }

    @Override
    public Map<String, User> loadAll() {
        Path usersFile = dataDirectory.resolve(AppConfig.USERS_FILE);
        Map<String, User> users = new HashMap<>();
        if (!Files.exists(usersFile)) {
            return users;
        }

        List<String> lines = FileStorageSupport.readLines(usersFile, "Unable to load users from disk.");
        int malformedLines = 0;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.isBlank()) {
                continue;
            }
            try {
                User user = User.fromFileString(line);
                users.put(user.getUsername().trim(), user);
            } catch (RuntimeException e) {
                malformedLines = FileStorageSupport.recordMalformedLine(usersFile, index + 1, malformedLines);
            }
        }
        return users;
    }

    @Override
    public void saveAll(Map<String, User> users) {
        List<String> lines = new ArrayList<>();
        for (User user : users.values()) {
            lines.add(user.toFileString());
        }
        FileStorageSupport.writeFile(dataDirectory.resolve(AppConfig.USERS_FILE), lines);
    }
}