package com.zooreserve.service;

import com.zooreserve.domain.enums.RoleCode;
import com.zooreserve.dto.AuthDtos.LoginRequest;
import com.zooreserve.dto.AuthDtos.LoginResponse;
import com.zooreserve.dto.AuthDtos.LoginUser;
import com.zooreserve.dto.AuthDtos.RegisterRequest;
import com.zooreserve.security.JwtTokenService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;

@Service
public class AuthService {
  private final JwtTokenService jwtTokenService;
  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;

  public AuthService(JwtTokenService jwtTokenService, JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
    this.jwtTokenService = jwtTokenService;
    this.jdbcTemplate = jdbcTemplate;
    this.passwordEncoder = passwordEncoder;
  }

  public LoginResponse login(LoginRequest request) {
    RoleCode role = request.role() == null ? RoleCode.VISITOR : request.role();
    if (request.username() == null || request.password() == null) {
      throw new BadCredentialsException("账号或密码错误");
    }
    LoginUser user = switch (role) {
      case VISITOR -> visitorLogin(request);
      case ADMIN, CHECKER -> adminLogin(request, role);
    };
    return new LoginResponse(jwtTokenService.issueToken(user.username(), role), user);
  }

  @Transactional
  public LoginResponse register(RegisterRequest request) {
    if (request.username() == null || request.username().isBlank()) {
      throw new IllegalStateException("账号不能为空");
    }
    Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, request.username());
    if (exists != null && exists > 0) {
      throw new IllegalStateException("账号已注册");
    }
    Integer phoneExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE phone = ?", Integer.class, request.phone());
    if (phoneExists != null && phoneExists > 0) {
      throw new IllegalStateException("手机号已注册");
    }

    GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement(
          "INSERT INTO user (username, phone, password_hash, status) VALUES (?, ?, ?, 'ENABLED')",
          Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, request.username());
      statement.setString(2, request.phone());
      statement.setString(3, passwordEncoder.encode(request.password()));
      return statement;
    }, keyHolder);

    Long userId = generatedId(keyHolder);
    Long roleId = jdbcTemplate.queryForObject("SELECT id FROM role WHERE code = 'VISITOR'", Long.class);
    jdbcTemplate.update("INSERT INTO user_role (user_id, role_id, user_type) VALUES (?, ?, 'VISITOR')", userId, roleId);

    String displayName = request.displayName() == null || request.displayName().isBlank() ? "亲子游客" : request.displayName();
    LoginUser user = new LoginUser(userId, request.username(), displayName, RoleCode.VISITOR);
    return new LoginResponse(jwtTokenService.issueToken(request.username(), RoleCode.VISITOR), user);
  }

  private LoginUser visitorLogin(LoginRequest request) {
    return jdbcTemplate.query("""
        SELECT id, username, password_hash, status
        FROM user
        WHERE username = ?
        """, resultSet -> {
      if (!resultSet.next()) {
        throw new BadCredentialsException("账号或密码错误");
      }
      if (!"ENABLED".equals(resultSet.getString("status"))
          || !passwordEncoder.matches(request.password(), resultSet.getString("password_hash"))) {
        throw new BadCredentialsException("账号或密码错误");
      }
      return new LoginUser(resultSet.getLong("id"), resultSet.getString("username"), "亲子游客", RoleCode.VISITOR);
    }, request.username());
  }

  private LoginUser adminLogin(LoginRequest request, RoleCode role) {
    return jdbcTemplate.query("""
        SELECT au.id, au.username, au.display_name, au.password_hash, au.status
        FROM admin_user au
        JOIN user_role ur ON ur.user_id = au.id AND ur.user_type = 'ADMIN'
        JOIN role r ON r.id = ur.role_id
        WHERE au.username = ? AND r.code = ?
        """, resultSet -> {
      if (!resultSet.next()) {
        throw new BadCredentialsException("账号或密码错误");
      }
      if (!"ENABLED".equals(resultSet.getString("status"))
          || !passwordEncoder.matches(request.password(), resultSet.getString("password_hash"))) {
        throw new BadCredentialsException("账号或密码错误");
      }
      return new LoginUser(resultSet.getLong("id"), resultSet.getString("username"), resultSet.getString("display_name"), role);
    }, request.username(), role.name());
  }

  private Long generatedId(GeneratedKeyHolder keyHolder) {
    if (keyHolder.getKeys() != null && keyHolder.getKeys().get("id") instanceof Number id) {
      return id.longValue();
    }
    if (keyHolder.getKeys() != null && keyHolder.getKeys().size() == 1) {
      Object value = keyHolder.getKeys().values().iterator().next();
      if (value instanceof Number id) {
        return id.longValue();
      }
    }
    if (keyHolder.getKey() != null) {
      return keyHolder.getKey().longValue();
    }
    throw new IllegalStateException("创建账号失败");
  }
}
