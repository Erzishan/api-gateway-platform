package com.apigateway.tenant.util;

import com.apigateway.tenant.constants.AppConstants;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class ApiKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Generates a full API key like: gw_abc12345xyz789...
    public String generateKey() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
        return AppConstants.API_KEY_PREFIX + randomPart;
    }

    // Extracts the first 8 characters for DB lookup
    public String extractPrefix(String fullKey) {
        return fullKey.substring(0, AppConstants.API_KEY_PREFIX_LENGTH);
    }

    // Creates SHA-256 hash for secure storage
    public String hashKey(String fullKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fullKey.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    // Verifies an incoming key against a stored hash
    public boolean verifyKey(String fullKey, String storedHash) {
        String incomingHash = hashKey(fullKey);
        return incomingHash.equals(storedHash);
    }
}

