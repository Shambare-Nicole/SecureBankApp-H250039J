package com.securebank.storage;

import com.securebank.config.AppConfig;
import com.securebank.exception.StorageException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Logger;

final class FileStorageSupport {
    private static final Logger LOGGER = Logger.getLogger(FileStorageSupport.class.getName());

    private FileStorageSupport() {
    }

    static void ensureDirectoryExists(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new StorageException("Unable to create data directory: " + directory, e);
        }
    }

    static List<String> readLines(Path file, String errorMessage) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException(errorMessage, e);
        }
    }

    static int recordMalformedLine(Path file, int lineNumber, int malformedLines) {
        LOGGER.warning("Skipping malformed record in " + file.getFileName() + " at line " + lineNumber + ".");
        int totalMalformedLines = malformedLines + 1;
        if (totalMalformedLines > AppConfig.MAX_MALFORMED_LINES) {
            throw new StorageException("Too many malformed lines in " + file.getFileName()
                    + "; refusing to load data safely.");
        }
        return totalMalformedLines;
    }

    static void writeFile(Path file, List<String> lines) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Unable to write file: " + file, e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException e) {
                    throw new StorageException("Unable to clean up temporary data file.", e);
                }
            }
        }
    }
}