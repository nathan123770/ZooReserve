package com.zooreserve.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public final class ActivityDtos {
  private ActivityDtos() {
  }

  public record ActivityResponse(Long id, String title, String category, LocalDateTime startTime, int capacity,
                                 int signedCount, String location, boolean paid, BigDecimal price, String couponScope) {
  }
}
