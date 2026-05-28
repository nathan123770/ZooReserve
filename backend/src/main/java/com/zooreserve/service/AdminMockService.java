package com.zooreserve.service;

import com.zooreserve.common.PageResult;
import com.zooreserve.dto.AdminDtos.DashboardSummary;
import com.zooreserve.dto.AdminDtos.SimpleRecord;
import com.zooreserve.dto.OrderDtos.OrderResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AdminMockService {
  private final MockOrderService orderService;
  private final AtomicLong sequence = new AtomicLong(3);
  private final Map<String, List<SimpleRecord>> records = new ConcurrentHashMap<>();

  public AdminMockService(MockOrderService orderService) {
    this.orderService = orderService;
    records.put("管理", List.of(
        new SimpleRecord(1L, "管理示例一", "ENABLED", "用于首版全量骨架的种子数据"),
        new SimpleRecord(2L, "管理示例二", "DRAFT", "后续可替换为真实 CRUD")
    ));
  }

  public DashboardSummary dashboard() {
    return new DashboardSummary(386, new BigDecimal("46820.00"), 219, 1780,
        List.of(Map.of("date", "05-24", "count", 42), Map.of("date", "05-25", "count", 57), Map.of("date", "05-26", "count", 81)));
  }

  public PageResult<OrderResponse> orders() {
    return PageResult.firstPage(List.of(orderService.seedOrder()));
  }

  public PageResult<SimpleRecord> simpleRecords(String domain) {
    return PageResult.firstPage(records.getOrDefault(domain, records.get("管理")));
  }

  public SimpleRecord create(String domain, SimpleRecord request) {
    SimpleRecord created = new SimpleRecord(sequence.getAndIncrement(), request.name(), "ENABLED", request.description());
    records.compute(domain, (key, existing) -> {
      List<SimpleRecord> current = existing == null ? List.of() : existing;
      java.util.ArrayList<SimpleRecord> next = new java.util.ArrayList<>(current);
      next.add(0, created);
      return List.copyOf(next);
    });
    return created;
  }
}
