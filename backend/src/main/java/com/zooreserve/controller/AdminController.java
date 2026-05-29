package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.common.PageResult;
import com.zooreserve.dto.CheckinDtos.CheckinResponse;
import com.zooreserve.dto.CheckinDtos.ManualCheckinRequest;
import com.zooreserve.dto.AdminDtos.DashboardSummary;
import com.zooreserve.service.AdminMockService;
import com.zooreserve.service.MockOrderService;
import jakarta.servlet.http.HttpServletRequest;
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
  private final MockOrderService orderService;

  public AdminController(AdminMockService adminMockService, MockOrderService orderService) {
    this.adminMockService = adminMockService;
    this.orderService = orderService;
  }

  @GetMapping("/dashboard/summary")
  public ApiResponse<DashboardSummary> dashboard() {
    return ApiResponse.ok(adminMockService.dashboard());
  }

  @GetMapping("/orders")
  public ApiResponse<PageResult<Map<String, Object>>> orders() {
    return ApiResponse.ok(adminMockService.records("orders"));
  }

  @PostMapping({"/tickets", "/tickets/types"})
  public ApiResponse<Map<String, Object>> createTicketType(@RequestBody(required = false) Map<String, Object> request) {
    return ApiResponse.ok(adminMockService.create("tickets", request == null ? Map.of() : request));
  }

  @PutMapping("/tickets/inventory")
  public ApiResponse<Map<String, Object>> updateInventory(@RequestBody Map<String, Object> payload) {
    return ApiResponse.ok(adminMockService.updateInventory(payload));
  }

  @PostMapping("/refunds/{id}/approve")
  public ApiResponse<Map<String, Object>> approveRefund(@PathVariable Long id) {
    Map<String, Object> result = orderService.approveRefund(id);
    adminMockService.logOperation("APPROVE_REFUND", "refund:" + id, "后台审核退款");
    return ApiResponse.ok(result);
  }

  @PostMapping({"/activities", "/animals", "/marketing", "/system", "/notices"})
  public ApiResponse<Map<String, Object>> createResource(@RequestBody(required = false) Map<String, Object> request,
                                                         HttpServletRequest servletRequest) {
    return ApiResponse.ok(adminMockService.create(domainFrom(servletRequest), request == null ? Map.of() : request));
  }

  @GetMapping({"/tickets", "/activities", "/animals", "/notices", "/users", "/marketing", "/checkins", "/system"})
  public ApiResponse<PageResult<Map<String, Object>>> listResource(HttpServletRequest request) {
    return ApiResponse.ok(adminMockService.records(domainFrom(request), request.getParameterMap()));
  }

  @PutMapping({"/tickets/{id}", "/activities/{id}", "/animals/{id}", "/marketing/{id}", "/system/{id}"})
  public ApiResponse<Map<String, Object>> updateResource(@PathVariable Long id,
                                                         @RequestBody Map<String, Object> payload,
                                                         HttpServletRequest request) {
    return ApiResponse.ok(adminMockService.update(domainFromNested(request), id, payload));
  }

  @PutMapping({"/tickets/{id}/status", "/activities/{id}/status", "/animals/{id}/status", "/marketing/{id}/status", "/system/{id}/status"})
  public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> payload,
                                                       HttpServletRequest request) {
    return ApiResponse.ok(adminMockService.toggleStatus(domainFromNestedStatus(request), id, payload));
  }

  @PostMapping("/checkins/manual")
  public ApiResponse<CheckinResponse> manualCheckin(@RequestBody ManualCheckinRequest request) {
    CheckinResponse response = orderService.manual(request);
    adminMockService.logOperation("MANUAL_CHECKIN", response.orderNo(), "后台人工核销");
    return ApiResponse.ok(response);
  }

  @GetMapping("/logs")
  public ApiResponse<PageResult<Map<String, Object>>> logs() {
    return ApiResponse.ok(adminMockService.records("logs"));
  }

  private String domainFrom(HttpServletRequest request) {
    String domain = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
    if ("notices".equals(domain) || "users".equals(domain)) {
      return "notices".equals(domain) ? "marketing" : "system";
    }
    return domain;
  }

  private String domainFromNested(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.substring(path.indexOf("/admin/") + 7, path.lastIndexOf('/'));
  }

  private String domainFromNestedStatus(HttpServletRequest request) {
    String nested = domainFromNested(request);
    return nested.substring(0, nested.lastIndexOf('/'));
  }
}
