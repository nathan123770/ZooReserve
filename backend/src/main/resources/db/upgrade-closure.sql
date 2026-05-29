USE zoo_reserve;

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
SET threshold_amount = CASE WHEN discount_type = 'AMOUNT' THEN 200.00 ELSE 0.00 END,
    total_quantity = COALESCE(total_quantity, 1000),
    valid_from = COALESCE(valid_from, '2026-01-01'),
    valid_to = COALESCE(valid_to, '2026-12-31'),
    scope = COALESCE(scope, 'TICKET')
WHERE status = 'ENABLED';
