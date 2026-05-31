package com.zooreserve.service;

import com.zooreserve.domain.enums.CheckinStatus;
import com.zooreserve.domain.enums.OrderStatus;
import com.zooreserve.domain.enums.PaymentStatus;
import com.zooreserve.dto.CheckinDtos.CheckinResponse;
import com.zooreserve.dto.CheckinDtos.ManualCheckinRequest;
import com.zooreserve.dto.CheckinDtos.ScanRequest;
import com.zooreserve.dto.OrderDtos.CreateOrderRequest;
import com.zooreserve.dto.OrderDtos.OrderItemResponse;
import com.zooreserve.dto.OrderDtos.OrderItemRequest;
import com.zooreserve.dto.OrderDtos.OrderResponse;
import com.zooreserve.dto.OrderDtos.QrCodeResponse;
import com.zooreserve.dto.PaymentDtos.PrepayRequest;
import com.zooreserve.dto.PaymentDtos.PrepayResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class MockOrderService {
  private final JdbcTemplate jdbcTemplate;

  public MockOrderService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public OrderResponse create(CreateOrderRequest request) {
    List<OrderItemRequest> items = request.items() == null ? List.of() : request.items();
    String requestedOrderType = request.orderType() == null ? "TICKET" : request.orderType();
    if ("ACTIVITY".equalsIgnoreCase(requestedOrderType)) {
      return createActivityOrder(request);
    }
    if (items.isEmpty()) {
      throw new IllegalStateException("请选择票种");
    }
    String session = request.session() == null || request.session().isBlank() ? "AM" : request.session();
    boolean annualPassProductOrder = "ANNUAL_PASS_PURCHASE".equalsIgnoreCase(requestedOrderType)
        || "ANNUAL_PASS_RENEWAL".equalsIgnoreCase(requestedOrderType);
    Long userId = currentVisitorId();
    BigDecimal originalAmount = BigDecimal.ZERO;
    int people = 0;

    for (var item : items) {
      Map<String, Object> ticket = ticketByCode(item.ticketTypeCode());
      int quantity = Math.max(1, item.quantity());
      if (!annualPassProductOrder) {
        int dailyUpdated = jdbcTemplate.update("""
          UPDATE daily_ticket_inventory
          SET remaining = remaining - ?
          WHERE visit_date = ? AND ticket_type_id = ? AND remaining >= ?
          """, quantity, Date.valueOf(request.visitDate()), ticket.get("id"), quantity);
      if (dailyUpdated == 0) {
        throw new IllegalStateException("搴撳瓨涓嶈冻");
      }
      int updated = jdbcTemplate.update("""
          UPDATE ticket_inventory
          SET remaining = remaining - ?
          WHERE visit_date = ? AND session_code = ? AND ticket_type_id = ? AND remaining >= ?
          """, quantity, Date.valueOf(request.visitDate()), session, ticket.get("id"), quantity);
      if (updated == 0) {
        throw new IllegalStateException("库存不足");
      }
      }
      originalAmount = originalAmount.add(((BigDecimal) ticket.get("price")).multiply(BigDecimal.valueOf(quantity)));
      people += quantity;
    }

    boolean annualPassOrder = "ANNUAL_PASS".equalsIgnoreCase(requestedOrderType);
    boolean annualPassRenewal = "ANNUAL_PASS_RENEWAL".equalsIgnoreCase(requestedOrderType);
    String orderType = annualPassOrder ? "ANNUAL_PASS"
        : annualPassRenewal ? "ANNUAL_PASS_RENEWAL"
        : "ANNUAL_PASS_PURCHASE".equalsIgnoreCase(requestedOrderType) ? "ANNUAL_PASS_PURCHASE" : "TICKET";
    Long annualPassId = annualPassOrder ? validateAnnualPass(userId, request.annualPassId(), request.visitDate())
        : annualPassRenewal ? validateOwnedAnnualPass(userId, request.annualPassId()) : null;
    Long couponId = annualPassOrder ? null : request.couponId();
    BigDecimal discountAmount = annualPassOrder ? originalAmount : discountFor(userId, couponId, originalAmount, request.visitDate(), "TICKET");
    BigDecimal amount = annualPassOrder ? BigDecimal.ZERO : originalAmount.subtract(discountAmount).max(BigDecimal.ZERO);
    String orderStatus = annualPassOrder || amount.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PENDING_PAYMENT";
    String paymentStatus = annualPassOrder || amount.compareTo(BigDecimal.ZERO) == 0 ? "PAY_SUCCESS" : "UNPAID";
    String orderNo = nextOrderNo(request.visitDate());

    GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
    BigDecimal finalOriginalAmount = originalAmount;
    BigDecimal finalDiscountAmount = discountAmount;
    BigDecimal finalAmount = amount;
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement("""
          INSERT INTO reservation_order
            (order_no, user_id, visit_date, session_code, order_type, original_amount, discount_amount,
             amount, coupon_id, annual_pass_id, order_status, payment_status)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, orderNo);
      statement.setLong(2, userId);
      statement.setDate(3, Date.valueOf(request.visitDate()));
      statement.setString(4, session);
      statement.setString(5, orderType);
      statement.setBigDecimal(6, finalOriginalAmount);
      statement.setBigDecimal(7, finalDiscountAmount);
      statement.setBigDecimal(8, finalAmount);
      statement.setObject(9, couponId);
      statement.setObject(10, annualPassId);
      statement.setString(11, orderStatus);
      statement.setString(12, paymentStatus);
      return statement;
    }, keyHolder);
    Long orderId = generatedId(keyHolder);

    if (couponId != null) {
      jdbcTemplate.update("UPDATE user_coupon SET status = 'LOCKED', order_id = ? WHERE id = ?", orderId, couponId);
    }
    for (var item : items) {
      Map<String, Object> ticket = ticketByCode(item.ticketTypeCode());
      jdbcTemplate.update("""
          INSERT INTO order_item (order_id, ticket_type_id, quantity, unit_price)
          VALUES (?, ?, ?, ?)
          """, orderId, ticket.get("id"), Math.max(1, item.quantity()), ticket.get("price"));
    }
    return responseById(orderId);
  }

  private OrderResponse createActivityOrder(CreateOrderRequest request) {
    if (request.activityId() == null) {
      throw new IllegalStateException("请选择活动");
    }
    int quantity = Math.max(1, request.quantity() == null ? 1 : request.quantity());
    Long userId = currentVisitorId();
    Map<String, Object> activity = activityById(request.activityId());
    if (((Number) activity.get("is_paid")).intValue() != 1) {
      throw new IllegalStateException("免费活动请直接报名");
    }
    ensureActivityOrderAllowed(userId, request.activityId());
    ensureActivityCapacity(request.activityId(), quantity);

    BigDecimal unitPrice = (BigDecimal) activity.get("price");
    BigDecimal originalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    LocalDate visitDate = activityStartDate(activity.get("start_time"));
    String couponScope = String.valueOf(activity.get("coupon_scope"));
    BigDecimal discountAmount = discountFor(userId, request.couponId(), originalAmount, visitDate, couponScope);
    BigDecimal amount = originalAmount.subtract(discountAmount).max(BigDecimal.ZERO);
    String orderStatus = amount.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PENDING_PAYMENT";
    String paymentStatus = amount.compareTo(BigDecimal.ZERO) == 0 ? "PAY_SUCCESS" : "UNPAID";
    String orderNo = nextOrderNo(visitDate);

    GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement("""
          INSERT INTO reservation_order
            (order_no, user_id, visit_date, session_code, order_type, original_amount, discount_amount,
             amount, coupon_id, order_status, payment_status)
          VALUES (?, ?, ?, 'ACTIVITY', 'ACTIVITY', ?, ?, ?, ?, ?, ?)
          """, Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, orderNo);
      statement.setLong(2, userId);
      statement.setDate(3, Date.valueOf(visitDate));
      statement.setBigDecimal(4, originalAmount);
      statement.setBigDecimal(5, discountAmount);
      statement.setBigDecimal(6, amount);
      statement.setObject(7, request.couponId());
      statement.setString(8, orderStatus);
      statement.setString(9, paymentStatus);
      return statement;
    }, keyHolder);
    Long orderId = generatedId(keyHolder);
    jdbcTemplate.update("""
        INSERT INTO order_activity_item (order_id, activity_id, activity_title, activity_category, quantity, unit_price)
        VALUES (?, ?, ?, ?, ?, ?)
        """, orderId, request.activityId(), activity.get("title"), activity.get("category"), quantity, unitPrice);
    if (request.couponId() != null) {
      jdbcTemplate.update("UPDATE user_coupon SET status = 'LOCKED', order_id = ? WHERE id = ?", orderId, request.couponId());
    }
    if ("PAY_SUCCESS".equals(paymentStatus)) {
      createActivitySignup(orderId);
      jdbcTemplate.update("""
          UPDATE user_coupon
          SET status = 'USED', used_at = CURRENT_TIMESTAMP
          WHERE order_id = ? AND status = 'LOCKED'
          """, orderId);
    }
    return responseById(orderId);
  }

  public List<OrderResponse> myOrders() {
    Long userId = currentVisitorId();
    return jdbcTemplate.query("""
        SELECT id, order_no, visit_date, session_code, order_type, original_amount, discount_amount,
               amount, order_status, payment_status, created_at,
               COALESCE((SELECT SUM(quantity) FROM order_item WHERE order_id = reservation_order.id), 0)
                 + COALESCE((SELECT SUM(quantity) FROM order_activity_item WHERE order_id = reservation_order.id), 0) people_count
        FROM reservation_order
        WHERE user_id = ?
        ORDER BY created_at DESC
        """, (rs, rowNum) -> mapOrder(rs), userId);
  }

  public QrCodeResponse qrcode(Long id) {
    Long userId = currentVisitorId();
    return jdbcTemplate.query("""
        SELECT order_no, visit_date, payment_status, order_type
        FROM reservation_order
        WHERE id = ? AND user_id = ?
        """, rs -> {
      if (!rs.next()) {
        throw new IllegalStateException("订单不存在");
      }
      if (!"PAY_SUCCESS".equals(rs.getString("payment_status"))) {
        throw new IllegalStateException("订单未支付，不能生成入园码");
      }
      String notice = "ACTIVITY".equals(rs.getString("order_type"))
          ? "请按活动时间到指定集合点核验参与。"
          : "请携带有效身份证件，于预约场次内从主入口核验入园。";
      return new QrCodeResponse(rs.getString("order_no"), "ZOORESERVE:ORDER:" + rs.getString("order_no"),
          rs.getDate("visit_date").toLocalDate(), notice);
    }, id, userId);
  }

  @Transactional
  public PrepayResponse prepay(PrepayRequest request) {
    Map<String, Object> order = visitorOrderByNo(request.orderNo());
    if ("PAY_SUCCESS".equals(order.get("payment_status"))) {
      return new PrepayResponse(request.orderNo(), "MOCK", (BigDecimal) order.get("amount"),
          PaymentStatus.PAY_SUCCESS, "mock://pay/" + request.orderNo());
    }
    String paymentNo = "PAY" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
    String channel = request.channel() == null || request.channel().isBlank() ? "MOCK" : request.channel();
    jdbcTemplate.update("""
        INSERT INTO payment_record (order_id, payment_no, channel, amount, status, callback_payload)
        VALUES (?, ?, ?, ?, 'PAY_SUCCESS', ?)
        """, order.get("id"), paymentNo, channel, order.get("amount"), "{\"tradeState\":\"SUCCESS\"}");
    jdbcTemplate.update("""
        UPDATE reservation_order
        SET order_status = 'PAID', payment_status = 'PAY_SUCCESS'
        WHERE id = ?
        """, order.get("id"));
    jdbcTemplate.update("""
        UPDATE user_coupon
        SET status = 'USED', used_at = CURRENT_TIMESTAMP
        WHERE order_id = ? AND status = 'LOCKED'
        """, order.get("id"));
    if ("ACTIVITY".equals(order.get("order_type"))) {
      createActivitySignup(((Number) order.get("id")).longValue());
    }
    if ("ANNUAL_PASS_RENEWAL".equals(order.get("order_type")) && order.get("annual_pass_id") != null) {
      renewAnnualPassAfterPayment(order.get("annual_pass_id"));
    }
    return new PrepayResponse(request.orderNo(), channel, (BigDecimal) order.get("amount"),
        PaymentStatus.PAY_SUCCESS, "mock://pay/" + request.orderNo());
  }

  @Transactional
  public OrderResponse cancel(Long id) {
    Map<String, Object> order = visitorOrderById(id);
    if (!"PENDING_PAYMENT".equals(order.get("order_status"))) {
      throw new IllegalStateException("只有待支付订单可以取消");
    }
    restoreInventory(id);
    jdbcTemplate.update("UPDATE user_coupon SET status = 'UNUSED', order_id = NULL WHERE order_id = ? AND status = 'LOCKED'", id);
    jdbcTemplate.update("UPDATE reservation_order SET order_status = 'CANCELLED', payment_status = 'CLOSED' WHERE id = ?", id);
    return responseById(id);
  }

  @Transactional
  public OrderResponse refund(String idOrNo) {
    Map<String, Object> order = visitorOrderByIdOrNo(idOrNo);
    if (!"PAY_SUCCESS".equals(order.get("payment_status"))) {
      throw new IllegalStateException("只有已支付订单可以申请退款");
    }
    String refundNo = "RF" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
    jdbcTemplate.update("""
        INSERT INTO refund_record (order_id, refund_no, amount, status, reason)
        VALUES (?, ?, ?, 'REFUNDING', ?)
        """, order.get("id"), refundNo, order.get("amount"), "游客申请退款");
    jdbcTemplate.update("UPDATE reservation_order SET order_status = 'REFUNDING' WHERE id = ?", order.get("id"));
    return responseById(((Number) order.get("id")).longValue());
  }

  @Transactional
  public Map<String, Object> approveRefund(Long refundId) {
    Map<String, Object> refund = jdbcTemplate.queryForMap("SELECT id, order_id FROM refund_record WHERE id = ?", refundId);
    Long orderId = ((Number) refund.get("order_id")).longValue();
    restoreInventory(orderId);
    jdbcTemplate.update("UPDATE refund_record SET status = 'REFUNDED' WHERE id = ?", refundId);
    jdbcTemplate.update("UPDATE reservation_order SET order_status = 'REFUNDED', payment_status = 'CLOSED' WHERE id = ?", orderId);
    jdbcTemplate.update("UPDATE activity_signup SET status = 'CANCELLED' WHERE order_id = ?", orderId);
    return Map.of("refundId", refundId, "status", "REFUNDED");
  }

  @Transactional
  public CheckinResponse scan(ScanRequest request) {
    String orderNo = normalizeOrderNo(request.qrContent());
    Map<String, Object> order = orderByNo(orderNo);
    if (!"PAY_SUCCESS".equals(order.get("payment_status"))) {
      throw new IllegalStateException("订单未支付，不能核销");
    }
    Integer checked = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checkin_record WHERE order_id = ?",
        Integer.class, order.get("id"));
    if (checked != null && checked > 0) {
      return checkinResponse(orderNo, CheckinStatus.EXCEPTION, "重复核销已拦截", LocalDateTime.now());
    }
    Long checkerId = request.checkerId() == null ? currentAdminId() : request.checkerId();
    LocalDateTime checkedAt = LocalDateTime.now();
    jdbcTemplate.update("""
        INSERT INTO checkin_record (order_id, checker_id, status, remark, checked_at)
        VALUES (?, ?, 'CHECKED_IN', ?, ?)
        """, order.get("id"), checkerId, "扫码核销成功", checkedAt);
    jdbcTemplate.update("UPDATE reservation_order SET order_status = 'CHECKED_IN' WHERE id = ?", order.get("id"));
    recordAnnualPassUsage(order);
    return checkinResponse(orderNo, CheckinStatus.CHECKED_IN, "扫码核销成功", checkedAt);
  }

  @Transactional
  public CheckinResponse manual(ManualCheckinRequest request) {
    String orderNo = request.orderNo() == null || request.orderNo().isBlank() ? findOrderByPhone(request.phone()) : request.orderNo();
    Map<String, Object> order = orderByNo(orderNo);
    if (!"PAY_SUCCESS".equals(order.get("payment_status"))) {
      throw new IllegalStateException("订单未支付，不能核销");
    }
    Long checkerId = request.checkerId() == null ? currentAdminId() : request.checkerId();
    LocalDateTime checkedAt = LocalDateTime.now();
    jdbcTemplate.update("""
        INSERT INTO checkin_record (order_id, checker_id, status, remark, checked_at)
        VALUES (?, ?, 'CHECKED_IN', ?, ?)
        """, order.get("id"), checkerId, request.remark() == null ? "人工核销成功" : request.remark(), checkedAt);
    jdbcTemplate.update("UPDATE reservation_order SET order_status = 'CHECKED_IN' WHERE id = ?", order.get("id"));
    recordAnnualPassUsage(order);
    return checkinResponse(orderNo, CheckinStatus.CHECKED_IN, request.remark(), checkedAt);
  }

  public OrderResponse seedOrder() {
    return jdbcTemplate.query("""
        SELECT id, order_no, visit_date, session_code, order_type, original_amount, discount_amount,
               amount, order_status, payment_status, created_at,
               COALESCE((SELECT SUM(quantity) FROM order_item WHERE order_id = reservation_order.id), 0)
                 + COALESCE((SELECT SUM(quantity) FROM order_activity_item WHERE order_id = reservation_order.id), 0) people_count
        FROM reservation_order
        ORDER BY id
        LIMIT 1
        """, rs -> rs.next() ? mapOrder(rs) : null);
  }

  private Map<String, Object> ticketByCode(String code) {
    return jdbcTemplate.queryForMap("SELECT id, price FROM ticket_type WHERE code = ? AND status = 'ENABLED'", code);
  }

  private Map<String, Object> orderByNo(String orderNo) {
    return jdbcTemplate.queryForMap("""
        SELECT id, order_no, visit_date, order_type, amount, order_status, payment_status, annual_pass_id
        FROM reservation_order
        WHERE order_no = ?
        """, orderNo);
  }

  private Map<String, Object> orderById(Long id) {
    return jdbcTemplate.queryForMap("""
        SELECT id, order_no, visit_date, order_type, amount, order_status, payment_status, annual_pass_id
        FROM reservation_order
        WHERE id = ?
        """, id);
  }

  private Map<String, Object> orderByIdOrNo(String idOrNo) {
    if (idOrNo != null && idOrNo.matches("\\d+")) {
      return orderById(Long.valueOf(idOrNo));
    }
    return orderByNo(idOrNo);
  }

  private Map<String, Object> visitorOrderByNo(String orderNo) {
    try {
      return jdbcTemplate.queryForMap("""
          SELECT id, order_no, visit_date, order_type, amount, order_status, payment_status, annual_pass_id
          FROM reservation_order
          WHERE order_no = ? AND user_id = ?
          """, orderNo, currentVisitorId());
    } catch (EmptyResultDataAccessException exception) {
      throw new IllegalStateException("订单不存在");
    }
  }

  private Map<String, Object> visitorOrderById(Long id) {
    try {
      return jdbcTemplate.queryForMap("""
          SELECT id, order_no, visit_date, order_type, amount, order_status, payment_status, annual_pass_id
          FROM reservation_order
          WHERE id = ? AND user_id = ?
          """, id, currentVisitorId());
    } catch (EmptyResultDataAccessException exception) {
      throw new IllegalStateException("订单不存在");
    }
  }

  private Map<String, Object> visitorOrderByIdOrNo(String idOrNo) {
    if (idOrNo != null && idOrNo.matches("\\d+")) {
      return visitorOrderById(Long.valueOf(idOrNo));
    }
    return visitorOrderByNo(idOrNo);
  }

  private String findOrderByPhone(String phone) {
    return jdbcTemplate.queryForObject("""
        SELECT o.order_no
        FROM reservation_order o
        JOIN user u ON u.id = o.user_id
        WHERE u.phone = ?
        ORDER BY o.created_at DESC
        LIMIT 1
        """, String.class, phone);
  }

  private CheckinResponse checkinResponse(String orderNo, CheckinStatus status, String remark, LocalDateTime checkedAt) {
    return jdbcTemplate.query("""
        SELECT o.visit_date, COALESCE(SUM(oi.quantity), 0) people_count,
               GROUP_CONCAT(CONCAT(tt.name, ' x', oi.quantity) SEPARATOR ', ') ticket_summary
        FROM reservation_order o
        LEFT JOIN order_item oi ON oi.order_id = o.id
        LEFT JOIN ticket_type tt ON tt.id = oi.ticket_type_id
        WHERE o.order_no = ?
        GROUP BY o.id, o.visit_date
        """, rs -> {
      if (!rs.next()) {
        throw new IllegalStateException("订单不存在");
      }
      return new CheckinResponse(orderNo, rs.getString("ticket_summary"), rs.getInt("people_count"),
          rs.getDate("visit_date").toLocalDate(), status, remark, checkedAt);
    }, orderNo);
  }

  private String normalizeOrderNo(String qrContent) {
    if (qrContent == null || qrContent.isBlank()) {
      throw new IllegalStateException("核销码不能为空");
    }
    return qrContent.replace("ZOORESERVE:ORDER:", "");
  }

  private Long validateAnnualPass(Long userId, Long annualPassId, LocalDate visitDate) {
    if (annualPassId == null) {
      throw new IllegalStateException("请选择年卡");
    }
    Integer count = jdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM annual_pass
        WHERE id = ? AND user_id = ? AND status = 'ACTIVE' AND started_at <= ? AND expires_at >= ?
        """, Integer.class, annualPassId, userId, Date.valueOf(visitDate), Date.valueOf(visitDate));
    if (count == null || count == 0) {
      throw new IllegalStateException("年卡不可用");
    }
    return annualPassId;
  }

  private Long validateOwnedAnnualPass(Long userId, Long annualPassId) {
    if (annualPassId == null) {
      throw new IllegalStateException("请选择需要续费的年卡");
    }
    Integer count = jdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM annual_pass
        WHERE id = ? AND user_id = ?
        """, Integer.class, annualPassId, userId);
    if (count == null || count == 0) {
      throw new IllegalStateException("年卡不存在或不属于当前用户");
    }
    return annualPassId;
  }

  private void renewAnnualPassAfterPayment(Object annualPassId) {
    Map<String, Object> pass = jdbcTemplate.queryForMap("""
        SELECT ap.expires_at, app.valid_days
        FROM annual_pass ap
        JOIN annual_pass_plan app ON app.id = ap.plan_id
        WHERE ap.id = ?
        """, annualPassId);
    LocalDate currentExpiresAt = ((Date) pass.get("expires_at")).toLocalDate();
    LocalDate baseDate = currentExpiresAt.isAfter(LocalDate.now()) ? currentExpiresAt : LocalDate.now();
    LocalDate renewedExpiresAt = baseDate.plusDays(((Number) pass.get("valid_days")).longValue());
    jdbcTemplate.update("UPDATE annual_pass SET expires_at = ?, status = 'ACTIVE' WHERE id = ?",
        Date.valueOf(renewedExpiresAt), annualPassId);
  }

  private BigDecimal discountFor(Long userId, Long userCouponId, BigDecimal originalAmount, LocalDate visitDate, String requiredScope) {
    if (userCouponId == null) {
      return BigDecimal.ZERO;
    }
    Map<String, Object> coupon = jdbcTemplate.queryForMap("""
        SELECT uc.id, c.discount_type, c.discount_value, c.threshold_amount, c.valid_from, c.valid_to, c.scope
        FROM user_coupon uc
        JOIN coupon c ON c.id = uc.coupon_id
        WHERE uc.id = ? AND uc.user_id = ? AND uc.status = 'UNUSED' AND c.status = 'ENABLED'
        """, userCouponId, userId);
    if (!couponScopeMatches(String.valueOf(coupon.get("scope")), requiredScope)) {
      throw new IllegalStateException("优惠券不适用于当前订单");
    }
    BigDecimal threshold = (BigDecimal) coupon.get("threshold_amount");
    if (originalAmount.compareTo(threshold) < 0) {
      throw new IllegalStateException("订单金额未达到优惠券门槛");
    }
    Date validFrom = (Date) coupon.get("valid_from");
    Date validTo = (Date) coupon.get("valid_to");
    if ((validFrom != null && visitDate.isBefore(validFrom.toLocalDate()))
        || (validTo != null && visitDate.isAfter(validTo.toLocalDate()))) {
      throw new IllegalStateException("优惠券不在有效期内");
    }
    BigDecimal value = (BigDecimal) coupon.get("discount_value");
    if ("PERCENT".equals(coupon.get("discount_type"))) {
      return originalAmount.subtract(originalAmount.multiply(value));
    }
    return value.min(originalAmount);
  }

  private boolean couponScopeMatches(String couponScope, String requiredScope) {
    if (couponScope == null || requiredScope == null) {
      return false;
    }
    if (couponScope.equals(requiredScope)) {
      return true;
    }
    return requiredScope.startsWith("ACTIVITY") && "ACTIVITY".equals(couponScope);
  }

  private void restoreInventory(Object orderId) {
    jdbcTemplate.query("""
        SELECT o.visit_date, o.session_code, oi.ticket_type_id, oi.quantity
        FROM reservation_order o
        JOIN order_item oi ON oi.order_id = o.id
        WHERE o.id = ?
        """, rs -> {
      while (rs.next()) {
        jdbcTemplate.update("""
            UPDATE daily_ticket_inventory
            SET remaining = LEAST(capacity, remaining + ?)
            WHERE visit_date = ? AND ticket_type_id = ?
            """, rs.getInt("quantity"), rs.getDate("visit_date"), rs.getLong("ticket_type_id"));
        jdbcTemplate.update("""
            UPDATE ticket_inventory
            SET remaining = LEAST(capacity, remaining + ?)
            WHERE visit_date = ? AND session_code = ? AND ticket_type_id = ?
            """, rs.getInt("quantity"), rs.getDate("visit_date"), rs.getString("session_code"), rs.getLong("ticket_type_id"));
      }
      return null;
    }, orderId);
  }

  private OrderResponse responseById(Long id) {
    return jdbcTemplate.query("""
        SELECT id, order_no, visit_date, session_code, order_type, original_amount, discount_amount,
               amount, order_status, payment_status, created_at,
               COALESCE((SELECT SUM(quantity) FROM order_item WHERE order_id = reservation_order.id), 0)
                 + COALESCE((SELECT SUM(quantity) FROM order_activity_item WHERE order_id = reservation_order.id), 0) people_count
        FROM reservation_order
        WHERE id = ?
        """, rs -> {
      if (!rs.next()) {
        throw new IllegalStateException("订单不存在");
      }
      return mapOrder(rs);
    }, id);
  }

  private void recordAnnualPassUsage(Map<String, Object> order) {
    Object annualPassId = order.get("annual_pass_id");
    if (annualPassId != null) {
      jdbcTemplate.update("""
          INSERT INTO annual_pass_usage (annual_pass_id, order_id)
          SELECT ?, ?
          WHERE NOT EXISTS (
            SELECT 1 FROM annual_pass_usage WHERE annual_pass_id = ? AND order_id = ?
          )
          """, annualPassId, order.get("id"), annualPassId, order.get("id"));
    }
  }

  private Long currentVisitorId() {
    String username = currentUsername("visitor");
    return jdbcTemplate.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, username);
  }

  private Long currentAdminId() {
    String username = currentUsername("checker");
    return jdbcTemplate.queryForObject("SELECT id FROM admin_user WHERE username = ?", Long.class, username);
  }

  private String currentUsername(String fallback) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || authentication.getName() == null ? fallback : authentication.getName();
  }

  private String nextOrderNo(LocalDate visitDate) {
    String prefix = "ZR" + visitDate.format(DateTimeFormatter.BASIC_ISO_DATE);
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_order WHERE order_no LIKE ?",
        Integer.class, prefix + "%");
    return prefix + String.format("%04d", (count == null ? 0 : count) + 1);
  }

  private OrderResponse mapOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
    Long orderId = rs.getLong("id");
    return new OrderResponse(orderId, rs.getString("order_no"), rs.getDate("visit_date").toLocalDate(),
        rs.getString("session_code"), rs.getInt("people_count"), rs.getBigDecimal("amount"),
        OrderStatus.valueOf(rs.getString("order_status")), PaymentStatus.valueOf(rs.getString("payment_status")),
        rs.getTimestamp("created_at").toLocalDateTime(), rs.getString("order_type"),
        rs.getBigDecimal("original_amount"), rs.getBigDecimal("discount_amount"), orderItems(orderId));
  }

  private Map<String, Object> activityById(Long activityId) {
    return jdbcTemplate.queryForMap("""
        SELECT id, title, category, start_time, capacity, is_paid, price, coupon_scope
        FROM activity
        WHERE id = ? AND status = 'PUBLISHED'
        """, activityId);
  }

  private LocalDate activityStartDate(Object value) {
    if (value instanceof LocalDateTime dateTime) {
      return dateTime.toLocalDate();
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toLocalDateTime().toLocalDate();
    }
    if (value instanceof Date date) {
      return date.toLocalDate();
    }
    throw new IllegalStateException("活动时间格式不正确");
  }

  private void ensureActivityCapacity(Long activityId, int quantity) {
    Map<String, Object> activity = jdbcTemplate.queryForMap("SELECT capacity FROM activity WHERE id = ?", activityId);
    Integer signed = jdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM activity_signup
        WHERE activity_id = ? AND status = 'SIGNED'
        """, Integer.class, activityId);
    int capacity = ((Number) activity.get("capacity")).intValue();
    if (signed == null || signed + quantity > capacity) {
      throw new IllegalStateException("活动名额不足");
    }
  }

  private void ensureActivityOrderAllowed(Long userId, Long activityId) {
    Integer signed = jdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM activity_signup
        WHERE activity_id = ? AND user_id = ? AND status = 'SIGNED'
        """, Integer.class, activityId, userId);
    if (signed != null && signed > 0) {
      throw new IllegalStateException("已报名该活动");
    }
    Integer pending = jdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM reservation_order o
        JOIN order_activity_item oi ON oi.order_id = o.id
        WHERE o.user_id = ? AND oi.activity_id = ? AND o.order_status = 'PENDING_PAYMENT'
        """, Integer.class, userId, activityId);
    if (pending != null && pending > 0) {
      throw new IllegalStateException("已有待支付活动订单，请先支付或取消");
    }
  }

  private void createActivitySignup(Long orderId) {
    Map<String, Object> item = jdbcTemplate.queryForMap("""
        SELECT activity_id, quantity
        FROM order_activity_item
        WHERE order_id = ?
        """, orderId);
    Map<String, Object> order = jdbcTemplate.queryForMap("SELECT user_id FROM reservation_order WHERE id = ?", orderId);
    Long activityId = ((Number) item.get("activity_id")).longValue();
    int quantity = ((Number) item.get("quantity")).intValue();
    ensureActivityCapacity(activityId, quantity);
    Integer signed = jdbcTemplate.queryForObject("""
        SELECT COUNT(*)
        FROM activity_signup
        WHERE activity_id = ? AND user_id = ? AND status = 'SIGNED'
        """, Integer.class, activityId, order.get("user_id"));
    if (signed != null && signed > 0) {
      throw new IllegalStateException("已报名该活动");
    }
    int created = jdbcTemplate.update("""
        UPDATE activity_signup
        SET order_id = ?, status = 'SIGNED'
        WHERE activity_id = ? AND user_id = ? AND status <> 'SIGNED'
        """, orderId, activityId, order.get("user_id"));
    if (created == 0) {
      created = jdbcTemplate.update("""
          INSERT INTO activity_signup (activity_id, user_id, order_id, status)
          VALUES (?, ?, ?, 'SIGNED')
          """, activityId, order.get("user_id"), orderId);
    }
    if (created == 0) {
      throw new IllegalStateException("已报名该活动");
    }
  }

  private List<OrderItemResponse> orderItems(Long orderId) {
    List<OrderItemResponse> ticketItems = jdbcTemplate.query("""
        SELECT tt.code, tt.name, oi.quantity, oi.unit_price
        FROM order_item oi
        JOIN ticket_type tt ON tt.id = oi.ticket_type_id
        WHERE oi.order_id = ?
        ORDER BY oi.id
        """, (rs, rowNum) -> new OrderItemResponse(rs.getString("code"), rs.getString("name"),
        rs.getInt("quantity"), rs.getBigDecimal("unit_price")), orderId);
    List<OrderItemResponse> activityItems = jdbcTemplate.query("""
        SELECT CONCAT('ACTIVITY:', activity_id) AS code, activity_title AS name, quantity, unit_price
        FROM order_activity_item
        WHERE order_id = ?
        ORDER BY id
        """, (rs, rowNum) -> new OrderItemResponse(rs.getString("code"), rs.getString("name"),
        rs.getInt("quantity"), rs.getBigDecimal("unit_price")), orderId);
    return java.util.stream.Stream.concat(ticketItems.stream(), activityItems.stream()).toList();
  }

  private Long generatedId(GeneratedKeyHolder keyHolder) {
    if (keyHolder.getKeys() != null && keyHolder.getKeys().get("id") instanceof Number id) {
      return id.longValue();
    }
    if (keyHolder.getKeys() != null && keyHolder.getKeys().size() == 1) {
      Object value = keyHolder.getKeys().values().iterator().next();
      if (value instanceof Number id) {
        return id.longValue();
      }
    }
    if (keyHolder.getKey() != null) {
      return keyHolder.getKey().longValue();
    }
    throw new IllegalStateException("创建记录失败");
  }
}
