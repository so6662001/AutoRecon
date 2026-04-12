-- AutoRecon - H2 Schema (MySQL compatible mode)
-- Converted from MySQL for demo profile

-- 1. enterprise
CREATE TABLE IF NOT EXISTS enterprise (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_name VARCHAR(200) NOT NULL,
  unified_credit_code VARCHAR(18) NOT NULL,
  contact_name VARCHAR(50),
  contact_phone VARCHAR(20),
  contact_email VARCHAR(100),
  enterprise_type TINYINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  logo_url VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 2. enterprise_auth
CREATE TABLE IF NOT EXISTS enterprise_auth (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  company_name VARCHAR(200) NOT NULL,
  unified_credit_code VARCHAR(18) NOT NULL,
  legal_person_name VARCHAR(50),
  legal_person_id_no VARCHAR(100),
  legal_person_phone VARCHAR(100),
  business_license_url VARCHAR(500),
  legal_person_id_front_url VARCHAR(500),
  legal_person_id_back_url VARCHAR(500),
  authorization_letter_url VARCHAR(500),
  third_party_org_id VARCHAR(100),
  auth_status TINYINT NOT NULL DEFAULT 0,
  auth_fail_reason VARCHAR(500),
  auth_passed_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 3. erp_connection
CREATE TABLE IF NOT EXISTS erp_connection (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  connection_name VARCHAR(100) NOT NULL,
  connection_type TINYINT NOT NULL,
  base_url VARCHAR(500),
  auth_type TINYINT,
  auth_config VARCHAR(4000),
  field_mapping VARCHAR(4000),
  pull_strategy TINYINT,
  cron_expression VARCHAR(50),
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 4. buyer_data_config
CREATE TABLE IF NOT EXISTS buyer_data_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  submit_mode TINYINT NOT NULL,
  excel_mapping_config VARCHAR(4000),
  default_confirm_mode TINYINT,
  ocr_enabled TINYINT NOT NULL DEFAULT 0,
  mobile_enabled TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 5. recon_template
CREATE TABLE IF NOT EXISTS recon_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  template_name VARCHAR(100) NOT NULL,
  template_type TINYINT NOT NULL,
  header_config VARCHAR(4000),
  column_config VARCHAR(4000),
  footer_config VARCHAR(4000),
  style_config VARCHAR(4000),
  group_by VARCHAR(100),
  sort_by VARCHAR(100),
  is_default TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 6. recon_bill
CREATE TABLE IF NOT EXISTS recon_bill (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_no VARCHAR(32) NOT NULL,
  batch_id VARCHAR(32),
  seller_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  template_id BIGINT,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  total_amount DECIMAL(18,2) DEFAULT 0,
  total_quantity DECIMAL(18,4) DEFAULT 0,
  total_weight DECIMAL(18,4) DEFAULT 0,
  currency VARCHAR(3) NOT NULL DEFAULT 'CNY',
  prev_balance DECIMAL(18,2) DEFAULT 0,
  current_trade_amount DECIMAL(18,2) DEFAULT 0,
  current_payment_amount DECIMAL(18,2) DEFAULT 0,
  current_balance DECIMAL(18,2) DEFAULT 0,
  payment_alloc_strategy TINYINT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL,
  match_mode TINYINT,
  include_payment_detail TINYINT NOT NULL DEFAULT 0,
  match_result TINYINT NOT NULL DEFAULT 0,
  dispute_prediction_score DECIMAL(5,2),
  seller_sign_status TINYINT NOT NULL DEFAULT 0,
  buyer_sign_status TINYINT NOT NULL DEFAULT 0,
  auto_confirm_deadline TIMESTAMP,
  auto_confirmed TINYINT NOT NULL DEFAULT 0,
  pdf_url VARCHAR(500),
  signed_pdf_url VARCHAR(500),
  source_type TINYINT NOT NULL DEFAULT 1,
  auto_recon_plan_id BIGINT,
  remark VARCHAR(500),
  created_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_recon_bill_bill_no ON recon_bill(bill_no);

-- 7. recon_bill_item
CREATE TABLE IF NOT EXISTS recon_bill_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  contract_no VARCHAR(50),
  contract_name VARCHAR(200),
  order_no VARCHAR(50),
  delivery_no VARCHAR(50),
  source_doc_type TINYINT,
  product_name VARCHAR(200),
  spec VARCHAR(100),
  material VARCHAR(50),
  origin VARCHAR(100),
  warehouse VARCHAR(100),
  quantity DECIMAL(18,4) DEFAULT 0,
  weight DECIMAL(18,4) DEFAULT 0,
  unit_price DECIMAL(18,2) DEFAULT 0,
  amount DECIMAL(18,2) DEFAULT 0,
  tax_rate DECIMAL(5,2) DEFAULT 0,
  tax_amount DECIMAL(18,2) DEFAULT 0,
  total_amount DECIMAL(18,2) DEFAULT 0,
  delivery_date DATE,
  settle_date DATE,
  match_status TINYINT NOT NULL DEFAULT 0,
  buyer_quantity DECIMAL(18,4),
  buyer_weight DECIMAL(18,4),
  buyer_amount DECIMAL(18,2),
  diff_quantity DECIMAL(18,4),
  diff_weight DECIMAL(18,4),
  diff_amount DECIMAL(18,2),
  paid_amount DECIMAL(18,2) DEFAULT 0,
  unpaid_amount DECIMAL(18,2) DEFAULT 0,
  invoice_no VARCHAR(50),
  invoice_status TINYINT NOT NULL DEFAULT 0,
  dispute_risk_level TINYINT NOT NULL DEFAULT 0,
  dispute_risk_reason VARCHAR(200),
  weight_diff_cause VARCHAR(100),
  buyer_data_source TINYINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 8. payment
CREATE TABLE IF NOT EXISTS payment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  payment_no VARCHAR(32) NOT NULL,
  payer_id BIGINT NOT NULL,
  payee_id BIGINT NOT NULL,
  payment_date DATE NOT NULL,
  payment_amount DECIMAL(18,2) NOT NULL,
  payment_method TINYINT,
  bank_serial_no VARCHAR(50),
  allocated_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  unallocated_amount DECIMAL(18,2),
  source TINYINT,
  status TINYINT NOT NULL,
  remark VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 9. payment_allocation
CREATE TABLE IF NOT EXISTS payment_allocation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  payment_id BIGINT NOT NULL,
  bill_id BIGINT NOT NULL,
  bill_item_id BIGINT,
  source_doc_no VARCHAR(50),
  allocated_amount DECIMAL(18,2) NOT NULL,
  allocation_type TINYINT,
  allocated_by BIGINT,
  allocated_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 10. dispute
CREATE TABLE IF NOT EXISTS dispute (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  bill_item_id BIGINT,
  dispute_type TINYINT NOT NULL,
  description VARCHAR(500),
  raised_by BIGINT NOT NULL,
  raised_by_side TINYINT NOT NULL,
  status TINYINT NOT NULL,
  resolved_at TIMESTAMP,
  resolution VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 11. dispute_message
CREATE TABLE IF NOT EXISTS dispute_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  dispute_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  sender_side TINYINT NOT NULL,
  message_type TINYINT NOT NULL,
  content CLOB,
  attachment_url VARCHAR(500),
  read_status TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 12. enterprise_seal
CREATE TABLE IF NOT EXISTS enterprise_seal (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  seal_name VARCHAR(100) NOT NULL,
  seal_type TINYINT NOT NULL,
  seal_source TINYINT,
  third_party_seal_id VARCHAR(100),
  third_party_org_id VARCHAR(100),
  seal_image_url VARCHAR(500),
  status TINYINT NOT NULL,
  legal_person_confirmed TINYINT NOT NULL DEFAULT 0,
  legal_person_confirm_at TIMESTAMP,
  legal_person_confirm_method TINYINT,
  disabled_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 13. seal_operator
CREATE TABLE IF NOT EXISTS seal_operator (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  operator_name VARCHAR(50) NOT NULL,
  phone VARCHAR(100),
  id_no VARCHAR(100),
  third_party_person_id VARCHAR(100),
  personal_auth_status TINYINT NOT NULL DEFAULT 0,
  allowed_seal_types VARCHAR(50),
  amount_limit DECIMAL(18,2) NOT NULL DEFAULT 0,
  require_approval TINYINT NOT NULL DEFAULT 0,
  approval_user_id BIGINT,
  verify_method TINYINT NOT NULL DEFAULT 1,
  auth_expire_date DATE,
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 14. sign_record
CREATE TABLE IF NOT EXISTS sign_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  sign_flow_id VARCHAR(100),
  sign_order_type TINYINT,
  sign_deadline TIMESTAMP,
  seller_sign_status TINYINT NOT NULL DEFAULT 0,
  seller_seal_id BIGINT,
  seller_operator_id BIGINT,
  seller_verify_method TINYINT,
  seller_sign_at TIMESTAMP,
  seller_sign_ip VARCHAR(50),
  seller_sign_device VARCHAR(100),
  seller_sign_channel TINYINT,
  buyer_sign_status TINYINT NOT NULL DEFAULT 0,
  buyer_seal_id BIGINT,
  buyer_operator_id BIGINT,
  buyer_verify_method TINYINT,
  buyer_sign_at TIMESTAMP,
  buyer_sign_ip VARCHAR(50),
  buyer_sign_device VARCHAR(100),
  buyer_sign_channel TINYINT,
  unsigned_pdf_url VARCHAR(500),
  signed_pdf_url VARCHAR(500),
  evidence_no VARCHAR(100),
  blockchain_hash VARCHAR(200),
  overall_status TINYINT NOT NULL DEFAULT 0,
  completed_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 15. sign_approval
CREATE TABLE IF NOT EXISTS sign_approval (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sign_record_id BIGINT NOT NULL,
  bill_id BIGINT NOT NULL,
  applicant_id BIGINT NOT NULL,
  approver_id BIGINT,
  seal_id BIGINT,
  bill_amount DECIMAL(18,2),
  approval_status TINYINT NOT NULL DEFAULT 0,
  approval_comment VARCHAR(500),
  approval_channel TINYINT,
  applied_at TIMESTAMP NOT NULL,
  approved_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 16. collection_plan
CREATE TABLE IF NOT EXISTS collection_plan (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  receivable_amount DECIMAL(18,2) NOT NULL,
  collected_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  remaining_amount DECIMAL(18,2) NOT NULL,
  due_date DATE,
  strategy_level VARCHAR(10),
  status TINYINT NOT NULL,
  current_stage TINYINT NOT NULL DEFAULT 1,
  next_action_date DATE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 17. collection_log
CREATE TABLE IF NOT EXISTS collection_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  plan_id BIGINT NOT NULL,
  bill_id BIGINT NOT NULL,
  action_type TINYINT NOT NULL,
  action_stage TINYINT,
  content VARCHAR(500),
  executed_at TIMESTAMP NOT NULL,
  executed_by VARCHAR(50),
  result VARCHAR(200),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 18. credit_score
CREATE TABLE IF NOT EXISTS credit_score (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  credit_score DECIMAL(5,2) NOT NULL,
  score_level VARCHAR(10),
  avg_payment_days DECIMAL(8,2),
  overdue_rate DECIMAL(5,2),
  dispute_rate DECIMAL(5,2),
  total_trade_amount DECIMAL(18,2),
  total_overdue_amount DECIMAL(18,2),
  score_factors VARCHAR(4000),
  last_calculated_at TIMESTAMP,
  trend TINYINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 19. invoice
CREATE TABLE IF NOT EXISTS invoice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  invoice_no VARCHAR(50) NOT NULL,
  invoice_code VARCHAR(20),
  invoice_type TINYINT,
  amount DECIMAL(18,2) DEFAULT 0,
  tax_amount DECIMAL(18,2) DEFAULT 0,
  total_amount DECIMAL(18,2) DEFAULT 0,
  invoice_date DATE,
  buyer_name VARCHAR(200),
  seller_name VARCHAR(200),
  status TINYINT NOT NULL,
  ocr_recognized TINYINT NOT NULL DEFAULT 0,
  original_file_url VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 20. invoice_link
CREATE TABLE IF NOT EXISTS invoice_link (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  bill_item_id BIGINT,
  invoice_id BIGINT NOT NULL,
  invoice_no VARCHAR(50),
  invoice_code VARCHAR(20),
  invoice_type TINYINT,
  invoice_amount DECIMAL(18,2),
  tax_amount DECIMAL(18,2),
  invoice_date DATE,
  link_amount DECIMAL(18,2),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 21. finance_apply
CREATE TABLE IF NOT EXISTS finance_apply (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  apply_no VARCHAR(32) NOT NULL,
  bill_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  factor_id BIGINT NOT NULL,
  apply_amount DECIMAL(18,2) NOT NULL,
  approved_amount DECIMAL(18,2),
  interest_rate DECIMAL(5,4),
  finance_term_days INT,
  status TINYINT NOT NULL,
  signed_pdf_url VARCHAR(500),
  invoice_urls VARCHAR(4000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 22. auto_recon_plan
CREATE TABLE IF NOT EXISTS auto_recon_plan (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  seller_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  plan_name VARCHAR(100) NOT NULL,
  frequency TINYINT NOT NULL,
  execution_day INT,
  execution_time TIME,
  template_id BIGINT,
  period_type TINYINT,
  auto_send TINYINT NOT NULL DEFAULT 1,
  include_payment TINYINT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  cron_expression VARCHAR(50),
  last_executed_at TIMESTAMP,
  next_execute_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 23. recon_calendar
CREATE TABLE IF NOT EXISTS recon_calendar (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  event_type TINYINT NOT NULL,
  event_date DATE NOT NULL,
  event_title VARCHAR(200) NOT NULL,
  related_bill_id BIGINT,
  related_buyer_id BIGINT,
  status TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 24. tolerance_learn
CREATE TABLE IF NOT EXISTS tolerance_learn (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  seller_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  dimension VARCHAR(20) NOT NULL,
  current_tolerance DECIMAL(10,4),
  suggested_tolerance DECIMAL(10,4),
  sample_count INT,
  match_rate_current DECIMAL(5,2),
  match_rate_suggested DECIMAL(5,2),
  false_positive_rate DECIMAL(5,2),
  confidence DECIMAL(5,2),
  adopted TINYINT NOT NULL DEFAULT 0,
  calculated_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 25. notify_subscription
CREATE TABLE IF NOT EXISTS notify_subscription (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  event_type VARCHAR(20) NOT NULL,
  channel_sms TINYINT NOT NULL DEFAULT 0,
  channel_email TINYINT NOT NULL DEFAULT 0,
  channel_wechat TINYINT NOT NULL DEFAULT 0,
  channel_app TINYINT NOT NULL DEFAULT 1,
  quiet_start TIME,
  quiet_end TIME,
  frequency_limit TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 26. buyer_engagement
CREATE TABLE IF NOT EXISTS buyer_engagement (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  buyer_enterprise_id BIGINT NOT NULL,
  seller_enterprise_id BIGINT NOT NULL,
  buyer_phone VARCHAR(20),
  buyer_contact_name VARCHAR(50),
  buyer_company_name VARCHAR(200),
  engagement_level TINYINT NOT NULL DEFAULT 0,
  first_link_sent_at TIMESTAMP,
  first_link_opened_at TIMESTAMP,
  registered_at TIMESTAMP,
  first_data_submit_at TIMESTAMP,
  seal_initialized_at TIMESTAMP,
  erp_connected_at TIMESTAMP,
  total_bills_sent INT NOT NULL DEFAULT 0,
  total_bills_confirmed INT NOT NULL DEFAULT 0,
  total_bills_ignored INT NOT NULL DEFAULT 0,
  last_active_at TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 27. guest_access_token
CREATE TABLE IF NOT EXISTS guest_access_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(32) NOT NULL,
  bill_id BIGINT NOT NULL,
  buyer_phone_hash VARCHAR(64),
  expire_at TIMESTAMP NOT NULL,
  phone_verified TINYINT NOT NULL DEFAULT 0,
  opened_count INT NOT NULL DEFAULT 0,
  first_opened_at TIMESTAMP,
  confirmed TINYINT NOT NULL DEFAULT 0,
  confirmed_at TIMESTAMP,
  dispute_message VARCHAR(500),
  ip_address VARCHAR(50),
  user_agent VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 28. subscription
CREATE TABLE IF NOT EXISTS subscription (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  plan_type TINYINT NOT NULL,
  billing_cycle TINYINT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  discount_rate DECIMAL(5,2) NOT NULL DEFAULT 1.0,
  actual_price DECIMAL(10,2) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  auto_renew TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL,
  trial_end_date DATE,
  referral_code VARCHAR(20),
  referred_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 29. service_usage
CREATE TABLE IF NOT EXISTS service_usage (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  service_type TINYINT NOT NULL,
  usage_month VARCHAR(7) NOT NULL,
  used_count INT NOT NULL DEFAULT 0,
  quota_total INT NOT NULL,
  quota_remaining INT NOT NULL,
  amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 30. billing_record
CREATE TABLE IF NOT EXISTS billing_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL,
  billing_month VARCHAR(7) NOT NULL,
  subscription_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  seal_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  collection_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  ocr_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  finance_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  other_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  total_fee DECIMAL(10,2) NOT NULL,
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  actual_amount DECIMAL(10,2) NOT NULL,
  payment_status TINYINT NOT NULL DEFAULT 0,
  paid_at TIMESTAMP,
  invoice_status TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 31. sys_user
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50),
  phone VARCHAR(20),
  email VARCHAR(100),
  role_type TINYINT NOT NULL,
  avatar_url VARCHAR(500),
  status TINYINT NOT NULL DEFAULT 1,
  last_login_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username ON sys_user(username);

-- 32. audit_log
CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT,
  user_id BIGINT,
  user_name VARCHAR(50),
  module VARCHAR(50) NOT NULL,
  action VARCHAR(50) NOT NULL,
  target_type VARCHAR(50),
  target_id BIGINT,
  detail VARCHAR(4000),
  ip_address VARCHAR(50),
  user_agent VARCHAR(200),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
);
