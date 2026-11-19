package com.ahmedesawy.E_Wallet.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ahmedesawy.E_Wallet.dto.request.DepositRequest;
import com.ahmedesawy.E_Wallet.dto.request.TransferRequest;
import com.ahmedesawy.E_Wallet.dto.request.UpdatePinRequest;
import com.ahmedesawy.E_Wallet.dto.request.WithdrawRequest;
import com.ahmedesawy.E_Wallet.dto.response.TransferResponse;
import com.ahmedesawy.E_Wallet.dto.response.WalletResponse;
import com.ahmedesawy.E_Wallet.entity.User;
import com.ahmedesawy.E_Wallet.repository.projection.LedgerEntryView;
import com.ahmedesawy.E_Wallet.repository.projection.TransactionHistoryView;
import com.ahmedesawy.E_Wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class WalletController {

  private final WalletService walletService;

  @GetMapping("/me")
  public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal User currentUser) {
    WalletResponse response = walletService.getWalletDetails(currentUser.getId());
    return ResponseEntity.ok(response);
  }

  @PutMapping("/me/pin")
  public ResponseEntity<Void> updatePin(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody UpdatePinRequest request) {
    walletService.updatePin(currentUser.getId(), request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me/transactions")
  public ResponseEntity<PagedModel<TransactionHistoryView>> getTransactionHistory(
      @AuthenticationPrincipal User currentUser,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(new PagedModel<>(walletService.getTransactionHistory(currentUser.getId(), pageable)));
  }

  @GetMapping("/me/ledger")
  public ResponseEntity<PagedModel<LedgerEntryView>> getLedgerHistory(
      @AuthenticationPrincipal User currentUser,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(new PagedModel<>(walletService.getLedgerHistory(currentUser.getId(), pageable)));
  }

  @PostMapping("/deposit")
  public ResponseEntity<WalletResponse> deposit(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody DepositRequest request) {
    WalletResponse response = walletService.deposit(currentUser.getId(), request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/withdraw")
  public ResponseEntity<WalletResponse> withdraw(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody WithdrawRequest request) {
    WalletResponse response = walletService.withdraw(currentUser.getId(), request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/transfer")
  public ResponseEntity<TransferResponse> transfer(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody TransferRequest request) {
    TransferResponse response = walletService.transfer(currentUser.getId(), request);
    return ResponseEntity.ok(response);
  }

}
