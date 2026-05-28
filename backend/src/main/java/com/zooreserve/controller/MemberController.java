package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.service.MemberService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
public class MemberController {
  private final MemberService memberService;

  public MemberController(MemberService memberService) {
    this.memberService = memberService;
  }

  @GetMapping("/profiles")
  public ApiResponse<List<Map<String, Object>>> profiles() {
    return ApiResponse.ok(memberService.profiles());
  }

  @PostMapping("/profiles")
  public ApiResponse<Map<String, Object>> createProfile(@RequestBody Map<String, Object> payload) {
    return ApiResponse.ok(memberService.createProfile(payload));
  }

  @PutMapping("/profiles/{id}")
  public ApiResponse<Map<String, Object>> updateProfile(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
    return ApiResponse.ok(memberService.updateProfile(id, payload));
  }

  @DeleteMapping("/profiles/{id}")
  public ApiResponse<Map<String, Object>> deleteProfile(@PathVariable Long id) {
    return ApiResponse.ok(memberService.deleteProfile(id));
  }

  @GetMapping("/coupons")
  public ApiResponse<List<Map<String, Object>>> coupons() {
    return ApiResponse.ok(memberService.coupons());
  }

  @PostMapping("/coupons/{couponId}/claim")
  public ApiResponse<Map<String, Object>> claimCoupon(@PathVariable Long couponId) {
    return ApiResponse.ok(memberService.claimCoupon(couponId));
  }

  @GetMapping("/annual-passes")
  public ApiResponse<List<Map<String, Object>>> annualPasses() {
    return ApiResponse.ok(memberService.annualPasses());
  }

  @PostMapping("/annual-passes/purchase")
  public ApiResponse<Map<String, Object>> purchaseAnnualPass(@RequestBody Map<String, Object> payload) {
    return ApiResponse.ok(memberService.purchaseAnnualPass(payload));
  }

  @PostMapping("/annual-passes/{id}/renew")
  public ApiResponse<Map<String, Object>> renewAnnualPass(@PathVariable Long id) {
    return ApiResponse.ok(memberService.renewAnnualPass(id));
  }

  @PostMapping("/annual-passes/{id}/holders")
  public ApiResponse<Map<String, Object>> addHolder(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
    return ApiResponse.ok(memberService.addHolder(id, payload));
  }
}
