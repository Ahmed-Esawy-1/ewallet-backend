package com.ahmedesawy.E_Wallet.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

  @NotNull(message = "Deposit amount is required")
  @DecimalMin(value = "0.01", message = "Deposit amount must be greater than zero")
  private BigDecimal amount;

}
