package com.zooreserve.dto;

import com.zooreserve.domain.enums.PaymentStatus;

import java.math.BigDecimal;

public final class PaymentDtos {
  private PaymentDtos() {
  }

  public record PrepayRequest(String orderNo, String channel) {
  }

  public record PrepayResponse(String orderNo, String channel, BigDecimal amount, PaymentStatus paymentStatus,
                               String mockPayUrl) {
  }
}
