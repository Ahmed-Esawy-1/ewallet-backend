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
public class TransferResponse {
  private Long transactionId;
  private Long senderWalletId;
  private Long receiverWalletId;
  private String receiverPhone;
  private BigDecimal amount;
  private BigDecimal senderNewBalance;
  private String status;
  private LocalDateTime timeStamp; 
}
