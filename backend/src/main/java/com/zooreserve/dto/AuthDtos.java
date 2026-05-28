package com.zooreserve.dto;

import com.zooreserve.domain.enums.RoleCode;

public final class AuthDtos {
  private AuthDtos() {
  }

  public record LoginRequest(String username, String password, RoleCode role) {
  }

  public record RegisterRequest(String username, String password, String phone, String displayName) {
  }

  public record LoginUser(Long id, String username, String displayName, RoleCode role) {
  }

  public record LoginResponse(String token, LoginUser user) {
  }
}
