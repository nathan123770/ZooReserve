package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.dto.AuthDtos.LoginRequest;
import com.zooreserve.dto.AuthDtos.LoginResponse;
import com.zooreserve.dto.AuthDtos.RegisterRequest;
import com.zooreserve.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
  }

  @PostMapping("/register")
  public ApiResponse<LoginResponse> register(@RequestBody RegisterRequest request) {
    return ApiResponse.ok(authService.register(request));
  }
}
