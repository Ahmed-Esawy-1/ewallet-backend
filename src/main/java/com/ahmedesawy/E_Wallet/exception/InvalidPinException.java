package com.ahmedesawy.E_Wallet.exception;

public class InvalidPinException extends RuntimeException {
  public InvalidPinException(String message) {
    super(message);
  }
}
