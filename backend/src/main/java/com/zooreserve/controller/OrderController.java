package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.dto.OrderDtos.CreateOrderRequest;
import com.zooreserve.dto.OrderDtos.OrderResponse;
import com.zooreserve.dto.OrderDtos.QrCodeResponse;
import com.zooreserve.service.MockOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final MockOrderService orderService;

  public OrderController(MockOrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  public ApiResponse<OrderResponse> create(@RequestBody CreateOrderRequest request) {
    return ApiResponse.ok(orderService.create(request));
  }

  @GetMapping("/my")
  public ApiResponse<List<OrderResponse>> myOrders() {
    return ApiResponse.ok(orderService.myOrders());
  }

  @GetMapping("/{id}/qrcode")
  public ApiResponse<QrCodeResponse> qrcode(@PathVariable Long id) {
    return ApiResponse.ok(orderService.qrcode(id));
  }
}
