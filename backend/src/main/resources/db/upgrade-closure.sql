USE zoo_reserve;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS daily_ticket_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  visit_date DATE NOT NULL,
  ticket_type_id BIGINT NOT NULL,
  capacity INT NOT NULL,
  remaining INT NOT NULL,
  UNIQUE KEY uk_daily_inventory (visit_date, ticket_type_id)
);

INSERT INTO daily_ticket_inventory (visit_date, ticket_type_id, capacity, remaining)
SELECT ti.visit_date, ti.ticket_type_id, SUM(ti.capacity), SUM(ti.remaining)
FROM ticket_inventory ti
LEFT JOIN daily_ticket_inventory di
  ON di.visit_date = ti.visit_date AND di.ticket_type_id = ti.ticket_type_id
WHERE di.id IS NULL
GROUP BY ti.visit_date, ti.ticket_type_id;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation_order' AND COLUMN_NAME = 'order_type') = 0, 'ALTER TABLE reservation_order ADD COLUMN order_type VARCHAR(32) NOT NULL DEFAULT ''TICKET''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation_order' AND COLUMN_NAME = 'original_amount') = 0, 'ALTER TABLE reservation_order ADD COLUMN original_amount DECIMAL(10,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation_order' AND COLUMN_NAME = 'discount_amount') = 0, 'ALTER TABLE reservation_order ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation_order' AND COLUMN_NAME = 'coupon_id') = 0, 'ALTER TABLE reservation_order ADD COLUMN coupon_id BIGINT', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation_order' AND COLUMN_NAME = 'annual_pass_id') = 0, 'ALTER TABLE reservation_order ADD COLUMN annual_pass_id BIGINT', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'coupon' AND COLUMN_NAME = 'threshold_amount') = 0, 'ALTER TABLE coupon ADD COLUMN threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'coupon' AND COLUMN_NAME = 'total_quantity') = 0, 'ALTER TABLE coupon ADD COLUMN total_quantity INT', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'coupon' AND COLUMN_NAME = 'claimed_quantity') = 0, 'ALTER TABLE coupon ADD COLUMN claimed_quantity INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'coupon' AND COLUMN_NAME = 'valid_from') = 0, 'ALTER TABLE coupon ADD COLUMN valid_from DATE', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'coupon' AND COLUMN_NAME = 'valid_to') = 0, 'ALTER TABLE coupon ADD COLUMN valid_to DATE', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'coupon' AND COLUMN_NAME = 'scope') = 0, 'ALTER TABLE coupon ADD COLUMN scope VARCHAR(64) NOT NULL DEFAULT ''TICKET''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_coupon' AND COLUMN_NAME = 'order_id') = 0, 'ALTER TABLE user_coupon ADD COLUMN order_id BIGINT', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_coupon' AND COLUMN_NAME = 'claimed_at') = 0, 'ALTER TABLE user_coupon ADD COLUMN claimed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_coupon' AND COLUMN_NAME = 'used_at') = 0, 'ALTER TABLE user_coupon ADD COLUMN used_at DATETIME', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DELETE uc1
FROM user_coupon uc1
JOIN user_coupon uc2
  ON uc1.user_id = uc2.user_id AND uc1.coupon_id = uc2.coupon_id AND uc1.id > uc2.id;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_coupon' AND INDEX_NAME = 'uk_user_coupon') = 0, 'ALTER TABLE user_coupon ADD UNIQUE KEY uk_user_coupon (user_id, coupon_id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'display_position') = 0, 'ALTER TABLE notice ADD COLUMN display_position VARCHAR(32) NOT NULL DEFAULT ''ALL''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'priority') = 0, 'ALTER TABLE notice ADD COLUMN priority INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'is_paid') = 0, 'ALTER TABLE activity ADD COLUMN is_paid TINYINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'price') = 0, 'ALTER TABLE activity ADD COLUMN price DECIMAL(10,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity' AND COLUMN_NAME = 'coupon_scope') = 0, 'ALTER TABLE activity ADD COLUMN coupon_scope VARCHAR(64) NOT NULL DEFAULT ''ACTIVITY''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_signup' AND COLUMN_NAME = 'order_id') = 0, 'ALTER TABLE activity_signup ADD COLUMN order_id BIGINT', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS order_activity_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  activity_title VARCHAR(128) NOT NULL,
  activity_category VARCHAR(64) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL
);

