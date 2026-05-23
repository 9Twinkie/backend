package com.monitoring.config.security;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

/**
 * Криптография JWT: общий ключ подписи/проверки.
 */
@Configuration
public class JwtCryptographyConfig {

    @Bean
    public SecretKey jwtSigningKey(JwtProperties properties) {
        return JwtSigningSupport.hmacSha256KeyFromSecret(properties.getSecret());
    }

    @Bean
    public JwtParser jwtParser(SecretKey jwtSigningKey) {
        return Jwts.parser().verifyWith(jwtSigningKey).build();
    }
}
