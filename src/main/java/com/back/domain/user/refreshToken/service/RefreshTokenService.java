package com.back.domain.user.refreshToken.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.back.domain.user.refreshToken.entity.RefreshToken;
import com.back.domain.user.refreshToken.repository.RefreshTokenRepository;
import com.back.global.config.security.jwt.JwtProperties;
import com.back.global.exception.AuthException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final JwtProperties jwtProperties;
    private Duration refreshTokenExpire;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void saveRefreshToken(Long userId, String token) {
        long ttlSeconds = jwtProperties.getRefreshExp().toSeconds();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiration(ttlSeconds) // Redis TTL은 초 단위이므로 Duration을 초로 변환
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken getRefreshTokenByUserId(Long userId) {
        return refreshTokenRepository.findById(userId)
                .orElseThrow(() -> new AuthException("401-2", "리프레시 토큰이 존재하지 않습니다."));
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findRefreshTokenByUserId(Long userId) {
        return refreshTokenRepository.findById(userId);
    }

    @Transactional
    public void deleteRefreshTokenByUserId(Long userId) {
        refreshTokenRepository.deleteById(userId);
    }

}
