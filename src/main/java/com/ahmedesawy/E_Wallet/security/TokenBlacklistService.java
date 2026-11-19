package com.ahmedesawy.E_Wallet.security;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

  private final StringRedisTemplate redisTemplate;

  private static final String BLACKLIST_PREFIX = "blacklist_token:";
  private static final String USER_REVOKED_BEFORE_PREFIX = "user_revoked_before:";

  public void blacklistAccessToken(String jti, long remainingTtlMs) {
    if (jti != null && !jti.isBlank() && remainingTtlMs > 0) {
      redisTemplate.opsForValue().set(
          BLACKLIST_PREFIX + jti,
          "revoked",
          Duration.ofMillis(remainingTtlMs));
    }
  }

  public boolean isAccessTokenBlacklisted(String jti) {
    return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
  }

  public void revokeAllUserAccessTokens(Long userId, long accessTokenMaxTtlMs) {
    redisTemplate.opsForValue().set(
        USER_REVOKED_BEFORE_PREFIX + userId,
        String.valueOf(System.currentTimeMillis()),
        Duration.ofMillis(accessTokenMaxTtlMs));
  }

  public boolean isUserTokenRevoked(Long userId, Long loginAtMs) {
    if (userId == null || loginAtMs == null)
      return false;

    String revokedBeforeStr = redisTemplate.opsForValue().get(USER_REVOKED_BEFORE_PREFIX + userId);
    if (revokedBeforeStr == null)
      return false;

    return loginAtMs < Long.parseLong(revokedBeforeStr);
  }

}
