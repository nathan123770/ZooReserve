USE zoo_reserve;

INSERT IGNORE INTO role (code, name) VALUES
('VISITOR', '游客'),
('ADMIN', '管理员'),
('CHECKER', '核销员'),
('OPS', '运营专员'),
('CUSTOMER_SERVICE', '客服');

INSERT IGNORE INTO permission (code, name, menu_path) VALUES
('dashboard:view', '查看数据看板', '/admin'),
('ticket:manage', '管理票务库存', '/admin/tickets'),
('order:manage', '管理订单', '/admin/orders'),
('activity:manage', '管理活动', '/admin/activities'),
('animal:manage', '管理动物展区', '/admin/animals'),
('checkin:manage', '管理核销', '/admin/checkins'),
('marketing:manage', '管理营销', '/admin/marketing'),
('system:manage', '管理系统账号', '/admin/system');

INSERT IGNORE INTO admin_user (username, display_name, password_hash, status) VALUES
('admin', '园区管理员', '{noop}admin123', 'ENABLED'),
('checker', '入口核销员', '{noop}checker123', 'ENABLED'),
('ops01', '运营专员', '{noop}ops123', 'ENABLED'),
('service01', '客服专员', '{noop}service123', 'ENABLED');

INSERT IGNORE INTO user (username, phone, password_hash, status) VALUES
('visitor', '13800000001', '{noop}visitor123', 'ENABLED'),
('family01', '13800000002', '{noop}visitor123', 'ENABLED'),
('student01', '13800000003', '{noop}visitor123', 'ENABLED');

INSERT IGNORE INTO user_role (user_id, role_id, user_type)
SELECT u.id, r.id, 'VISITOR'
FROM user u
JOIN role r ON r.code = 'VISITOR'
WHERE u.username IN ('visitor', 'family01', 'student01');

INSERT IGNORE INTO user_role (user_id, role_id, user_type)
SELECT au.id, r.id, 'ADMIN'
FROM admin_user au
JOIN role r ON r.code = 'ADMIN'
WHERE au.username = 'admin';

INSERT IGNORE INTO user_role (user_id, role_id, user_type)
SELECT au.id, r.id, 'ADMIN'
FROM admin_user au
JOIN role r ON r.code = 'CHECKER'
WHERE au.username = 'checker';

INSERT IGNORE INTO user_role (user_id, role_id, user_type)
SELECT au.id, r.id, 'ADMIN'
FROM admin_user au
JOIN role r ON r.code = 'OPS'
WHERE au.username = 'ops01';

INSERT IGNORE INTO visitor_profile (user_id, real_name, id_card_no, phone, is_default)
SELECT id, '张小鹿', '110101201801010011', '13800000001', 1
FROM user
WHERE username = 'visitor';

INSERT IGNORE INTO visitor_profile (user_id, real_name, id_card_no, phone, is_default)
SELECT id, '林晨', '110101199205060021', '13800000002', 1
FROM user
WHERE username = 'family01';

INSERT IGNORE INTO visitor_profile (user_id, real_name, id_card_no, phone, is_default)
SELECT id, '周然', '110101200407080031', '13800000003', 1
FROM user
WHERE username = 'student01';

INSERT IGNORE INTO ticket_type (code, name, price, description, status) VALUES
('ADULT', '成人票', 120.00, '18周岁以上游客，预约当日指定场次入园', 'ENABLED'),
('CHILD', '儿童票', 60.00, '1.2米至1.5米儿童，需成人陪同入园', 'ENABLED'),
('SENIOR', '老人票', 60.00, '65周岁以上游客，入园需出示有效证件', 'ENABLED'),
('STUDENT', '学生票', 80.00, '全日制学生优惠票，入园需出示学生证', 'ENABLED'),
('FAMILY', '亲子套票', 260.00, '两名成人与一名儿童同行入园', 'ENABLED'),
('ANNUAL', '亲子年卡', 699.00, '两大一小全年不限次预约入园', 'ENABLED');

INSERT IGNORE INTO ticket_inventory (visit_date, session_code, ticket_type_id, capacity, remaining)
SELECT d.visit_date, s.session_code, tt.id,
       CASE
         WHEN tt.code = 'ANNUAL' THEN 120
         WHEN tt.code = 'FAMILY' THEN 160
         WHEN tt.code IN ('CHILD', 'SENIOR', 'STUDENT') THEN 400
         ELSE 800
       END AS capacity,
       CASE
         WHEN tt.code = 'ANNUAL' THEN 99
         WHEN tt.code = 'FAMILY' THEN 126
         WHEN tt.code IN ('CHILD', 'SENIOR', 'STUDENT') THEN 260
         ELSE 520
       END AS remaining
