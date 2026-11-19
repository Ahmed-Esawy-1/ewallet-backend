package com.ahmedesawy.E_Wallet.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ahmedesawy.E_Wallet.enums.EntryType;

public interface LedgerEntryView {
  Long getId();
  Long getTransactionId();
  EntryType getEntryType();
  BigDecimal getAmount();
  LocalDateTime getCreatedAt();
}
