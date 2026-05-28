package com.zooreserve.service;

import com.zooreserve.dto.ActivityDtos.ActivityResponse;
import com.zooreserve.dto.TicketDtos.TicketInventoryResponse;
import com.zooreserve.dto.TicketDtos.TicketTypeResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MockCatalogService {

  public List<TicketTypeResponse> ticketTypes() {
    return List.of(
        new TicketTypeResponse("ADULT", "成人票", new BigDecimal("120.00"), "18 周岁以上游客", false),
        new TicketTypeResponse("CHILD", "儿童票", new BigDecimal("60.00"), "1.2 米至 1.5 米儿童", false),
        new TicketTypeResponse("SENIOR", "老人票", new BigDecimal("60.00"), "65 周岁以上游客", false),
        new TicketTypeResponse("ANNUAL", "亲子年卡", new BigDecimal("699.00"), "两大一小全年畅游", true)
    );
  }

  public List<TicketInventoryResponse> inventory(LocalDate date, String session) {
    String normalizedSession = session == null || session.isBlank() ? "AM" : session;
    return ticketTypes().stream()
        .map(type -> new TicketInventoryResponse(date, normalizedSession, type.code(), 800, "ANNUAL".equals(type.code()) ? 99 : 520))
        .toList();
  }

  public List<ActivityResponse> activities() {
    return List.of(
        new ActivityResponse(1L, "长颈鹿科普讲解", "科普讲解", LocalDateTime.of(2026, 6, 1, 10, 0), 40, 18, "草食动物区"),
        new ActivityResponse(2L, "小小饲养员亲子课堂", "亲子课堂", LocalDateTime.of(2026, 6, 1, 14, 30), 24, 21, "自然教育中心"),
        new ActivityResponse(3L, "夏夜动物园", "夜游活动", LocalDateTime.of(2026, 6, 2, 19, 0), 100, 63, "主入口集合")
    );
  }
}