FROM (
  SELECT DATE '2026-05-29' AS visit_date UNION ALL
  SELECT DATE '2026-05-30' UNION ALL
  SELECT DATE '2026-05-31' UNION ALL
  SELECT DATE '2026-06-01' UNION ALL
  SELECT DATE '2026-06-02' UNION ALL
  SELECT DATE '2026-06-03' UNION ALL
  SELECT DATE '2026-06-04'
) d
CROSS JOIN (
  SELECT 'AM' AS session_code UNION ALL
  SELECT 'PM' UNION ALL
  SELECT 'NIGHT'
) s
CROSS JOIN ticket_type tt
WHERE tt.status = 'ENABLED';

INSERT IGNORE INTO daily_ticket_inventory (visit_date, ticket_type_id, capacity, remaining)
SELECT visit_date, ticket_type_id, SUM(capacity), SUM(remaining)
FROM ticket_inventory
GROUP BY visit_date, ticket_type_id;

INSERT INTO activity (title, category, start_time, capacity, location, status)
SELECT '长颈鹿科普讲解', '科普讲解', '2026-06-01 10:00:00', 40, '草食动物区', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '长颈鹿科普讲解' AND start_time = '2026-06-01 10:00:00');

INSERT INTO activity (title, category, start_time, capacity, location, status)
SELECT '小小饲养员亲子课堂', '亲子课堂', '2026-06-01 14:30:00', 24, '自然教育中心', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '小小饲养员亲子课堂' AND start_time = '2026-06-01 14:30:00');

INSERT INTO activity (title, category, start_time, capacity, location, status)
SELECT '夏夜动物园', '夜游活动', '2026-06-02 19:00:00', 100, '主入口集合', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '夏夜动物园' AND start_time = '2026-06-02 19:00:00');

INSERT INTO activity (title, category, start_time, capacity, location, status)
SELECT '雨林探秘导览', '主题导览', '2026-06-03 11:00:00', 35, '热带雨林馆', 'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '雨林探秘导览' AND start_time = '2026-06-03 11:00:00');

INSERT IGNORE INTO activity_signup (activity_id, user_id, status)
SELECT a.id, u.id, 'SIGNED'
FROM activity a
JOIN user u ON u.username IN ('visitor', 'family01')
WHERE a.title IN ('长颈鹿科普讲解', '小小饲养员亲子课堂');

INSERT INTO zone (name, description, map_x, map_y)
SELECT '草食动物区', '长颈鹿、斑马和羚羊共同生活的开放式展区', 31.250000, 48.500000
WHERE NOT EXISTS (SELECT 1 FROM zone WHERE name = '草食动物区');

INSERT INTO zone (name, description, map_x, map_y)
SELECT '猛兽山谷', '狮虎等大型猫科动物展区，设有高处观景平台', 62.100000, 42.300000
WHERE NOT EXISTS (SELECT 1 FROM zone WHERE name = '猛兽山谷');

INSERT INTO zone (name, description, map_x, map_y)
SELECT '热带雨林馆', '室内恒温雨林生态馆，展示灵长类与鸟类', 47.800000, 65.200000
WHERE NOT EXISTS (SELECT 1 FROM zone WHERE name = '热带雨林馆');

INSERT INTO zone (name, description, map_x, map_y)
SELECT '熊猫竹园', '大熊猫主题展区，配置科普长廊和休憩区', 25.500000, 70.100000
WHERE NOT EXISTS (SELECT 1 FROM zone WHERE name = '熊猫竹园');

INSERT INTO animal (zone_id, name, species, description, media_url, status)
SELECT z.id, '星星', '长颈鹿', '性格温和的成年雌性长颈鹿，喜欢在上午进食金合欢叶。', '/media/animals/giraffe.jpg', 'VISIBLE'
FROM zone z
WHERE z.name = '草食动物区'
  AND NOT EXISTS (SELECT 1 FROM animal WHERE name = '星星' AND species = '长颈鹿');

INSERT INTO animal (zone_id, name, species, description, media_url, status)
SELECT z.id, '阿山', '东北虎', '猛兽山谷明星动物，通常在下午较为活跃。', '/media/animals/tiger.jpg', 'VISIBLE'
FROM zone z
WHERE z.name = '猛兽山谷'
  AND NOT EXISTS (SELECT 1 FROM animal WHERE name = '阿山' AND species = '东北虎');

