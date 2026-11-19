package com.ahmedesawy.E_Wallet.service.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ahmedesawy.E_Wallet.dto.response.AdminWalletStatusResponse;
import com.ahmedesawy.E_Wallet.entity.Wallet;
import com.ahmedesawy.E_Wallet.enums.WalletStatus;
import com.ahmedesawy.E_Wallet.exception.ResourceNotFoundException;
import com.ahmedesawy.E_Wallet.repository.TransactionRepository;
import com.ahmedesawy.E_Wallet.repository.WalletRepository;
import com.ahmedesawy.E_Wallet.repository.projection.SuspiciousTransactionView;
import com.ahmedesawy.E_Wallet.security.RefreshTokenService;
import com.ahmedesawy.E_Wallet.security.TokenBlacklistService;
import com.ahmedesawy.E_Wallet.service.AdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final CacheManager cacheManager;
  private final RefreshTokenService refreshTokenService;
  private final TokenBlacklistService tokenBlacklistService;

  @Value("${jwt.access-token-expiration}")
  private long accessTokenExpirationMs;

  @Override
  @Transactional
  public AdminWalletStatusResponse updateWalletStatus(Long walletId, WalletStatus newStatus) {
    Wallet wallet = walletRepository.findByIdWithLock(walletId)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet bot found"));

    if (wallet.getStatus() == newStatus) {
      throw new IllegalArgumentException("Wallet is already in status: " + newStatus);
    }

    wallet.setStatus(newStatus);
    Long userId = wallet.getUser().getId();

    // Evict Redis wallet
    Cache cache = cacheManager.getCache("wallets");
    if (cache != null) {
      cache.evict(userId);
    }

    // When blocking the wallet
    if (newStatus == WalletStatus.BLOCKED) {
      refreshTokenService.revokeAllUserSessions(userId);
      tokenBlacklistService.revokeAllUserAccessTokens(userId, accessTokenExpirationMs);
      log.warn("Admin BLOCKED walletId={} (userId={}) and revoked all active sessions", walletId, userId);
    } else {
      log.info("Admin UNBLOCKED walletId={} (userId={})", walletId, userId);
    }

    return AdminWalletStatusResponse.builder()
        .walletId(walletId)
        .userId(userId)
        .status(wallet.getStatus())
        .message("Wallet status successfully updated to " + newStatus)
        .build();

    }

  @Override
  @Transactional(readOnly = true)
  public Page<SuspiciousTransactionView> getSuspiciousTransactions(BigDecimal highValueThreshold, Pageable pageable) {
    return transactionRepository.findSupiciousTransaction(highValueThreshold, pageable);
  }

}
