package com.ahmedesawy.E_Wallet.dto.response;

import com.ahmedesawy.E_Wallet.enums.WalletStatus;

import lombok.Builder;
import lombok.Data;

@Data 
@Builder 
public class AdminWalletStatusResponse {
  private Long walletId;
  private Long userId;
  private WalletStatus status;
  private String message;
}
