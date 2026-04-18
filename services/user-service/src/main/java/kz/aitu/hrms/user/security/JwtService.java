package kz.aitu.hrms.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kz.aitu.hrms.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * User-service's JWT provider. Issues access/refresh tokens and validates them.
 * Subject = user UUID. Claims include email and role so the gateway can forward
 * them as headers to downstream services without re-querying the user database.
 */
@Slf4j
@Component
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

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

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_ROLE, user.getRole().name());
        claims.put(CLAIM_TYPE, TYPE_ACCESS);
        return build(claims, user.getId().toString(), accessTokenExpiryMs);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_TYPE, TYPE_REFRESH);
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
        return TYPE_ACCESS.equals(extractClaim(token, c -> c.get(CLAIM_TYPE, String.class)));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(extractClaim(token, c -> c.get(CLAIM_TYPE, String.class)));
    }

    public String extractUserId(String token) {
        return parse(token).getSubject();
    }

    public String extractEmail(String token) {
        return extractClaim(token, c -> c.get(CLAIM_EMAIL, String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, c -> c.get(CLAIM_ROLE, String.class));
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