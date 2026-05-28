package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.dto.CheckinDtos.CheckinResponse;
import com.zooreserve.dto.CheckinDtos.ManualCheckinRequest;
import com.zooreserve.dto.CheckinDtos.ScanRequest;
import com.zooreserve.service.MockOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {
  private final MockOrderService orderService;

  public CheckinController(MockOrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping("/scan")
  public ApiResponse<CheckinResponse> scan(@RequestBody ScanRequest request) {
    return ApiResponse.ok(orderService.scan(request));
  }

  @GetMapping("/order/search")
  public ApiResponse<CheckinResponse> search(@RequestParam(required = false) String orderNo,
                                             @RequestParam(required = false) String phone) {
    return ApiResponse.ok(orderService.manual(new ManualCheckinRequest(orderNo == null ? "ZR202606010001" : orderNo, phone, 1L, "查询成功")));
  }

  @PostMapping("/manual")
  public ApiResponse<CheckinResponse> manual(@RequestBody ManualCheckinRequest request) {
    return ApiResponse.ok(orderService.manual(request));
  }
}
