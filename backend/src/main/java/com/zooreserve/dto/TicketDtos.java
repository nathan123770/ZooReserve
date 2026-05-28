package com.zooreserve.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TicketDtos {
  private TicketDtos() {
  }

  public record TicketTypeResponse(String code, String name, BigDecimal price, String description, boolean annualPass) {
  }

  public record TicketInventoryResponse(LocalDate date, String session, String ticketTypeCode, int capacity, int remaining) {
  }
}
