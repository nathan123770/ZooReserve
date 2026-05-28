package com.zooreserve.service;

import com.zooreserve.common.PageResult;
import com.zooreserve.dto.AdminDtos.DashboardSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminMockService {
  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;

  public AdminMockService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
    this.jdbcTemplate = jdbcTemplate;
    this.passwordEncoder = passwordEncoder;
  }

  public DashboardSummary dashboard() {
    Long todayReservations = count("SELECT COUNT(*) FROM reservation_order WHERE visit_date = ?", LocalDate.now());
    BigDecimal paidAmount = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM payment_record WHERE status = 'PAY_SUCCESS'", BigDecimal.class);
    Long checkedInPeople = count("SELECT COALESCE(SUM(oi.quantity), 0) FROM checkin_record cr JOIN order_item oi ON oi.order_id = cr.order_id");
    Long remainingCapacity = count("SELECT COALESCE(SUM(remaining), 0) FROM ticket_inventory");
    List<Map<String, Object>> trend = jdbcTemplate.queryForList("""
        SELECT DATE(start_time) AS date, COUNT(s.id) AS count
        FROM activity a
        LEFT JOIN activity_signup s ON s.activity_id = a.id
        GROUP BY DATE(start_time)
        ORDER BY date
        """);
    return new DashboardSummary(todayReservations, paidAmount, checkedInPeople, remainingCapacity, trend);
  }

  public PageResult<Map<String, Object>> records(String domain) {
    List<Map<String, Object>> records = switch (domain) {
      case "tickets" -> jdbcTemplate.queryForList("""
          SELECT tt.id, tt.code, tt.name, tt.price, tt.description, tt.status,
                 COALESCE(MAX(ti.capacity), 0) AS capacity, COALESCE(SUM(ti.remaining), 0) AS remaining
          FROM ticket_type tt
          LEFT JOIN ticket_inventory ti ON ti.ticket_type_id = tt.id
          GROUP BY tt.id, tt.code, tt.name, tt.price, tt.description, tt.status
          ORDER BY tt.id
          """);
      case "orders" -> jdbcTemplate.queryForList("""
          SELECT o.id, o.order_no AS orderNo, u.username AS visitor, u.phone, o.visit_date AS visitDate,
                 o.session_code AS session, o.amount, o.order_status AS status, o.payment_status AS paymentStatus,
                 o.created_at AS createdAt
          FROM reservation_order o
          LEFT JOIN user u ON u.id = o.user_id
          ORDER BY o.created_at DESC
          """);
      case "activities" -> jdbcTemplate.queryForList("""
          SELECT a.id, a.title, a.category, a.start_time AS startTime, a.capacity,
                 COUNT(s.id) AS signed, a.location, a.status
          FROM activity a
          LEFT JOIN activity_signup s ON s.activity_id = a.id
          GROUP BY a.id, a.title, a.category, a.start_time, a.capacity, a.location, a.status
          ORDER BY a.start_time
          """);
      case "animals" -> jdbcTemplate.queryForList("""
          SELECT a.id, a.name, a.species, z.name AS zone, a.media_url AS media,
                 CONCAT('P-', a.id) AS guidePoint, a.status, a.description
          FROM animal a
          LEFT JOIN zone z ON z.id = a.zone_id
          ORDER BY a.id
          """);
      case "checkins" -> jdbcTemplate.queryForList("""
          SELECT cr.id, o.order_no AS orderNo, au.display_name AS checker,
                 COALESCE((SELECT SUM(quantity) FROM order_item WHERE order_id = o.id), 0) AS people,
                 cr.checked_at AS checkedAt, cr.remark, cr.status
          FROM checkin_record cr
          JOIN reservation_order o ON o.id = cr.order_id
          JOIN admin_user au ON au.id = cr.checker_id
          ORDER BY cr.checked_at DESC
          """);
      case "marketing" -> marketingRecords();
      case "system" -> jdbcTemplate.queryForList("""
          SELECT au.id, au.username, au.display_name AS displayName, r.name AS role,
                 au.created_at AS lastLogin, r.code AS scope, au.status
          FROM admin_user au
          LEFT JOIN user_role ur ON ur.user_id = au.id AND ur.user_type = 'ADMIN'
          LEFT JOIN role r ON r.id = ur.role_id
          ORDER BY au.id
          """);
      case "logs" -> jdbcTemplate.queryForList("""
          SELECT id, action AS name, resource, detail AS description, created_at AS createdAt, 'ENABLED' AS status
          FROM operation_log
          ORDER BY created_at DESC
          """);
      default -> List.of();
    };
    return PageResult.firstPage(records);
  }

  public Map<String, Object> create(String domain, Map<String, Object> payload) {
    return switch (domain) {
      case "tickets" -> createTicket(payload);
      case "activities" -> createActivity(payload);
      case "animals" -> createAnimal(payload);
      case "marketing" -> createMarketing(payload);
      case "system" -> createAdminUser(payload);
      default -> payload;
    };
  }

  public Map<String, Object> updateInventory(Map<String, Object> payload) {
    String ticketCode = text(payload, "ticketTypeCode", text(payload, "code"));
    String session = text(payload, "session", "AM");
    LocalDate visitDate = LocalDate.parse(text(payload, "visitDate", LocalDate.now().toString()));
    Long ticketTypeId = jdbcTemplate.queryForObject("SELECT id FROM ticket_type WHERE code = ?", Long.class, ticketCode);
    int updated = jdbcTemplate.update("""
        UPDATE ticket_inventory
        SET capacity = ?, remaining = ?
        WHERE ticket_type_id = ? AND visit_date = ? AND session_code = ?
        """, integer(payload, "capacity", 0), integer(payload, "remaining", 0), ticketTypeId, visitDate, session);
    if (updated == 0) {
      jdbcTemplate.update("""
          INSERT INTO ticket_inventory (ticket_type_id, visit_date, session_code, capacity, remaining)
          VALUES (?, ?, ?, ?, ?)
          """, ticketTypeId, visitDate, session, integer(payload, "capacity", 0), integer(payload, "remaining", 0));
    }
    return Map.of("updated", true);
  }

  public Map<String, Object> update(String domain, Long id, Map<String, Object> payload) {
    switch (domain) {
      case "tickets" -> jdbcTemplate.update("UPDATE ticket_type SET name = ?, price = ?, description = ?, status = ? WHERE id = ?",
          text(payload, "name"), decimal(payload, "price"), text(payload, "description"), text(payload, "status", "ENABLED"), id);
      case "activities" -> jdbcTemplate.update("UPDATE activity SET title = ?, category = ?, capacity = ?, location = ?, status = ? WHERE id = ?",
          text(payload, "title"), text(payload, "category"), integer(payload, "capacity", 0), text(payload, "location"), text(payload, "status", "PUBLISHED"), id);
      case "animals" -> jdbcTemplate.update("UPDATE animal SET name = ?, species = ?, description = ?, status = ? WHERE id = ?",
          text(payload, "name"), text(payload, "species"), text(payload, "description"), text(payload, "status", "VISIBLE"), id);
      case "system" -> jdbcTemplate.update("UPDATE admin_user SET display_name = ?, status = ? WHERE id = ?",
          text(payload, "displayName"), text(payload, "status", "ENABLED"), id);
      default -> {
      }
    }
    return byId(domain, id);
  }

  public Map<String, Object> toggleStatus(String domain, Long id, String status) {
    String table = switch (domain) {
      case "tickets" -> "ticket_type";
      case "activities" -> "activity";
      case "animals" -> "animal";
      case "system" -> "admin_user";
      default -> "";
    };
    if (!table.isBlank()) {
      jdbcTemplate.update("UPDATE " + table + " SET status = ? WHERE id = ?", status, id);
    }
    return byId(domain, id);
  }

  private Map<String, Object> createTicket(Map<String, Object> payload) {
    GeneratedKeyHolder keyHolder = insert("INSERT INTO ticket_type (code, name, price, description, status) VALUES (?, ?, ?, ?, ?)",
        text(payload, "code", text(payload, "name").toUpperCase()), text(payload, "name"), decimal(payload, "price"),
        text(payload, "description"), text(payload, "status", "ENABLED"));
    return byId("tickets", keyId(keyHolder));
  }

  private Map<String, Object> createActivity(Map<String, Object> payload) {
    GeneratedKeyHolder keyHolder = insert("INSERT INTO activity (title, category, start_time, capacity, location, status) VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?)",
        text(payload, "title"), text(payload, "category"), integer(payload, "capacity", 0), text(payload, "location"), text(payload, "status", "PUBLISHED"));
    return byId("activities", keyId(keyHolder));
  }

  private Map<String, Object> createAnimal(Map<String, Object> payload) {
    Long zoneId = ensureZone(text(payload, "zone", "未分区"));
    GeneratedKeyHolder keyHolder = insert("INSERT INTO animal (zone_id, name, species, description, media_url, status) VALUES (?, ?, ?, ?, ?, ?)",
        zoneId, text(payload, "name"), text(payload, "species"), text(payload, "description"), "", text(payload, "status", "VISIBLE"));
    return byId("animals", keyId(keyHolder));
  }

  private Map<String, Object> createMarketing(Map<String, Object> payload) {
    String type = text(payload, "type", "优惠券");
    if (type.contains("公告")) {
      GeneratedKeyHolder keyHolder = insert("INSERT INTO notice (title, content, status, published_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
          text(payload, "name"), text(payload, "description"), text(payload, "status", "PUBLISHED"));
      return Map.of("id", keyId(keyHolder), "name", text(payload, "name"), "type", "公告", "status", text(payload, "status", "PUBLISHED"));
    }
    GeneratedKeyHolder keyHolder = insert("INSERT INTO coupon (name, discount_type, discount_value, status) VALUES (?, 'AMOUNT', ?, ?)",
        text(payload, "name"), decimal(payload, "discountValue", BigDecimal.ZERO), text(payload, "status", "ENABLED"));
    return Map.of("id", keyId(keyHolder), "name", text(payload, "name"), "type", "优惠券", "status", text(payload, "status", "ENABLED"));
  }

  private Map<String, Object> createAdminUser(Map<String, Object> payload) {
    GeneratedKeyHolder keyHolder = insert("INSERT INTO admin_user (username, display_name, password_hash, status) VALUES (?, ?, ?, ?)",
        text(payload, "username"), text(payload, "displayName"), passwordEncoder.encode(text(payload, "password", "admin123")), text(payload, "status", "ENABLED"));
    return byId("system", keyId(keyHolder));
  }

  private List<Map<String, Object>> marketingRecords() {
    List<Map<String, Object>> records = new java.util.ArrayList<>();
    records.addAll(jdbcTemplate.queryForList("SELECT id, name, '优惠券' AS type, discount_value AS budget, status FROM coupon ORDER BY id"));
    records.addAll(jdbcTemplate.queryForList("SELECT id, title AS name, '公告' AS type, published_at AS period, status FROM notice ORDER BY id"));
    return records;
  }

  private Map<String, Object> byId(String domain, Long id) {
    return records(domain).records().stream()
        .filter(record -> String.valueOf(record.get("id")).equals(String.valueOf(id)))
        .findFirst()
        .orElseGet(LinkedHashMap::new);
  }

  private GeneratedKeyHolder insert(String sql, Object... args) {
    GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      for (int i = 0; i < args.length; i++) {
        statement.setObject(i + 1, args[i]);
      }
      return statement;
    }, keyHolder);
    return keyHolder;
  }

  private Long ensureZone(String zoneName) {
    List<Long> ids = jdbcTemplate.query("SELECT id FROM zone WHERE name = ?", (rs, rowNum) -> rs.getLong(1), zoneName);
    if (!ids.isEmpty()) {
      return ids.get(0);
    }
    return keyId(insert("INSERT INTO zone (name) VALUES (?)", zoneName));
  }

  private Long keyId(GeneratedKeyHolder keyHolder) {
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
    throw new IllegalStateException("创建记录失败");
  }

  private Long count(String sql, Object... args) {
    Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
    return value == null ? 0L : value;
  }

  private String text(Map<String, Object> payload, String key) {
    return text(payload, key, "");
  }

  private String text(Map<String, Object> payload, String key, String fallback) {
    Object value = payload.get(key);
    return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
  }

  private BigDecimal decimal(Map<String, Object> payload, String key) {
    return decimal(payload, key, BigDecimal.ZERO);
  }

  private BigDecimal decimal(Map<String, Object> payload, String key, BigDecimal fallback) {
    Object value = payload.get(key);
    return value == null || String.valueOf(value).isBlank() ? fallback : new BigDecimal(String.valueOf(value));
  }

  private int integer(Map<String, Object> payload, String key, int fallback) {
    Object value = payload.get(key);
    return value == null || String.valueOf(value).isBlank() ? fallback : Integer.parseInt(String.valueOf(value));
  }
}
