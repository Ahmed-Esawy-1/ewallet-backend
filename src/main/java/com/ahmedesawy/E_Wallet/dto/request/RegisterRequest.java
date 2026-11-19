package com.ahmedesawy.E_Wallet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterRequest {

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Email is required")
  @Size(max = 150, message = "Not exceed 150 characters")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 6, message = "Password must be at least 6 characters")
  private String password;

  @NotBlank(message = "Phone number is required")
  @Pattern(regexp = "^\\+?[0-9]{10,20}$", message = "Invalid phone number format")
  private String phone;

  @NotBlank(message = "PIN is reuired")
  @Pattern(regexp = "^\\d{4}$")
  private String pin;

}
