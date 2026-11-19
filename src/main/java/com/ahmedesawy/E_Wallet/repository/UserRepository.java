package com.ahmedesawy.E_Wallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmedesawy.E_Wallet.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
  boolean existsByEmail(String email);
  boolean existsByPhone(String phone);
  Optional<User> findByEmail(String email);
  Optional<User> findByPhone(String phone);
}
