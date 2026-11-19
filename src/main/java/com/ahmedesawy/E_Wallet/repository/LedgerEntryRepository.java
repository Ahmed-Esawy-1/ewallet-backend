package com.ahmedesawy.E_Wallet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmedesawy.E_Wallet.entity.LedgerEntry;
import com.ahmedesawy.E_Wallet.repository.projection.LedgerEntryView;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

  Page<LedgerEntryView> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable Pageable);

}
