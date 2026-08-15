package com.likelion.drjudge.domain.jwt.jwt;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private static final String TOKEN_TYPE_KEY = "type";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidityMs;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidityMs;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    //  토큰 발급
    public String createAccessToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenValidityMs);
        return createToken(userId, now, expiration, ACCESS_TOKEN_TYPE);
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenValidityMs);
        return createToken(userId, now, expiration, REFRESH_TOKEN_TYPE);
    }

    private String createToken(Long userId, Date issuedAt, Date expiration, String tokenType) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_KEY, tokenType)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    private Date getTodayMidnight() {
        LocalDateTime midnight = LocalDate.now().plusDays(1).atStartOfDay();
        return Date.from(midnight.atZone(ZoneId.systemDefault()).toInstant());
    }

    //  토큰 검증/파싱
    public Claims resolveAccessClaims(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_KEY, String.class))) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Claims resolveAccessClaimsAllowExpired(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_KEY, String.class))) {
                return null;
            }
            return claims;
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_KEY, String.class))) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Claims resolveRefreshClaims(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_KEY, String.class))) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Long extractUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    public long getRemainingValidityMs(Claims claims) {
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 요청에서 토큰 추출
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ") && bearerToken.length() > 7) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public long getAccessTokenValidityMs() {
        return accessTokenValidityMs;
    }

    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }
}