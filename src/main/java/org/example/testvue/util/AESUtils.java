package org.example.testvue.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * AES-256 symmetric encryption for sensitive credentials.
 * Key is read from application.properties (aes.secret-key).
 */
@Component
public class AESUtils {

    private static final String ALGORITHM = "AES";
    private final SecretKeySpec keySpec;

    public AESUtils(@Value("${aes.secret-key}") String rawKey) {
        this.keySpec = deriveKey(rawKey);
    }

    /** Hash raw key to a fixed 32-byte AES-256 key. */
    private static SecretKeySpec deriveKey(String raw) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(raw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive AES key", e);
        }
    }

    /** Encrypt plaintext → Base64-encoded ciphertext. */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed", e);
        }
    }

    /** Decrypt Base64-encoded ciphertext → plaintext. */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) return "";
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt failed", e);
        }
    }

    /** Mask value for safe display: replace with "********" if non-empty. */
    public static String mask(String value) {
        return (value != null && !value.isEmpty()) ? "********" : "";
    }
}
