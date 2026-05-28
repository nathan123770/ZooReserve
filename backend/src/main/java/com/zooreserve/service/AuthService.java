package com.zooreserve.service;

import com.zooreserve.domain.enums.RoleCode;
import com.zooreserve.dto.AuthDtos.LoginRequest;
import com.zooreserve.dto.AuthDtos.LoginResponse;
import com.zooreserve.dto.AuthDtos.LoginUser;
import com.zooreserve.dto.AuthDtos.RegisterRequest;
import com.zooreserve.security.JwtTokenService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final JwtTokenService jwtTokenService;
  private final java.util.Map<String, LoginUser> visitors = new java.util.concurrent.ConcurrentHashMap<>();

  public AuthService(JwtTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  public LoginResponse login(LoginRequest request) {
    RoleCode role = request.role() == null ? RoleCode.VISITOR : request.role();
    LoginUser registeredVisitor = visitors.get(request.username());
    if (role == RoleCode.VISITOR && registeredVisitor != null) {
      return new LoginResponse(jwtTokenService.issueToken(request.username(), role), registeredVisitor);
    }
    String displayName = switch (role) {
      case ADMIN -> "园区管理员";
      case CHECKER -> "入口核销员";
      case VISITOR -> "亲子游客";
    };
    LoginUser user = new LoginUser(1L, request.username(), displayName, role);
    return new LoginResponse(jwtTokenService.issueToken(request.username(), role), user);
  }

  public LoginResponse register(RegisterRequest request) {
    if (request.username() == null || request.username().isBlank()) {
      throw new IllegalStateException("账号不能为空");
    }
    if (visitors.containsKey(request.username())) {
      throw new IllegalStateException("账号已注册");
    }
    String displayName = request.displayName() == null || request.displayName().isBlank() ? "亲子游客" : request.displayName();
    LoginUser user = new LoginUser((long) visitors.size() + 100L, request.username(), displayName, RoleCode.VISITOR);
    visitors.put(request.username(), user);
    return new LoginResponse(jwtTokenService.issueToken(request.username(), RoleCode.VISITOR), user);
  }
}
