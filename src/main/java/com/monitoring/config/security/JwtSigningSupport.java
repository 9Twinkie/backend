package com.monitoring.config.security;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Утилита получения ключа подписи: нормализует произвольную строку секрета до 32 байт SHA-256.
 */
final class JwtSigningSupport {

    private JwtSigningSupport() {
    }

    /**
     * Строит HMAC-ключ фиксированной длины для HS256 из пользовательской строки (удобно для dev-секретов).
     */
    static SecretKey hmacSha256KeyFromSecret(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Алгоритм SHA-256 недоступен в JRE", e);
        }
    }
}
