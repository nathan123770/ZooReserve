package com.zooreserve.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class AdminDtos {
  private AdminDtos() {
  }

  public record DashboardSummary(long todayReservations, BigDecimal paidAmount, long checkedInPeople,
                                 long remainingCapacity, List<Map<String, Object>> activityTrend) {
  }

  public record SimpleRecord(Long id, String name, String status, String description) {
  }
}