INSERT INTO animal (zone_id, name, species, description, media_url, status)
SELECT z.id, '豆包', '大熊猫', '喜欢竹笋和攀爬架，是熊猫竹园的人气成员。', '/media/animals/panda.jpg', 'VISIBLE'
FROM zone z
WHERE z.name = '熊猫竹园'
  AND NOT EXISTS (SELECT 1 FROM animal WHERE name = '豆包' AND species = '大熊猫');

INSERT INTO animal (zone_id, name, species, description, media_url, status)
SELECT z.id, '小橙', '金刚鹦鹉', '羽色鲜艳，常参与雨林馆定时科普互动。', '/media/animals/macaw.jpg', 'VISIBLE'
FROM zone z
WHERE z.name = '热带雨林馆'
  AND NOT EXISTS (SELECT 1 FROM animal WHERE name = '小橙' AND species = '金刚鹦鹉');

INSERT IGNORE INTO coupon (name, discount_type, discount_value, status) VALUES
('新客满200减30', 'AMOUNT', 30.00, 'ENABLED'),
('亲子套票九折券', 'PERCENT', 0.90, 'ENABLED'),
('夜游活动满100减20', 'AMOUNT', 20.00, 'ENABLED');

UPDATE coupon
SET threshold_amount = CASE WHEN discount_type = 'AMOUNT' THEN 200.00 ELSE 0.00 END,
    total_quantity = COALESCE(total_quantity, 1000),
    valid_from = COALESCE(valid_from, '2026-01-01'),
    valid_to = COALESCE(valid_to, '2026-12-31'),
    scope = COALESCE(scope, 'TICKET')
WHERE name IN ('新客满200减30', '亲子套票九折券', '夜游活动满100减20');

INSERT INTO user_coupon (user_id, coupon_id, status)
SELECT u.id, c.id, 'UNUSED'
FROM user u
JOIN coupon c ON c.name IN ('新客满200减30', '亲子套票九折券')
WHERE u.username = 'visitor'
  AND NOT EXISTS (
    SELECT 1 FROM user_coupon uc WHERE uc.user_id = u.id AND uc.coupon_id = c.id
  );

INSERT INTO user_coupon (user_id, coupon_id, status)
SELECT u.id, c.id, 'USED'
FROM user u
JOIN coupon c ON c.name = '夜游活动满100减20'
WHERE u.username = 'family01'
  AND NOT EXISTS (
    SELECT 1 FROM user_coupon uc WHERE uc.user_id = u.id AND uc.coupon_id = c.id
  );

INSERT IGNORE INTO annual_pass_plan (code, name, price, valid_days, holder_limit, benefits, status) VALUES
('FAMILY', '亲子年卡', 699.00, 365, 3, '全年不限次入园,活动优先报名,餐饮95折', 'ENABLED');

INSERT INTO annual_pass (user_id, plan_id, pass_no, started_at, expires_at, status)
SELECT u.id, p.id, 'AP202605280001', '2026-05-28', '2027-05-28', 'ACTIVE'
FROM user u
JOIN annual_pass_plan p ON p.code = 'FAMILY'
WHERE u.username = 'visitor'
  AND NOT EXISTS (SELECT 1 FROM annual_pass WHERE pass_no = 'AP202605280001');

INSERT INTO annual_pass_holder (annual_pass_id, profile_id, status)
SELECT ap.id, vp.id, 'ACTIVE'
FROM annual_pass ap
JOIN user u ON u.id = ap.user_id
JOIN visitor_profile vp ON vp.user_id = u.id
WHERE ap.pass_no = 'AP202605280001'
  AND NOT EXISTS (
    SELECT 1 FROM annual_pass_holder aph WHERE aph.annual_pass_id = ap.id AND aph.profile_id = vp.id
  );

INSERT INTO reservation_order (order_no, user_id, visit_date, session_code, amount, order_status, payment_status, created_at)
SELECT 'ZR202606010001', u.id, '2026-06-01', 'AM', 240.00, 'PAID', 'PAY_SUCCESS', '2026-05-28 09:30:00'
FROM user u
WHERE u.username = 'visitor'
  AND NOT EXISTS (SELECT 1 FROM reservation_order WHERE order_no = 'ZR202606010001');

INSERT INTO reservation_order (order_no, user_id, visit_date, session_code, amount, order_status, payment_status, created_at)
SELECT 'ZR202606010002', u.id, '2026-06-01', 'PM', 260.00, 'PENDING_PAYMENT', 'UNPAID', '2026-05-28 10:15:00'
FROM user u
WHERE u.username = 'family01'
  AND NOT EXISTS (SELECT 1 FROM reservation_order WHERE order_no = 'ZR202606010002');

