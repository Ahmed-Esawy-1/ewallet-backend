package com.ahmedesawy.E_Wallet.security;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.ahmedesawy.E_Wallet.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

  @Value("${jwt.secret}")
  private String secretKey;

  @Value("${jwt.access-token-expiration}")
  private long accessTokenExpiration;

  public String generateAccessToken(User user) {
    Map<String, Object> extraClaims = Map.of(
        "userId", user.getId(),
        "role", user.getRole().name(),
        "loginAtMs", System.currentTimeMillis());

    return Jwts.builder()
        .claims(extraClaims)
        .subject(user.getEmail())
        .id(UUID.randomUUID().toString())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(getSignInKey())
        .compact();
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  // ---- CLAIMS -----
  public String extractJti(String token) {
    return extractClaim(token, claims -> claims.getId());
  }

  public Long extractUserId(String token) {
    return extractClaim(token, claims -> {
      Number val = claims.get("userId", Long.class);
      return val != null ? val.longValue() : null;
    });
  }

  public String extractUsername(String token) {
    return extractClaim(token, claims -> claims.getSubject());
  }

  public Long extractLoginAtMs(String token) {
    return extractClaim(token, claims -> {
      Number val = claims.get("loginAtMs", Long.class);
      return val != null ? val.longValue() : null;
    });
  }

  public Date extractIssuedAt(String token) {
    return extractClaim(token, claims -> claims.getIssuedAt());
  }

  public Date extractExpiration(String token) {
    // return extractClaim(token, Claims::getExpiration);
    return extractClaim(token, claims -> claims.getExpiration());
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSignInKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername())) &&
        !isTokenExpired(token);
  }

  public boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public long getRemainingEpirationMs(String token) {
    Date expiration = extractExpiration(token);
    return Math.max(0, expiration.getTime() - System.currentTimeMillis());
  }

}
