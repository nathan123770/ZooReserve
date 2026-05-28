package com.zooreserve.dto;

import com.zooreserve.domain.enums.CheckinStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CheckinDtos {
  private CheckinDtos() {
  }

  public record ScanRequest(String qrContent, Long checkerId) {
  }

  public record ManualCheckinRequest(String orderNo, String phone, Long checkerId, String remark) {
  }

  public record CheckinResponse(String orderNo, String ticketSummary, int peopleCount, LocalDate visitDate,
                                CheckinStatus checkinStatus, String remark, LocalDateTime checkedAt) {
  }
}
