package com.ahmedesawy.E_Wallet.service.impl;

import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ahmedesawy.E_Wallet.dto.request.DepositRequest;
import com.ahmedesawy.E_Wallet.dto.request.TransferRequest;
import com.ahmedesawy.E_Wallet.dto.request.UpdatePinRequest;
import com.ahmedesawy.E_Wallet.dto.request.WithdrawRequest;
import com.ahmedesawy.E_Wallet.dto.response.TransferResponse;
import com.ahmedesawy.E_Wallet.dto.response.WalletResponse;
import com.ahmedesawy.E_Wallet.entity.LedgerEntry;
import com.ahmedesawy.E_Wallet.entity.Transaction;
import com.ahmedesawy.E_Wallet.entity.Wallet;
import com.ahmedesawy.E_Wallet.enums.EntryType;
import com.ahmedesawy.E_Wallet.enums.TransactionStatus;
import com.ahmedesawy.E_Wallet.enums.TransactionType;
import com.ahmedesawy.E_Wallet.enums.WalletStatus;
import com.ahmedesawy.E_Wallet.exception.InsufficientBalanceException;
import com.ahmedesawy.E_Wallet.exception.InvalidPinException;
import com.ahmedesawy.E_Wallet.exception.ResourceNotFoundException;
import com.ahmedesawy.E_Wallet.mapper.WalletMapper;
import com.ahmedesawy.E_Wallet.repository.LedgerEntryRepository;
import com.ahmedesawy.E_Wallet.repository.TransactionRepository;
import com.ahmedesawy.E_Wallet.repository.WalletRepository;
import com.ahmedesawy.E_Wallet.repository.projection.LedgerEntryView;
import com.ahmedesawy.E_Wallet.repository.projection.TransactionHistoryView;
import com.ahmedesawy.E_Wallet.repository.projection.WalletIdView;
import com.ahmedesawy.E_Wallet.service.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final LedgerEntryRepository ledgerEntryRepository;

  private final PasswordEncoder passwordEncoder;
  private final WalletMapper walletMapper;
  private final CacheManager cacheManager;

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = "wallets", key = "#userId")
  public WalletResponse getWalletDetails(Long userId) {
    Wallet wallet = walletRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

    return walletMapper.mapToWalletResponse(wallet);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TransactionHistoryView> getTransactionHistory(Long userId, Pageable pageable) {
    Long walletId = walletRepository.findWalletIdByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    return transactionRepository.findWalletHistory(walletId, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LedgerEntryView> getLedgerHistory(Long userId, Pageable pageable) {
    Long walletId = walletRepository.findWalletIdByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    return ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
  }

  @Override
  @Transactional
  public void updatePin(Long userId, UpdatePinRequest request) {
    Wallet wallet = walletRepository.findByUserIdWithLock(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

    if (wallet.getStatus() != WalletStatus.ACTIVE) {
      throw new IllegalStateException("Wallet is not ACTIVE");
    }

    wallet.setPinCode(passwordEncoder.encode(request.getPin().trim()));
    evictWalletCache(userId);
  }

  @Override
  @Transactional
  @CacheEvict(value = "wallets", key = "#userId")
  public WalletResponse deposit(Long userId, DepositRequest request) {
    Wallet wallet = walletRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

    // Verify
    if (wallet.getStatus() != WalletStatus.ACTIVE) {
      throw new IllegalStateException("Wallet is BLOCKED");
    }

    // Update Wallet
    wallet.setBalance(wallet.getBalance().add(request.getAmount()));

    // Audit Transaction
    Transaction transaction = Transaction.builder()
        .sourceWalletId(null)
        .targetWalletId(wallet.getId())
        .amount(request.getAmount())
        .type(TransactionType.DEPOSIT)
        .status(TransactionStatus.SUCCESS)
        .build();

    transactionRepository.save(transaction);

    return walletMapper.mapToWalletResponse(wallet);
  }

  @Override
  @Transactional
  @CacheEvict(value = "wallets", key = "#userId")
  public WalletResponse withdraw(Long userId, WithdrawRequest request) {
    Wallet wallet = walletRepository.findByUserIdWithLock(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

    // Verify
    if (wallet.getPinCode() == null || !passwordEncoder.matches(request.getPin().trim(), wallet.getPinCode())) {
      throw new InvalidPinException("Invalid transaction PIN");
    }
    if (wallet.getStatus() != WalletStatus.ACTIVE) {
      throw new IllegalStateException("Wallet is BLOCKED");
    }
    if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
      throw new InsufficientBalanceException("Insufficient funds.");
    }

    // Update Wallet Balance
    wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));

    // Audit Transaction
    Transaction transaction = Transaction.builder()
        .sourceWalletId(wallet.getId())
        .targetWalletId(null)
        .amount(request.getAmount())
        .type(TransactionType.WITHDRAWAL)
        .status(TransactionStatus.SUCCESS)
        .build();

    transactionRepository.save(transaction);

    return walletMapper.mapToWalletResponse(wallet);
  }

  @Override
  @Transactional
  public TransferResponse transfer(Long senderUserId, TransferRequest request) {
    // Sender and Receiver
    Long senderWalletId = walletRepository.findWalletIdByUserId(senderUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

    WalletIdView receiverInfo = walletRepository.findWalletIdsByUserPhone(request.getReceiverPhone())
        .orElseThrow(() -> new ResourceNotFoundException("Recipient not found with phone"));

    Long receiverWalletId = receiverInfo.getId();

    // Prevent self-transfer
    if (senderWalletId.equals(receiverWalletId)) {
      throw new IllegalArgumentException("Cannot transfer money to your own wallet");
    }

    // Deterministic Lock Ordering (Always lock smaller walletId firstto prevent
    // deadlocks)
    Long firstLockId = Math.min(senderWalletId, receiverWalletId);
    Long secondLockId = Math.max(senderWalletId, receiverWalletId);

    Wallet firstLocked = walletRepository.findByIdWithLock(firstLockId)
        .orElseThrow(() -> new ResourceNotFoundException("wallet not found"));
    Wallet secondLocked = walletRepository.findByIdWithLock(secondLockId)
        .orElseThrow(() -> new ResourceNotFoundException("wallet not found"));

    Wallet senderWallet = firstLocked.getId().equals(senderWalletId) ? firstLocked : secondLocked;
    Wallet receiverWallet = firstLocked.getId().equals(receiverWalletId) ? firstLocked : secondLocked;

    // Validate Wallet Status (Must be ACTIVE)
    if (senderWallet.getStatus() != WalletStatus.ACTIVE || receiverWallet.getStatus() != WalletStatus.ACTIVE) {
      throw new IllegalStateException("Both sender and receiver wallets must be ACTIVE");
    }

    // Verify Sender PIN
    if (senderWallet.getPinCode() == null ||
        !passwordEncoder.matches(request.getPin().trim(), senderWallet.getPinCode())) {
      throw new InvalidPinException("Invalid transaction PIN");
    }

    // Verify Sufficient Balance
    if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
      throw new InsufficientBalanceException("Insufficient funds");
    }

    // Update Wallets Balance
    senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
    receiverWallet.setBalance(receiverWallet.getBalance().add(request.getAmount()));

    // Save Transaction
    Transaction transaction = Transaction.builder()
        .sourceWalletId(senderWalletId)
        .targetWalletId(receiverWalletId)
        .amount(request.getAmount())
        .type(TransactionType.TRANSFER)
        .status(TransactionStatus.SUCCESS)
        .build();
    Transaction savedTransaction = transactionRepository.save(transaction);

    // Recored Double-Entry Ledger (DEPIT for sender, CREDIT for receiver)
    LedgerEntry debitEntry = LedgerEntry.builder()
        .transactionId(savedTransaction.getId())
        .walletId(senderWalletId)
        .entryType(EntryType.DEBIT)
        .amount(request.getAmount())
        .build();

    LedgerEntry creditEntry = LedgerEntry.builder()
        .transactionId(savedTransaction.getId())
        .walletId(receiverWalletId)
        .entryType(EntryType.CREDIT)
        .amount(request.getAmount())
        .build();

    ledgerEntryRepository.saveAll(List.of(debitEntry, creditEntry));

    // Evict Redis cache for Both Sender and receiver
    evictWalletCache(senderUserId);
    evictWalletCache(receiverWallet.getUser().getId());

    return TransferResponse.builder()
        .transactionId(savedTransaction.getId())
        .senderWalletId(senderWalletId)
        .receiverWalletId(receiverWalletId)
        .receiverPhone(request.getReceiverPhone())
        .amount(request.getAmount())
        .senderNewBalance(senderWallet.getBalance())
        .status(savedTransaction.getStatus().name())
        .timeStamp(savedTransaction.getCreatedAt())
        .build();
  }

  // ---- HELPERS ---------
  private void evictWalletCache(Long userId) {
    Cache cache = cacheManager.getCache("wallets");
    if (cache != null) {
      ;
      cache.evict(userId);
    }
  }

}
