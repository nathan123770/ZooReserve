package com.zooreserve.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class MemberService {
  private final JdbcTemplate jdbcTemplate;

  public MemberService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> profiles() {
    return jdbcTemplate.queryForList("""
        SELECT id, real_name AS name, id_card_no AS idCard, phone, '本人/家人' AS relation, is_default AS isDefault
        FROM visitor_profile
        WHERE user_id = ?
        ORDER BY is_default DESC, id
        """, currentVisitorId());
  }

  @Transactional
  public Map<String, Object> createProfile(Map<String, Object> payload) {
    Long userId = currentVisitorId();
    boolean isDefault = Boolean.TRUE.equals(payload.get("isDefault"));
    if (isDefault) {
      jdbcTemplate.update("UPDATE visitor_profile SET is_default = 0 WHERE user_id = ?", userId);
    }
    GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement("""
          INSERT INTO visitor_profile (user_id, real_name, id_card_no, phone, is_default)
          VALUES (?, ?, ?, ?, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      statement.setLong(1, userId);
      statement.setString(2, stringValue(payload, "name", "游客"));
      statement.setString(3, stringValue(payload, "idCard", "UNKNOWN-" + System.currentTimeMillis()));
      statement.setString(4, stringValue(payload, "phone", ""));
      statement.setInt(5, isDefault ? 1 : 0);
      return statement;
    }, keyHolder);
    return profileById(generatedId(keyHolder));
  }

  @Transactional
  public Map<String, Object> updateProfile(Long id, Map<String, Object> payload) {
    Long userId = currentVisitorId();
    boolean isDefault = Boolean.TRUE.equals(payload.get("isDefault"));
    if (isDefault) {
      jdbcTemplate.update("UPDATE visitor_profile SET is_default = 0 WHERE user_id = ?", userId);
    }
    jdbcTemplate.update("""
        UPDATE visitor_profile
        SET real_name = ?, id_card_no = ?, phone = ?, is_default = ?
        WHERE id = ? AND user_id = ?
        """, stringValue(payload, "name", "游客"), stringValue(payload, "idCard", ""),
        stringValue(payload, "phone", ""), isDefault ? 1 : 0, id, userId);
    return profileById(id);
  }

  public Map<String, Object> deleteProfile(Long id) {
    jdbcTemplate.update("DELETE FROM visitor_profile WHERE id = ? AND user_id = ?", id, currentVisitorId());
    return Map.of("deleted", true, "id", id);
  }

  public List<Map<String, Object>> coupons() {
    return jdbcTemplate.queryForList("""
        SELECT uc.id, c.name, c.discount_type AS "discountType", c.discount_value AS "discountValue",
               c.threshold_amount AS "thresholdAmount", c.valid_from AS "validFrom", c.valid_to AS "expiresAt",
               c.scope, uc.status
        FROM user_coupon uc
        JOIN coupon c ON c.id = uc.coupon_id
        WHERE uc.user_id = ?
        ORDER BY CASE uc.status
          WHEN 'UNUSED' THEN 1
          WHEN 'LOCKED' THEN 2
          WHEN 'USED' THEN 3
          ELSE 4
        END, uc.id
        """, currentVisitorId());
  }

  public List<Map<String, Object>> availableCoupons() {
    Long userId = currentVisitorId();
    return jdbcTemplate.queryForList("""
        SELECT c.id, c.name, c.discount_type AS "discountType", c.discount_value AS "discountValue",
               c.threshold_amount AS "thresholdAmount", c.valid_from AS "validFrom", c.valid_to AS "expiresAt",
               c.scope, c.total_quantity AS "totalQuantity", c.claimed_quantity AS "claimedQuantity", c.status
        FROM coupon c
        WHERE c.status = 'ENABLED'
          AND (c.valid_from IS NULL OR c.valid_from <= CURRENT_DATE)
          AND (c.valid_to IS NULL OR c.valid_to >= CURRENT_DATE)
          AND (c.total_quantity IS NULL OR c.claimed_quantity < c.total_quantity)
          AND NOT EXISTS (
            SELECT 1 FROM user_coupon uc WHERE uc.user_id = ? AND uc.coupon_id = c.id
          )
        ORDER BY c.valid_to, c.id
        """, userId);
  }

  public List<Map<String, Object>> notices(String position) {
    String normalizedPosition = position == null || position.isBlank() ? "ALL" : position.toUpperCase();
    return jdbcTemplate.queryForList("""
        SELECT id, title, content, display_position AS "displayPosition", priority, status, published_at AS "publishedAt"
        FROM notice
        WHERE status = 'PUBLISHED'
          AND published_at IS NOT NULL
          AND published_at <= CURRENT_TIMESTAMP
          AND (display_position = 'ALL' OR display_position = ?)
        ORDER BY priority DESC, published_at DESC, id DESC
        """, normalizedPosition);
  }

  @Transactional
  public Map<String, Object> claimCoupon(Long couponId) {
    Long userId = currentVisitorId();
    Integer exists = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_coupon WHERE user_id = ? AND coupon_id = ?", Integer.class, userId, couponId);
    if (exists != null && exists > 0) {
      throw new IllegalStateException("优惠券已领取");
    }
    Map<String, Object> coupon = jdbcTemplate.queryForMap("""
        SELECT status, total_quantity, claimed_quantity, valid_from, valid_to
        FROM coupon
        WHERE id = ?
        """, couponId);
    if (!"ENABLED".equals(coupon.get("status"))) {
      throw new IllegalStateException("优惠券未启用");
    }
    Number totalQuantity = (Number) coupon.get("total_quantity");
    Number claimedQuantity = (Number) coupon.get("claimed_quantity");
    if (totalQuantity != null && claimedQuantity != null && claimedQuantity.intValue() >= totalQuantity.intValue()) {
      throw new IllegalStateException("优惠券已领完");
    }
    java.sql.Date validFrom = (java.sql.Date) coupon.get("valid_from");
    java.sql.Date validTo = (java.sql.Date) coupon.get("valid_to");
    LocalDate today = LocalDate.now();
    if ((validFrom != null && today.isBefore(validFrom.toLocalDate()))
        || (validTo != null && today.isAfter(validTo.toLocalDate()))) {
      throw new IllegalStateException("优惠券不在领取期内");
    }
    jdbcTemplate.update("INSERT INTO user_coupon (user_id, coupon_id, status) VALUES (?, ?, 'UNUSED')", userId, couponId);
    jdbcTemplate.update("UPDATE coupon SET claimed_quantity = claimed_quantity + 1 WHERE id = ?", couponId);
    return Map.of("claimed", true, "couponId", couponId);
  }

  public List<Map<String, Object>> annualPasses() {
    return jdbcTemplate.queryForList("""
        SELECT ap.id, ap.pass_no AS passNo, app.name, ap.started_at AS startedAt, ap.expires_at AS expiresAt,
               ap.status, app.benefits,
               (SELECT GROUP_CONCAT(vp.real_name)
                FROM annual_pass_holder aph
                JOIN visitor_profile vp ON vp.id = aph.profile_id
                WHERE aph.annual_pass_id = ap.id AND aph.status = 'ACTIVE') AS boundVisitors
        FROM annual_pass ap
        JOIN annual_pass_plan app ON app.id = ap.plan_id
        WHERE ap.user_id = ?
        ORDER BY ap.expires_at DESC
        """, currentVisitorId());
  }

  @Transactional
  public Map<String, Object> purchaseAnnualPass(Map<String, Object> payload) {
    Long userId = currentVisitorId();
    Long planId = Long.valueOf(String.valueOf(payload.getOrDefault("planId", 1)));
    Map<String, Object> plan = jdbcTemplate.queryForMap("SELECT valid_days FROM annual_pass_plan WHERE id = ? AND status = 'ENABLED'", planId);
    LocalDate today = LocalDate.now();
    String passNo = "AP" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + System.currentTimeMillis() % 10000;
    jdbcTemplate.update("""
        INSERT INTO annual_pass (user_id, plan_id, pass_no, started_at, expires_at, status)
        VALUES (?, ?, ?, ?, ?, 'ACTIVE')
        """, userId, planId, passNo, java.sql.Date.valueOf(today),
        java.sql.Date.valueOf(today.plusDays(((Number) plan.get("valid_days")).longValue())));
    return Map.of("purchased", true, "passNo", passNo);
  }

  public Map<String, Object> renewAnnualPass(Long id) {
    jdbcTemplate.update("""
        UPDATE annual_pass ap
        JOIN annual_pass_plan app ON app.id = ap.plan_id
        SET ap.expires_at = DATE_ADD(GREATEST(ap.expires_at, CURRENT_DATE), INTERVAL app.valid_days DAY),
            ap.status = 'ACTIVE'
        WHERE ap.id = ? AND ap.user_id = ?
        """, id, currentVisitorId());
    return Map.of("renewed", true, "id", id);
  }

  public Map<String, Object> addHolder(Long id, Map<String, Object> payload) {
    Long profileId = Long.valueOf(String.valueOf(payload.get("profileId")));
    jdbcTemplate.update("""
        INSERT INTO annual_pass_holder (annual_pass_id, profile_id, status)
        SELECT ?, ?, 'ACTIVE'
        WHERE NOT EXISTS (
          SELECT 1 FROM annual_pass_holder WHERE annual_pass_id = ? AND profile_id = ?
        )
        """, id, profileId, id, profileId);
    return Map.of("added", true, "annualPassId", id, "profileId", profileId);
  }

  private Map<String, Object> profileById(Long id) {
    return jdbcTemplate.queryForMap("""
        SELECT id, real_name AS name, id_card_no AS idCard, phone, '本人/家人' AS relation, is_default AS isDefault
        FROM visitor_profile
        WHERE id = ?
        """, id);
  }

  private String stringValue(Map<String, Object> payload, String key, String fallback) {
    Object value = payload.get(key);
    return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
  }

  private Long currentVisitorId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication == null || authentication.getName() == null ? "visitor" : authentication.getName();
    return jdbcTemplate.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, username);
  }

  private Long generatedId(GeneratedKeyHolder keyHolder) {
    if (keyHolder.getKeys() != null && keyHolder.getKeys().get("id") instanceof Number id) {
      return id.longValue();
    }
    if (keyHolder.getKey() != null) {
      return keyHolder.getKey().longValue();
    }
    throw new IllegalStateException("创建记录失败");
  }
}
