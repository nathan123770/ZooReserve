INSERT INTO role (id, code, name) VALUES
(1, 'VISITOR', '游客'),
(2, 'ADMIN', '管理员'),
(3, 'CHECKER', '核销员');

INSERT INTO admin_user (id, username, display_name, password_hash, status) VALUES
(1, 'admin', '园区管理员', '{noop}admin123', 'ENABLED'),
(2, 'checker', '入口核销员', '{noop}checker123', 'ENABLED');

INSERT INTO user (id, username, phone, password_hash, status) VALUES
(1, 'visitor', '13800000001', '{noop}visitor123', 'ENABLED'),
(2, 'family01', '13800000002', '{noop}visitor123', 'ENABLED');

INSERT INTO user_role (user_id, role_id, user_type) VALUES
(1, 1, 'VISITOR'),
(2, 1, 'VISITOR'),
(1, 2, 'ADMIN'),
(2, 3, 'ADMIN');

INSERT INTO ticket_type (id, code, name, price, description, status) VALUES
(1, 'ADULT', '成人票', 120.00, '18周岁以上游客', 'ENABLED'),
(2, 'CHILD', '儿童票', 60.00, '1.2米至1.5米儿童', 'ENABLED'),
(3, 'ANNUAL', '亲子年卡', 699.00, '两大一小全年畅游', 'ENABLED');

INSERT INTO ticket_inventory (visit_date, session_code, ticket_type_id, capacity, remaining) VALUES
('2026-06-01', 'AM', 1, 800, 520),
('2026-06-01', 'AM', 2, 400, 300),
('2026-06-01', 'AM', 3, 120, 99),
('2026-06-01', 'PM', 1, 800, 520),
('2026-06-02', 'AM', 2, 400, 300),
('2026-06-03', 'AM', 1, 800, 520);

INSERT INTO visitor_profile (id, user_id, real_name, id_card_no, phone, is_default) VALUES
(1, 1, '测试游客', '330101202605280001', '13800000001', 1),
(2, 1, '测试儿童', '330101202605280002', '13800000001', 0);

INSERT INTO reservation_order (id, order_no, user_id, visit_date, session_code, original_amount, discount_amount, amount, order_status, payment_status, created_at) VALUES
(1, 'ZR202606010001', 1, '2026-06-01', 'AM', 240.00, 0.00, 240.00, 'PAID', 'PAY_SUCCESS', '2026-05-28 09:30:00');

INSERT INTO order_item (order_id, ticket_type_id, quantity, unit_price) VALUES
(1, 1, 2, 120.00);

INSERT INTO activity (id, title, category, start_time, capacity, location, status) VALUES
(1, '长颈鹿科普讲解', '科普讲解', '2026-06-01 10:00:00', 40, '草食动物区', 'PUBLISHED');

INSERT INTO activity_signup (activity_id, user_id, status) VALUES
(1, 1, 'SIGNED');

INSERT INTO zone (id, name, description, map_x, map_y) VALUES
(1, '草食动物区', '开放式展区', 31.25, 48.5);

INSERT INTO animal (id, zone_id, name, species, description, media_url, status) VALUES
(1, 1, '星星', '长颈鹿', '温和的长颈鹿', '/media/animals/giraffe.jpg', 'VISIBLE');

INSERT INTO coupon (id, name, discount_type, discount_value, threshold_amount, total_quantity, claimed_quantity, valid_from, valid_to, scope, status) VALUES
(1, '新客满200减30', 'AMOUNT', 30.00, 200.00, 1000, 1, '2026-01-01', '2026-12-31', 'TICKET', 'ENABLED');

INSERT INTO user_coupon (id, user_id, coupon_id, status) VALUES
(1, 1, 1, 'UNUSED');

INSERT INTO annual_pass_plan (id, code, name, price, valid_days, holder_limit, benefits, status) VALUES
(1, 'FAMILY', '亲子年卡', 699.00, 365, 3, '全年不限次入园,活动优先报名,餐饮95折', 'ENABLED');

INSERT INTO annual_pass (id, user_id, plan_id, pass_no, started_at, expires_at, status) VALUES
(1, 1, 1, 'AP202605280001', '2026-05-28', '2027-05-28', 'ACTIVE');

INSERT INTO annual_pass_holder (annual_pass_id, profile_id, status) VALUES
(1, 1, 'ACTIVE'),
(1, 2, 'ACTIVE');

INSERT INTO notice (id, title, content, status, published_at) VALUES
(1, '端午假期预约提醒', '请提前实名预约', 'PUBLISHED', '2026-05-28 08:00:00');
