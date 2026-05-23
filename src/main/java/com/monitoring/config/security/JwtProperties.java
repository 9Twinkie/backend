package com.monitoring.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Параметры JWT: секрет и время жизни токена.
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Секрет для HMAC-SHA256; перед продом заменить на криптостойкое значение (≥32 символа рекомендуется).
     */
    private String secret = "change-me";

    /**
     * TTL access-токена в миллисекундах.
     */
    private long expirationMs = 86_400_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
