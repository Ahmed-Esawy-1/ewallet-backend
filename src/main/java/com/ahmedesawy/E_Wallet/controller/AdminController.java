package com.ahmedesawy.E_Wallet.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ahmedesawy.E_Wallet.dto.request.WalletStatusUpdateRequest;
import com.ahmedesawy.E_Wallet.dto.response.AdminWalletStatusResponse;
import com.ahmedesawy.E_Wallet.repository.projection.SuspiciousTransactionView;
import com.ahmedesawy.E_Wallet.service.AdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final AdminService adminService;

  @PatchMapping("/wallets/{walletId}/status")
  public ResponseEntity<AdminWalletStatusResponse> updateWalletStatus(
      @PathVariable Long walletId,
      @Valid @RequestBody WalletStatusUpdateRequest request) {
      return ResponseEntity.ok(adminService.updateWalletStatus(walletId, request.getStatus()));
  }

  @GetMapping ("/transactions/suspicious")
  public ResponseEntity<Page<SuspiciousTransactionView>> getSuspiciousTransactions(
    @RequestParam(defaultValue = "10000.00") BigDecimal threshold,
    @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    return ResponseEntity.ok(adminService.getSuspiciousTransactions(threshold, pageable));
  }

}
