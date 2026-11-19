package com.ahmedesawy.E_Wallet.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ahmedesawy.E_Wallet.dto.request.DepositRequest;
import com.ahmedesawy.E_Wallet.dto.request.TransferRequest;
import com.ahmedesawy.E_Wallet.dto.request.UpdatePinRequest;
import com.ahmedesawy.E_Wallet.dto.request.WithdrawRequest;
import com.ahmedesawy.E_Wallet.dto.response.TransferResponse;
import com.ahmedesawy.E_Wallet.dto.response.WalletResponse;
import com.ahmedesawy.E_Wallet.repository.projection.LedgerEntryView;
import com.ahmedesawy.E_Wallet.repository.projection.TransactionHistoryView;

public interface WalletService {
  WalletResponse getWalletDetails(Long userId);
  void updatePin(Long userId, UpdatePinRequest request);
  WalletResponse deposit(Long userId, DepositRequest request);
  WalletResponse withdraw(Long userId, WithdrawRequest request);
  TransferResponse transfer(Long senderUserId, TransferRequest request);

  // View Transaction & Ledger History
  Page<TransactionHistoryView> getTransactionHistory(Long userId, Pageable pageable);
  Page<LedgerEntryView> getLedgerHistory(Long userId, Pageable pageable);
}

