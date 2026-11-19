package com.ahmedesawy.E_Wallet.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ahmedesawy.E_Wallet.dto.response.AdminWalletStatusResponse;
import com.ahmedesawy.E_Wallet.enums.WalletStatus;
import com.ahmedesawy.E_Wallet.repository.projection.SuspiciousTransactionView;

public interface AdminService {
  AdminWalletStatusResponse updateWalletStatus(Long walletId, WalletStatus newStatus);
  Page<SuspiciousTransactionView> getSuspiciousTransactions(BigDecimal highValueThreshold, Pageable pageable);
}
