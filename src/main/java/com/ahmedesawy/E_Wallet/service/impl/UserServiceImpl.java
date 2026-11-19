package com.ahmedesawy.E_Wallet.service.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ahmedesawy.E_Wallet.dto.request.LoginRequest;
import com.ahmedesawy.E_Wallet.dto.request.RefreshTokenRequest;
import com.ahmedesawy.E_Wallet.dto.request.RegisterRequest;
import com.ahmedesawy.E_Wallet.dto.response.AuthResponse;
import com.ahmedesawy.E_Wallet.dto.response.UserResponse;
import com.ahmedesawy.E_Wallet.entity.User;
import com.ahmedesawy.E_Wallet.entity.Wallet;
import com.ahmedesawy.E_Wallet.enums.WalletStatus;
import com.ahmedesawy.E_Wallet.exception.ResourceNotFoundException;
import com.ahmedesawy.E_Wallet.exception.UserAlreadyExistsException;
import com.ahmedesawy.E_Wallet.repository.UserRepository;
import com.ahmedesawy.E_Wallet.repository.WalletRepository;
import com.ahmedesawy.E_Wallet.security.JwtService;
import com.ahmedesawy.E_Wallet.security.RefreshTokenService;
import com.ahmedesawy.E_Wallet.security.TokenBlacklistService;
import com.ahmedesawy.E_Wallet.service.UserService;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final WalletRepository walletRepository;
  private final PasswordEncoder passwordEncoder;

  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final TokenBlacklistService tokenBlacklistService;
  private final AuthenticationManager authenticationManager;

  @Value("${app.default-currency:EGP}")
  private String defaultCurrency;

  @Value("${jwt.access-token-expiration}")
  private Long accessTokenExpirationMs;

  @Override
  @Transactional
  public UserResponse registerUser(RegisterRequest request) {
    String normalizedName = request.getName().trim();
    String normalizedEmail = request.getEmail().trim();
    String normalizedPhone = request.getPhone().trim();

    // Validation
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new UserAlreadyExistsException("Email '" + normalizedEmail + "' is already registered");
    }
    if (userRepository.existsByPhone(normalizedPhone)) {
      throw new UserAlreadyExistsException("Phone number '" + normalizedPhone + "' is already registered");
    }

    // Create User
    String normalizedPassword = passwordEncoder.encode(request.getPassword().trim());
    User user = User.builder()
        .name(normalizedName)
        .email(normalizedEmail)
        .password(normalizedPassword)
        .phone(normalizedPhone)
        .build();

    User savedUser = userRepository.save(user);

    // Create Wallet
    String normalizedPin = passwordEncoder.encode(request.getPin().trim());
    Wallet wallet = Wallet.builder()
        .user(user)
        .balance(BigDecimal.ZERO)
        .currency(defaultCurrency)
        .pinCode(normalizedPin)
        .status(WalletStatus.ACTIVE)
        .build();

    Wallet savedWallet = walletRepository.save(wallet);

    // return
    return UserResponse.builder()
        .userId(savedUser.getId())
        .name(savedUser.getName())
        .email(savedUser.getEmail())
        .phone(savedUser.getPhone())
        .walletId(savedWallet.getId())
        .walletBalance(savedWallet.getBalance())
        .walletStatus(savedWallet.getStatus().name())
        .createdAt(savedUser.getCreatedAt())
        .build();

  }

  @Override
  public AuthResponse login(LoginRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail().trim(),
            request.getPassword()));

    User user = (User) authentication.getPrincipal();

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = refreshTokenService.createRefreshToken(user.getId());

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .userId(user.getId())
        .email(user.getEmail())
        .role(user.getRole().name())
        .build();
  }

  @Override
  public AuthResponse refreshToken(RefreshTokenRequest request, String authHeader) {
    String oldRefreshToken = request.getRefreshToken().trim();

    // Validate refresh token and userId
    Long userId = refreshTokenService.validateAndGetUserId(oldRefreshToken);

    // Fetch user
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // New Token and Delete and blacklist old token
    refreshTokenService.deleteRefreshToken(oldRefreshToken);
    revokeCurrentAccessToken(authHeader);

    String newAccessToken = jwtService.generateAccessToken(user);
    String newRefreshToken = refreshTokenService.createRefreshToken(userId);

    return AuthResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .userId(user.getId())
        .email(user.getEmail())
        .role(user.getRole().name())
        .build();
  }

  @Override
  public void logout(RefreshTokenRequest request, String authHeader) {
    refreshTokenService.deleteRefreshToken(request.getRefreshToken().trim());
    revokeCurrentAccessToken(authHeader);
  }

  @Override
  public void logoutAllDevices(Long userId, String authHeader) {
    refreshTokenService.revokeAllUserSessions(userId);
    tokenBlacklistService.revokeAllUserAccessTokens(userId, accessTokenExpirationMs);
    revokeCurrentAccessToken(authHeader);
  }

  // ---- HELPERS -------
  private void revokeCurrentAccessToken(String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String accessToken = authHeader.substring(7).trim();
      try {
        String jti = jwtService.extractJti(accessToken);
        String blacklistKey = (jti != null && !jti.isBlank()) ? jti : accessToken;
        long remainingMs = jwtService.getRemainingEpirationMs(accessToken);
        tokenBlacklistService.blacklistAccessToken(blacklistKey, remainingMs);
      } catch (JwtException ignored) {
        // Accessto token already expired
      }
    }
  }

}
