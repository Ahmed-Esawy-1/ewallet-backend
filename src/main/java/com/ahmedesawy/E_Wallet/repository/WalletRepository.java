package com.ahmedesawy.E_Wallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ahmedesawy.E_Wallet.entity.Wallet;
import com.ahmedesawy.E_Wallet.repository.projection.WalletIdView;

import jakarta.persistence.LockModeType;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

  Optional<Wallet> findByUserId(Long userId);

  @Query("SELECT w.id FROM Wallet w WHERE w.user.id = :userId")
  Optional<Long> findWalletIdByUserId(Long userId);

  @Query("SELECT w.id AS id, w.user.id AS userId FROM Wallet w WHERE w.user.phone = :phone")
  Optional<WalletIdView> findWalletIdsByUserPhone(@Param("phone") String phone);

  // Pessimistic Lock by User ID (used in Deposit & Withdraw)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
  Optional<Wallet> findByUserIdWithLock(@Param("userId") Long userId);
  
  // Pessimistic Lock by Wallet ID (used in P2P Transfer)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query ("SELECT w FROM Wallet w WHERE w.id = :walletId")
  Optional<Wallet> findByIdWithLock(@Param("walletId") Long walletId);
}
