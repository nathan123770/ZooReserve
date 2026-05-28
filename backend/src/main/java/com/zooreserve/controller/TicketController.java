package com.zooreserve.controller;

import com.zooreserve.common.ApiResponse;
import com.zooreserve.dto.TicketDtos.TicketInventoryResponse;
import com.zooreserve.dto.TicketDtos.TicketTypeResponse;
import com.zooreserve.service.MockCatalogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
  private final MockCatalogService catalogService;

  public TicketController(MockCatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/types")
  public ApiResponse<List<TicketTypeResponse>> types() {
    return ApiResponse.ok(catalogService.ticketTypes());
  }

  @GetMapping("/inventory")
  public ApiResponse<List<TicketInventoryResponse>> inventory(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String session) {
    return ApiResponse.ok(catalogService.inventory(date, session));
  }
}
