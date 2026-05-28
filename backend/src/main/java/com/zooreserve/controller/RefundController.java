package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

  @PostMapping("/apply")
  public ApiResponse<Map<String, Object>> apply() {
    return ApiResponse.ok(Map.of("refundNo", "RF202606010001", "status", "REFUNDING"));
  }
}
