package com.ahmedesawy.E_Wallet.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.ahmedesawy.E_Wallet.entity.Transaction;
import com.ahmedesawy.E_Wallet.entity.User;
import com.ahmedesawy.E_Wallet.entity.Wallet;
import com.ahmedesawy.E_Wallet.enums.Role;
import com.ahmedesawy.E_Wallet.enums.TransactionStatus;
import com.ahmedesawy.E_Wallet.enums.TransactionType;
import com.ahmedesawy.E_Wallet.enums.WalletStatus;
import com.ahmedesawy.E_Wallet.repository.projection.SuspiciousTransactionView;
import com.ahmedesawy.E_Wallet.repository.projection.TransactionHistoryView;

@DataJpaTest
public class TransactionRepositoryTest {

  @TestConfiguration
  static class TestCacheConfig {
    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }
  }

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private TransactionRepository transactionRepository;

  private Wallet walletOne;
  private Wallet walletTwo;

  @BeforeEach
  void setUp() {
    User userOne = User.builder()
        .name("Ahmed")
        .email("ahmed@gmail.com")
        .phone("0101111111111")
        .password("$2a$10$D/mYqIPbhrGIlgVQr12X5OiJuU0SWMwV8i8OAZirpSdP2VE2EjRNm") // 123456
        .role(Role.CUSTOMER)
        .build();
    entityManager.persist(userOne);

    User userTwo = User.builder()
        .name("Ali")
        .email("ali@gmail.com")
        .phone("0102222222222")
        .password("$2a$10$D/mYqIPbhrGIlgVQr12X5OiJuU0SWMwV8i8OAZirpSdP2VE2EjRNm") // 123456
        .role(Role.CUSTOMER)
        .build();
    entityManager.persist(userTwo);

    walletOne = Wallet.builder()
        .user(userOne)
        .balance(new BigDecimal("1000.00"))
        .currency("EGP")
        .pinCode("$2a$10$a2SjYY6Kpo8jzdZm0STNVu1.jWZXFDNrycGCzkQtFTpxWhSQkIv3G") // 1234
        .status(WalletStatus.ACTIVE)
        .build();
    entityManager.persist(walletOne);

    walletTwo = Wallet.builder()
        .user(userTwo)
        .balance(new BigDecimal("500.00"))
        .currency("EGP")
        .pinCode("$2a$10$bzKVYPWb92w8SFQLeJwDg.ugNmKBzhFPjraqAbO.kNfY5t3I.0Afu") // 4567
        .status(WalletStatus.ACTIVE)
        .build();
    entityManager.persist(walletTwo);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("findWalletHistory: Should return deposits and transfers with resolved phone numbers")
  void findWalletHistory_ShouldReturnJoinedTransactions() {
    // Arrange
    Transaction depositToWalletOne = Transaction.builder()
        .sourceWalletId(null)
        .targetWalletId(walletOne.getId())
        .amount(new BigDecimal("500.00"))
        .type(TransactionType.DEPOSIT)
        .status(TransactionStatus.SUCCESS)
        .build();
    entityManager.persist(depositToWalletOne);

    Transaction transferFromWalletOneToWalletTwo = Transaction.builder()
        .sourceWalletId(walletOne.getId())
        .targetWalletId(walletTwo.getId())
        .amount(new BigDecimal("200.00"))
        .type(TransactionType.TRANSFER)
        .status(TransactionStatus.SUCCESS)
        .build();
    entityManager.persist(transferFromWalletOneToWalletTwo);

    Transaction withdrawFromWalletTwo = Transaction.builder()
        .sourceWalletId(walletTwo.getId())
        .targetWalletId(null)
        .amount(new BigDecimal("50.00"))
        .type(TransactionType.WITHDRAWAL)
        .status(TransactionStatus.SUCCESS)
        .build();
    entityManager.persist(withdrawFromWalletTwo);

    entityManager.flush();
    entityManager.clear();

    // Act
    Page<TransactionHistoryView> page = transactionRepository.findWalletHistory(
        walletOne.getId(),
        PageRequest.of(0, 10));

    // Assert
    // 2 Because we use WalletOne (deposit & transfer) & withdraw in walletTwo
    assertEquals(2, page.getTotalElements());

    // Check Deposit
    TransactionHistoryView depositTx = page.getContent()
        .stream()
        .filter(tx -> tx.getType() == TransactionType.DEPOSIT)
        .findFirst()
        .orElseThrow();
    assertNull(depositTx.getSourcePhone());
    assertEquals("0101111111111", depositTx.getTargetPhone());

    // Check Transfer
    TransactionHistoryView transferTx = page.getContent()
        .stream()
        .filter(tx -> tx.getType() == TransactionType.TRANSFER)
        .findFirst()
        .orElseThrow();
    assertEquals("0101111111111", transferTx.getSourcePhone());
    assertEquals("0102222222222", transferTx.getTargetPhone());
    assertEquals(new BigDecimal("200.00"), transferTx.getAmount());

  }

  @Test
  @DisplayName("findSuspiciousTransactions: Should return transactions that are FAILED or >= threshold")
  void findSuspiciousTransactions_ShouldFilterByAmountOrFailedStatus() {
    // Arrange
    Transaction normalTransfer = Transaction.builder()
        .sourceWalletId(walletOne.getId())
        .targetWalletId(walletTwo.getId())
        .amount(new BigDecimal("200.00"))
        .type(TransactionType.TRANSFER)
        .status(TransactionStatus.SUCCESS)
        .build();
    entityManager.persist(normalTransfer);

    Transaction highValueTransfer = Transaction.builder()
        .sourceWalletId(walletTwo.getId())
        .targetWalletId(walletOne.getId())
        .amount(new BigDecimal("15000.00"))
        .type(TransactionType.TRANSFER)
        .status(TransactionStatus.SUCCESS)
        .build();
    entityManager.persist(highValueTransfer);

    Transaction failedWithdraw = Transaction.builder()
        .sourceWalletId(walletOne.getId())
        .targetWalletId(null)
        .amount(new BigDecimal("50.00"))
        .type(TransactionType.WITHDRAWAL)
        .status(TransactionStatus.FAILED)
        .build();
    entityManager.persist(failedWithdraw);

    entityManager.flush();
    entityManager.clear();

    // Act
    Page<SuspiciousTransactionView> page = transactionRepository.findSupiciousTransaction(
        new BigDecimal("10000.00"),
        PageRequest.of(0, 10));

    // Assert
    // 2 Because 15_000.00 suspicious and failed withdraw
    assertEquals(2, page.getTotalElements());
    assertTrue(page.getContent().stream()
        .anyMatch(tx -> tx.getStatus() == TransactionStatus.FAILED));
    assertTrue(page.getContent().stream()
        .anyMatch(tx -> tx.getAmount().compareTo(new BigDecimal("15000.00")) == 0));

  }

}
