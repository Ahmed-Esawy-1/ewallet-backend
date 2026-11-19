package com.ahmedesawy.E_Wallet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class UserResponse {
  private Long userId;
  private String name;
  private String email;
  private String phone;
  private Long walletId;
  private BigDecimal walletBalance;
  private String walletStatus;
  private LocalDateTime createdAt;
}
