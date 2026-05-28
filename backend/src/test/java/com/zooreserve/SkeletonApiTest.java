package com.zooreserve;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkeletonApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void publicTicketAndActivityEndpointsExposeSeedData() throws Exception {
    mockMvc.perform(get("/api/tickets/types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].code").value("ADULT"));

    mockMvc.perform(get("/api/tickets/inventory?date=2026-06-01&session=AM"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].remaining").isNumber());

    mockMvc.perform(get("/api/activities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].title").isString());
  }

  @Test
  void loginIssuesTokenAndRoleAwarePayload() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"admin123\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").isString())
        .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
  }

  @Test
  void adminEndpointsRequireAdminToken() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/summary"))
        .andExpect(status().isUnauthorized());

    String visitorToken = tokenFor("visitor", "VISITOR");
    mockMvc.perform(get("/api/admin/dashboard/summary")
            .header("Authorization", "Bearer " + visitorToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void visitorOrderPaymentQrAndCheckinFlowHaveMockResponses() throws Exception {
    String visitorToken = tokenFor("visitor", "VISITOR");
    String checkerToken = tokenFor("checker", "CHECKER");

    mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"visitDate\":\"2026-06-01\",\"session\":\"AM\",\"items\":[{\"ticketTypeCode\":\"ADULT\",\"quantity\":2}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(hasKey("orderNo")));

    mockMvc.perform(post("/api/payments/prepay")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderNo\":\"ZR202606010001\",\"channel\":\"MOCK\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.paymentStatus").value("PAY_SUCCESS"));

    mockMvc.perform(get("/api/orders/1/qrcode")
            .header("Authorization", "Bearer " + visitorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.qrContent").isString());

    mockMvc.perform(post("/api/checkin/scan")
            .header("Authorization", "Bearer " + checkerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"qrContent\":\"ZR202606010001\",\"checkerId\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.checkinStatus").value("CHECKED_IN"));
  }

  @Test
  void adminDashboardAndManagementEndpointsReturnSkeletonData() throws Exception {
    String adminToken = tokenFor("admin", "ADMIN");

    mockMvc.perform(get("/api/admin/dashboard/summary")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.todayReservations").isNumber());

    mockMvc.perform(get("/api/admin/orders")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records").isArray());

    mockMvc.perform(get("/api/admin/logs")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records").isArray());
  }

  private String tokenFor(String username, String role) throws Exception {
    String password = switch (role) {
      case "ADMIN" -> "admin123";
      case "CHECKER" -> "checker123";
      default -> "visitor123";
    };
    String login = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"role\":\"" + role + "\"}"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return login.split("\"token\":\"")[1].split("\"")[0];
  }
}
