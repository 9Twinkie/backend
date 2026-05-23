package com.monitoring.config.security;

import com.monitoring.core.application.ports.out.security.TokenIssuer;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Реализация выпуска JWT на jjwt; роль кладётся в claim как строка из домена (ADMIN/ENGINEER).
 */
@Component
public class JjwtTokenIssuer implements TokenIssuer {

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JjwtTokenIssuer(SecretKey jwtSigningKey, JwtProperties properties) {
        this.signingKey = jwtSigningKey;
        this.properties = properties;
    }

    @Override
    public String issue(String username, String role) {
        var now = Instant.now();
        var exp = now.plusMillis(properties.getExpirationMs());
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }
}
