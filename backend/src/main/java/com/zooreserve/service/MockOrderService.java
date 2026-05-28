package com.zooreserve.service;

import com.zooreserve.domain.enums.CheckinStatus;
import com.zooreserve.domain.enums.OrderStatus;
import com.zooreserve.domain.enums.PaymentStatus;
import com.zooreserve.dto.CheckinDtos.CheckinResponse;
import com.zooreserve.dto.CheckinDtos.ManualCheckinRequest;
import com.zooreserve.dto.CheckinDtos.ScanRequest;
import com.zooreserve.dto.OrderDtos.CreateOrderRequest;
import com.zooreserve.dto.OrderDtos.OrderResponse;
import com.zooreserve.dto.OrderDtos.QrCodeResponse;
import com.zooreserve.dto.PaymentDtos.PrepayRequest;
import com.zooreserve.dto.PaymentDtos.PrepayResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MockOrderService {
  private final AtomicLong sequence = new AtomicLong(2);
  private final Map<String, OrderResponse> orders = new ConcurrentHashMap<>();
  private final Map<String, PrepayResponse> payments = new ConcurrentHashMap<>();
  private final Map<String, CheckinResponse> checkins = new ConcurrentHashMap<>();
  private final Map<String, Integer> inventory = new ConcurrentHashMap<>();

  public MockOrderService() {
    inventory.put("2026-06-01:AM:ADULT", 520);
    inventory.put("2026-06-01:AM:CHILD", 520);
    inventory.put("2026-06-01:AM:SENIOR", 520);
    inventory.put("2026-06-01:PM:ADULT", 520);
    orders.put(seedOrder().orderNo(), seedOrder());
  }

  public synchronized OrderResponse create(CreateOrderRequest request) {
    long id = sequence.getAndIncrement();
    List<com.zooreserve.dto.OrderDtos.OrderItemRequest> items = request.items() == null ? List.of() : request.items();
    int people = items.isEmpty() ? 1 : items.stream().mapToInt(item -> Math.max(1, item.quantity())).sum();
    for (com.zooreserve.dto.OrderDtos.OrderItemRequest item : items) {
      String key = inventoryKey(request.visitDate(), request.session(), item.ticketTypeCode());
      int remaining = inventory.getOrDefault(key, 520);
      if (remaining < item.quantity()) {
        throw new IllegalStateException("库存不足");
      }
      inventory.put(key, remaining - item.quantity());
    }
    BigDecimal amount = new BigDecimal("120.00").multiply(BigDecimal.valueOf(people));
    OrderResponse order = new OrderResponse(id, "ZR20260601000" + id, request.visitDate(), request.session(), people, amount,
        OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID, LocalDateTime.now());
    orders.put(order.orderNo(), order);
    return order;
  }

  public List<OrderResponse> myOrders() {
    return orders.values().stream().toList();
  }

  public QrCodeResponse qrcode(Long id) {
    OrderResponse order = orders.values().stream()
        .filter(record -> record.id().equals(id))
        .findFirst()
        .orElse(seedOrder());
    return new QrCodeResponse(order.orderNo(), "ZOORESERVE:ORDER:" + order.orderNo(), order.visitDate(),
        "请携带有效身份证件，于预约场次内从主入口核验入园。");
  }

  public PrepayResponse prepay(PrepayRequest request) {
    return payments.computeIfAbsent(request.orderNo(), orderNo -> {
      OrderResponse order = orders.getOrDefault(orderNo, seedOrder());
      OrderResponse paidOrder = new OrderResponse(order.id(), order.orderNo(), order.visitDate(), order.session(),
          order.peopleCount(), order.amount(), OrderStatus.PAID, PaymentStatus.PAY_SUCCESS, order.createdAt());
      orders.put(orderNo, paidOrder);
      return new PrepayResponse(orderNo, request.channel() == null ? "MOCK" : request.channel(),
          paidOrder.amount(), PaymentStatus.PAY_SUCCESS, "mock://pay/" + orderNo);
    });
  }

  public CheckinResponse scan(ScanRequest request) {
    String orderNo = request.qrContent() == null ? "ZR202606010001" : request.qrContent().replace("ZOORESERVE:ORDER:", "");
    if (checkins.containsKey(orderNo)) {
      CheckinResponse checked = checkins.get(orderNo);
      return new CheckinResponse(orderNo, checked.ticketSummary(), checked.peopleCount(), checked.visitDate(),
          CheckinStatus.EXCEPTION, "重复核销已拦截", LocalDateTime.now());
    }
    OrderResponse order = orders.getOrDefault(orderNo, seedOrder());
    CheckinResponse response = new CheckinResponse(orderNo, "成人票 x" + order.peopleCount(), order.peopleCount(), order.visitDate(),
        CheckinStatus.CHECKED_IN, "扫码核销成功", LocalDateTime.now());
    checkins.put(orderNo, response);
    orders.put(orderNo, new OrderResponse(order.id(), order.orderNo(), order.visitDate(), order.session(), order.peopleCount(),
        order.amount(), OrderStatus.CHECKED_IN, order.paymentStatus(), order.createdAt()));
    return response;
  }

  public CheckinResponse manual(ManualCheckinRequest request) {
    return new CheckinResponse(request.orderNo(), "成人票 x2", 2, LocalDate.of(2026, 6, 1),
        CheckinStatus.CHECKED_IN, request.remark(), LocalDateTime.now());
  }

  public OrderResponse seedOrder() {
    return new OrderResponse(1L, "ZR202606010001", LocalDate.of(2026, 6, 1), "AM", 2,
        new BigDecimal("240.00"), OrderStatus.PAID, PaymentStatus.PAY_SUCCESS, LocalDateTime.now().minusHours(2));
  }

  private String inventoryKey(LocalDate visitDate, String session, String ticketTypeCode) {
    return visitDate + ":" + (session == null ? "AM" : session) + ":" + ticketTypeCode;
  }
}
