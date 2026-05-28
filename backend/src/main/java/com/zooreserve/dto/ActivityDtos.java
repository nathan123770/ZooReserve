package com.zooreserve.dto;

import java.time.LocalDateTime;

public final class ActivityDtos {
  private ActivityDtos() {
  }

  public record ActivityResponse(Long id, String title, String category, LocalDateTime startTime, int capacity,
                                 int signedCount, String location) {
  }
}
