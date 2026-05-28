package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.dto.PaymentDtos.PrepayRequest;
import com.zooreserve.dto.PaymentDtos.PrepayResponse;
import com.zooreserve.service.MockOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  private final MockOrderService orderService;

  public PaymentController(MockOrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping("/prepay")
  public ApiResponse<PrepayResponse> prepay(@RequestBody PrepayRequest request) {
    return ApiResponse.ok(orderService.prepay(request));
  }

  @PostMapping("/callback")
  public ApiResponse<Map<String, Object>> callback(@RequestBody Map<String, Object> payload) {
    return ApiResponse.ok(Map.of("handled", true, "idempotent", true, "payload", payload));
  }
}
