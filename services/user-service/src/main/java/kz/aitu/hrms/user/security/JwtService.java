package kz.aitu.hrms.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kz.aitu.hrms.common.jwt.JwtClaims;
import kz.aitu.hrms.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * User-service's JWT provider. Issues access/refresh tokens and validates them.
 * Subject = user UUID. Claims include email and role so the gateway can forward
 * them as headers to downstream services without re-querying the user database.
 */
@Slf4j
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms:900000}") long accessTokenExpiryMs,
            @Value("${app.jwt.refresh-token-expiry-ms:604800000}") long refreshTokenExpiryMs) {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    public String generateAccessToken(User user, Set<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaims.EMAIL, user.getEmail());
        claims.put(JwtClaims.ROLE, user.getRole().name());
        claims.put(JwtClaims.PERMISSIONS, permissions != null ? permissions : Collections.emptySet());
        if (user.getEmployeeId() != null) {
            claims.put(JwtClaims.EMPLOYEE_ID, user.getEmployeeId().toString());
        }
        claims.put(JwtClaims.TYPE, JwtClaims.TYPE_ACCESS);
        return build(claims, user.getId().toString(), accessTokenExpiryMs);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaims.EMAIL, user.getEmail());
        claims.put(JwtClaims.TYPE, JwtClaims.TYPE_REFRESH);
        return build(claims, user.getId().toString(), refreshTokenExpiryMs);
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT invalid: {}", ex.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return JwtClaims.TYPE_ACCESS.equals(extractClaim(token, c -> c.get(JwtClaims.TYPE, String.class)));
    }

    public boolean isRefreshToken(String token) {
        return JwtClaims.TYPE_REFRESH.equals(extractClaim(token, c -> c.get(JwtClaims.TYPE, String.class)));
    }

    public String extractUserId(String token) {
        return parse(token).getSubject();
    }

    public String extractEmail(String token) {
        return extractClaim(token, c -> c.get(JwtClaims.EMAIL, String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, c -> c.get(JwtClaims.ROLE, String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Object raw = extractClaim(token, c -> c.get(JwtClaims.PERMISSIONS));
        if (raw instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    public UUID extractEmployeeId(String token) {
        String raw = extractClaim(token, c -> c.get(JwtClaims.EMPLOYEE_ID, String.class));
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Date extractExpiration(String token) {
        return parse(token).getExpiration();
    }

    public long remainingLifeMs(String token) {
        return Math.max(0L, extractExpiration(token).getTime() - System.currentTimeMillis());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parse(token));
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String build(Map<String, Object> claims, String subject, long ttlMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMs))
                .signWith(signingKey)
                .compact();
    }
}