package com.ahmedesawy.E_Wallet.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ahmedesawy.E_Wallet.enums.WalletStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(nullable = false, length = 3)
  @Builder.Default
  private String currency = "EGP";

  @Column(name = "pin_code", nullable = false)
  private String pinCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private WalletStatus status = WalletStatus.ACTIVE;

  @CreationTimestamp
  private LocalDateTime createdAt;

}