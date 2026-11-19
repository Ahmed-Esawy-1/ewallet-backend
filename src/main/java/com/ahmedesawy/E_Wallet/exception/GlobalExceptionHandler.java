package com.ahmedesawy.E_Wallet.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // Handle Validation Errors ex: @NotBlank
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptios(MethodArgumentNotValidException ex) {
    Map<String, String> fieldsErrors = new HashMap<>();

    ex.getBindingResult().getFieldErrors()
        .forEach(error -> fieldsErrors.put(error.getField(), error.getDefaultMessage()));

    return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", fieldsErrors);
  }

  // Handle Illegal Exceptions
  @ExceptionHandler ({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<Map<String, Object>> handleIllegalExceptions(RuntimeException ex) {
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  
  }

  // Handle Resource Exists
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
    return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  // Handle Resource Exists
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFoundException(ResourceNotFoundException ex) {
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
  }

  // Handle Insufficient Balance
  @ExceptionHandler(InsufficientBalanceException.class)
  public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException ex) {
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null); 
  }

  // Handle Invalid Pin Code
  @ExceptionHandler(InvalidPinException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidPin(InvalidPinException ex) {
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null); 
  }

  // ---- Helper ----
  private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, Object details) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("messages", message);
    if (details != null) {
      body.put("details", details);
    }
    return new ResponseEntity<>(body, status);
  }

}
