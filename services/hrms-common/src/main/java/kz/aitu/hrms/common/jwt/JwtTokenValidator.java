package kz.aitu.hrms.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight JWT validator for services that only need to verify and parse tokens
 * (api-gateway, employee, attendance, …). Token <em>generation</em> lives only in
 * user-service ({@code kz.aitu.hrms.user.security.JwtService}), since that is the
 * sole service that owns the signing key with write access.
 *
 * Claim names are pulled from {@link JwtClaims} so the issuer and every reader
 * stay in sync at compile time.
 */
@Component
public class JwtTokenValidator {

    private final SecretKey signingKey;

    public JwtTokenValidator(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return parseClaims(token).get(JwtClaims.EMAIL, String.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get(JwtClaims.ROLE, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Object raw = parseClaims(token).get(JwtClaims.PERMISSIONS);
        if (raw instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    /**
     * Returns the {@code employeeId} claim as a {@link UUID}, or {@code null}
     * when the claim is absent, blank, or unparseable. Some users (system
     * accounts, super-admins) have no employee profile and therefore no
     * employeeId in their token.
     */
    public UUID extractEmployeeId(String token) {
        String raw = parseClaims(token).get(JwtClaims.EMPLOYEE_ID, String.class);
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}