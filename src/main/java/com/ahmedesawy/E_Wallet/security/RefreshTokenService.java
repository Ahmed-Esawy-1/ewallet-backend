package com.ahmedesawy.E_Wallet.security;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final StringRedisTemplate redisTemplate;

  @Value("${jwt.refresh-token-expiration}")
  private long refreshTokenExpirationMs;

  private static final String REFRESH_PREFIX = "refresh_token:";
  private static final String USER_SESSIONS_PREFIX = "user_sessions:";

  // Create Token
  public String createRefreshToken(Long userId) {
    String token = UUID.randomUUID().toString();
    Duration ttl = Duration.ofMillis(refreshTokenExpirationMs);

    // Store token -> userId
    redisTemplate.opsForValue().set(
        REFRESH_PREFIX + token,
        String.valueOf(userId),
        ttl);

    // Track token for user'sactive sessions
    String userKey = USER_SESSIONS_PREFIX + userId;
    redisTemplate.opsForSet().add(userKey, token);
    redisTemplate.expire(userKey, ttl);

    return token;
  }

  // Get Id After Validate
  public Long validateAndGetUserId(String token) {
    String userIdStr = redisTemplate.opsForValue().get(REFRESH_PREFIX + token);
    if (userIdStr == null) {
      throw new IllegalArgumentException("Invalid or expired refresh token");
    }
    return Long.parseLong(userIdStr);
  }

  // Delete From Redis
  public void deleteRefreshToken(String token) {
    String cleanToken = token.trim();
    String userIdStr = redisTemplate.opsForValue().get(REFRESH_PREFIX + cleanToken);

    redisTemplate.delete(REFRESH_PREFIX + cleanToken);
    if (userIdStr != null) {
      redisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + userIdStr, cleanToken);
    }
  }

  public void revokeAllUserSessions(Long userId) {
    String userSessionKey = USER_SESSIONS_PREFIX + userId;
    Set<String> tokens = redisTemplate.opsForSet().members(userSessionKey);
    if (tokens != null && !tokens.isEmpty()) {
      for (String token : tokens) {
        redisTemplate.delete(REFRESH_PREFIX + token);
      }
    }
    redisTemplate.delete(userSessionKey);
  }

}
