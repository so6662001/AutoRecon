-- Pickup Express - H2 Schema (MySQL compatible mode)
-- Converted from MySQL init-schema.sql for demo/test

-- 1. contract
CREATE TABLE IF NOT EXISTS contract (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_no VARCHAR(50) NOT NULL,
  erp_contract_id VARCHAR(50),
  contract_type TINYINT,
  seller_id BIGINT,
  buyer_id BIGINT,
  buyer_contact_name VARCHAR(50),
  buyer_contact_phone VARCHAR(20),
  total_quantity DECIMAL(18,4),
  total_weight DECIMAL(18,4),
  total_amount DECIMAL(18,2),
  picked_weight DECIMAL(18,4) DEFAULT 0,
  picked_amount DECIMAL(18,2) DEFAULT 0,
  settled_amount DECIMAL(18,2) DEFAULT 0,
  paid_amount DECIMAL(18,2) DEFAULT 0,
  payment_terms VARCHAR(200),
  delivery_deadline DATE,
  warehouse_id BIGINT,
  warehouse_name VARCHAR(100),
  template_id BIGINT,
  template_version INT,
  platform_terms_version VARCHAR(10),
  custom_clauses CLOB,
  sign_required TINYINT DEFAULT 0,
  sign_status TINYINT DEFAULT 0,
  signed_pdf_url VARCHAR(500),
  sign_evidence_no VARCHAR(100),
  pickup_mode TINYINT DEFAULT 1,
  allow_no_code_pickup TINYINT DEFAULT 1,
  status TINYINT DEFAULT 0,
  erp_sync_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_contract_no ON contract(contract_no);
CREATE INDEX idx_contract_seller_id ON contract(seller_id);
CREATE INDEX idx_contract_buyer_id ON contract(buyer_id);
CREATE INDEX idx_contract_status ON contract(status);

-- 2. contract_item
CREATE TABLE IF NOT EXISTS contract_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_id BIGINT NOT NULL,
  line_no INT,
  product_name VARCHAR(200),
  spec VARCHAR(100),
  material VARCHAR(50),
  origin VARCHAR(100),
  heat_no VARCHAR(50),
  batch_no VARCHAR(50),
  warehouse_location VARCHAR(100),
  quantity DECIMAL(18,4),
  weight DECIMAL(18,4),
  unit_price DECIMAL(18,2),
  amount DECIMAL(18,2),
  picked_quantity DECIMAL(18,4) DEFAULT 0,
  picked_weight DECIMAL(18,4) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_contract_item_contract_id ON contract_item(contract_id);

-- 3. contract_template
CREATE TABLE IF NOT EXISTS contract_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT,
  template_name VARCHAR(100),
  contract_type TINYINT,
  template_source TINYINT,
  template_content CLOB,
  template_file_url VARCHAR(500),
  field_mapping VARCHAR(4000),
  has_origin_field TINYINT DEFAULT 1,
  platform_terms_priority TINYINT DEFAULT 2,
  applicable_buyers VARCHAR(4000),
  is_default TINYINT DEFAULT 0,
  version INT DEFAULT 1,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);

-- 4. pickup_order
CREATE TABLE IF NOT EXISTS pickup_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pickup_no VARCHAR(32) NOT NULL,
  contract_id BIGINT,
  contract_no VARCHAR(50),
  buyer_id BIGINT,
  pickup_code VARCHAR(10),
  pickup_code_qr VARCHAR(500),
  pickup_code_status TINYINT DEFAULT 0,
  pickup_code_expire_at TIMESTAMP,
  dispatch_mode TINYINT,
  dispatch_status TINYINT DEFAULT 0,
  customer_confirmed TINYINT DEFAULT 0,
  customer_confirmed_at TIMESTAMP,
  vehicle_plate VARCHAR(20),
  driver_name VARCHAR(50),
  driver_phone VARCHAR(20),
  carrier_id BIGINT,
  carrier_name VARCHAR(100),
  driver_assigned TINYINT DEFAULT 0,
  driver_assigned_at TIMESTAMP,
  warehouse_id BIGINT,
  warehouse_name VARCHAR(100),
  expected_arrival_at TIMESTAMP,
  actual_arrival_at TIMESTAMP,
  arrival_gps_lat DECIMAL(10,7),
  arrival_gps_lng DECIMAL(10,7),
  delivery_mode TINYINT,
  delivery_data_source VARCHAR(20),
  total_lifts INT DEFAULT 0,
  total_pieces INT DEFAULT 0,
  total_weight DECIMAL(18,4) DEFAULT 0,
  total_amount DECIMAL(18,2) DEFAULT 0,
  delivery_status TINYINT DEFAULT 0,
  settlement_status TINYINT DEFAULT 0,
  evidence_package_id BIGINT,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_pickup_no ON pickup_order(pickup_no);
