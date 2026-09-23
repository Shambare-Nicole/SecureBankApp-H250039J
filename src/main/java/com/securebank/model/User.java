package com.securebank.model;

public class User {
    private final String username;
    private final String passwordHash;
    private final String salt;

    public User(String username, String passwordHash, String salt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public String toFileString() {
        return username + "|" + passwordHash + "|" + salt;
    }

    public static User fromFileString(String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid user record: " + line);
        }
        return new User(parts[0], parts[1], parts[2]);
    }
}
