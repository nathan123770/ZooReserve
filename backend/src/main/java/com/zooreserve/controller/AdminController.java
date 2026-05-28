package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.common.PageResult;
import com.zooreserve.dto.AdminDtos.DashboardSummary;
import com.zooreserve.dto.AdminDtos.SimpleRecord;
import com.zooreserve.dto.OrderDtos.OrderResponse;
import com.zooreserve.service.AdminMockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final AdminMockService adminMockService;

  public AdminController(AdminMockService adminMockService) {
    this.adminMockService = adminMockService;
  }

  @GetMapping("/dashboard/summary")
  public ApiResponse<DashboardSummary> dashboard() {
    return ApiResponse.ok(adminMockService.dashboard());
  }

  @GetMapping("/orders")
  public ApiResponse<PageResult<OrderResponse>> orders() {
    return ApiResponse.ok(adminMockService.orders());
  }

  @PostMapping("/tickets/types")
  public ApiResponse<SimpleRecord> createTicketType(@RequestBody(required = false) SimpleRecord request) {
    SimpleRecord payload = request == null ? new SimpleRecord(null, "新票种", "ENABLED", "后台创建") : request;
    return ApiResponse.ok(adminMockService.create("tickets", payload));
  }

  @PutMapping("/tickets/inventory")
  public ApiResponse<Map<String, Object>> updateInventory() {
    return ApiResponse.ok(Map.of("updated", true));
  }

  @PostMapping("/refunds/{id}/approve")
  public ApiResponse<Map<String, Object>> approveRefund(@PathVariable Long id) {
    return ApiResponse.ok(Map.of("refundId", id, "status", "REFUNDED"));
  }

  @PostMapping({"/activities", "/animals", "/notices"})
  public ApiResponse<SimpleRecord> createResource(@RequestBody(required = false) SimpleRecord request) {
    SimpleRecord payload = request == null ? new SimpleRecord(null, "新资源", "ENABLED", "后台创建") : request;
    return ApiResponse.ok(adminMockService.create("管理", payload));
  }

  @GetMapping({"/tickets", "/activities", "/animals", "/notices", "/users", "/marketing", "/checkins", "/system"})
  public ApiResponse<PageResult<SimpleRecord>> listResource() {
    return ApiResponse.ok(adminMockService.simpleRecords("管理"));
  }

  @GetMapping("/logs")
  public ApiResponse<PageResult<SimpleRecord>> logs() {
    return ApiResponse.ok(adminMockService.simpleRecords("操作日志"));
  }
}
