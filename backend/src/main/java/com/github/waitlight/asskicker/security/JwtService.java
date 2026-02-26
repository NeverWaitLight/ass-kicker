package com.github.waitlight.asskicker.security;

import com.github.waitlight.asskicker.model.User;
import com.github.waitlight.asskicker.model.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties properties;
    private SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret length must be at least 32 bytes.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        return generateToken(user, JwtTokenType.ACCESS, properties.getAccessTokenTtl());
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, JwtTokenType.REFRESH, properties.getRefreshTokenTtl());
    }

    public JwtPayload parseAccessToken(String token) {
        return parseToken(token, JwtTokenType.ACCESS);
    }

    public JwtPayload parseRefreshToken(String token) {
        return parseToken(token, JwtTokenType.REFRESH);
    }

    private String generateToken(User user, JwtTokenType tokenType, java.time.Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .claim("role", user.getRole().name())
                .claim("type", tokenType.name())
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private JwtPayload parseToken(String token, JwtTokenType expectedType) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
        Claims claims = jws.getBody();
        String type = claims.get("type", String.class);
        if (type == null || !expectedType.name().equals(type)) {
            throw new JwtException("Invalid token type.");
        }
        String roleValue = claims.get("role", String.class);
        if (roleValue == null) {
            throw new JwtException("Missing role claim.");
        }
        UserRole role = UserRole.valueOf(roleValue);
        String userId = claims.getSubject();
        return new JwtPayload(userId, role, expectedType);
    }
}
