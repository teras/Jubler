package com.panayotis.jubler.os;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Encryption {
    private static final String ALGORITHM = "AES";

    public static byte[] encryptKey(String decryptedKey, String password) {
        if (decryptedKey.isEmpty() || password.isEmpty()) return null;
        try {
            Key ssKey = new SecretKeySpec(getMD5Checksum(password).getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, ssKey);
            return c.doFinal(decryptedKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDecryptedKey(byte[] encryptedKey, String password) {
        if (encryptedKey == null || encryptedKey.length == 0 || password.isEmpty()) return "";
        try {
            Key ssKey = new SecretKeySpec(getMD5Checksum(password).getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, ssKey);
            byte[] decr = c.doFinal(encryptedKey);
            return new String(decr);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getMD5Checksum(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashInBytes = md.digest(input.getBytes());

            // bytes to hex
            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // MD5 should always be available
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    public static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] base64Decode(String data) {
        return Base64.getDecoder().decode(data);
    }

}
