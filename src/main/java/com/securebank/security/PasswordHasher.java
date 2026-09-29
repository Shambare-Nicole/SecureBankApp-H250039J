package com.securebank.security;

public interface PasswordHasher {
    String hash(String password, byte[] salt);

    boolean verify(String password, String storedHash);
}