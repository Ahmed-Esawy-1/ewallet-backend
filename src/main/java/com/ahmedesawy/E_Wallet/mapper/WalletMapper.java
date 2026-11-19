package com.ahmedesawy.E_Wallet.mapper;

import org.springframework.stereotype.Component;

import com.ahmedesawy.E_Wallet.dto.response.WalletResponse;
import com.ahmedesawy.E_Wallet.entity.Wallet;

@Component 
public class WalletMapper {

  public WalletResponse mapToWalletResponse(Wallet wallet) {
    return WalletResponse.builder()
        .walletId(wallet.getId())
        .userId(wallet.getUser().getId())
        .balance(wallet.getBalance())
        .currency(wallet.getCurrency())
        .status(wallet.getStatus().name())
        .build();
  }
  

}
