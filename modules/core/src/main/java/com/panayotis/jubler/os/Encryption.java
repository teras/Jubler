package com.panayotis.jubler.os;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.Base64;
import java.util.Optional;

/**
 * Utility class for AES-GCM encryption/decryption using a password.
 * - Uses PBKDF2WithHmacSHA256 to derive a strong AES key from a password.
 * - Generates a random salt (16 bytes) and IV (12 bytes) per encryption.
 * - Prepends salt + IV to the ciphertext for storage.
 * - Output is Base64-encoded for convenient storage or transmission.
 */
public class Encryption {

    /**
     * Derives a 256-bit AES key from a password and salt using PBKDF2.
     *
     * @param password the user-provided password
     * @param salt     the random salt (16 bytes recommended)
     * @return SecretKeySpec suitable for AES
     */
    private static SecretKeySpec deriveKey(String password, byte[] salt) throws Exception {
        // 65,536 iterations, 256-bit key
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(f.generateSecret(spec).getEncoded(), "AES");
    }

    /**
     * Encrypts plaintext with AES-256-GCM using a password.
     * <p>
     * Output format (Base64-encoded): [16-byte salt][12-byte IV][ciphertext...]
     *
     * @param plaintext the text to encrypt
     * @param password  the password to derive the key
     * @return Base64 string containing salt + IV + ciphertext
     */
    public static Optional<String> encrypt(String plaintext, String password) {
        try {
            // Generate random 16-byte salt and 12-byte IV
            byte[] salt = new byte[16], iv = new byte[12];
            SecureRandom r = new SecureRandom();
            r.nextBytes(salt);
            r.nextBytes(iv);

            // Initialize AES/GCM cipher
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(128, iv));

            // Perform encryption
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // Combine salt + IV + ciphertext into a single byte array
            byte[] output = new byte[salt.length + iv.length + ciphertext.length];
            System.arraycopy(salt, 0, output, 0, salt.length);
            System.arraycopy(iv, 0, output, salt.length, iv.length);
            System.arraycopy(ciphertext, 0, output, salt.length + iv.length, ciphertext.length);

            // Return as Base64 for safe storage/transmission
            return Optional.of(Base64.getEncoder().encodeToString(output));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Decrypts a Base64-encoded AES-256-GCM message produced by encrypt().
     *
     * @param base64Ciphertext the Base64 string containing [salt][IV][ciphertext]
     * @param password         the password to derive the key
     * @return the original plaintext string
     */
    public static Optional<String> decrypt(String base64Ciphertext, String password) {
        try {
            byte[] allBytes = Base64.getDecoder().decode(base64Ciphertext);

            // Extract components
            byte[] salt = new byte[16];
            byte[] iv = new byte[12];
            byte[] ciphertext = new byte[allBytes.length - salt.length - iv.length];

            System.arraycopy(allBytes, 0, salt, 0, salt.length);
            System.arraycopy(allBytes, salt.length, iv, 0, iv.length);
            System.arraycopy(allBytes, salt.length + iv.length, ciphertext, 0, ciphertext.length);

            // Initialize AES/GCM cipher for decryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(128, iv));

            // Decrypt and return plaintext
            byte[] plaintextBytes = cipher.doFinal(ciphertext);
            return Optional.of(new String(plaintextBytes, "UTF-8"));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
