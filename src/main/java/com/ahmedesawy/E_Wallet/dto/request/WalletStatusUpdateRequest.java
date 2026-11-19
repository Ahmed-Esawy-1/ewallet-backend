package com.ahmedesawy.E_Wallet.dto.request;

import com.ahmedesawy.E_Wallet.enums.WalletStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatusUpdateRequest {

  @NotNull(message = "Wallet status is required (ACTIVE or BLOCKED)")
  private WalletStatus status;

}