INSERT INTO reservation_order (order_no, user_id, visit_date, session_code, amount, order_status, payment_status, created_at)
SELECT 'ZR202606020001', u.id, '2026-06-02', 'NIGHT', 80.00, 'CHECKED_IN', 'PAY_SUCCESS', '2026-05-28 11:20:00'
FROM user u
WHERE u.username = 'student01'
  AND NOT EXISTS (SELECT 1 FROM reservation_order WHERE order_no = 'ZR202606020001');

INSERT INTO order_item (order_id, ticket_type_id, quantity, unit_price)
SELECT o.id, tt.id, 2, 120.00
FROM reservation_order o
JOIN ticket_type tt ON tt.code = 'ADULT'
WHERE o.order_no = 'ZR202606010001'
  AND NOT EXISTS (SELECT 1 FROM order_item oi WHERE oi.order_id = o.id AND oi.ticket_type_id = tt.id);

INSERT INTO order_item (order_id, ticket_type_id, quantity, unit_price)
SELECT o.id, tt.id, 1, 260.00
FROM reservation_order o
JOIN ticket_type tt ON tt.code = 'FAMILY'
WHERE o.order_no = 'ZR202606010002'
  AND NOT EXISTS (SELECT 1 FROM order_item oi WHERE oi.order_id = o.id AND oi.ticket_type_id = tt.id);

INSERT INTO order_item (order_id, ticket_type_id, quantity, unit_price)
SELECT o.id, tt.id, 1, 80.00
FROM reservation_order o
JOIN ticket_type tt ON tt.code = 'STUDENT'
WHERE o.order_no = 'ZR202606020001'
  AND NOT EXISTS (SELECT 1 FROM order_item oi WHERE oi.order_id = o.id AND oi.ticket_type_id = tt.id);

INSERT IGNORE INTO payment_record (order_id, payment_no, channel, amount, status, callback_payload, created_at)
SELECT o.id, 'PAY202605280001', 'MOCK', 240.00, 'PAY_SUCCESS', JSON_OBJECT('tradeState', 'SUCCESS', 'orderNo', o.order_no), '2026-05-28 09:35:00'
FROM reservation_order o
WHERE o.order_no = 'ZR202606010001';

INSERT IGNORE INTO payment_record (order_id, payment_no, channel, amount, status, callback_payload, created_at)
SELECT o.id, 'PAY202605280002', 'MOCK', 80.00, 'PAY_SUCCESS', JSON_OBJECT('tradeState', 'SUCCESS', 'orderNo', o.order_no), '2026-05-28 11:25:00'
FROM reservation_order o
WHERE o.order_no = 'ZR202606020001';

INSERT IGNORE INTO refund_record (order_id, refund_no, amount, status, reason, created_at)
SELECT o.id, 'RF202605280001', 60.00, 'PROCESSING', '游客申请儿童票差额退款', '2026-05-28 12:00:00'
FROM reservation_order o
WHERE o.order_no = 'ZR202606010001';

INSERT INTO checkin_record (order_id, checker_id, status, remark, checked_at)
SELECT o.id, au.id, 'CHECKED_IN', '扫码核销成功', '2026-06-02 19:05:00'
FROM reservation_order o
JOIN admin_user au ON au.username = 'checker'
WHERE o.order_no = 'ZR202606020001'
  AND NOT EXISTS (SELECT 1 FROM checkin_record cr WHERE cr.order_id = o.id);

INSERT INTO notice (title, content, status, published_at)
SELECT '端午假期预约提醒', '端午假期客流较大，请提前完成实名预约并按预约场次入园。', 'PUBLISHED', '2026-05-28 08:00:00'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '端午假期预约提醒');

INSERT INTO notice (title, content, status, published_at)
SELECT '夏夜动物园开放公告', '6月2日起每周五、周六开放夜游场次，部分展馆开放时间延长至21:00。', 'PUBLISHED', '2026-05-28 08:30:00'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '夏夜动物园开放公告');

INSERT INTO notice (title, content, status, published_at)
SELECT '雨林馆维护提示', '热带雨林馆每日12:30至13:00进行环境维护，期间暂停入馆。', 'PUBLISHED', '2026-05-28 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '雨林馆维护提示');

INSERT INTO operation_log (operator_id, action, resource, detail, ip, created_at)
SELECT au.id, 'IMPORT_SEED_DATA', 'database', '初始化 ZooReserve 演示数据', '127.0.0.1', NOW()
FROM admin_user au
WHERE au.username = 'admin'
  AND NOT EXISTS (
    SELECT 1 FROM operation_log WHERE action = 'IMPORT_SEED_DATA' AND resource = 'database'
  );
