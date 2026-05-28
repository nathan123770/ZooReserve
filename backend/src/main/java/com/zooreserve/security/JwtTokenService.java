package com.zooreserve.security;

import com.zooreserve.domain.enums.RoleCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
  private final SecretKey key = Keys.hmacShaKeyFor("ZooReserveJwtSecretForSkeletonOnly2026".getBytes(StandardCharsets.UTF_8));

  public String issueToken(String username, RoleCode role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .claim("role", role.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(7200)))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
