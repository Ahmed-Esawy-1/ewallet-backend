package com.ahmedesawy.E_Wallet.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ahmedesawy.E_Wallet.enums.TransactionStatus;
import com.ahmedesawy.E_Wallet.enums.TransactionType;

public interface SuspiciousTransactionView {
  Long getId();
  Long getSourceWalletId();
  Long getTargetWalletId();
  BigDecimal getAmount();
  TransactionType getType();
  TransactionStatus getStatus();
  LocalDateTime getCreatedAt();
}