CREATE INDEX idx_pickup_order_contract_id ON pickup_order(contract_id);
CREATE INDEX idx_pickup_order_buyer_id ON pickup_order(buyer_id);
CREATE INDEX idx_pickup_order_status ON pickup_order(status);

-- 5. lift_record
CREATE TABLE IF NOT EXISTS lift_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pickup_order_id BIGINT NOT NULL,
  lift_seq INT,
  product_name VARCHAR(200),
  spec VARCHAR(100),
  material VARCHAR(50),
  heat_no VARCHAR(50),
  batch_no VARCHAR(50),
  pieces INT,
  theoretical_weight DECIMAL(18,4),
  actual_weight DECIMAL(18,4),
  operator_id VARCHAR(50),
  operator_name VARCHAR(50),
  wms_record_id VARCHAR(50),
  data_source TINYINT,
  photo_url VARCHAR(500),
  uploaded_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_lift_record_pickup_order_id ON lift_record(pickup_order_id);

-- 6. delivery_confirm
CREATE TABLE IF NOT EXISTS delivery_confirm (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pickup_order_id BIGINT NOT NULL,
  operator_id VARCHAR(50),
  operator_name VARCHAR(50),
  signature_url VARCHAR(500),
  total_lifts INT,
  total_pieces INT,
  total_weight DECIMAL(18,4),
  confirmed_at TIMESTAMP,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_delivery_confirm_pickup_order_id ON delivery_confirm(pickup_order_id);

-- 7. delivery_photo
CREATE TABLE IF NOT EXISTS delivery_photo (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pickup_order_id BIGINT NOT NULL,
  photo_type TINYINT,
  photo_url VARCHAR(500),
  gps_lat DECIMAL(10,7),
  gps_lng DECIMAL(10,7),
  taken_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_delivery_photo_pickup_order_id ON delivery_photo(pickup_order_id);

-- 8. settlement_order
CREATE TABLE IF NOT EXISTS settlement_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  settlement_no VARCHAR(32) NOT NULL,
  pickup_order_id BIGINT,
  contract_id BIGINT,
  contract_no VARCHAR(50),
  buyer_id BIGINT,
  total_weight DECIMAL(18,4),
  total_amount DECIMAL(18,2),
  tax_amount DECIMAL(18,2),
  total_with_tax DECIMAL(18,2),
  deducted_prepayment DECIMAL(18,2) DEFAULT 0,
  receivable_amount DECIMAL(18,2),
  settlement_detail VARCHAR(4000),
  pdf_url VARCHAR(500),
  customer_viewed TINYINT DEFAULT 0,
  customer_viewed_at TIMESTAMP,
  synced_to_recon TINYINT DEFAULT 0,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_settlement_no ON settlement_order(settlement_no);
CREATE INDEX idx_settlement_contract_id ON settlement_order(contract_id);
CREATE INDEX idx_settlement_buyer_id ON settlement_order(buyer_id);

-- 9. evidence_package
CREATE TABLE IF NOT EXISTS evidence_package (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pickup_order_id BIGINT,
  contract_id BIGINT,
  package_hash VARCHAR(64),
  contract_pdf_url VARCHAR(500),
  pickup_order_pdf_url VARCHAR(500),
  delivery_data_url VARCHAR(500),
  photos_urls VARCHAR(4000),
  signature_url VARCHAR(500),
  settlement_pdf_url VARCHAR(500),
  blockchain_hash VARCHAR(200),
  evidence_status TINYINT DEFAULT 0,
  archived_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_evidence_package_pickup_order_id ON evidence_package(pickup_order_id);

-- 10. progress_event
CREATE TABLE IF NOT EXISTS progress_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pickup_order_id BIGINT,
  contract_id BIGINT,
  event_type VARCHAR(30),
  event_title VARCHAR(200),
  event_detail VARCHAR(4000),
  operator VARCHAR(50),
  notify_targets VARCHAR(200),
  notified TINYINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_progress_event_pickup_order_id ON progress_event(pickup_order_id);

-- 11. authorized_pickup_person
CREATE TABLE IF NOT EXISTS authorized_pickup_person (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  buyer_id BIGINT NOT NULL,
  contract_id BIGINT,
  person_name VARCHAR(50),
  id_card_no VARCHAR(100),
  phone VARCHAR(20),
  vehicle_plate VARCHAR(20),
  max_pickup_weight DECIMAL(18,4),
  registered_by TINYINT,
  buyer_confirmed TINYINT DEFAULT 0,
  buyer_confirmed_at TIMESTAMP,
  valid_from DATE,
  valid_until DATE,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_authorized_pickup_buyer_id ON authorized_pickup_person(buyer_id);

-- 12. pickup_verification
CREATE TABLE IF NOT EXISTS pickup_verification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pickup_order_id BIGINT,
  contract_id BIGINT,
  buyer_id BIGINT,
  driver_name VARCHAR(50),
  driver_phone VARCHAR(20),
  vehicle_plate VARCHAR(20),
  is_pre_registered TINYINT DEFAULT 0,
  authorized_person_id BIGINT,
  sms_sent TINYINT DEFAULT 0,
  sms_sent_at TIMESTAMP,
  sms_delivered TINYINT DEFAULT 0,
  sms_delivery_receipt VARCHAR(200),
  sms_link_clicked TINYINT DEFAULT 0,
  sms_link_clicked_at TIMESTAMP,
  phone_call_made TINYINT DEFAULT 0,
  phone_call_result TINYINT DEFAULT 0,
  phone_call_recording_url VARCHAR(500),
  phone_call_at TIMESTAMP,
  driver_id_photo_url VARCHAR(500),
  driver_face_photo_url VARCHAR(500),
  driver_license_photo_url VARCHAR(500),
  verification_level TINYINT DEFAULT 2,
  verification_result TINYINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_pickup_verification_pickup_order_id ON pickup_verification(pickup_order_id);

-- 13. trading_habit_record
CREATE TABLE IF NOT EXISTS trading_habit_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  buyer_id BIGINT NOT NULL,
  driver_name VARCHAR(50),
  driver_phone VARCHAR(20) NOT NULL,
  vehicle_plate VARCHAR(20),
  total_pickups INT DEFAULT 0,
  total_weight DECIMAL(18,4) DEFAULT 0,
  total_amount DECIMAL(18,2) DEFAULT 0,
  paid_pickups INT DEFAULT 0,
  denied_pickups INT DEFAULT 0,
  last_pickup_at TIMESTAMP,
  first_pickup_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX uk_trading_habit_buyer_driver ON trading_habit_record(buyer_id, driver_phone);

-- 14. warehouse
CREATE TABLE IF NOT EXISTS warehouse (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT,
  warehouse_name VARCHAR(100),
  warehouse_code VARCHAR(50),
  address VARCHAR(300),
  contact_name VARCHAR(50),
  contact_phone VARCHAR(20),
  gps_lat DECIMAL(10,7),
  gps_lng DECIMAL(10,7),
  delivery_mode TINYINT,
  wms_config VARCHAR(4000),
  default_delivery_mode TINYINT DEFAULT 2,
  backup_delivery_mode TINYINT,
  has_wms TINYINT DEFAULT 0,
  wms_type TINYINT,
  third_party_wms_config VARCHAR(4000),
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);

-- 15. carrier
CREATE TABLE IF NOT EXISTS carrier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT,
  carrier_name VARCHAR(100),
  contact_name VARCHAR(50),
  contact_phone VARCHAR(20),
  address VARCHAR(300),
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);

-- 16. supplement_record
CREATE TABLE IF NOT EXISTS supplement_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pickup_order_id BIGINT,
  submitted_by BIGINT,
  submit_reason VARCHAR(500),
  delivery_detail VARCHAR(4000),
  document_urls VARCHAR(4000),
  approval_status TINYINT DEFAULT 0,
  approved_by BIGINT,
  approved_at TIMESTAMP,
  approval_comment VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_supplement_record_pickup_order_id ON supplement_record(pickup_order_id);

-- 17. notification_log
CREATE TABLE IF NOT EXISTS notification_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  pickup_order_id BIGINT,
  contract_id BIGINT,
  buyer_id BIGINT,
  channel TINYINT,
  recipient_phone VARCHAR(20),
  content VARCHAR(500),
  sent_at TIMESTAMP,
  delivered TINYINT DEFAULT 0,
  delivery_receipt VARCHAR(200),
  link_clicked TINYINT DEFAULT 0,
  link_clicked_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
CREATE INDEX idx_notification_log_pickup_order_id ON notification_log(pickup_order_id);

-- 18. confirm_timeout_config
CREATE TABLE IF NOT EXISTS confirm_timeout_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT,
  buyer_id BIGINT,
  scenario VARCHAR(30),
  timeout_value INT,
  timeout_unit VARCHAR(10),
  reminder_before_hours INT DEFAULT 6,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);

-- =============================================================================
-- Analytics Module
-- =============================================================================

-- 19. analytics_event
CREATE TABLE IF NOT EXISTS analytics_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id VARCHAR(36) NOT NULL,
  event_type VARCHAR(30) NOT NULL,
  event_name VARCHAR(100) NOT NULL,
  event_time TIMESTAMP NOT NULL,
  system VARCHAR(30) NOT NULL DEFAULT 'autorecon',
  user_id BIGINT DEFAULT 0,
  enterprise_id BIGINT DEFAULT 0,
  role_type TINYINT DEFAULT 0,
  is_guest TINYINT DEFAULT 0,
  session_id VARCHAR(36) DEFAULT '',
  device_id VARCHAR(64) DEFAULT '',
  platform VARCHAR(20) DEFAULT 'web',
  os VARCHAR(30) DEFAULT '',
  browser VARCHAR(30) DEFAULT '',
  screen_width SMALLINT DEFAULT 0,
  screen_height SMALLINT DEFAULT 0,
  is_mobile TINYINT DEFAULT 0,
  page_path VARCHAR(200) DEFAULT '',
  page_name VARCHAR(50) DEFAULT '',
  page_title VARCHAR(100) DEFAULT '',
  page_module VARCHAR(30) DEFAULT '',
  referrer VARCHAR(200) DEFAULT '',
  duration INT DEFAULT 0,
  action_category VARCHAR(50) DEFAULT '',
  action_label VARCHAR(50) DEFAULT '',
  action_value VARCHAR(200) DEFAULT '',
  action_extra CLOB,
  perf_fcp SMALLINT DEFAULT 0,
  perf_lcp SMALLINT DEFAULT 0,
  perf_fid SMALLINT DEFAULT 0,
  perf_cls FLOAT DEFAULT 0,
  ip VARCHAR(45) DEFAULT '',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_analytics_event_id ON analytics_event(event_id);
CREATE INDEX IF NOT EXISTS idx_analytics_event_time ON analytics_event(event_time);
CREATE INDEX IF NOT EXISTS idx_analytics_system ON analytics_event(system);
CREATE INDEX IF NOT EXISTS idx_analytics_enterprise ON analytics_event(enterprise_id);

-- 20. analytics_page_daily
CREATE TABLE IF NOT EXISTS analytics_page_daily (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  stat_date DATE NOT NULL,
  system VARCHAR(30) NOT NULL,
  page_path VARCHAR(200) NOT NULL,
  page_name VARCHAR(50) DEFAULT '',
  page_module VARCHAR(30) DEFAULT '',
  pv INT DEFAULT 0,
  uv INT DEFAULT 0,
  sessions INT DEFAULT 0,
  avg_duration INT DEFAULT 0,
  bounce_count INT DEFAULT 0,
  avg_fcp SMALLINT DEFAULT 0,
  avg_lcp SMALLINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_page_daily_date_sys_path ON analytics_page_daily(stat_date, system, page_path);

-- 21. analytics_action_daily
CREATE TABLE IF NOT EXISTS analytics_action_daily (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  stat_date DATE NOT NULL,
  system VARCHAR(30) NOT NULL,
  event_name VARCHAR(100) NOT NULL,
  action_category VARCHAR(50) DEFAULT '',
  action_count INT DEFAULT 0,
  action_users INT DEFAULT 0,
  action_enterprises INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_action_daily_date_sys_evt ON analytics_action_daily(stat_date, system, event_name, action_category);

-- 22. analytics_report_config
CREATE TABLE IF NOT EXISTS analytics_report_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_name VARCHAR(100) NOT NULL,
  report_type VARCHAR(50) NOT NULL,
  system VARCHAR(30),
  config_json CLOB,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);

-- 23. analytics_funnel_config
CREATE TABLE IF NOT EXISTS analytics_funnel_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  funnel_name VARCHAR(100) NOT NULL,
  system VARCHAR(30),
  steps_json CLOB NOT NULL,
  window_hours INT DEFAULT 24,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);

-- 24. analytics_alert_rule
CREATE TABLE IF NOT EXISTS analytics_alert_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alert_name VARCHAR(100) NOT NULL,
  metric VARCHAR(100) NOT NULL,
  condition_type VARCHAR(20) NOT NULL,
  threshold DECIMAL(10,2) NOT NULL,
  window_minutes INT DEFAULT 60,
  notify_channels VARCHAR(200),
  enabled TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
