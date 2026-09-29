package com.securebank.security;

import com.securebank.config.AppConfig;
import com.securebank.exception.AuthenticationException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Pbkdf2PasswordHasher implements PasswordHasher {

    @Override
    public String hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, AppConfig.HASH_ITERATIONS,
                    AppConfig.HASH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(AppConfig.HASH_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return AppConfig.HASH_ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new AuthenticationException("Unable to hash password securely.", e);
        }
    }

    @Override
    public boolean verify(String password, String storedHash) {
        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, AppConfig.HASH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(AppConfig.HASH_ALGORITHM);
            byte[] actualHash = factory.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}