package com.ahmedesawy.E_Wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ahmedesawy.E_Wallet.dto.request.DepositRequest;
import com.ahmedesawy.E_Wallet.dto.request.TransferRequest;
import com.ahmedesawy.E_Wallet.dto.request.UpdatePinRequest;
import com.ahmedesawy.E_Wallet.dto.request.WithdrawRequest;
import com.ahmedesawy.E_Wallet.dto.response.TransferResponse;
import com.ahmedesawy.E_Wallet.dto.response.WalletResponse;
import com.ahmedesawy.E_Wallet.entity.LedgerEntry;
import com.ahmedesawy.E_Wallet.entity.Transaction;
import com.ahmedesawy.E_Wallet.entity.User;
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
import com.ahmedesawy.E_Wallet.service.impl.WalletServiceImpl;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest {

  @Mock
  private WalletRepository walletRepository;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private LedgerEntryRepository ledgerEntryRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private WalletMapper walletMapper;
  @Mock
  private CacheManager cacheManager;
  @Mock
  private Cache walletsCache;
  @Mock
  private TransactionHistoryView txView;
  @Mock
  private LedgerEntryView ledgerView;
  @Mock
  private WalletIdView receiverInfo;

  @InjectMocks
  private WalletServiceImpl walletSerive;

  private Wallet wallet;
  private Wallet receiverWallet;
  private WalletResponse walletResponse;

  private Long userId = 1L;
  private Long wrongUserId = 99L;

  private Long walletId = 10L;
  private Long receiverWalletId = 20L;

  private Pageable pageable = PageRequest.of(0, 10);

  // Before Start
  @BeforeEach
  void setUp() {
    User user = User.builder()
        .id(userId)
        .name("Ahmed")
        .phone("0123456789")
        .build();

    User receiverUser = User.builder()
        .id(2L)
        .name("mohamed")
        .phone("01099999999")
        .build();

    wallet = Wallet.builder()
        .id(walletId)
        .user(user)
        .balance(new BigDecimal("250.00"))
        .currency("EGP")
        .pinCode("$2a$10$TvPDwFMkWJKorHurlb/U1OZQscA2FoZURQRfKy46z/LKSjYZowQhu")
        .status(WalletStatus.ACTIVE)
        .build();

    receiverWallet = Wallet.builder()
        .id(receiverWalletId)
        .user(receiverUser)
        .balance(new BigDecimal("100.00"))
        .currency("EGP")
        .pinCode("$2a$10$TvPDwFMkWJKorHurlb/U1OZQscA2FoZURQRfKy46z/LKSjYZowQhu")
        .status(WalletStatus.ACTIVE)
        .build();

    walletResponse = WalletResponse.builder()
        .walletId(walletId)
        .userId(userId)
        .balance(new BigDecimal("250.00"))
        .currency("EGP")
        .status("ACTIVE")
        .build();
  }

  // ---- getWalletDetails(Long userId) ------
  @Test
  @DisplayName("getWalletDetails: Should return WalletResponse when wallet exists")
  void getWalletDetails_Success() {
    // Arrange
    when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
    when(walletMapper.mapToWalletResponse(wallet)).thenReturn(walletResponse);

    // Act
    WalletResponse result = walletSerive.getWalletDetails(1L);

    // Assert
    assertNotNull(result);
    assertEquals(walletId, result.getWalletId());
    assertEquals(new BigDecimal("250.00"), result.getBalance());
    verify(walletRepository, times(1)).findByUserId(userId);
    verify(walletMapper, times(1)).mapToWalletResponse(wallet);
  }

  @Test
  @DisplayName("getWalletDetails: Should throw ResourceNotFoundException when wallet not found")
  void getWalletDetails_WalletNotFound_ThrowsException() {
    // Arrange
    when(walletRepository.findByUserId(wrongUserId)).thenReturn(Optional.empty());

    // Act
    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
        () -> walletSerive.getWalletDetails(wrongUserId));

    // Assert
    assertEquals("Wallet not found", ex.getMessage());
    verify(walletMapper, never()).mapToWalletResponse(any());
  }

  // ---- getTransactionHistory(Long userId, Pageable pageable) ----
  @Test
  @DisplayName("getTransactionHistory: Should return paginated TransactionHistoryView when wallet exists")
  void getTransactionHistory_Success() {
    // Arrange
    Page<TransactionHistoryView> expectedPage = new PageImpl<>(List.of(txView), pageable, 1);

    when(walletRepository.findWalletIdByUserId(userId)).thenReturn(Optional.of(walletId));
    when(transactionRepository.findWalletHistory(walletId, pageable)).thenReturn(expectedPage);

    // Act
    Page<TransactionHistoryView> result = walletSerive.getTransactionHistory(userId, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    verify(walletRepository, times(1)).findWalletIdByUserId(userId);
    verify(transactionRepository, times(1)).findWalletHistory(walletId, pageable);
  }

  @Test
  @DisplayName("getTransactionHistory: Should throw ResourceNotFoundException when wallet does not exist")
  void getTransactionHistory_WalletNotFound_ThrowsException() {
    // Arrange
    when(walletRepository.findWalletIdByUserId(wrongUserId)).thenReturn(Optional.empty());

    // Act
    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
        () -> walletSerive.getTransactionHistory(wrongUserId, pageable));

    // Assert
    assertEquals("Wallet not found", ex.getMessage());
    verify(transactionRepository, never()).findWalletHistory(any(), any());
  }

  // ---- getLedgerHistory(Long userId, Pageable pageable) ----
  @Test
  @DisplayName("getLedgerHistory: Should return paginated LedgerEntryView when wallet exists")
  void getLedgerHistory_Success() {
    // Arrange
    Page<LedgerEntryView> expectedPage = new PageImpl<>(List.of(ledgerView), pageable, 1);

    when(walletRepository.findWalletIdByUserId(userId)).thenReturn(Optional.of(walletId));
    when(ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable)).thenReturn(expectedPage);

    // Act
    Page<LedgerEntryView> result = walletSerive.getLedgerHistory(userId, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    verify(walletRepository, times(1)).findWalletIdByUserId(userId);
    verify(ledgerEntryRepository, times(1)).findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
  }

  @Test
  @DisplayName("getLedgerHistory: Should throw ResourceNotFoundException when wallet does not exist")
  void getLedgerHistory_WalletNotFound_ThrowsException() {
    // Arrange
    when(walletRepository.findWalletIdByUserId(wrongUserId)).thenReturn(Optional.empty());

    // Act
    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
        () -> walletSerive.getLedgerHistory(wrongUserId, pageable));

    // Assert
    assertEquals("Wallet not found", ex.getMessage());
    verify(ledgerEntryRepository, never()).findByWalletIdOrderByCreatedAtDesc(any(), any());
  }

  // ---- UpdatePin(Long userId, UpdatePinRequest request) ----
  @Test
  @DisplayName("updatePin: Should encode trimmed PIN, update wallet, and evict cache when ACTIVE")
  void updatePin_Success() {
    // Arrange
    UpdatePinRequest request = new UpdatePinRequest("5678 ");
    when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
    when(passwordEncoder.encode("5678")).thenReturn("$2a$10$8osY6BbhuRn8IO2JJHi59.fM.pZYWGoqSUALFgstovRjwOUBQx.Gq");
    when(cacheManager.getCache("wallets")).thenReturn(walletsCache);

    // Act
    walletSerive.updatePin(userId, request);

    // Assert
    assertEquals("$2a$10$8osY6BbhuRn8IO2JJHi59.fM.pZYWGoqSUALFgstovRjwOUBQx.Gq", wallet.getPinCode());
    verify(passwordEncoder, times(1)).encode("5678");
    verify(walletsCache, times(1)).evict(1L);
  }

  @Test
  @DisplayName("updatePin: Should throw ResourceNotFoundException when wallet does not exist")
  void updatePin_WalletNotFound_ThrowsExceptions() {
    // Arrange
    UpdatePinRequest request = new UpdatePinRequest(" 5678");
    when(walletRepository.findByUserIdWithLock(wrongUserId)).thenReturn(Optional.empty());

    // Act
    ResourceNotFoundException ex = assertThrows(
        ResourceNotFoundException.class,
        () -> walletSerive.updatePin(wrongUserId, request));

    // Assert
    assertEquals("Wallet not found", ex.getMessage());
    verify(passwordEncoder, never()).encode(any());
    verify(walletsCache, never()).evict(any());
  }

  @Test
  @DisplayName("\"updatePin: Should throw IllegalStateException when wallet is BLOCKED")
  void updatePin_BlockedWallet_ThrowsExceptions() {
    // Arrange
    wallet.setStatus(WalletStatus.BLOCKED);
    UpdatePinRequest request = new UpdatePinRequest("5678");
    when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));

    // Act
    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> walletSerive.updatePin(userId, request));

    // Assert
    assertEquals("Wallet is not ACTIVE", ex.getMessage());
    assertEquals("$2a$10$TvPDwFMkWJKorHurlb/U1OZQscA2FoZURQRfKy46z/LKSjYZowQhu", wallet.getPinCode());
    verify(passwordEncoder, never()).encode(any());
    verify(walletsCache, never()).evict(any());
  }

  // ---- deposit(Long userId, DepositRequest request) -----
  @Test
  @DisplayName("deposit: Should increase balance, save DEPOSIT transaction, and return WalletResponse")
  void deposit_Success() {
    // Arrange
    DepositRequest request = new DepositRequest(new BigDecimal("150.00"));
    walletResponse.setBalance(new BigDecimal("400.00"));
    when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
    when(walletMapper.mapToWalletResponse(wallet)).thenReturn(walletResponse);

    // Act
    WalletResponse result = walletSerive.deposit(userId, request);

    // Assert
    assertNotNull(result);
    assertEquals(new BigDecimal("400.00"), wallet.getBalance());
    assertEquals(new BigDecimal("400.00"), result.getBalance());

    // Capture the exact Transaction passed
    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository, timeout(1)).save(txCaptor.capture());

    Transaction savedTx = txCaptor.getValue();
    assertNull(savedTx.getSourceWalletId());
    assertEquals(walletId, savedTx.getTargetWalletId());
    assertEquals(new BigDecimal("150.00"), savedTx.getAmount());
    assertEquals(TransactionType.DEPOSIT, savedTx.getType());
    assertEquals(TransactionStatus.SUCCESS, savedTx.getStatus());
  }

  @Test
  @DisplayName("deposit: Should throw ResourceNotFoundException when wallet does not exist")
  void deposit_WalletNotFound_ThrowsException() {
    // Arrange
    DepositRequest request = new DepositRequest(new BigDecimal("150.00"));
    when(walletRepository.findByUserId(wrongUserId)).thenReturn(Optional.empty());

    // Act
    ResourceNotFoundException ex = assertThrows(
        ResourceNotFoundException.class,
        () -> walletSerive.deposit(wrongUserId, request));

    // Assert
    assertEquals("Wallet not found", ex.getMessage());
    verify(transactionRepository, never()).save(any());
    verify(walletMapper, never()).mapToWalletResponse(any());
  }

  @Test
  @DisplayName("deposit: Should throw IllegalStateException when wallet is BLOCKED")
  void deposit_BlockedWallet_ThrowsException() {
    // Arrange
    wallet.setStatus(WalletStatus.BLOCKED);
    DepositRequest request = new DepositRequest(new BigDecimal("150.00"));
    when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

    // Act
    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> walletSerive.deposit(userId, request));

    // Assert
    assertEquals("Wallet is BLOCKED", ex.getMessage());
    assertEquals(new BigDecimal("250.00"), wallet.getBalance());
    verify(transactionRepository, never()).save(any());
  }

  // ---- withdraw(Long userId, WithdrawRequest request) ----
  @Test
  @DisplayName("withdraw: Should deduct balance, save WITHDRAWAL transaction, and return WalletResponse")
  void withdraw_Success() {
    // Arrange
    WithdrawRequest request = new WithdrawRequest(new BigDecimal("150.00"), "1234");
    walletResponse.setBalance(new BigDecimal("100.00"));
    when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
    when(passwordEncoder.matches("1234", wallet.getPinCode())).thenReturn(true);
    when(walletMapper.mapToWalletResponse(wallet)).thenReturn(walletResponse);

    // Act
    WalletResponse result = walletSerive.withdraw(userId, request);

    // Assert
    assertNotNull(result);
    assertEquals(new BigDecimal("100.00"), wallet.getBalance());
    assertEquals(new BigDecimal("100.00"), result.getBalance());

    ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository, times(1)).save(txCaptor.capture());

    Transaction savedTx = txCaptor.getValue();
    assertEquals(walletId, savedTx.getSourceWalletId());
    assertNull(savedTx.getTargetWalletId());
    assertEquals(new BigDecimal("150.00"), savedTx.getAmount());
    assertEquals(TransactionType.WITHDRAWAL, savedTx.getType());
    assertEquals(TransactionStatus.SUCCESS, savedTx.getStatus());
  }

  @Test
  @DisplayName("withdraw: Should throw ResourceNotFoundException when wallet does not exist")
  void withdraw_WalletNotFound_ThrowsExceptions() {
    // Arrange
    WithdrawRequest request = new WithdrawRequest(new BigDecimal("150.00"), "1234");
    when(walletRepository.findByUserIdWithLock(wrongUserId)).thenReturn(Optional.empty());

    // Act
    ResourceNotFoundException ex = assertThrows(
        ResourceNotFoundException.class,
        () -> walletSerive.withdraw(wrongUserId, request));

    // Assert
    assertEquals("Wallet not found", ex.getMessage());
    verify(passwordEncoder, never()).matches(any(), any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("withdraw: Should throw IllegalStateException when wallet is BLOCKED")
  void withdraw_BlockedWallet_ThrowsExceptions() {
    // Arrange
    wallet.setStatus(WalletStatus.BLOCKED);
    WithdrawRequest request = new WithdrawRequest(new BigDecimal("150.00"), "1234");
    when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
    when(passwordEncoder.matches("1234", wallet.getPinCode())).thenReturn(true);

    // Act
    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> walletSerive.withdraw(userId, request));

    // Assert
    assertEquals("Wallet is BLOCKED", ex.getMessage());
    assertEquals(new BigDecimal("250.00"), wallet.getBalance());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("withdraw: Should throw InvalidPinException when PIN does not match")
  void withdraw_InvalidPin_ThrowsException() {
    // Arrange
    WithdrawRequest request = new WithdrawRequest(new BigDecimal("150.00"), "9999");
    when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
    when(passwordEncoder.matches("9999", wallet.getPinCode())).thenReturn(false);

    // Act
    InvalidPinException ex = assertThrows(
        InvalidPinException.class,
        () -> walletSerive.withdraw(userId, request));

    // Assert
    assertEquals("Invalid transaction PIN", ex.getMessage());
    assertEquals(new BigDecimal("250.00"), wallet.getBalance());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("withdraw: Should throw InsufficientBalanceException when balance is less than amount")
  void withdraw_InsufficientBalance_ThrowsException() {
    // Arrange
    WithdrawRequest request = new WithdrawRequest(new BigDecimal("1500.00"), "1234");
    when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
    when(passwordEncoder.matches("1234", wallet.getPinCode())).thenReturn(true);

    // Act
    assertThrows(
        InsufficientBalanceException.class,
        () -> walletSerive.withdraw(userId, request));

    // Assert
    assertEquals(new BigDecimal("250.00"), wallet.getBalance());
    verify(transactionRepository, never()).save(any());
  }

  // ---- transfer(Long senderUserId, TransferRequest request) -----
  @Test
  @DisplayName("transfer: Should move funds, save DEBIT & CREDIT ledger entries, and evict both caches")
  void transfer_Success() {
    // Arrange
    TransferRequest request = new TransferRequest(
        "01099999999",
        new BigDecimal("150.00"),
        "1234");

    when(receiverInfo.getId()).thenReturn(receiverWalletId);

    when(walletRepository.findWalletIdByUserId(userId)).thenReturn(Optional.of(walletId));
    when(walletRepository.findWalletIdsByUserPhone("01099999999")).thenReturn(Optional.of(receiverInfo));

    // Deterministic Lock Ordering
    when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
    when(walletRepository.findByIdWithLock(receiverWalletId)).thenReturn(Optional.of(receiverWallet));

    when(passwordEncoder.matches("1234", wallet.getPinCode())).thenReturn(true);

    // Simulate DB assigning ID = 50L when transactionRepository.save() is called
    when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
      Transaction tx = invocation.getArgument(0);
      tx.setId(50L);
      return tx;
    });
    when(cacheManager.getCache("wallets")).thenReturn(walletsCache);

    // Act
    TransferResponse response = walletSerive.transfer(userId, request);

    // Assert
    assertNotNull(response);
    assertEquals(50L, response.getTransactionId());
    assertEquals(walletId, response.getSenderWalletId());
    assertEquals(receiverWalletId, response.getReceiverWalletId());
    assertEquals("01099999999", response.getReceiverPhone());
    assertEquals(new BigDecimal("100.00"), wallet.getBalance());
    assertEquals(new BigDecimal("100.00"), response.getSenderNewBalance());
    assertEquals(new BigDecimal("250.00"), receiverWallet.getBalance());
    assertEquals("SUCCESS", response.getStatus());

    // Double Entry Ledger (DEPIT & CREDIT)
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
    verify(ledgerEntryRepository, times(1)).saveAll(ledgerCaptor.capture());

    List<LedgerEntry> savedEntries = ledgerCaptor.getValue();
    assertEquals(2, savedEntries.size());
    // Debit
    assertTrue(
        savedEntries.stream().anyMatch(e -> e.getTransactionId().equals(50L) &&
            e.getWalletId().equals(walletId) &&
            e.getEntryType() == EntryType.DEBIT &&
            e.getAmount().compareTo(new BigDecimal("150.00")) == 0));
    // Credit
    assertTrue(
        savedEntries.stream().anyMatch(e -> e.getTransactionId().equals(50L) &&
            e.getWalletId().equals(receiverWalletId) &&
            e.getEntryType() == EntryType.CREDIT &&
            e.getAmount().compareTo(new BigDecimal("150.00")) == 0));

    // Verify redis cache evicted for sender(userId) and receiver(2L)
    verify(walletsCache, times(1)).evict(userId);
    verify(walletsCache, times(1)).evict(2L);
  }

  @Test
  @DisplayName("transfer: Should throw IllegalArgumentException when transferring to own wallet")
  void transfer_SelfTransfer_ThrowsException() {
    // Arrange
    TransferRequest request = new TransferRequest(
        "0123456789", // same User's Phone
        new BigDecimal("100.00"),
        "1234");
    WalletIdView sameWalletInfo = mock(WalletIdView.class);
    when(sameWalletInfo.getId()).thenReturn(walletId);

    when(walletRepository.findWalletIdByUserId(userId)).thenReturn(Optional.of(walletId));
    when(walletRepository.findWalletIdsByUserPhone("0123456789")).thenReturn(Optional.of(sameWalletInfo));

    // Act
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> walletSerive.transfer(userId, request));

    // Assert
    assertEquals("Cannot transfer money to your own wallet", ex.getMessage());
    verify(walletRepository, never()).findByIdWithLock(any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("transfer: Should throw IllegalStateException when receiver wallet is BLOCKED")
  void transfer_ReceiverBlocked_ThrowsException() {
    // Arrange
    receiverWallet.setStatus(WalletStatus.BLOCKED);
    TransferRequest request = new TransferRequest(
        "01099999999",
        new BigDecimal("100.00"),
        "1234");

    when(receiverInfo.getId()).thenReturn(receiverWalletId);

    when(walletRepository.findWalletIdByUserId(userId)).thenReturn(Optional.of(walletId));
    when(walletRepository.findWalletIdsByUserPhone("01099999999")).thenReturn(Optional.of(receiverInfo));

    // Deterministic Lock Ordering
    when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
    when(walletRepository.findByIdWithLock(receiverWalletId)).thenReturn(Optional.of(receiverWallet));

    // Act
    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> walletSerive.transfer(userId, request));

    // Assert
    assertEquals("Both sender and receiver wallets must be ACTIVE", ex.getMessage());
    verify(passwordEncoder, never()).matches(any(), any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("transfer: Should throw InvalidPinException when sender PIN is wrong")
  void transfer_InvalidPin_ThrowsException() {
    // Arrange
    TransferRequest request = new TransferRequest(
        "01099999999",
        new BigDecimal("100.00"),
        "9999");

    when(receiverInfo.getId()).thenReturn(receiverWalletId);

    when(walletRepository.findWalletIdByUserId(userId)).thenReturn(Optional.of(walletId));
    when(walletRepository.findWalletIdsByUserPhone("01099999999")).thenReturn(Optional.of(receiverInfo));

    // Deterministic Lock Ordering
    when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
    when(walletRepository.findByIdWithLock(receiverWalletId)).thenReturn(Optional.of(receiverWallet));

    when(passwordEncoder.matches("9999", wallet.getPinCode())).thenReturn(false);

    // Act
    InvalidPinException ex = assertThrows(
        InvalidPinException.class,
        () -> walletSerive.transfer(userId, request));

    // Assert
    assertEquals("Invalid transaction PIN", ex.getMessage());
    assertEquals(new BigDecimal("250.00"), wallet.getBalance());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("transfer: Should throw InsufficientBalanceException when sender balance is less than amount")
  void transfer_InsufficientFunds_ThrowsException() {
    // Arrange
    TransferRequest request = new TransferRequest(
        "01099999999",
        new BigDecimal("1000.00"),
        "1234");

    when(receiverInfo.getId()).thenReturn(receiverWalletId);

    when(walletRepository.findWalletIdByUserId(userId)).thenReturn(Optional.of(walletId));
    when(walletRepository.findWalletIdsByUserPhone("01099999999")).thenReturn(Optional.of(receiverInfo));

    // Deterministic Lock Ordering
    when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(wallet));
    when(walletRepository.findByIdWithLock(receiverWalletId)).thenReturn(Optional.of(receiverWallet));

    when(passwordEncoder.matches("1234", wallet.getPinCode())).thenReturn(true);

    // Act
    InsufficientBalanceException ex = assertThrows(
        InsufficientBalanceException.class,
        () -> walletSerive.transfer(userId, request));

    // Assert
    assertEquals("Insufficient funds", ex.getMessage());
    assertEquals(new BigDecimal("250.00"), wallet.getBalance());
    assertEquals(new BigDecimal("100.00"), receiverWallet.getBalance());
    verify(transactionRepository, never()).save(any());
  }

}
