package com.globalict.iot_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public void blacklistToken(String token) {
        if (!jwtService.isTokenValid(token) && !jwtService.isRefreshTokenValid(token)) {
            return;
        }

        String jti = jwtService.extractJti(token);
        long ttl = jwtService.getRemainingExpiration(token);

        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "revoked",
                    ttl,
                    TimeUnit.MILLISECONDS
            );
            log.info("Token blacklisted: jti={}, ttl={}ms", jti, ttl);
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            String jti = jwtService.extractJti(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
        } catch (Exception e) {
            return false;
        }
    }
}
