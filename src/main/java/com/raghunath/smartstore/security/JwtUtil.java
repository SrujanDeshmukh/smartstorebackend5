package com.raghunath.smartstore.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final Key key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;
    private final long clockSkew;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access.expiration}") long accessTokenValidity,
            @Value("${jwt.refresh.expiration}") long refreshTokenValidity,
            @Value("${jwt.clock.skew:60000}") long clockSkew) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
        this.clockSkew = clockSkew;
    }

    // Generate Access Token with email and role
    public String generateAccessToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("type", "ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Overloaded method for backward compatibility (defaults to USER role)
    public String generateAccessToken(String email) {
        return generateAccessToken(email, "USER");
    }

    // Generate Refresh Token
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate Token with Clock Skew tolerance
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(clockSkew / 1000)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Check if token is expired without throwing exception
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    // Check if token will expire soon (for proactive refresh)
    public boolean isTokenExpiringSoon(String token) {
        return isTokenExpiringSoon(token, 5 * 60 * 1000); // 5 minutes buffer
    }

    // Check if token will expire within specified buffer time
    public boolean isTokenExpiringSoon(String token, long bufferTimeMillis) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            return expiration.getTime() - now.getTime() <= bufferTimeMillis;
        } catch (Exception e) {
            return true; // If we can't parse, assume it needs refresh
        }
    }

    // Get remaining time until token expires (in milliseconds)
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            return Math.max(0, expiration.getTime() - now.getTime());
        } catch (Exception e) {
            return 0; // Token is invalid or expired
        }
    }

    // Extract username even from expired token (useful for refresh scenarios)
    public String extractUsernameFromExpiredToken(String token) {
        try {
            return extractUsername(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims().getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // Extract role even from expired token
    public String extractRoleFromExpiredToken(String token) {
        try {
            return extractRole(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    // Standard extract methods
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    // Generic claim extraction
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extract all claims from token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Validate specific token type (ACCESS or REFRESH)
    public boolean validateTokenType(String token, String expectedType) {
        try {
            String tokenType = extractTokenType(token);
            return expectedType.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    // Validate access token specifically
    public boolean validateAccessToken(String token) {
        return isTokenValid(token) && validateTokenType(token, "ACCESS");
    }

    // Validate refresh token specifically
    public boolean validateRefreshToken(String token) {
        return isTokenValid(token) && validateTokenType(token, "REFRESH");
    }

    // Generate new access token from refresh token (for refresh endpoint)
    public String generateAccessTokenFromRefreshToken(String refreshToken) {
        if (!validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String email = extractUsername(refreshToken);
        // For refresh tokens, we might not have role stored, so default to USER
        // In a real app, you'd fetch the user's current role from database
        String role = "USER"; // You can enhance this to fetch from database

        return generateAccessToken(email, role);
    }

    // Enhanced token validation with detailed response
    public TokenValidationResult validateTokenWithDetails(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(clockSkew / 1000)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return new TokenValidationResult(true, "Token is valid", claims);
        } catch (ExpiredJwtException e) {
            return new TokenValidationResult(false, "Token has expired", e.getClaims());
        } catch (MalformedJwtException e) {
            return new TokenValidationResult(false, "Token is malformed", null);
        } catch (SignatureException e) {
            return new TokenValidationResult(false, "Token signature is invalid", null);
        } catch (IllegalArgumentException e) {
            return new TokenValidationResult(false, "Token is null or empty", null);
        } catch (Exception e) {
            return new TokenValidationResult(false, "Token validation failed: " + e.getMessage(), null);
        }
    }

    // Inner class for detailed token validation results
    public static class TokenValidationResult {
        private final boolean valid;
        private final String message;
        private final Claims claims;

        public TokenValidationResult(boolean valid, String message, Claims claims) {
            this.valid = valid;
            this.message = message;
            this.claims = claims;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public Claims getClaims() { return claims; }
    }

    // Utility method to get token info as string (for debugging)
    public String getTokenInfo(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return String.format(
                    "Token Info - Subject: %s, Type: %s, Role: %s, Issued: %s, Expires: %s",
                    claims.getSubject(),
                    claims.get("type", String.class),
                    claims.get("role", String.class),
                    claims.getIssuedAt(),
                    claims.getExpiration()
            );
        } catch (Exception e) {
            return "Invalid token: " + e.getMessage();
        }
    }

    // Check if token is about to expire (within 30 seconds)
    public boolean needsImmediateRefresh(String token) {
        return isTokenExpiringSoon(token, 30 * 1000); // 30 seconds
    }

    // Get token age in milliseconds
    public long getTokenAge(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date issuedAt = claims.getIssuedAt();
            Date now = new Date();
            return now.getTime() - issuedAt.getTime();
        } catch (Exception e) {
            return -1; // Invalid token
        }
    }
}
