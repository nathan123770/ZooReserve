package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.dto.ActivityDtos.ActivityResponse;
import com.zooreserve.service.MockCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
  private final MockCatalogService catalogService;

  public ActivityController(MockCatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping
  public ApiResponse<List<ActivityResponse>> activities() {
    return ApiResponse.ok(catalogService.activities());
  }

  @PostMapping("/{id}/signup")
  public ApiResponse<Map<String, Object>> signup(@PathVariable Long id) {
    return ApiResponse.ok(Map.of("activityId", id, "status", "SIGNED"));
  }
}
