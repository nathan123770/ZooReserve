package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MarketingController {
  private final MemberService memberService;

  public MarketingController(MemberService memberService) {
    this.memberService = memberService;
  }

  @GetMapping("/notices")
  public ApiResponse<List<Map<String, Object>>> notices(@RequestParam(required = false) String position) {
    return ApiResponse.ok(memberService.notices(position));
  }

  @GetMapping("/coupons/available")
  public ApiResponse<List<Map<String, Object>>> availableCoupons() {
    return ApiResponse.ok(memberService.availableCoupons());
  }
}