DELETE uc
FROM user_coupon uc
JOIN coupon duplicate_coupon ON duplicate_coupon.id = uc.coupon_id
JOIN (
  SELECT name, MIN(id) AS keep_id
  FROM coupon
  GROUP BY name
) keep_coupon ON keep_coupon.name = duplicate_coupon.name AND keep_coupon.keep_id <> duplicate_coupon.id
JOIN user_coupon existing_uc
  ON existing_uc.user_id = uc.user_id AND existing_uc.coupon_id = keep_coupon.keep_id;

UPDATE user_coupon uc
JOIN coupon duplicate_coupon ON duplicate_coupon.id = uc.coupon_id
JOIN (
  SELECT name, MIN(id) AS keep_id
  FROM coupon
  GROUP BY name
) keep_coupon ON keep_coupon.name = duplicate_coupon.name
SET uc.coupon_id = keep_coupon.keep_id
WHERE duplicate_coupon.id <> keep_coupon.keep_id;

DELETE duplicate_coupon
FROM coupon duplicate_coupon
JOIN (
  SELECT name, MIN(id) AS keep_id
  FROM coupon
  GROUP BY name
  HAVING COUNT(*) > 1
) keep_coupon ON keep_coupon.name = duplicate_coupon.name
WHERE duplicate_coupon.id <> keep_coupon.keep_id;

UPDATE activity
SET is_paid = CASE WHEN category IN ('亲子课堂', '夜游活动') THEN 1 ELSE is_paid END,
    price = CASE
      WHEN category = '夜游活动' AND price = 0 THEN 128.00
      WHEN category = '亲子课堂' AND price = 0 THEN 88.00
      ELSE price
    END,
    coupon_scope = CASE
      WHEN category = '夜游活动' THEN 'ACTIVITY_NIGHT'
      WHEN category = '亲子课堂' THEN 'ACTIVITY_PARENT_CHILD'
      ELSE coupon_scope
    END
WHERE category IN ('亲子课堂', '夜游活动');

UPDATE activity_signup s
JOIN activity a ON a.id = s.activity_id
SET s.status = 'CANCELLED'
WHERE a.is_paid = 1 AND s.order_id IS NULL AND s.status = 'SIGNED';

CREATE TABLE IF NOT EXISTS annual_pass_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  valid_days INT NOT NULL,
  holder_limit INT NOT NULL,
  benefits VARCHAR(1000),
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED'
);

CREATE TABLE IF NOT EXISTS annual_pass (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  pass_no VARCHAR(64) NOT NULL UNIQUE,
  started_at DATE NOT NULL,
  expires_at DATE NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS annual_pass_holder (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  annual_pass_id BIGINT NOT NULL,
  profile_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE KEY uk_pass_holder (annual_pass_id, profile_id)
);

CREATE TABLE IF NOT EXISTS annual_pass_usage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  annual_pass_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pass_order (annual_pass_id, order_id)
);

INSERT IGNORE INTO annual_pass_plan (code, name, price, valid_days, holder_limit, benefits, status) VALUES
('FAMILY', '亲子年卡', 699.00, 365, 3, '全年不限次入园,活动优先报名,餐饮95折', 'ENABLED');

UPDATE coupon
SET threshold_amount = CASE
      WHEN name = '夜游活动满100减20' THEN 100.00
      WHEN name = '亲子课堂满80减10' THEN 80.00
      WHEN discount_type = 'AMOUNT' THEN 200.00
      ELSE 0.00
    END,
    total_quantity = COALESCE(total_quantity, 1000),
    valid_from = COALESCE(valid_from, '2026-01-01'),
    valid_to = COALESCE(valid_to, '2026-12-31'),
    scope = CASE
      WHEN name = '夜游活动满100减20' THEN 'ACTIVITY_NIGHT'
      WHEN name = '亲子课堂满80减10' THEN 'ACTIVITY_PARENT_CHILD'
      ELSE COALESCE(scope, 'TICKET')
    END
WHERE status = 'ENABLED';

INSERT INTO coupon (name, discount_type, discount_value, threshold_amount, total_quantity, valid_from, valid_to, scope, status)
SELECT '亲子课堂满80减10', 'AMOUNT', 10.00, 80.00, 1000, '2026-01-01', '2026-12-31', 'ACTIVITY_PARENT_CHILD', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM coupon WHERE name = '亲子课堂满80减10');

INSERT INTO user_coupon (user_id, coupon_id, status)
SELECT u.id, c.id, 'UNUSED'
FROM user u
JOIN coupon c ON c.name IN ('夜游活动满100减20', '亲子课堂满80减10')
WHERE u.username = 'visitor'
  AND NOT EXISTS (
    SELECT 1 FROM user_coupon uc WHERE uc.user_id = u.id AND uc.coupon_id = c.id
  );
