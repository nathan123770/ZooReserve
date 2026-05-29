package com.zooreserve.service;

import com.zooreserve.dto.ActivityDtos.ActivityResponse;
import com.zooreserve.dto.TicketDtos.TicketInventoryResponse;
import com.zooreserve.dto.TicketDtos.TicketTypeResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MockCatalogService {
  private final JdbcTemplate jdbcTemplate;

  public MockCatalogService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<TicketTypeResponse> ticketTypes() {
    return jdbcTemplate.query("""
        SELECT code, name, price, description
        FROM ticket_type
        WHERE status = 'ENABLED'
        ORDER BY id
        """, (rs, rowNum) -> new TicketTypeResponse(
        rs.getString("code"),
        rs.getString("name"),
        rs.getBigDecimal("price"),
        rs.getString("description"),
        "ANNUAL".equals(rs.getString("code"))));
  }

  public List<TicketInventoryResponse> inventory(LocalDate date, String session) {
    String normalizedSession = session == null || session.isBlank() ? "AM" : session;
    return jdbcTemplate.query("""
        SELECT ti.visit_date, ti.session_code, tt.code, ti.capacity, ti.remaining,
               COALESCE(di.capacity, ti.capacity) AS daily_capacity,
               COALESCE(di.remaining, ti.remaining) AS daily_remaining
        FROM ticket_inventory ti
        JOIN ticket_type tt ON tt.id = ti.ticket_type_id
        LEFT JOIN daily_ticket_inventory di ON di.ticket_type_id = ti.ticket_type_id AND di.visit_date = ti.visit_date
        WHERE ti.visit_date = ? AND ti.session_code = ? AND tt.status = 'ENABLED'
        ORDER BY tt.id
        """, (rs, rowNum) -> new TicketInventoryResponse(
        rs.getDate("visit_date").toLocalDate(),
        rs.getString("session_code"),
        rs.getString("code"),
        rs.getInt("capacity"),
        rs.getInt("remaining"),
        rs.getInt("daily_capacity"),
        rs.getInt("daily_remaining")), date, normalizedSession);
  }

  public List<ActivityResponse> activities() {
    return jdbcTemplate.query("""
        SELECT a.id, a.title, a.category, a.start_time, a.capacity, a.location,
               COUNT(s.id) AS signed_count
        FROM activity a
        LEFT JOIN activity_signup s ON s.activity_id = a.id AND s.status = 'SIGNED'
        WHERE a.status = 'PUBLISHED'
        GROUP BY a.id, a.title, a.category, a.start_time, a.capacity, a.location
        ORDER BY a.start_time
        """, (rs, rowNum) -> new ActivityResponse(
        rs.getLong("id"),
        rs.getString("title"),
        rs.getString("category"),
        rs.getTimestamp("start_time").toLocalDateTime(),
        rs.getInt("capacity"),
        rs.getInt("signed_count"),
        rs.getString("location")));
  }

  public void signup(Long activityId, Long userId) {
    Integer existing = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM activity_signup WHERE activity_id = ? AND user_id = ?",
        Integer.class, activityId, userId);
    if (existing != null && existing > 0) {
      return;
    }
    Integer capacity = jdbcTemplate.queryForObject("SELECT capacity FROM activity WHERE id = ?", Integer.class, activityId);
    Integer signed = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM activity_signup WHERE activity_id = ? AND status = 'SIGNED'",
        Integer.class, activityId);
    if (capacity == null || signed == null || signed >= capacity) {
      throw new IllegalStateException("活动名额不足");
    }
    jdbcTemplate.update("INSERT INTO activity_signup (activity_id, user_id, status) VALUES (?, ?, 'SIGNED')", activityId, userId);
  }
}
