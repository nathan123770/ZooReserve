package com.zooreserve.service;

import com.zooreserve.common.PageResult;
import com.zooreserve.dto.AdminDtos.DashboardSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminMockService {
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    Long remainingCapacity = count("SELECT COALESCE(SUM(remaining), 0) FROM daily_ticket_inventory");
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
    return records(domain, Map.of());
  }

  public PageResult<Map<String, Object>> records(String domain, Map<String, String[]> params) {
    List<Map<String, Object>> records = switch (domain) {
      case "tickets" -> ticketRecords(params);
      case "orders" -> normalizeDateTimes(jdbcTemplate.queryForList("""
          SELECT o.id, o.order_no AS "orderNo", u.username AS visitor, u.phone, o.visit_date AS "visitDate",
                 o.session_code AS session, o.amount, o.order_status AS status, o.payment_status AS "paymentStatus",
                 o.created_at AS "createdAt",
                 (SELECT rr.id FROM refund_record rr WHERE rr.order_id = o.id ORDER BY rr.id DESC LIMIT 1) AS "refundId"
          FROM reservation_order o
          LEFT JOIN user u ON u.id = o.user_id
          ORDER BY o.created_at DESC
          """));
      case "activities" -> normalizeDateTimes(jdbcTemplate.queryForList("""
          SELECT a.id, a.title, a.category, a.start_time AS "startTime", a.capacity,
                 COUNT(s.id) AS signed, a.location, a.is_paid AS "paid", a.price, a.coupon_scope AS "couponScope", a.status
          FROM activity a
          LEFT JOIN activity_signup s ON s.activity_id = a.id AND s.status = 'SIGNED'
          GROUP BY a.id, a.title, a.category, a.start_time, a.capacity, a.location, a.is_paid, a.price, a.coupon_scope, a.status
          ORDER BY a.start_time
          """));
      case "animals" -> jdbcTemplate.queryForList("""
          SELECT a.id, a.name, a.species, z.name AS zone, a.media_url AS media,
                 CONCAT('P-', a.id) AS "guidePoint", a.status, a.description
          FROM animal a
          LEFT JOIN zone z ON z.id = a.zone_id
          ORDER BY a.id
          """);
      case "checkins" -> normalizeDateTimes(jdbcTemplate.queryForList("""
          SELECT cr.id, o.order_no AS "orderNo", au.display_name AS checker,
                 COALESCE((SELECT SUM(quantity) FROM order_item WHERE order_id = o.id), 0) AS people,
                 cr.checked_at AS "checkedAt", cr.remark, cr.status
          FROM checkin_record cr
          JOIN reservation_order o ON o.id = cr.order_id
          JOIN admin_user au ON au.id = cr.checker_id
          ORDER BY cr.checked_at DESC
          """));
      case "marketing" -> marketingRecords();
      case "system" -> normalizeDateTimes(jdbcTemplate.queryForList("""
          SELECT au.id, au.username, au.display_name AS "displayName", r.name AS role,
                 au.created_at AS "lastLogin", r.code AS scope, au.status
          FROM admin_user au
          LEFT JOIN user_role ur ON ur.user_id = au.id AND ur.user_type = 'ADMIN'
          LEFT JOIN role r ON r.id = ur.role_id
          ORDER BY au.id
          """));
      case "logs" -> normalizeDateTimes(jdbcTemplate.queryForList("""
          SELECT id, action AS name, resource, detail AS description, created_at AS "createdAt", 'ENABLED' AS status
          FROM operation_log
          ORDER BY created_at DESC
          """));
      default -> List.of();
    };
    return PageResult.firstPage(records);
  }

  public Map<String, Object> create(String domain, Map<String, Object> payload) {
    Map<String, Object> record = switch (domain) {
      case "tickets" -> createTicket(payload);
      case "activities" -> createActivity(payload);
      case "animals" -> createAnimal(payload);
      case "marketing" -> createMarketing(payload);
      case "system" -> createAdminUser(payload);
      default -> payload;
    };
    logOperation("CREATE_" + domain.toUpperCase(), domain, "后台新增记录");
    return record;
  }

  public Map<String, Object> updateInventory(Map<String, Object> payload) {
    String ticketCode = text(payload, "ticketTypeCode", text(payload, "code"));
    String session = text(payload, "session", "AM");
    LocalDate visitDate = LocalDate.parse(text(payload, "visitDate", LocalDate.now().toString()));
    Long ticketTypeId = jdbcTemplate.queryForObject("SELECT id FROM ticket_type WHERE code = ?", Long.class, ticketCode);
    Integer dailyCapacity = integerOrNull(payload, "dailyCapacity");
    Integer dailyRemaining = integerOrNull(payload, "dailyRemaining");
    if (dailyCapacity != null || dailyRemaining != null) {
      upsertDailyInventory(ticketTypeId, visitDate, dailyCapacity, dailyRemaining);
    }
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
    logOperation("UPDATE_INVENTORY", ticketCode, visitDate + " " + session);
    return Map.of("updated", true);
  }

  public Map<String, Object> update(String domain, Long id, Map<String, Object> payload) {
    switch (domain) {
      case "tickets" -> jdbcTemplate.update("UPDATE ticket_type SET name = ?, price = ?, description = ?, status = ? WHERE id = ?",
          text(payload, "name"), decimal(payload, "price"), text(payload, "description"), text(payload, "status", "ENABLED"), id);
      case "activities" -> jdbcTemplate.update("""
          UPDATE activity
          SET title = ?, category = ?, start_time = ?, capacity = ?, location = ?, is_paid = ?, price = ?, coupon_scope = ?, status = ?
          WHERE id = ?
          """, text(payload, "title"), text(payload, "category"), parseDateTime(text(payload, "startTime")),
          integer(payload, "capacity", 0), text(payload, "location"), paidFlag(payload), decimal(payload, "price", BigDecimal.ZERO),
          text(payload, "couponScope", defaultActivityScope(text(payload, "category"))), text(payload, "status", "PUBLISHED"), id);
      case "animals" -> {
        Long zoneId = ensureZone(text(payload, "zone", "未分区"));
        jdbcTemplate.update("UPDATE animal SET zone_id = ?, name = ?, species = ?, description = ?, media_url = ?, status = ? WHERE id = ?",
            zoneId, text(payload, "name"), text(payload, "species"), text(payload, "description"), text(payload, "media"), text(payload, "status", "VISIBLE"), id);
      }
      case "marketing" -> updateMarketing(id, payload);
      case "system" -> jdbcTemplate.update("UPDATE admin_user SET display_name = ?, status = ? WHERE id = ?",
          text(payload, "displayName"), text(payload, "status", "ENABLED"), id);
      default -> {
      }
    }
    logOperation("UPDATE_" + domain.toUpperCase(), domain + ":" + id, "后台更新记录");
    return byId(domain, id, text(payload, "resourceType", null));
  }

  public Map<String, Object> toggleStatus(String domain, Long id, Map<String, Object> payload) {
    String status = text(payload, "status", "ENABLED");
    String table = switch (domain) {
      case "tickets" -> "ticket_type";
      case "activities" -> "activity";
      case "animals" -> "animal";
      case "marketing" -> marketingTable(payload);
      case "system" -> "admin_user";
      default -> "";
    };
    if (!table.isBlank()) {
      if ("notice".equals(table) && "PUBLISHED".equals(status)) {
        jdbcTemplate.update("""
            UPDATE notice
            SET status = ?, published_at = CASE WHEN published_at IS NULL THEN CURRENT_TIMESTAMP ELSE published_at END
            WHERE id = ?
            """, status, id);
      } else {
        jdbcTemplate.update("UPDATE " + table + " SET status = ? WHERE id = ?", status, id);
      }
      logOperation("STATUS_" + domain.toUpperCase(), domain + ":" + id, status);
    }
    return byId(domain, id, text(payload, "resourceType", null));
  }

  public void logOperation(String action, String resource, String detail) {
    jdbcTemplate.update("INSERT INTO operation_log (operator_id, action, resource, detail, ip) VALUES (1, ?, ?, ?, 'admin')",
        action, resource, detail);
  }

  private List<Map<String, Object>> ticketRecords(Map<String, String[]> params) {
    LocalDate visitDate = LocalDate.parse(param(params, "visitDate", LocalDate.now().toString()));
    String session = param(params, "session", "AM");
    return jdbcTemplate.queryForList("""
        SELECT tt.id, tt.code, tt.name, tt.price, tt.description, tt.status, ? AS "visitDate", ? AS session,
               tt.code AS "ticketTypeCode", COALESCE(ti.capacity, 0) AS capacity, COALESCE(ti.remaining, 0) AS remaining,
               COALESCE(di.capacity, 0) AS "dailyCapacity", COALESCE(di.remaining, 0) AS "dailyRemaining"
        FROM ticket_type tt
        LEFT JOIN ticket_inventory ti ON ti.ticket_type_id = tt.id AND ti.visit_date = ? AND ti.session_code = ?
        LEFT JOIN daily_ticket_inventory di ON di.ticket_type_id = tt.id AND di.visit_date = ?
        ORDER BY tt.id
        """, visitDate.toString(), session, visitDate, session, visitDate);
  }

  private Map<String, Object> createTicket(Map<String, Object> payload) {
    GeneratedKeyHolder keyHolder = insert("INSERT INTO ticket_type (code, name, price, description, status) VALUES (?, ?, ?, ?, ?)",
        text(payload, "code", text(payload, "name").toUpperCase()), text(payload, "name"), decimal(payload, "price"),
        text(payload, "description"), text(payload, "status", "ENABLED"));
    return byId("tickets", keyId(keyHolder), null);
  }

  private Map<String, Object> createActivity(Map<String, Object> payload) {
    GeneratedKeyHolder keyHolder = insert("""
        INSERT INTO activity (title, category, start_time, capacity, location, is_paid, price, coupon_scope, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        text(payload, "title"), text(payload, "category"), parseDateTime(text(payload, "startTime", LocalDateTime.now().format(DATE_TIME))),
        integer(payload, "capacity", 0), text(payload, "location"), paidFlag(payload), decimal(payload, "price", BigDecimal.ZERO),
        text(payload, "couponScope", defaultActivityScope(text(payload, "category"))), text(payload, "status", "PUBLISHED"));
    return byId("activities", keyId(keyHolder), null);
  }

  private Map<String, Object> createAnimal(Map<String, Object> payload) {
    Long zoneId = ensureZone(text(payload, "zone", "未分区"));
    GeneratedKeyHolder keyHolder = insert("INSERT INTO animal (zone_id, name, species, description, media_url, status) VALUES (?, ?, ?, ?, ?, ?)",
        zoneId, text(payload, "name"), text(payload, "species"), text(payload, "description"), text(payload, "media"), text(payload, "status", "VISIBLE"));
    return byId("animals", keyId(keyHolder), null);
  }

  private Map<String, Object> createMarketing(Map<String, Object> payload) {
    String resourceType = text(payload, "resourceType", text(payload, "type", "COUPON"));
    if (isNotice(resourceType)) {
      String status = text(payload, "status", "PUBLISHED");
      GeneratedKeyHolder keyHolder = insert("""
          INSERT INTO notice (title, content, display_position, priority, status, published_at)
          VALUES (?, ?, ?, ?, ?, ?)
          """, text(payload, "name"), text(payload, "description"), text(payload, "displayPosition", "ALL"),
          integer(payload, "priority", 0), status, publishedAt(status));
      return byId("marketing", keyId(keyHolder), "NOTICE");
    }
    GeneratedKeyHolder keyHolder = insert("""
        INSERT INTO coupon (name, discount_type, discount_value, threshold_amount, total_quantity, valid_from, valid_to, scope, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, text(payload, "name"), text(payload, "discountType", "AMOUNT"),
        decimal(payload, "discountValue", BigDecimal.ZERO), decimal(payload, "thresholdAmount", BigDecimal.ZERO),
        integer(payload, "totalQuantity", 0), parseDateOrNull(text(payload, "validFrom", "")),
        parseDateOrNull(text(payload, "validTo", "")), text(payload, "scope", "TICKET"), text(payload, "status", "ENABLED"));
    return byId("marketing", keyId(keyHolder), "COUPON");
  }

  private Map<String, Object> createAdminUser(Map<String, Object> payload) {
    GeneratedKeyHolder keyHolder = insert("INSERT INTO admin_user (username, display_name, password_hash, status) VALUES (?, ?, ?, ?)",
        text(payload, "username"), text(payload, "displayName"), passwordEncoder.encode(text(payload, "password", "admin123")), text(payload, "status", "ENABLED"));
    return byId("system", keyId(keyHolder), null);
  }

  private List<Map<String, Object>> marketingRecords() {
    List<Map<String, Object>> records = new java.util.ArrayList<>();
    records.addAll(jdbcTemplate.queryForList("""
        SELECT id, name, '优惠券' AS type, 'COUPON' AS "resourceType", discount_value AS "discountValue",
               threshold_amount AS "thresholdAmount", total_quantity AS "totalQuantity",
               claimed_quantity AS claimed, discount_type AS "discountType", valid_from AS "validFrom",
               valid_to AS "validTo", scope, status
        FROM coupon ORDER BY id
        """));
    records.addAll(normalizeDateTimes(jdbcTemplate.queryForList("""
        SELECT id, title AS name, '公告' AS type, 'NOTICE' AS "resourceType", content AS description,
               display_position AS "displayPosition", priority, published_at AS period, status
        FROM notice ORDER BY id
        """)));
    return records;
  }

  private void updateMarketing(Long id, Map<String, Object> payload) {
    if (isNotice(text(payload, "resourceType", text(payload, "type")))) {
      String status = text(payload, "status", "PUBLISHED");
      jdbcTemplate.update("""
          UPDATE notice
          SET title = ?, content = ?, display_position = ?, priority = ?, status = ?,
              published_at = CASE WHEN ? = 'PUBLISHED' AND published_at IS NULL THEN CURRENT_TIMESTAMP ELSE published_at END
          WHERE id = ?
          """, text(payload, "name"), text(payload, "description"), text(payload, "displayPosition", "ALL"),
          integer(payload, "priority", 0), status, status, id);
      return;
    }
    jdbcTemplate.update("""
        UPDATE coupon
        SET name = ?, discount_type = ?, discount_value = ?, threshold_amount = ?, total_quantity = ?,
            valid_from = ?, valid_to = ?, scope = ?, status = ?
        WHERE id = ?
        """, text(payload, "name"), text(payload, "discountType", "AMOUNT"),
        decimal(payload, "discountValue", BigDecimal.ZERO), decimal(payload, "thresholdAmount", BigDecimal.ZERO),
        integer(payload, "totalQuantity", 0), parseDateOrNull(text(payload, "validFrom", "")),
        parseDateOrNull(text(payload, "validTo", "")), text(payload, "scope", "TICKET"), text(payload, "status", "ENABLED"), id);
  }

  private Map<String, Object> byId(String domain, Long id, String resourceType) {
    return records(domain).records().stream()
        .filter(record -> String.valueOf(record.get("id")).equals(String.valueOf(id)))
        .filter(record -> resourceType == null || resourceType.equals(String.valueOf(record.get("resourceType"))))
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

  private Integer integerOrNull(Map<String, Object> payload, String key) {
    Object value = payload.get(key);
    return value == null || String.valueOf(value).isBlank() ? null : Integer.parseInt(String.valueOf(value));
  }

  private void upsertDailyInventory(Long ticketTypeId, LocalDate visitDate, Integer capacity, Integer remaining) {
    Integer existingCapacity = jdbcTemplate.query("""
        SELECT capacity
        FROM daily_ticket_inventory
        WHERE ticket_type_id = ? AND visit_date = ?
        """, rs -> rs.next() ? rs.getInt("capacity") : null, ticketTypeId, visitDate);
    Integer existingRemaining = jdbcTemplate.query("""
        SELECT remaining
        FROM daily_ticket_inventory
        WHERE ticket_type_id = ? AND visit_date = ?
        """, rs -> rs.next() ? rs.getInt("remaining") : null, ticketTypeId, visitDate);
    int nextCapacity = capacity == null ? (existingCapacity == null ? 0 : existingCapacity) : capacity;
    int nextRemaining = remaining == null ? (existingRemaining == null ? nextCapacity : existingRemaining) : remaining;
    int updated = jdbcTemplate.update("""
        UPDATE daily_ticket_inventory
        SET capacity = ?, remaining = ?
        WHERE ticket_type_id = ? AND visit_date = ?
        """, nextCapacity, nextRemaining, ticketTypeId, visitDate);
    if (updated == 0) {
      jdbcTemplate.update("""
          INSERT INTO daily_ticket_inventory (ticket_type_id, visit_date, capacity, remaining)
          VALUES (?, ?, ?, ?)
          """, ticketTypeId, visitDate, nextCapacity, nextRemaining);
    }
  }

  private String param(Map<String, String[]> params, String key, String fallback) {
    String[] values = params.get(key);
    return values == null || values.length == 0 || values[0].isBlank() ? fallback : values[0];
  }

  private LocalDateTime parseDateTime(String value) {
    if (value == null || value.isBlank()) {
      return LocalDateTime.now();
    }
    String normalized = value.replace('T', ' ');
    if (normalized.length() == 16) {
      normalized += ":00";
    }
    return LocalDateTime.parse(normalized, DATE_TIME);
  }

  private int paidFlag(Map<String, Object> payload) {
    Object value = payload.get("paid");
    if (value == null) {
      value = payload.get("isPaid");
    }
    if (value == null) {
      return decimal(payload, "price", BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;
    }
    return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value)) ? 1 : 0;
  }

  private String defaultActivityScope(String category) {
    if (category != null && (category.contains("夜游") || category.contains("夏夜"))) {
      return "ACTIVITY_NIGHT";
    }
    if (category != null && category.contains("亲子")) {
      return "ACTIVITY_PARENT_CHILD";
    }
    return "ACTIVITY";
  }

  private LocalDate parseDateOrNull(String value) {
    return value == null || value.isBlank() ? null : LocalDate.parse(value);
  }

  private LocalDateTime publishedAt(String status) {
    return "PUBLISHED".equals(status) ? LocalDateTime.now() : null;
  }

  private String marketingTable(Map<String, Object> payload) {
    return isNotice(text(payload, "resourceType", text(payload, "type"))) ? "notice" : "coupon";
  }

  private boolean isNotice(String type) {
    return type != null && ("NOTICE".equalsIgnoreCase(type) || type.contains("公告"));
  }

  private List<Map<String, Object>> normalizeDateTimes(List<Map<String, Object>> records) {
    return records.stream().map(record -> {
      Map<String, Object> normalized = new LinkedHashMap<>(record);
      normalized.replaceAll((key, value) -> {
        if (value instanceof Timestamp timestamp) {
          return timestamp.toLocalDateTime().format(DATE_TIME);
        }
        if (value instanceof LocalDateTime dateTime) {
          return dateTime.format(DATE_TIME);
        }
        return value;
      });
      return normalized;
    }).toList();
  }
}
