package com.ahmedesawy.E_Wallet.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest {

  @NotNull(message = "Withdrawl amount is required")
  @DecimalMin(value = "0.01", message = "Withdrawl amount must be greater than zero")
  private BigDecimal amount;

  @NotBlank(message = "PIN is required")
  @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
  private String pin;

}
