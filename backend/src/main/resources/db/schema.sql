CREATE DATABASE IF NOT EXISTS zoo_reserve DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zoo_reserve;

CREATE TABLE IF NOT EXISTS user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  phone VARCHAR(32) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(128) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  menu_path VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  user_type VARCHAR(32) NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id, user_type)
);

CREATE TABLE IF NOT EXISTS visitor_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  real_name VARCHAR(64) NOT NULL,
  id_card_no VARCHAR(64) NOT NULL,
  phone VARCHAR(32),
  is_default TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_card (user_id, id_card_no)
);

CREATE TABLE IF NOT EXISTS ticket_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  description VARCHAR(255),
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED'
);

CREATE TABLE IF NOT EXISTS ticket_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  visit_date DATE NOT NULL,
  session_code VARCHAR(32) NOT NULL,
  ticket_type_id BIGINT NOT NULL,
  capacity INT NOT NULL,
  remaining INT NOT NULL,
  UNIQUE KEY uk_inventory (visit_date, session_code, ticket_type_id)
);

CREATE TABLE IF NOT EXISTS daily_ticket_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  visit_date DATE NOT NULL,
  ticket_type_id BIGINT NOT NULL,
  capacity INT NOT NULL,
  remaining INT NOT NULL,
  UNIQUE KEY uk_daily_inventory (visit_date, ticket_type_id)
);

CREATE TABLE IF NOT EXISTS reservation_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT,
  visit_date DATE NOT NULL,
  session_code VARCHAR(32) NOT NULL,
  order_type VARCHAR(32) NOT NULL DEFAULT 'TICKET',
  original_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  amount DECIMAL(10,2) NOT NULL,
  coupon_id BIGINT,
  annual_pass_id BIGINT,
  order_status VARCHAR(32) NOT NULL,
  payment_status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_order_user (user_id),
  INDEX idx_order_visit (visit_date, session_code)
);

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  ticket_type_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  payment_no VARCHAR(64) NOT NULL UNIQUE,
  channel VARCHAR(32) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  callback_payload JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refund_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  refund_no VARCHAR(64) NOT NULL UNIQUE,
  amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  reason VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS checkin_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  checker_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  remark VARCHAR(255),
  checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_checkin_order (order_id)
);

CREATE TABLE IF NOT EXISTS activity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  start_time DATETIME NOT NULL,
  capacity INT NOT NULL,
  location VARCHAR(128),
  is_paid TINYINT NOT NULL DEFAULT 0,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  coupon_scope VARCHAR(64) NOT NULL DEFAULT 'ACTIVITY',
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED'
);

CREATE TABLE IF NOT EXISTS activity_signup (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  order_id BIGINT,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_activity_user (activity_id, user_id)
);

CREATE TABLE IF NOT EXISTS order_activity_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  activity_title VARCHAR(128) NOT NULL,
  activity_category VARCHAR(64) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS zone (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500),
  map_x DECIMAL(10,6),
  map_y DECIMAL(10,6)
);

CREATE TABLE IF NOT EXISTS animal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  zone_id BIGINT,
  name VARCHAR(128) NOT NULL,
  species VARCHAR(128),
  description VARCHAR(500),
  media_url VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE'
);

CREATE TABLE IF NOT EXISTS coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  discount_type VARCHAR(32) NOT NULL,
  discount_value DECIMAL(10,2) NOT NULL,
  threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  total_quantity INT,
  claimed_quantity INT NOT NULL DEFAULT 0,
  valid_from DATE,
  valid_to DATE,
  scope VARCHAR(64) NOT NULL DEFAULT 'TICKET',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED'
);

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'UNUSED',
  order_id BIGINT,
  claimed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_at DATETIME,
  UNIQUE KEY uk_user_coupon (user_id, coupon_id)
);

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

CREATE TABLE IF NOT EXISTS notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  display_position VARCHAR(32) NOT NULL DEFAULT 'ALL',
  priority INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  published_at DATETIME
);

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT,
  action VARCHAR(128) NOT NULL,
  resource VARCHAR(128),
  detail VARCHAR(1000),
  ip VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

INSERT IGNORE INTO role (code, name) VALUES
('VISITOR', '游客'),
('ADMIN', '管理员'),
('CHECKER', '核销员');
