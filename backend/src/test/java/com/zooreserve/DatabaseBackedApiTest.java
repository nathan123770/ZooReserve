package com.zooreserve;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatabaseBackedApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void loginRejectsInvalidOrMissingDatabaseCredentials() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"admin123\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.username").value("admin"));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"wrong\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"ghost\",\"password\":\"admin123\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"admin123\",\"role\":\"VISITOR\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registerCreatesDatabaseVisitorAndRejectsDuplicatePhone() throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"newvisitor\",\"password\":\"newpass123\",\"phone\":\"13800009999\",\"displayName\":\"新游客\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").isString())
        .andExpect(jsonPath("$.data.user.username").value("newvisitor"));

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE username = 'newvisitor'", Integer.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM user_role ur
        JOIN role r ON r.id = ur.role_id
        JOIN user u ON u.id = ur.user_id
        WHERE u.username = 'newvisitor' AND r.code = 'VISITOR' AND ur.user_type = 'VISITOR'
        """, Integer.class)).isEqualTo(1);

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"phonecopy\",\"password\":\"newpass123\",\"phone\":\"13800009999\",\"displayName\":\"重复手机\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("手机号已注册"));
  }

  @Test
  void ticketOrderPaymentAndCheckinFlowPersistsToDatabase() throws Exception {
    String visitorToken = tokenFor("visitor", "visitor123", "VISITOR");
    String checkerToken = tokenFor("checker", "checker123", "CHECKER");

    Integer beforeInventory = jdbcTemplate.queryForObject(
        "SELECT remaining FROM ticket_inventory WHERE visit_date = DATE '2026-06-01' AND session_code = 'AM' AND ticket_type_id = 1",
        Integer.class);
    Integer beforeDailyInventory = jdbcTemplate.queryForObject(
        "SELECT remaining FROM daily_ticket_inventory WHERE visit_date = DATE '2026-06-01' AND ticket_type_id = 1",
        Integer.class);
    Integer beforePmInventory = jdbcTemplate.queryForObject(
        "SELECT remaining FROM ticket_inventory WHERE visit_date = DATE '2026-06-01' AND session_code = 'PM' AND ticket_type_id = 1",
        Integer.class);

    String orderJson = mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"visitDate\":\"2026-06-01\",\"session\":\"AM\",\"items\":[{\"ticketTypeCode\":\"ADULT\",\"quantity\":2}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.orderNo").isString())
        .andExpect(jsonPath("$.data.orderType").value("TICKET"))
        .andExpect(jsonPath("$.data.originalAmount").value(240.0))
        .andExpect(jsonPath("$.data.discountAmount").value(0))
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andExpect(jsonPath("$.data.items[0].ticketTypeCode").value("ADULT"))
        .andExpect(jsonPath("$.data.items[0].quantity").value(2))
        .andReturn()
        .getResponse()
        .getContentAsString();
    String orderNo = orderJson.split("\"orderNo\":\"")[1].split("\"")[0];

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_order WHERE order_no = ?", Integer.class, orderNo))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT remaining FROM ticket_inventory WHERE ticket_type_id = 1 AND visit_date = DATE '2026-06-01' AND session_code = 'AM'", Integer.class))
        .isEqualTo(beforeInventory - 2);
    assertThat(jdbcTemplate.queryForObject("SELECT remaining FROM daily_ticket_inventory WHERE ticket_type_id = 1 AND visit_date = DATE '2026-06-01'", Integer.class))
        .isEqualTo(beforeDailyInventory - 2);
    assertThat(jdbcTemplate.queryForObject("SELECT remaining FROM ticket_inventory WHERE ticket_type_id = 1 AND visit_date = DATE '2026-06-01' AND session_code = 'PM'", Integer.class))
        .isEqualTo(beforePmInventory);

    mockMvc.perform(post("/api/payments/prepay")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderNo\":\"" + orderNo + "\",\"channel\":\"MOCK\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.paymentStatus").value("PAY_SUCCESS"));
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_record WHERE order_id = (SELECT id FROM reservation_order WHERE order_no = ?)", Integer.class, orderNo))
        .isEqualTo(1);

    mockMvc.perform(post("/api/checkin/scan")
            .header("Authorization", "Bearer " + checkerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"qrContent\":\"ZOORESERVE:ORDER:" + orderNo + "\",\"checkerId\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.checkinStatus").value("CHECKED_IN"));
    assertThat(jdbcTemplate.queryForObject("SELECT order_status FROM reservation_order WHERE order_no = ?", String.class, orderNo))
        .isEqualTo("CHECKED_IN");
  }

  @Test
  void orderCouponCancelRefundAndAnnualPassFlowPersistsToDatabase() throws Exception {
    String visitorToken = tokenFor("visitor", "visitor123", "VISITOR");
    String adminToken = tokenFor("admin", "admin123", "ADMIN");

    Long couponId = jdbcTemplate.queryForObject("""
        SELECT uc.id
        FROM user_coupon uc
        JOIN coupon c ON c.id = uc.coupon_id
        JOIN user u ON u.id = uc.user_id
        WHERE u.username = 'visitor' AND uc.status = 'UNUSED'
        ORDER BY uc.id
        LIMIT 1
        """, Long.class);

    String discountedJson = mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"visitDate\":\"2026-06-01\",\"session\":\"PM\",\"couponId\":" + couponId + ",\"items\":[{\"ticketTypeCode\":\"ADULT\",\"quantity\":2}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.amount").value(210.0))
        .andExpect(jsonPath("$.data.orderType").value("TICKET"))
        .andExpect(jsonPath("$.data.originalAmount").value(240.0))
        .andExpect(jsonPath("$.data.discountAmount").value(30.0))
        .andExpect(jsonPath("$.data.items[0].ticketTypeCode").value("ADULT"))
        .andExpect(jsonPath("$.data.items[0].quantity").value(2))
        .andReturn()
        .getResponse()
        .getContentAsString();
    String discountedNo = discountedJson.split("\"orderNo\":\"")[1].split("\"")[0];

    assertThat(jdbcTemplate.queryForObject("SELECT status FROM user_coupon WHERE id = ?", String.class, couponId))
        .isEqualTo("LOCKED");

    mockMvc.perform(post("/api/payments/prepay")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderNo\":\"" + discountedNo + "\",\"channel\":\"MOCK\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.paymentStatus").value("PAY_SUCCESS"));
    assertThat(jdbcTemplate.queryForObject("SELECT status FROM user_coupon WHERE id = ?", String.class, couponId))
        .isEqualTo("USED");

    Integer beforeCancelInventory = jdbcTemplate.queryForObject("""
        SELECT ti.remaining
        FROM ticket_inventory ti
        JOIN ticket_type tt ON tt.id = ti.ticket_type_id
        WHERE ti.visit_date = DATE '2026-06-02' AND ti.session_code = 'AM' AND tt.code = 'CHILD'
        """, Integer.class);
    String cancelJson = mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"visitDate\":\"2026-06-02\",\"session\":\"AM\",\"items\":[{\"ticketTypeCode\":\"CHILD\",\"quantity\":1}]}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    Long cancelId = Long.valueOf(cancelJson.split("\"id\":")[1].split(",")[0]);
    mockMvc.perform(post("/api/orders/" + cancelId + "/cancel")
            .header("Authorization", "Bearer " + visitorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"))
        .andExpect(jsonPath("$.data.items[0].ticketTypeCode").value("CHILD"))
        .andExpect(jsonPath("$.data.originalAmount").value(60.0));
    assertThat(jdbcTemplate.queryForObject("""
        SELECT ti.remaining
        FROM ticket_inventory ti
        JOIN ticket_type tt ON tt.id = ti.ticket_type_id
        WHERE ti.visit_date = DATE '2026-06-02' AND ti.session_code = 'AM' AND tt.code = 'CHILD'
        """, Integer.class)).isEqualTo(beforeCancelInventory);

    mockMvc.perform(post("/api/orders/" + discountedNo + "/refund")
            .header("Authorization", "Bearer " + visitorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.orderStatus").value("REFUNDING"))
        .andExpect(jsonPath("$.data.items[0].ticketTypeCode").value("ADULT"))
        .andExpect(jsonPath("$.data.discountAmount").value(30.0));
    Long refundId = jdbcTemplate.queryForObject("""
        SELECT rr.id
        FROM refund_record rr
        JOIN reservation_order ro ON ro.id = rr.order_id
        WHERE ro.order_no = ?
        """, Long.class, discountedNo);
    mockMvc.perform(post("/api/admin/refunds/" + refundId + "/approve")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REFUNDED"));

    Long annualPassId = jdbcTemplate.queryForObject("SELECT id FROM annual_pass WHERE status = 'ACTIVE' LIMIT 1", Long.class);
    String annualJson = mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"visitDate\":\"2026-06-03\",\"session\":\"AM\",\"orderType\":\"ANNUAL_PASS\",\"annualPassId\":" + annualPassId + ",\"items\":[{\"ticketTypeCode\":\"ADULT\",\"quantity\":1}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.amount").value(0))
        .andExpect(jsonPath("$.data.orderType").value("ANNUAL_PASS"))
        .andExpect(jsonPath("$.data.originalAmount").value(120.0))
        .andExpect(jsonPath("$.data.discountAmount").value(120.0))
        .andExpect(jsonPath("$.data.items[0].ticketTypeCode").value("ADULT"))
        .andExpect(jsonPath("$.data.paymentStatus").value("PAY_SUCCESS"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    String annualNo = annualJson.split("\"orderNo\":\"")[1].split("\"")[0];

    mockMvc.perform(post("/api/checkin/scan")
            .header("Authorization", "Bearer " + tokenFor("checker", "checker123", "CHECKER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"qrContent\":\"ZOORESERVE:ORDER:" + annualNo + "\",\"checkerId\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.checkinStatus").value("CHECKED_IN"));
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM annual_pass_usage WHERE annual_pass_id = ?", Integer.class, annualPassId))
        .isEqualTo(1);

    java.sql.Date beforeRenewExpiresAt = jdbcTemplate.queryForObject("SELECT expires_at FROM annual_pass WHERE id = ?",
        java.sql.Date.class, annualPassId);
    String renewalJson = mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"visitDate\":\"2026-06-03\",\"session\":\"AM\",\"orderType\":\"ANNUAL_PASS_RENEWAL\",\"annualPassId\":" + annualPassId + ",\"items\":[{\"ticketTypeCode\":\"ANNUAL\",\"quantity\":1}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.orderType").value("ANNUAL_PASS_RENEWAL"))
        .andExpect(jsonPath("$.data.amount").value(699.0))
        .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    String renewalNo = renewalJson.split("\"orderNo\":\"")[1].split("\"")[0];
    assertThat(jdbcTemplate.queryForObject("SELECT expires_at FROM annual_pass WHERE id = ?",
        java.sql.Date.class, annualPassId)).isEqualTo(beforeRenewExpiresAt);

    mockMvc.perform(post("/api/payments/prepay")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderNo\":\"" + renewalNo + "\",\"channel\":\"MOCK\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.paymentStatus").value("PAY_SUCCESS"));
    assertThat(jdbcTemplate.queryForObject("SELECT expires_at FROM annual_pass WHERE id = ?",
        java.sql.Date.class, annualPassId).toLocalDate()).isAfter(beforeRenewExpiresAt.toLocalDate());

    mockMvc.perform(get("/api/orders/my")
            .header("Authorization", "Bearer " + visitorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].orderType").isString())
        .andExpect(jsonPath("$.data[0].originalAmount").exists())
        .andExpect(jsonPath("$.data[0].discountAmount").exists())
        .andExpect(jsonPath("$.data[0].items").isArray());
  }

  @Test
  void memberEndpointsReadAndMutateDatabaseRecords() throws Exception {
    String visitorToken = tokenFor("visitor", "visitor123", "VISITOR");

    mockMvc.perform(get("/api/member/profiles")
            .header("Authorization", "Bearer " + visitorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").isString());

    mockMvc.perform(post("/api/member/profiles")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"测试游客\",\"idCard\":\"110101202605280001\",\"phone\":\"13800001111\",\"relation\":\"家人\",\"isDefault\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("测试游客"));

    mockMvc.perform(get("/api/member/coupons")
            .header("Authorization", "Bearer " + visitorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].status").isString());

    mockMvc.perform(get("/api/member/annual-passes")
            .header("Authorization", "Bearer " + visitorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
  }

  @Test
  void adminEndpointsReadAndMutateDatabaseRecords() throws Exception {
    String adminToken = tokenFor("admin", "admin123", "ADMIN");

    mockMvc.perform(get("/api/admin/tickets")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[*].code", hasItem("ADULT")));

    mockMvc.perform(post("/api/admin/tickets")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"TEST\",\"name\":\"测试票\",\"price\":12,\"description\":\"测试\",\"status\":\"ENABLED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("TEST"));

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_type WHERE code = 'TEST'", Integer.class))
        .isEqualTo(1);

    mockMvc.perform(put("/api/admin/tickets/inventory")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"visitDate\":\"2026-06-01\",\"session\":\"AM\",\"ticketTypeCode\":\"ADULT\",\"dailyCapacity\":1000,\"dailyRemaining\":800,\"capacity\":500,\"remaining\":300}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(true));

    assertThat(jdbcTemplate.queryForObject("SELECT remaining FROM ticket_inventory WHERE ticket_type_id = 1 AND visit_date = DATE '2026-06-01' AND session_code = 'AM'", Integer.class))
        .isEqualTo(300);
    assertThat(jdbcTemplate.queryForObject("SELECT remaining FROM daily_ticket_inventory WHERE ticket_type_id = 1 AND visit_date = DATE '2026-06-01'", Integer.class))
        .isEqualTo(800);

    mockMvc.perform(get("/api/admin/dashboard/summary")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.remainingCapacity", greaterThanOrEqualTo(0)));
  }

  @Test
  void adminCoreClosureMutatesBusinessRecords() throws Exception {
    String adminToken = tokenFor("admin", "admin123", "ADMIN");
    String checkerToken = tokenFor("checker", "checker123", "CHECKER");
    String visitorToken = tokenFor("visitor", "visitor123", "VISITOR");

    mockMvc.perform(get("/api/admin/tickets?visitDate=2026-06-02&session=PM")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0]", hasKey("visitDate")))
        .andExpect(jsonPath("$.data.records[0]", hasKey("session")))
        .andExpect(jsonPath("$.data.records[0]", hasKey("ticketTypeCode")));

    Long activityId = jdbcTemplate.queryForObject("SELECT id FROM activity ORDER BY id LIMIT 1", Long.class);
    mockMvc.perform(put("/api/admin/activities/" + activityId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"后台闭环活动\",\"category\":\"科普讲解\",\"startTime\":\"2026-06-06 09:45:00\",\"capacity\":33,\"location\":\"自然课堂\",\"status\":\"PUBLISHED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("后台闭环活动"))
        .andExpect(jsonPath("$.data.startTime").value("2026-06-06 09:45:00"));

    mockMvc.perform(post("/api/admin/marketing")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"闭环优惠券\",\"type\":\"COUPON\",\"discountValue\":15,\"thresholdAmount\":100,\"totalQuantity\":50,\"status\":\"ENABLED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("闭环优惠券"));
    Long couponId = jdbcTemplate.queryForObject("SELECT id FROM coupon WHERE name = '闭环优惠券'", Long.class);
    mockMvc.perform(put("/api/admin/marketing/" + couponId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"闭环优惠券改\",\"type\":\"COUPON\",\"discountValue\":20,\"thresholdAmount\":120,\"totalQuantity\":60,\"status\":\"ENABLED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("闭环优惠券改"));
    mockMvc.perform(put("/api/admin/marketing/" + couponId + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"DISABLED\",\"type\":\"COUPON\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("DISABLED"));

    String orderJson = mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"visitDate\":\"2026-06-02\",\"session\":\"PM\",\"items\":[{\"ticketTypeCode\":\"ADULT\",\"quantity\":1}]}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    String orderNo = orderJson.split("\"orderNo\":\"")[1].split("\"")[0];
    mockMvc.perform(post("/api/payments/prepay")
            .header("Authorization", "Bearer " + visitorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderNo\":\"" + orderNo + "\",\"channel\":\"MOCK\"}"))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/checkins/manual")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderNo\":\"" + orderNo + "\",\"checkerId\":2,\"remark\":\"后台人工核销\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.checkinStatus").value("CHECKED_IN"));

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checkin_record cr JOIN reservation_order ro ON ro.id = cr.order_id WHERE ro.order_no = ?", Integer.class, orderNo))
        .isEqualTo(1);
  }

  private String tokenFor(String username, String password, String role) throws Exception {
    String login = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"role\":\"" + role + "\"}"))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return login.split("\"token\":\"")[1].split("\"")[0];
  }
}
