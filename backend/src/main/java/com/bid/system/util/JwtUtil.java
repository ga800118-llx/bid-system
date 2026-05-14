package com.bid.system.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    private static final String SECRET = "bid-system-jwt-secret-key-2024-very-long-and-secure";
    private static final long EXPIRATION = 86400000;

    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, EXPIRATION);
    }

    public static String generateToken(Long userId, String username, long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        long safeExpiration = expirationMillis <= 0 ? EXPIRATION : expirationMillis;
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + safeExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public static String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        Object username = claims.get("username");
        return username == null ? claims.getSubject() : String.valueOf(username);
    }

    public static String getRole(String token) {
        return (String) parseToken(token).get("role");
    }

    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        if (userId != null) {
            return Long.valueOf(String.valueOf(userId));
        }
        String subject = claims.getSubject();
        if (subject != null && subject.matches("\\d+")) {
            return Long.valueOf(subject);
        }
        return null;
    }

    public static boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}
