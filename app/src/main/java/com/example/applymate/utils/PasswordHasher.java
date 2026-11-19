package com.example.applymate.utils;

import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String FORMAT_ID = "pbkdf2_hmac_sha1";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hashAndClear(char[] password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_BYTES];
        byte[] derivedKey = null;
        try {
            SECURE_RANDOM.nextBytes(salt);
            derivedKey = deriveKey(password, salt, ITERATIONS);
            return FORMAT_ID + "$" + ITERATIONS + "$"
                    + Base64.encodeToString(salt, Base64.NO_WRAP) + "$"
                    + Base64.encodeToString(derivedKey, Base64.NO_WRAP);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(salt, (byte) 0);
            if (derivedKey != null) {
                Arrays.fill(derivedKey, (byte) 0);
            }
        }
    }

    public static boolean verifyAndClear(char[] password, String storedHash)
            throws GeneralSecurityException {
        byte[] salt = null;
        byte[] expectedHash = null;
        byte[] actualHash = null;
        try {
            String[] parts = storedHash.split("\\$", -1);
            final int iterations;
            final int saltIndex;
            final int hashIndex;
            if (parts.length == 4 && FORMAT_ID.equals(parts[0])) {
                saltIndex = 2;
                hashIndex = 3;
                try {
                    iterations = Integer.parseInt(parts[1]);
                } catch (NumberFormatException exception) {
                    return false;
                }
            } else if (parts.length == 3) {
                saltIndex = 1;
                hashIndex = 2;
                try {
                    iterations = Integer.parseInt(parts[0]);
                } catch (NumberFormatException exception) {
                    return false;
                }
            } else {
                return false;
            }

            if (iterations < 1 || iterations > 1_000_000) {
                return false;
            }
            salt = Base64.decode(parts[saltIndex], Base64.NO_WRAP);
            expectedHash = Base64.decode(parts[hashIndex], Base64.NO_WRAP);
            if (salt.length != SALT_BYTES || expectedHash.length != HASH_BITS / 8) {
                return false;
            }
            actualHash = deriveKey(password, salt, iterations);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            return false;
        } finally {
            Arrays.fill(password, '\0');
            if (salt != null) {
                Arrays.fill(salt, (byte) 0);
            }
            if (expectedHash != null) {
                Arrays.fill(expectedHash, (byte) 0);
            }
            if (actualHash != null) {
                Arrays.fill(actualHash, (byte) 0);
            }
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(spec)
                    .getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
