package com.ahmedesawy.E_Wallet.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ahmedesawy.E_Wallet.enums.EntryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leader_entries")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class LedgerEntry {

  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "transaction_id", nullable = false)
  private Long transactionId; 

  @Column(name="wallet_id", nullable = false)
  private Long walletId;

  @Enumerated(EnumType.STRING)
  @Column(name = "entry_type", nullable = false, length = 10)
  private EntryType entryType;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @CreationTimestamp 
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

}