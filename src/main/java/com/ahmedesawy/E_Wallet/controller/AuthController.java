package com.ahmedesawy.E_Wallet.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ahmedesawy.E_Wallet.dto.request.LoginRequest;
import com.ahmedesawy.E_Wallet.dto.request.RefreshTokenRequest;
import com.ahmedesawy.E_Wallet.dto.request.RegisterRequest;
import com.ahmedesawy.E_Wallet.dto.response.AuthResponse;
import com.ahmedesawy.E_Wallet.dto.response.UserResponse;
import com.ahmedesawy.E_Wallet.entity.User;
import com.ahmedesawy.E_Wallet.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    return new ResponseEntity<>(userService.registerUser(request), HttpStatus.CREATED);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(userService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
    return ResponseEntity.ok(userService.refreshToken(request, authHeader));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @Valid @RequestBody RefreshTokenRequest request,
      HttpServletRequest httpRequest)

  {
    String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
    userService.logout(request, authHeader);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/logout-all")
  public ResponseEntity<Void> logoutAllDevices(
      @AuthenticationPrincipal User currentUser,
      HttpServletRequest httpRequest)

  {
    String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
    userService.logoutAllDevices(currentUser.getId(), authHeader);
    return ResponseEntity.noContent().build();
  }

}
