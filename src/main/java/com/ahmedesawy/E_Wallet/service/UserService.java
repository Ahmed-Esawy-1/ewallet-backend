package com.ahmedesawy.E_Wallet.service;

import com.ahmedesawy.E_Wallet.dto.request.LoginRequest;
import com.ahmedesawy.E_Wallet.dto.request.RefreshTokenRequest;
import com.ahmedesawy.E_Wallet.dto.request.RegisterRequest;
import com.ahmedesawy.E_Wallet.dto.response.AuthResponse;
import com.ahmedesawy.E_Wallet.dto.response.UserResponse;

public interface UserService {
  UserResponse registerUser(RegisterRequest request);

  AuthResponse login(LoginRequest request);

  AuthResponse refreshToken(RefreshTokenRequest request, String authHeader);

  void logout(RefreshTokenRequest request, String authHeader);
  void logoutAllDevices(Long userId, String authHeader);
}
