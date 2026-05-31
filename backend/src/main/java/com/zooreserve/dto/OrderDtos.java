package com.zooreserve.dto;

import com.zooreserve.domain.enums.OrderStatus;
import com.zooreserve.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {
  private OrderDtos() {
  }

  public record OrderItemRequest(String ticketTypeCode, int quantity) {
  }

  public record CreateOrderRequest(LocalDate visitDate, String session, List<OrderItemRequest> items,
                                   Long couponId, Long annualPassId, List<Long> visitorProfileIds,
                                   String orderType, Long activityId, Integer quantity) {
  }

  public record OrderItemResponse(String ticketTypeCode, String ticketTypeName, int quantity, BigDecimal unitPrice) {
  }

  public record OrderResponse(Long id, String orderNo, LocalDate visitDate, String session, int peopleCount,
                              BigDecimal amount, OrderStatus orderStatus, PaymentStatus paymentStatus,
                              LocalDateTime createdAt, String orderType, BigDecimal originalAmount,
                              BigDecimal discountAmount, List<OrderItemResponse> items) {
  }

  public record QrCodeResponse(String orderNo, String qrContent, LocalDate visitDate, String entranceNotice) {
  }
}
