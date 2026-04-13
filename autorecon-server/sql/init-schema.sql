-- AutoRecon - Steel Industry Reconciliation Platform
-- MySQL 8.0 Schema Initialization
-- Charset: utf8mb4, Engine: InnoDB

CREATE DATABASE IF NOT EXISTS autorecon
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE autorecon;

-- =============================================================================
-- 1. enterprise - 企业账户
-- =============================================================================
CREATE TABLE enterprise (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  company_name VARCHAR(200) NOT NULL COMMENT '企业名称',
  unified_credit_code VARCHAR(18) NOT NULL COMMENT '统一社会信用代码',
  contact_name VARCHAR(50) COMMENT '联系人姓名',
  contact_phone VARCHAR(20) COMMENT '联系电话',
  contact_email VARCHAR(100) COMMENT '联系邮箱',
  enterprise_type TINYINT NOT NULL COMMENT '1-seller 2-buyer 3-both',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  logo_url VARCHAR(500) COMMENT '企业Logo URL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_enterprise_unified_credit_code (unified_credit_code),
  INDEX idx_enterprise_enterprise_type (enterprise_type),
  INDEX idx_enterprise_status (status),
  INDEX idx_enterprise_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业账户';

-- =============================================================================
-- 2. enterprise_auth - 企业认证信息
-- =============================================================================
CREATE TABLE enterprise_auth (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  company_name VARCHAR(200) NOT NULL COMMENT '企业名称',
  unified_credit_code VARCHAR(18) NOT NULL COMMENT '统一社会信用代码',
  legal_person_name VARCHAR(50) COMMENT '法人姓名',
  legal_person_id_no VARCHAR(100) COMMENT '法人身份证号(加密存储,明文18位)',
  legal_person_phone VARCHAR(100) COMMENT '法人手机号(加密存储,明文11-20位)',
  business_license_url VARCHAR(500) COMMENT '营业执照URL',
  legal_person_id_front_url VARCHAR(500) COMMENT '法人身份证正面URL',
  legal_person_id_back_url VARCHAR(500) COMMENT '法人身份证背面URL',
  authorization_letter_url VARCHAR(500) COMMENT '授权书URL',
  third_party_org_id VARCHAR(100) COMMENT '第三方机构ID',
  auth_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-not 1-pending 2-passed 3-failed',
  auth_fail_reason VARCHAR(500) COMMENT '认证失败原因',
  auth_passed_at DATETIME COMMENT '认证通过时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_enterprise_auth_enterprise_id (enterprise_id),
  INDEX idx_enterprise_auth_auth_status (auth_status),
  INDEX idx_enterprise_auth_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业认证信息';

-- =============================================================================
-- 3. erp_connection - ERP连接配置
-- =============================================================================
CREATE TABLE erp_connection (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  connection_name VARCHAR(100) NOT NULL COMMENT '连接名称',
  connection_type TINYINT NOT NULL COMMENT '1-REST 2-WebService 3-DB 4-File',
  base_url VARCHAR(500) COMMENT '基础URL',
  auth_type TINYINT COMMENT '认证类型',
  auth_config JSON COMMENT '认证配置',
  field_mapping JSON COMMENT '字段映射配置',
  pull_strategy TINYINT COMMENT '拉取策略',
  cron_expression VARCHAR(50) COMMENT 'Cron表达式',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_erp_connection_enterprise_id (enterprise_id),
  INDEX idx_erp_connection_status (status),
  INDEX idx_erp_connection_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP连接配置';

-- =============================================================================
-- 4. buyer_data_config - 买方数据提交配置
-- =============================================================================
CREATE TABLE buyer_data_config (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  submit_mode TINYINT NOT NULL COMMENT '1-ERP 2-Excel 3-Online 4-OCR 5-Mobile',
  excel_mapping_config JSON COMMENT 'Excel映射配置',
  default_confirm_mode TINYINT COMMENT '默认确认模式',
  ocr_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'OCR是否启用',
  mobile_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '移动端是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_buyer_data_config_enterprise_id (enterprise_id),
  INDEX idx_buyer_data_config_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='买方数据提交配置';

-- =============================================================================
-- 5. recon_template - 对账单模板
-- =============================================================================
CREATE TABLE recon_template (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
  template_type TINYINT NOT NULL COMMENT '1-standard 2-simple 3-custom',
  header_config JSON COMMENT '表头配置',
  column_config JSON COMMENT '列配置',
  footer_config JSON COMMENT '表尾配置',
  style_config JSON COMMENT '样式配置',
  group_by VARCHAR(100) COMMENT '分组字段',
  sort_by VARCHAR(100) COMMENT '排序字段',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认模板',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_recon_template_enterprise_id (enterprise_id),
  INDEX idx_recon_template_template_type (template_type),
  INDEX idx_recon_template_status (status),
  INDEX idx_recon_template_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账单模板';

-- =============================================================================
-- 6. recon_bill - 对账单 (core table)
-- =============================================================================
CREATE TABLE recon_bill (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bill_no VARCHAR(32) NOT NULL COMMENT '对账单号',
  batch_id VARCHAR(32) COMMENT '批次ID',
  seller_id BIGINT NOT NULL COMMENT 'FK enterprise.id 卖方',
  buyer_id BIGINT NOT NULL COMMENT 'FK enterprise.id 买方',
  template_id BIGINT COMMENT 'FK recon_template.id',
  period_start DATE NOT NULL COMMENT '对账周期开始',
  period_end DATE NOT NULL COMMENT '对账周期结束',
  total_amount DECIMAL(18,2) DEFAULT 0 COMMENT '总金额',
  total_quantity DECIMAL(18,4) DEFAULT 0 COMMENT '总数量',
  total_weight DECIMAL(18,4) DEFAULT 0 COMMENT '总重量',
  currency VARCHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  prev_balance DECIMAL(18,2) DEFAULT 0 COMMENT '上期余额',
  current_trade_amount DECIMAL(18,2) DEFAULT 0 COMMENT '本期交易金额',
  current_payment_amount DECIMAL(18,2) DEFAULT 0 COMMENT '本期付款金额',
  current_balance DECIMAL(18,2) DEFAULT 0 COMMENT '本期余额',
  payment_alloc_strategy TINYINT NOT NULL DEFAULT 1 COMMENT '付款分配策略',
  status VARCHAR(20) NOT NULL COMMENT '状态',
  match_mode TINYINT COMMENT '匹配模式',
  include_payment_detail TINYINT NOT NULL DEFAULT 0 COMMENT '是否包含付款明细',
  match_result TINYINT NOT NULL DEFAULT 0 COMMENT '匹配结果',
  dispute_prediction_score DECIMAL(5,2) COMMENT '争议预测评分',
  seller_sign_status TINYINT NOT NULL DEFAULT 0 COMMENT '卖方签章状态',
  buyer_sign_status TINYINT NOT NULL DEFAULT 0 COMMENT '买方签章状态',
  auto_confirm_deadline DATETIME COMMENT '自动确认截止时间',
  auto_confirmed TINYINT NOT NULL DEFAULT 0 COMMENT '是否已自动确认',
  pdf_url VARCHAR(500) COMMENT '对账单PDF URL',
  signed_pdf_url VARCHAR(500) COMMENT '已签章PDF URL',
  source_type TINYINT NOT NULL DEFAULT 1 COMMENT '来源类型',
  auto_recon_plan_id BIGINT COMMENT 'FK auto_recon_plan.id',
  remark VARCHAR(500) COMMENT '备注',
  created_by BIGINT COMMENT 'FK sys_user.id 创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  UNIQUE KEY uk_recon_bill_bill_no (bill_no),
  INDEX idx_recon_bill_seller_id (seller_id),
  INDEX idx_recon_bill_buyer_id (buyer_id),
  INDEX idx_recon_bill_status (status),
  INDEX idx_recon_bill_bill_no (bill_no),
  INDEX idx_recon_bill_period_start (period_start),
  INDEX idx_recon_bill_period_end (period_end),
  INDEX idx_recon_bill_batch_id (batch_id),
  INDEX idx_recon_bill_template_id (template_id),
  INDEX idx_recon_bill_auto_recon_plan_id (auto_recon_plan_id),
  INDEX idx_recon_bill_created_at (created_at),
  INDEX idx_recon_bill_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账单';

-- =============================================================================
-- 7. recon_bill_item - 对账单明细
-- =============================================================================
CREATE TABLE recon_bill_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL COMMENT 'FK recon_bill.id',
  line_no INT NOT NULL COMMENT '行号',
  contract_no VARCHAR(50) COMMENT '合同号',
  contract_name VARCHAR(200) COMMENT '合同名称',
  order_no VARCHAR(50) COMMENT '订单号',
  delivery_no VARCHAR(50) COMMENT '发货单号',
  source_doc_type TINYINT COMMENT '来源单据类型',
  product_name VARCHAR(200) COMMENT '产品名称',
  spec VARCHAR(100) COMMENT '规格',
  material VARCHAR(50) COMMENT '材质',
  origin VARCHAR(100) COMMENT '产地',
  warehouse VARCHAR(100) COMMENT '仓库',
  quantity DECIMAL(18,4) DEFAULT 0 COMMENT '数量',
  weight DECIMAL(18,4) DEFAULT 0 COMMENT '重量',
  unit_price DECIMAL(18,2) DEFAULT 0 COMMENT '单价',
  amount DECIMAL(18,2) DEFAULT 0 COMMENT '金额',
  tax_rate DECIMAL(5,2) DEFAULT 0 COMMENT '税率',
  tax_amount DECIMAL(18,2) DEFAULT 0 COMMENT '税额',
  total_amount DECIMAL(18,2) DEFAULT 0 COMMENT '含税总金额',
  delivery_date DATE COMMENT '发货日期',
  settle_date DATE COMMENT '结算日期',
  match_status TINYINT NOT NULL DEFAULT 0 COMMENT '匹配状态',
  buyer_quantity DECIMAL(18,4) COMMENT '买方数量',
  buyer_weight DECIMAL(18,4) COMMENT '买方重量',
  buyer_amount DECIMAL(18,2) COMMENT '买方金额',
  diff_quantity DECIMAL(18,4) COMMENT '数量差异',
  diff_weight DECIMAL(18,4) COMMENT '重量差异',
  diff_amount DECIMAL(18,2) COMMENT '金额差异',
  paid_amount DECIMAL(18,2) DEFAULT 0 COMMENT '已付金额',
  unpaid_amount DECIMAL(18,2) DEFAULT 0 COMMENT '未付金额',
  invoice_no VARCHAR(50) COMMENT '发票号',
  invoice_status TINYINT NOT NULL DEFAULT 0 COMMENT '发票状态',
  dispute_risk_level TINYINT NOT NULL DEFAULT 0 COMMENT '争议风险等级',
  dispute_risk_reason VARCHAR(200) COMMENT '争议风险原因',
  weight_diff_cause VARCHAR(100) COMMENT '重量差异原因',
  buyer_data_source TINYINT COMMENT '买方数据来源',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_recon_bill_item_bill_id (bill_id),
  INDEX idx_recon_bill_item_contract_no (contract_no),
  INDEX idx_recon_bill_item_order_no (order_no),
  INDEX idx_recon_bill_item_delivery_no (delivery_no),
  INDEX idx_recon_bill_item_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账单明细';

-- =============================================================================
-- 8. payment - 付款记录
-- =============================================================================
CREATE TABLE payment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  payment_no VARCHAR(32) NOT NULL COMMENT '付款单号',
  payer_id BIGINT NOT NULL COMMENT 'FK enterprise.id 付款方',
  payee_id BIGINT NOT NULL COMMENT 'FK enterprise.id 收款方',
  payment_date DATE NOT NULL COMMENT '付款日期',
  payment_amount DECIMAL(18,2) NOT NULL COMMENT '付款金额',
  payment_method TINYINT COMMENT '付款方式',
  bank_serial_no VARCHAR(50) COMMENT '银行流水号',
  allocated_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已分配金额',
  unallocated_amount DECIMAL(18,2) COMMENT '未分配金额',
  source TINYINT COMMENT '来源',
  status TINYINT NOT NULL COMMENT '状态',
  remark VARCHAR(500) COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_payment_payment_no (payment_no),
  INDEX idx_payment_payer_id (payer_id),
  INDEX idx_payment_payee_id (payee_id),
  INDEX idx_payment_payment_date (payment_date),
  INDEX idx_payment_status (status),
  INDEX idx_payment_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='付款记录';

-- =============================================================================
-- 9. payment_allocation - 付款抵扣明细
-- =============================================================================
CREATE TABLE payment_allocation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  payment_id BIGINT NOT NULL COMMENT 'FK payment.id',
  bill_id BIGINT COMMENT 'FK recon_bill.id 可空(跨期欠款分配)',
  bill_item_id BIGINT COMMENT 'FK recon_bill_item.id',
  source_doc_no VARCHAR(50) COMMENT '源单据号',
  allocated_amount DECIMAL(18,2) NOT NULL COMMENT '分配金额',
  allocation_type TINYINT COMMENT '分配类型',
  allocated_by BIGINT COMMENT 'FK sys_user.id 分配人',
  allocated_at DATETIME NOT NULL COMMENT '分配时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_payment_allocation_payment_id (payment_id),
  INDEX idx_payment_allocation_bill_id (bill_id),
  INDEX idx_payment_allocation_bill_item_id (bill_item_id),
  INDEX idx_payment_allocation_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='付款抵扣明细';

-- =============================================================================
-- 10. dispute - 异议记录
-- =============================================================================
CREATE TABLE dispute (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL COMMENT 'FK recon_bill.id',
  bill_item_id BIGINT COMMENT 'FK recon_bill_item.id',
  dispute_type TINYINT NOT NULL COMMENT '1-quantity 2-weight 3-price 4-spec 5-missing 6-other',
  description VARCHAR(500) COMMENT '异议描述',
  raised_by BIGINT NOT NULL COMMENT 'FK sys_user.id 提出人',
  raised_by_side TINYINT NOT NULL COMMENT '1-seller 2-buyer',
  status TINYINT NOT NULL COMMENT '1-open 2-processing 3-resolved 4-escalated',
  resolved_at DATETIME COMMENT '解决时间',
  resolution VARCHAR(500) COMMENT '解决方案',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_dispute_bill_id (bill_id),
  INDEX idx_dispute_status (status),
  INDEX idx_dispute_bill_item_id (bill_item_id),
  INDEX idx_dispute_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异议记录';

-- =============================================================================
-- 11. dispute_message - 异议沟通记录
-- =============================================================================
CREATE TABLE dispute_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  dispute_id BIGINT NOT NULL COMMENT 'FK dispute.id',
  sender_id BIGINT NOT NULL COMMENT 'FK sys_user.id 发送人',
  sender_side TINYINT NOT NULL COMMENT '发送方 1-seller 2-buyer',
  message_type TINYINT NOT NULL COMMENT '1-text 2-image 3-file 4-voice',
  content TEXT COMMENT '消息内容',
  attachment_url VARCHAR(500) COMMENT '附件URL',
  read_status TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_dispute_message_dispute_id (dispute_id),
  INDEX idx_dispute_message_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异议沟通记录';

-- =============================================================================
-- 12. enterprise_seal - 企业印章
-- =============================================================================
CREATE TABLE enterprise_seal (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  seal_name VARCHAR(100) NOT NULL COMMENT '印章名称',
  seal_type TINYINT NOT NULL COMMENT '1-official 2-contract 3-finance 4-legal_person',
  seal_source TINYINT COMMENT '印章来源',
  third_party_seal_id VARCHAR(100) COMMENT '第三方印章ID',
  third_party_org_id VARCHAR(100) COMMENT '第三方机构ID',
  seal_image_url VARCHAR(500) COMMENT '印章图片URL',
  status TINYINT NOT NULL COMMENT '0-init 1-active 2-disabled 3-revoked',
  legal_person_confirmed TINYINT NOT NULL DEFAULT 0 COMMENT '法人是否确认',
  legal_person_confirm_at DATETIME COMMENT '法人确认时间',
  legal_person_confirm_method TINYINT COMMENT '法人确认方式',
  disabled_at DATETIME COMMENT '停用时间',
  created_by BIGINT COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_enterprise_seal_enterprise_id (enterprise_id),
  INDEX idx_enterprise_seal_status (status),
  INDEX idx_enterprise_seal_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业印章';

-- =============================================================================
-- 13. seal_operator - 签章经办人
-- =============================================================================
CREATE TABLE seal_operator (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  user_id BIGINT NOT NULL COMMENT 'FK sys_user.id',
  operator_name VARCHAR(50) NOT NULL COMMENT '经办人姓名',
  phone VARCHAR(100) COMMENT '手机号(加密存储,明文11-20位)',
  id_no VARCHAR(100) COMMENT '身份证号(加密存储,明文18位)',
  third_party_person_id VARCHAR(100) COMMENT '第三方人员ID',
  personal_auth_status TINYINT NOT NULL DEFAULT 0 COMMENT '个人认证状态',
  allowed_seal_types VARCHAR(50) COMMENT '允许使用的印章类型',
  amount_limit DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额限额',
  require_approval TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要审批',
  approval_user_id BIGINT COMMENT 'FK sys_user.id 审批人',
  verify_method TINYINT NOT NULL DEFAULT 1 COMMENT '验证方式',
  auth_expire_date DATE COMMENT '授权到期日',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_seal_operator_enterprise_id (enterprise_id),
  INDEX idx_seal_operator_user_id (user_id),
  INDEX idx_seal_operator_status (status),
  INDEX idx_seal_operator_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签章经办人';

-- =============================================================================
-- 14. sign_record - 签章记录
-- =============================================================================
CREATE TABLE sign_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL COMMENT 'FK recon_bill.id',
  sign_flow_id VARCHAR(100) COMMENT '签章流程ID',
  sign_order_type TINYINT COMMENT '签章顺序类型',
  sign_deadline DATETIME COMMENT '签章截止时间',
  seller_sign_status TINYINT NOT NULL DEFAULT 0 COMMENT '卖方签章状态',
  seller_seal_id BIGINT COMMENT 'FK enterprise_seal.id 卖方印章',
  seller_operator_id BIGINT COMMENT 'FK seal_operator.id 卖方经办人',
  seller_verify_method TINYINT COMMENT '卖方验证方式',
  seller_sign_at DATETIME COMMENT '卖方签章时间',
  seller_sign_ip VARCHAR(50) COMMENT '卖方签章IP',
  seller_sign_device VARCHAR(100) COMMENT '卖方签章设备',
  seller_sign_location VARCHAR(200) COMMENT '卖方签署地理位置',
  seller_sign_channel TINYINT COMMENT '卖方签章渠道',
  buyer_sign_status TINYINT NOT NULL DEFAULT 0 COMMENT '买方签章状态',
  buyer_seal_id BIGINT COMMENT 'FK enterprise_seal.id 买方印章',
  buyer_operator_id BIGINT COMMENT 'FK seal_operator.id 买方经办人',
  buyer_verify_method TINYINT COMMENT '买方验证方式',
  buyer_sign_at DATETIME COMMENT '买方签章时间',
  buyer_sign_ip VARCHAR(50) COMMENT '买方签章IP',
  buyer_sign_device VARCHAR(100) COMMENT '买方签章设备',
  buyer_sign_location VARCHAR(200) COMMENT '买方签署地理位置',
  buyer_sign_channel TINYINT COMMENT '买方签章渠道',
  unsigned_pdf_url VARCHAR(500) COMMENT '未签章PDF URL',
  signed_pdf_url VARCHAR(500) COMMENT '已签章PDF URL',
  evidence_no VARCHAR(100) COMMENT '存证编号',
  blockchain_hash VARCHAR(200) COMMENT '区块链哈希',
  overall_status TINYINT NOT NULL DEFAULT 0 COMMENT '整体状态',
  completed_at DATETIME COMMENT '完成时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_sign_record_bill_id (bill_id),
  INDEX idx_sign_record_sign_flow_id (sign_flow_id),
  INDEX idx_sign_record_overall_status (overall_status),
  INDEX idx_sign_record_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签章记录';

-- =============================================================================
-- 15. sign_approval - 签章审批
-- =============================================================================
CREATE TABLE sign_approval (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sign_record_id BIGINT NOT NULL COMMENT 'FK sign_record.id',
  bill_id BIGINT NOT NULL COMMENT 'FK recon_bill.id',
  applicant_id BIGINT NOT NULL COMMENT 'FK sys_user.id 申请人',
  approver_id BIGINT COMMENT 'FK sys_user.id 审批人',
  seal_id BIGINT COMMENT 'FK enterprise_seal.id',
  bill_amount DECIMAL(18,2) COMMENT '对账单金额',
  approval_status TINYINT NOT NULL DEFAULT 0 COMMENT '审批状态',
  approval_comment VARCHAR(500) COMMENT '审批意见',
  approval_channel TINYINT COMMENT '审批渠道',
  applied_at DATETIME NOT NULL COMMENT '申请时间',
  approved_at DATETIME COMMENT '审批时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_sign_approval_sign_record_id (sign_record_id),
  INDEX idx_sign_approval_bill_id (bill_id),
  INDEX idx_sign_approval_approval_status (approval_status),
  INDEX idx_sign_approval_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签章审批';

-- =============================================================================
-- 16. collection_plan - 催收计划
-- =============================================================================
CREATE TABLE collection_plan (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL COMMENT 'FK recon_bill.id',
  seller_id BIGINT NOT NULL COMMENT 'FK enterprise.id 卖方',
  buyer_id BIGINT NOT NULL COMMENT 'FK enterprise.id 买方',
  receivable_amount DECIMAL(18,2) NOT NULL COMMENT '应收金额',
  collected_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已收金额',
  remaining_amount DECIMAL(18,2) NOT NULL COMMENT '剩余金额',
  due_date DATE COMMENT '到期日',
  strategy_level VARCHAR(10) COMMENT '策略等级',
  status TINYINT NOT NULL COMMENT '1-active 2-paused 3-completed',
  current_stage TINYINT NOT NULL DEFAULT 1 COMMENT '当前阶段',
  next_action_date DATE COMMENT '下次行动日期',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_collection_plan_bill_id (bill_id),
  INDEX idx_collection_plan_seller_id (seller_id),
  INDEX idx_collection_plan_buyer_id (buyer_id),
  INDEX idx_collection_plan_status (status),
  INDEX idx_collection_plan_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='催收计划';

-- =============================================================================
-- 17. collection_log - 催收执行记录
-- =============================================================================
CREATE TABLE collection_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  plan_id BIGINT NOT NULL COMMENT 'FK collection_plan.id',
  bill_id BIGINT NOT NULL COMMENT 'FK recon_bill.id',
  action_type TINYINT NOT NULL COMMENT '1-sms 2-email 3-wechat 4-call 5-manual',
  action_stage TINYINT COMMENT '行动阶段',
  content VARCHAR(500) COMMENT '内容',
  executed_at DATETIME NOT NULL COMMENT '执行时间',
  executed_by VARCHAR(50) COMMENT '执行人',
  result VARCHAR(200) COMMENT '结果',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_collection_log_plan_id (plan_id),
  INDEX idx_collection_log_bill_id (bill_id),
  INDEX idx_collection_log_executed_at (executed_at),
  INDEX idx_collection_log_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='催收执行记录';

-- =============================================================================
-- 18. credit_score - 催收信用评分
-- =============================================================================
CREATE TABLE credit_score (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id 买方企业',
  seller_id BIGINT NOT NULL COMMENT 'FK enterprise.id 卖方企业',
  credit_score DECIMAL(5,2) NOT NULL COMMENT '信用评分',
  score_level VARCHAR(10) COMMENT '评分等级',
  avg_payment_days DECIMAL(8,2) COMMENT '平均付款天数',
  overdue_rate DECIMAL(5,2) COMMENT '逾期率',
  dispute_rate DECIMAL(5,2) COMMENT '争议率',
  total_trade_amount DECIMAL(18,2) COMMENT '总交易金额',
  total_overdue_amount DECIMAL(18,2) COMMENT '总逾期金额',
  score_factors JSON COMMENT '评分因子',
  last_calculated_at DATETIME COMMENT '最后计算时间',
  trend TINYINT COMMENT '趋势',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  UNIQUE KEY uk_credit_score_enterprise_seller (enterprise_id, seller_id),
  INDEX idx_credit_score_enterprise_id (enterprise_id),
  INDEX idx_credit_score_seller_id (seller_id),
  INDEX idx_credit_score_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='催收信用评分';

-- =============================================================================
-- 19. invoice - 发票记录
-- =============================================================================
CREATE TABLE invoice (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  invoice_no VARCHAR(50) NOT NULL COMMENT '发票号码',
  invoice_code VARCHAR(20) COMMENT '发票代码',
  invoice_type TINYINT COMMENT '发票类型',
  amount DECIMAL(18,2) DEFAULT 0 COMMENT '金额',
  tax_amount DECIMAL(18,2) DEFAULT 0 COMMENT '税额',
  total_amount DECIMAL(18,2) DEFAULT 0 COMMENT '价税合计',
  invoice_date DATE COMMENT '开票日期',
  buyer_name VARCHAR(200) COMMENT '购方名称',
  seller_name VARCHAR(200) COMMENT '销方名称',
  status TINYINT NOT NULL COMMENT '状态',
  ocr_recognized TINYINT NOT NULL DEFAULT 0 COMMENT '是否OCR识别',
  original_file_url VARCHAR(500) COMMENT '原文件URL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_invoice_enterprise_id (enterprise_id),
  INDEX idx_invoice_invoice_no (invoice_no),
  INDEX idx_invoice_invoice_date (invoice_date),
  INDEX idx_invoice_status (status),
  INDEX idx_invoice_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票记录';

-- =============================================================================
-- 20. invoice_link - 发票关联
-- =============================================================================
CREATE TABLE invoice_link (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL COMMENT 'FK recon_bill.id',
  bill_item_id BIGINT COMMENT 'FK recon_bill_item.id',
  invoice_id BIGINT NOT NULL COMMENT 'FK invoice.id',
  invoice_no VARCHAR(50) COMMENT '发票号码',
  invoice_code VARCHAR(20) COMMENT '发票代码',
  invoice_type TINYINT COMMENT '发票类型',
  invoice_amount DECIMAL(18,2) COMMENT '发票金额',
  tax_amount DECIMAL(18,2) COMMENT '税额',
  invoice_date DATE COMMENT '开票日期',
  link_amount DECIMAL(18,2) COMMENT '关联金额',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_invoice_link_bill_id (bill_id),
  INDEX idx_invoice_link_bill_item_id (bill_item_id),
  INDEX idx_invoice_link_invoice_id (invoice_id),
  INDEX idx_invoice_link_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票关联';

-- =============================================================================
-- 21. finance_apply - 融资申请
-- =============================================================================
CREATE TABLE finance_apply (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  apply_no VARCHAR(32) NOT NULL COMMENT '申请单号',
  bill_id BIGINT NOT NULL COMMENT 'FK recon_bill.id',
  seller_id BIGINT NOT NULL COMMENT 'FK enterprise.id 卖方',
  buyer_id BIGINT NOT NULL COMMENT 'FK enterprise.id 买方',
  factor_id BIGINT NOT NULL COMMENT 'FK enterprise.id 保理商',
  apply_amount DECIMAL(18,2) NOT NULL COMMENT '申请金额',
  approved_amount DECIMAL(18,2) COMMENT '批准金额',
  interest_rate DECIMAL(5,4) COMMENT '利率',
  finance_term_days INT COMMENT '融资期限(天)',
  status TINYINT NOT NULL COMMENT '状态',
  signed_pdf_url VARCHAR(500) COMMENT '已签章PDF URL',
  invoice_urls JSON COMMENT '发票URL列表',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_finance_apply_apply_no (apply_no),
  INDEX idx_finance_apply_bill_id (bill_id),
  INDEX idx_finance_apply_seller_id (seller_id),
  INDEX idx_finance_apply_buyer_id (buyer_id),
  INDEX idx_finance_apply_status (status),
  INDEX idx_finance_apply_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='融资申请';

-- =============================================================================
-- 22. auto_recon_plan - 定期自动对账计划
-- =============================================================================
CREATE TABLE auto_recon_plan (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  seller_id BIGINT NOT NULL COMMENT 'FK enterprise.id 卖方',
  buyer_id BIGINT COMMENT 'FK enterprise.id 买方 可空表示全部客户',
  plan_name VARCHAR(100) NOT NULL COMMENT '计划名称',
  frequency TINYINT NOT NULL COMMENT '频率',
  execution_day INT COMMENT '执行日',
  execution_time TIME COMMENT '执行时间',
  template_id BIGINT COMMENT 'FK recon_template.id',
  period_type TINYINT COMMENT '周期类型',
  auto_send TINYINT NOT NULL DEFAULT 1 COMMENT '是否自动发送',
  include_payment TINYINT NOT NULL DEFAULT 1 COMMENT '是否包含付款',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  cron_expression VARCHAR(50) COMMENT 'Cron表达式',
  last_executed_at DATETIME COMMENT '上次执行时间',
  next_execute_at DATETIME COMMENT '下次执行时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_auto_recon_plan_seller_id (seller_id),
  INDEX idx_auto_recon_plan_buyer_id (buyer_id),
  INDEX idx_auto_recon_plan_status (status),
  INDEX idx_auto_recon_plan_next_execute_at (next_execute_at),
  INDEX idx_auto_recon_plan_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定期自动对账计划';

-- =============================================================================
-- 23. recon_calendar - 对账日历事件
-- =============================================================================
CREATE TABLE recon_calendar (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  event_type TINYINT NOT NULL COMMENT '事件类型',
  event_date DATE NOT NULL COMMENT '事件日期',
  event_title VARCHAR(200) NOT NULL COMMENT '事件标题',
  related_bill_id BIGINT COMMENT 'FK recon_bill.id 关联对账单',
  related_buyer_id BIGINT COMMENT 'FK enterprise.id 关联买方',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_recon_calendar_enterprise_id (enterprise_id),
  INDEX idx_recon_calendar_event_date (event_date),
  INDEX idx_recon_calendar_event_type (event_type),
  INDEX idx_recon_calendar_related_bill_id (related_bill_id),
  INDEX idx_recon_calendar_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账日历事件';

-- =============================================================================
-- 24. tolerance_learn - 容差学习记录
-- =============================================================================
CREATE TABLE tolerance_learn (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  seller_id BIGINT NOT NULL COMMENT 'FK enterprise.id 卖方',
  buyer_id BIGINT NOT NULL COMMENT 'FK enterprise.id 买方',
  dimension VARCHAR(20) NOT NULL COMMENT '维度',
  current_tolerance DECIMAL(10,4) COMMENT '当前容差',
  suggested_tolerance DECIMAL(10,4) COMMENT '建议容差',
  sample_count INT COMMENT '样本数量',
  match_rate_current DECIMAL(5,2) COMMENT '当前匹配率',
  match_rate_suggested DECIMAL(5,2) COMMENT '建议匹配率',
  false_positive_rate DECIMAL(5,2) COMMENT '误报率',
  confidence DECIMAL(5,2) COMMENT '置信度',
  adopted TINYINT NOT NULL DEFAULT 0 COMMENT '是否已采纳',
  calculated_at DATETIME COMMENT '计算时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_tolerance_learn_seller_id (seller_id),
  INDEX idx_tolerance_learn_buyer_id (buyer_id),
  INDEX idx_tolerance_learn_dimension (dimension),
  INDEX idx_tolerance_learn_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='容差学习记录';

-- =============================================================================
-- 25. notify_subscription - 提醒订阅配置
-- =============================================================================
CREATE TABLE notify_subscription (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  user_id BIGINT NOT NULL COMMENT 'FK sys_user.id',
  event_type VARCHAR(20) NOT NULL COMMENT '事件类型',
  channel_sms TINYINT NOT NULL DEFAULT 0 COMMENT '短信渠道',
  channel_email TINYINT NOT NULL DEFAULT 0 COMMENT '邮件渠道',
  channel_wechat TINYINT NOT NULL DEFAULT 0 COMMENT '微信渠道',
  channel_app TINYINT NOT NULL DEFAULT 1 COMMENT 'APP渠道',
  quiet_start TIME COMMENT '免打扰开始时间',
  quiet_end TIME COMMENT '免打扰结束时间',
  frequency_limit TINYINT NOT NULL DEFAULT 1 COMMENT '频率限制',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_notify_subscription_enterprise_id (enterprise_id),
  INDEX idx_notify_subscription_user_id (user_id),
  INDEX idx_notify_subscription_event_type (event_type),
  INDEX idx_notify_subscription_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒订阅配置';

-- =============================================================================
-- 26. buyer_engagement - 买方参与等级
-- =============================================================================
CREATE TABLE buyer_engagement (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  buyer_enterprise_id BIGINT COMMENT 'FK enterprise.id 买方企业 可空(L0访客未注册)',
  seller_enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id 卖方企业',
  buyer_phone VARCHAR(20) COMMENT '买方手机号',
  buyer_contact_name VARCHAR(50) COMMENT '买方联系人',
  buyer_company_name VARCHAR(200) COMMENT '买方公司名称',
  engagement_level TINYINT NOT NULL DEFAULT 0 COMMENT '参与等级',
  first_link_sent_at DATETIME COMMENT '首次链接发送时间',
  first_link_opened_at DATETIME COMMENT '首次链接打开时间',
  registered_at DATETIME COMMENT '注册时间',
  first_data_submit_at DATETIME COMMENT '首次数据提交时间',
  seal_initialized_at DATETIME COMMENT '印章初始化时间',
  erp_connected_at DATETIME COMMENT 'ERP连接时间',
  total_bills_sent INT NOT NULL DEFAULT 0 COMMENT '已发送对账单总数',
  total_bills_confirmed INT NOT NULL DEFAULT 0 COMMENT '已确认对账单总数',
  total_bills_ignored INT NOT NULL DEFAULT 0 COMMENT '已忽略对账单总数',
  last_active_at DATETIME COMMENT '最后活跃时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_buyer_engagement_buyer_enterprise_id (buyer_enterprise_id),
  INDEX idx_buyer_engagement_seller_enterprise_id (seller_enterprise_id),
  INDEX idx_buyer_engagement_engagement_level (engagement_level),
  INDEX idx_buyer_engagement_status (status),
  INDEX idx_buyer_engagement_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='买方参与等级';

-- =============================================================================
-- 27. guest_access_token - 免注册访问令牌
-- =============================================================================
CREATE TABLE guest_access_token (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(32) NOT NULL COMMENT '访问令牌',
  bill_id BIGINT NOT NULL COMMENT 'FK recon_bill.id',
  buyer_phone_hash VARCHAR(64) COMMENT '买方手机号哈希',
  expire_at DATETIME NOT NULL COMMENT '过期时间',
  phone_verified TINYINT NOT NULL DEFAULT 0 COMMENT '手机是否已验证',
  opened_count INT NOT NULL DEFAULT 0 COMMENT '打开次数',
  first_opened_at DATETIME COMMENT '首次打开时间',
  confirmed TINYINT NOT NULL DEFAULT 0 COMMENT '是否已确认',
  confirmed_at DATETIME COMMENT '确认时间',
  dispute_message VARCHAR(500) COMMENT '异议留言',
  ip_address VARCHAR(50) COMMENT 'IP地址',
  user_agent VARCHAR(500) COMMENT 'User Agent',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  UNIQUE KEY uk_guest_access_token_token (token),
  INDEX idx_guest_access_token_bill_id (bill_id),
  INDEX idx_guest_access_token_expire_at (expire_at),
  INDEX idx_guest_access_token_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='免注册访问令牌';

-- =============================================================================
-- 28. subscription - 企业订阅
-- =============================================================================
CREATE TABLE subscription (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  plan_type TINYINT NOT NULL COMMENT '套餐类型',
  billing_cycle TINYINT NOT NULL COMMENT '计费周期',
  unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
  discount_rate DECIMAL(5,2) NOT NULL DEFAULT 1.0 COMMENT '折扣率',
  actual_price DECIMAL(10,2) NOT NULL COMMENT '实际价格',
  start_date DATE NOT NULL COMMENT '开始日期',
  end_date DATE NOT NULL COMMENT '结束日期',
  auto_renew TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动续费',
  status TINYINT NOT NULL COMMENT '状态',
  trial_end_date DATE COMMENT '试用结束日期',
  referral_code VARCHAR(20) COMMENT '推荐码',
  referred_by BIGINT COMMENT 'FK enterprise.id 推荐人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_subscription_enterprise_id (enterprise_id),
  INDEX idx_subscription_status (status),
  INDEX idx_subscription_end_date (end_date),
  INDEX idx_subscription_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业订阅';

-- =============================================================================
-- 29. service_usage - 增值服务用量
-- =============================================================================
CREATE TABLE service_usage (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  service_type TINYINT NOT NULL COMMENT '服务类型',
  usage_month VARCHAR(7) NOT NULL COMMENT '用量月份 YYYY-MM',
  used_count INT NOT NULL DEFAULT 0 COMMENT '已用数量',
  quota_total INT NOT NULL COMMENT '总配额',
  quota_remaining INT NOT NULL COMMENT '剩余配额',
  amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '金额',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_service_usage_enterprise_id (enterprise_id),
  INDEX idx_service_usage_usage_month (usage_month),
  INDEX idx_service_usage_service_type (service_type),
  INDEX idx_service_usage_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='增值服务用量';

-- =============================================================================
-- 30. billing_record - 账单记录
-- =============================================================================
CREATE TABLE billing_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  billing_month VARCHAR(7) NOT NULL COMMENT '账单月份 YYYY-MM',
  subscription_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订阅费',
  seal_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '签章费',
  collection_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '催收费',
  ocr_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'OCR费',
  finance_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '融资费',
  other_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '其他费用',
  total_fee DECIMAL(10,2) NOT NULL COMMENT '总费用',
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '折扣金额',
  actual_amount DECIMAL(10,2) NOT NULL COMMENT '实际金额',
  payment_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态',
  paid_at DATETIME COMMENT '支付时间',
  invoice_status TINYINT NOT NULL DEFAULT 0 COMMENT '发票状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_billing_record_enterprise_id (enterprise_id),
  INDEX idx_billing_record_billing_month (billing_month),
  INDEX idx_billing_record_payment_status (payment_status),
  INDEX idx_billing_record_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单记录';

-- =============================================================================
-- 31. sys_user - 系统用户
-- =============================================================================
CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT COMMENT 'FK enterprise.id',
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  password VARCHAR(100) NOT NULL COMMENT '密码',
  real_name VARCHAR(50) COMMENT '真实姓名',
  phone VARCHAR(20) COMMENT '手机号',
  email VARCHAR(100) COMMENT '邮箱',
  role_type TINYINT NOT NULL COMMENT '1-seller_admin 2-seller_operator 3-seller_finance 4-buyer_admin 5-buyer_operator 6-platform_admin',
  avatar_url VARCHAR(500) COMMENT '头像URL',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  last_login_at DATETIME COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  UNIQUE KEY uk_sys_user_username (username),
  INDEX idx_sys_user_enterprise_id (enterprise_id),
  INDEX idx_sys_user_role_type (role_type),
  INDEX idx_sys_user_status (status),
  INDEX idx_sys_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

-- =============================================================================
-- 32. audit_log - 审计日志
-- =============================================================================
CREATE TABLE audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT COMMENT 'FK enterprise.id',
  user_id BIGINT COMMENT 'FK sys_user.id',
  user_name VARCHAR(50) COMMENT '用户名',
  module VARCHAR(50) NOT NULL COMMENT '模块',
  action VARCHAR(50) NOT NULL COMMENT '操作',
  target_type VARCHAR(50) COMMENT '目标类型',
  target_id BIGINT COMMENT '目标ID',
  detail JSON COMMENT '详情',
  ip_address VARCHAR(50) COMMENT 'IP地址',
  user_agent VARCHAR(200) COMMENT 'User Agent',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  INDEX idx_audit_log_enterprise_id (enterprise_id),
  INDEX idx_audit_log_user_id (user_id),
  INDEX idx_audit_log_module (module),
  INDEX idx_audit_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';

-- =============================================================================
-- 33. enterprise_settings - 企业扩展设置（超时规则、通知模板、对账规则 JSON）
-- =============================================================================
CREATE TABLE enterprise_settings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  enterprise_id BIGINT NOT NULL COMMENT 'FK enterprise.id',
  no_diff_days INT COMMENT '无差异确认天数',
  spec_change_hours INT COMMENT '品规变更确认小时',
  over_diff_hours INT COMMENT '数量超差确认小时',
  settle_days INT COMMENT '结算单确认天数',
  reminder_hours INT COMMENT '到期前提醒小时',
  timeout_customers_json JSON COMMENT '按客户单独配置 JSON',
  notification_templates_json JSON COMMENT '通知模板列表 JSON',
  recon_rules_json JSON COMMENT '对账规则 JSON',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_enterprise_settings_enterprise_id (enterprise_id),
  INDEX idx_enterprise_settings_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业扩展设置';

-- =============================================================================
-- 34. agreement_version - 协议版本管理
-- =============================================================================
CREATE TABLE agreement_version (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agreement_type TINYINT NOT NULL COMMENT '协议类型: 1-用户服务协议 2-隐私保护政策',
    version_no VARCHAR(20) NOT NULL COMMENT '版本号(如v1.0, v2.0)',
    title VARCHAR(200) NOT NULL COMMENT '协议标题',
    content LONGTEXT COMMENT '协议内容(Markdown/HTML)',
    content_url VARCHAR(500) COMMENT 'OSS存储URL(PDF/HTML归档)',
    summary VARCHAR(500) COMMENT '本次更新摘要',
    effective_date DATE NOT NULL COMMENT '生效日期',
    published_by BIGINT COMMENT '发布人',
    published_at DATETIME COMMENT '发布时间',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-草稿 1-已发布 2-已废弃',
    require_reconfirm TINYINT DEFAULT 1 COMMENT '是否要求用户重新确认: 0-否 1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
    INDEX idx_agreement_type_status (agreement_type, status),
    INDEX idx_effective_date (effective_date),
    INDEX idx_agreement_version_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='协议版本管理';

-- =============================================================================
-- 35. agreement_confirmation - 用户协议确认记录
-- =============================================================================
CREATE TABLE agreement_confirmation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    enterprise_id BIGINT COMMENT '企业ID',
    agreement_version_id BIGINT NOT NULL COMMENT '协议版本ID',
    agreement_type TINYINT NOT NULL COMMENT '协议类型',
    version_no VARCHAR(20) NOT NULL COMMENT '确认的版本号',
    confirmed_at DATETIME NOT NULL COMMENT '确认时间',
    confirm_method TINYINT DEFAULT 1 COMMENT '确认方式: 1-登录弹窗确认 2-注册时确认 3-免注册确认',
    ip_address VARCHAR(50) COMMENT '确认时IP',
    user_agent VARCHAR(500) COMMENT '确认时设备信息',
    content_snapshot_url VARCHAR(500) COMMENT '确认时协议内容快照(OSS URL)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
    INDEX idx_user_agreement (user_id, agreement_type),
    INDEX idx_version (agreement_version_id),
    INDEX idx_agreement_confirmation_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户协议确认记录';
