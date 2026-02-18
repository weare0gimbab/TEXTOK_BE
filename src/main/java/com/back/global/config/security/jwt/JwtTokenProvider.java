package com.back.global.config.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.back.domain.user.user.entity.UserRole;
import com.back.global.config.security.SecurityUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessExp());

        Claims claims = Jwts.claims()
                .subject(String.valueOf(userId))
                .add("role", "ROLE_" + role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .build();

        return Jwts.builder()
                .claims(claims)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getRefreshExp());

        Claims claims = Jwts.claims()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .build();

        return Jwts.builder()
                .claims(claims)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public SecurityUser parseUserFromAccessToken(String token) {
        Claims claims = parseClaims(token);
        Long userId = Long.parseLong(claims.getSubject());

        String roleStr = claims.get("role", String.class);
        UserRole role = UserRole.valueOf(roleStr.replace("ROLE_", ""));

        return new SecurityUser(userId, role);
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // 임시 토큰 생성 (소셜 로그인 후 추가 정보 입력용)
    public String generateTemporaryToken(Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMinutes(5));

        return Jwts.builder()
                .subject(String.valueOf(userId)) // (최신 API)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("typ", "TEMP") // 구분용 (선택이지만 추천)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}