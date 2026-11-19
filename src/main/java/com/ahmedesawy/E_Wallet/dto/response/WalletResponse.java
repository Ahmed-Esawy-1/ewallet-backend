package com.ahmedesawy.E_Wallet.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse implements Serializable {
  private Long walletId;
  private Long userId;
  private BigDecimal balance;
  private String currency;
  private String status;
}
