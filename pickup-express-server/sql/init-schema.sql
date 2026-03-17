-- Pickup Express (提货通) Evidence Chain System
-- MySQL 8.0 schema - InnoDB, utf8mb4
-- All tables: id(BIGINT PK), created_at, updated_at, deleted(TINYINT default 0)

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. contract - 销售合同
-- ----------------------------
DROP TABLE IF EXISTS `contract`;
CREATE TABLE `contract` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `contract_no` VARCHAR(50) NOT NULL COMMENT '合同号',
    `erp_contract_id` VARCHAR(50) DEFAULT NULL COMMENT 'ERP合同ID',
    `contract_type` TINYINT DEFAULT NULL COMMENT '合同类型: 1-留货合同 2-订货合同',
    `seller_id` BIGINT DEFAULT NULL COMMENT '卖方企业ID',
    `buyer_id` BIGINT DEFAULT NULL COMMENT '买方企业ID',
    `buyer_contact_name` VARCHAR(50) DEFAULT NULL COMMENT '客户联系人',
    `buyer_contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '客户联系电话',
    `total_quantity` DECIMAL(18,4) DEFAULT NULL COMMENT '合同总数量',
    `total_weight` DECIMAL(18,4) DEFAULT NULL COMMENT '合同总重量(吨)',
    `total_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '合同总金额',
    `picked_weight` DECIMAL(18,4) DEFAULT 0 COMMENT '已提货重量',
    `picked_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '已提货金额',
    `settled_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '已结算金额',
    `paid_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '已付款金额',
    `payment_terms` VARCHAR(200) DEFAULT NULL COMMENT '付款条件',
    `delivery_deadline` DATE DEFAULT NULL COMMENT '交货/提货期限',
    `warehouse_id` BIGINT DEFAULT NULL COMMENT '提货仓库ID',
    `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '提货仓库名称',
    `template_id` BIGINT DEFAULT NULL COMMENT '合同模板ID',
    `template_version` INT DEFAULT NULL COMMENT '模板版本',
    `platform_terms_version` VARCHAR(10) DEFAULT NULL COMMENT '平台条款版本',
    `custom_clauses` TEXT DEFAULT NULL COMMENT '自定义条款',
    `sign_required` TINYINT DEFAULT 0 COMMENT '是否要求签约: 0-不要求 1-要求',
    `sign_status` TINYINT DEFAULT 0 COMMENT '签约状态: 0-未签 1-客户已签 2-双方已签 3-拒签',
    `signed_pdf_url` VARCHAR(500) DEFAULT NULL COMMENT '签章后合同PDF',
    `sign_evidence_no` VARCHAR(100) DEFAULT NULL COMMENT '签章存证编号',
    `pickup_mode` TINYINT DEFAULT 1 COMMENT '提货模式: 1-静默模式 2-主动模式',
    `allow_no_code_pickup` TINYINT DEFAULT 1 COMMENT '是否允许无码提货: 1-允许 0-不允许',
    `status` TINYINT DEFAULT 0 COMMENT '合同状态',
    `erp_sync_at` DATETIME DEFAULT NULL COMMENT 'ERP同步时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contract_no` (`contract_no`),
    KEY `idx_contract_seller_id` (`seller_id`),
    KEY `idx_contract_buyer_id` (`buyer_id`),
    KEY `idx_contract_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售合同';

-- ----------------------------
-- 2. contract_item - 合同明细
-- ----------------------------
DROP TABLE IF EXISTS `contract_item`;
CREATE TABLE `contract_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `contract_id` BIGINT NOT NULL COMMENT '合同ID',
    `line_no` INT DEFAULT NULL COMMENT '行号',
    `product_name` VARCHAR(200) DEFAULT NULL COMMENT '品名',
    `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格',
    `material` VARCHAR(50) DEFAULT NULL COMMENT '材质',
    `origin` VARCHAR(100) DEFAULT NULL COMMENT '产地',
    `heat_no` VARCHAR(50) DEFAULT NULL COMMENT '炉号',
    `batch_no` VARCHAR(50) DEFAULT NULL COMMENT '批号',
    `warehouse_location` VARCHAR(100) DEFAULT NULL COMMENT '库位',
    `quantity` DECIMAL(18,4) DEFAULT NULL COMMENT '数量',
    `weight` DECIMAL(18,4) DEFAULT NULL COMMENT '重量',
    `unit_price` DECIMAL(18,2) DEFAULT NULL COMMENT '单价',
    `amount` DECIMAL(18,2) DEFAULT NULL COMMENT '金额',
    `picked_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '已提数量',
    `picked_weight` DECIMAL(18,4) DEFAULT 0 COMMENT '已提重量',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_contract_item_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同明细';

-- ----------------------------
-- 3. contract_template - 合同模板
-- ----------------------------
DROP TABLE IF EXISTS `contract_template`;
CREATE TABLE `contract_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `enterprise_id` BIGINT DEFAULT NULL COMMENT '企业ID',
    `template_name` VARCHAR(100) DEFAULT NULL COMMENT '模板名称',
    `contract_type` TINYINT DEFAULT NULL COMMENT '合同类型',
    `template_source` TINYINT DEFAULT NULL COMMENT '模板来源',
    `template_content` LONGTEXT DEFAULT NULL COMMENT '模板内容',
    `template_file_url` VARCHAR(500) DEFAULT NULL COMMENT '模板文件URL',
    `field_mapping` JSON DEFAULT NULL COMMENT '字段映射',
    `has_origin_field` TINYINT DEFAULT 1 COMMENT '是否有产地字段',
    `platform_terms_priority` TINYINT DEFAULT 2 COMMENT '平台条款优先级',
    `applicable_buyers` JSON DEFAULT NULL COMMENT '适用买方',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认',
    `version` INT DEFAULT 1 COMMENT '版本',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同模板';

-- ----------------------------
-- 4. pickup_order - 提货单
-- ----------------------------
DROP TABLE IF EXISTS `pickup_order`;
CREATE TABLE `pickup_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pickup_no` VARCHAR(32) NOT NULL COMMENT '提货单号',
    `contract_id` BIGINT DEFAULT NULL COMMENT '合同ID',
    `contract_no` VARCHAR(50) DEFAULT NULL COMMENT '合同号',
    `buyer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
    `pickup_code` VARCHAR(10) DEFAULT NULL COMMENT '提货码',
    `pickup_code_qr` VARCHAR(500) DEFAULT NULL COMMENT '提货码二维码URL',
    `pickup_code_status` TINYINT DEFAULT 0 COMMENT '提货码状态: 0-未使用 1-已验证 2-已提货 3-已过期',
    `pickup_code_expire_at` DATETIME DEFAULT NULL COMMENT '提货码过期时间',
    `dispatch_mode` TINYINT DEFAULT NULL COMMENT '派车模式: 1-客户派车 2-销售派车 3-承运公司',
    `dispatch_status` TINYINT DEFAULT 0 COMMENT '派车状态: 0-待派车 1-待确认 2-已确认 3-已拒绝',
    `customer_confirmed` TINYINT DEFAULT 0 COMMENT '客户是否确认',
    `customer_confirmed_at` DATETIME DEFAULT NULL COMMENT '客户确认时间',
    `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
    `driver_name` VARCHAR(50) DEFAULT NULL COMMENT '驾驶员姓名',
    `driver_phone` VARCHAR(20) DEFAULT NULL COMMENT '驾驶员电话',
    `carrier_id` BIGINT DEFAULT NULL COMMENT '承运公司ID',
    `carrier_name` VARCHAR(100) DEFAULT NULL COMMENT '承运公司名称',
    `driver_assigned` TINYINT DEFAULT 0 COMMENT '驾驶员是否已分配',
    `driver_assigned_at` DATETIME DEFAULT NULL COMMENT '驾驶员分配时间',
    `warehouse_id` BIGINT DEFAULT NULL COMMENT '提货仓库ID',
    `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '仓库名称',
    `expected_arrival_at` DATETIME DEFAULT NULL COMMENT '预计到达时间',
    `actual_arrival_at` DATETIME DEFAULT NULL COMMENT '实际到达时间',
    `arrival_gps_lat` DECIMAL(10,7) DEFAULT NULL COMMENT '到达GPS纬度',
    `arrival_gps_lng` DECIMAL(10,7) DEFAULT NULL COMMENT '到达GPS经度',
    `delivery_mode` TINYINT DEFAULT NULL COMMENT '发货模式: 1-自有WMS 2-H5助手 3-第三方WMS 4-驾驶员确认 5-事后补录',
    `delivery_data_source` VARCHAR(20) DEFAULT NULL COMMENT '数据来源',
    `total_lifts` INT DEFAULT 0 COMMENT '总吊数',
    `total_pieces` INT DEFAULT 0 COMMENT '总件数',
    `total_weight` DECIMAL(18,4) DEFAULT 0 COMMENT '已发总重量',
    `total_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '结算总金额',
    `delivery_status` TINYINT DEFAULT 0 COMMENT '发货状态: 0-未开始 1-发货中 2-已完成',
    `settlement_status` TINYINT DEFAULT 0 COMMENT '结算状态: 0-未结算 1-已结算',
    `evidence_package_id` BIGINT DEFAULT NULL COMMENT '证据包ID',
    `status` TINYINT DEFAULT 1 COMMENT '整体状态: 1-待派车 2-待提货 3-提货中 4-发货中 5-已完成 6-已取消',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pickup_no` (`pickup_no`),
    KEY `idx_pickup_order_contract_id` (`contract_id`),
    KEY `idx_pickup_order_buyer_id` (`buyer_id`),
    KEY `idx_pickup_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提货单';

-- ----------------------------
-- 5. lift_record - 发货吊装记录
-- ----------------------------
DROP TABLE IF EXISTS `lift_record`;
CREATE TABLE `lift_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pickup_order_id` BIGINT NOT NULL COMMENT '提货单ID',
    `lift_seq` INT DEFAULT NULL COMMENT '吊序号',
    `product_name` VARCHAR(200) DEFAULT NULL COMMENT '品名',
    `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格',
    `material` VARCHAR(50) DEFAULT NULL COMMENT '材质',
    `heat_no` VARCHAR(50) DEFAULT NULL COMMENT '炉号',
    `batch_no` VARCHAR(50) DEFAULT NULL COMMENT '批号',
    `pieces` INT DEFAULT NULL COMMENT '件数',
    `theoretical_weight` DECIMAL(18,4) DEFAULT NULL COMMENT '理论重量',
    `actual_weight` DECIMAL(18,4) DEFAULT NULL COMMENT '过磅重量',
    `operator_id` VARCHAR(50) DEFAULT NULL COMMENT '操作员工号',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作员姓名',
    `wms_record_id` VARCHAR(50) DEFAULT NULL COMMENT 'WMS记录ID',
    `data_source` TINYINT DEFAULT NULL COMMENT '数据来源: 1-自有WMS 2-H5助手 3-第三方WMS 4-驾驶员确认 5-事后补录',
    `photo_url` VARCHAR(500) DEFAULT NULL COMMENT '本吊拍照URL',
    `uploaded_at` DATETIME DEFAULT NULL COMMENT '上传时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_lift_record_pickup_order_id` (`pickup_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发货吊装记录';

-- ----------------------------
-- 6. delivery_confirm - 发货确认
-- ----------------------------
DROP TABLE IF EXISTS `delivery_confirm`;
CREATE TABLE `delivery_confirm` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pickup_order_id` BIGINT NOT NULL COMMENT '提货单ID',
    `operator_id` VARCHAR(50) DEFAULT NULL COMMENT '操作员工号',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作员姓名',
    `signature_url` VARCHAR(500) DEFAULT NULL COMMENT '电子签名图片URL',
    `total_lifts` INT DEFAULT NULL COMMENT '总吊数',
    `total_pieces` INT DEFAULT NULL COMMENT '总件数',
    `total_weight` DECIMAL(18,4) DEFAULT NULL COMMENT '总重量',
    `confirmed_at` DATETIME DEFAULT NULL COMMENT '确认时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_delivery_confirm_pickup_order_id` (`pickup_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发货确认';

-- ----------------------------
-- 7. delivery_photo - 发货照片
-- ----------------------------
DROP TABLE IF EXISTS `delivery_photo`;
CREATE TABLE `delivery_photo` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pickup_order_id` BIGINT NOT NULL COMMENT '提货单ID',
    `photo_type` TINYINT DEFAULT NULL COMMENT '照片类型: 1-装车 2-货物 3-车牌 4-磅单 5-其他',
    `photo_url` VARCHAR(500) DEFAULT NULL COMMENT '照片URL',
    `gps_lat` DECIMAL(10,7) DEFAULT NULL COMMENT 'GPS纬度',
    `gps_lng` DECIMAL(10,7) DEFAULT NULL COMMENT 'GPS经度',
    `taken_at` DATETIME DEFAULT NULL COMMENT '拍摄时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_delivery_photo_pickup_order_id` (`pickup_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发货照片';

-- ----------------------------
-- 8. settlement_order - 结算单
-- ----------------------------
DROP TABLE IF EXISTS `settlement_order`;
CREATE TABLE `settlement_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `settlement_no` VARCHAR(32) NOT NULL COMMENT '结算单号',
    `pickup_order_id` BIGINT DEFAULT NULL COMMENT '提货单ID',
    `contract_id` BIGINT DEFAULT NULL COMMENT '合同ID',
    `contract_no` VARCHAR(50) DEFAULT NULL COMMENT '合同号',
    `buyer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
    `total_weight` DECIMAL(18,4) DEFAULT NULL COMMENT '结算重量',
    `total_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '结算金额',
    `tax_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '税额',
    `total_with_tax` DECIMAL(18,2) DEFAULT NULL COMMENT '价税合计',
    `deducted_prepayment` DECIMAL(18,2) DEFAULT 0 COMMENT '已扣预付款',
    `receivable_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '应收金额',
    `settlement_detail` JSON DEFAULT NULL COMMENT '结算明细',
    `pdf_url` VARCHAR(500) DEFAULT NULL COMMENT '结算单PDF',
    `customer_viewed` TINYINT DEFAULT 0 COMMENT '客户是否已查看',
    `customer_viewed_at` DATETIME DEFAULT NULL COMMENT '客户查看时间',
    `synced_to_recon` TINYINT DEFAULT 0 COMMENT '是否已同步至对账系统',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-已生成 2-已通知 3-客户已查看',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settlement_no` (`settlement_no`),
    KEY `idx_settlement_contract_id` (`contract_id`),
    KEY `idx_settlement_buyer_id` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算单';

-- ----------------------------
-- 9. evidence_package - 证据包
-- ----------------------------
DROP TABLE IF EXISTS `evidence_package`;
CREATE TABLE `evidence_package` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pickup_order_id` BIGINT DEFAULT NULL COMMENT '提货单ID',
    `contract_id` BIGINT DEFAULT NULL COMMENT '合同ID',
    `package_hash` VARCHAR(64) DEFAULT NULL COMMENT '证据包SHA-256哈希',
    `contract_pdf_url` VARCHAR(500) DEFAULT NULL COMMENT '签章合同PDF',
    `pickup_order_pdf_url` VARCHAR(500) DEFAULT NULL COMMENT '提货单PDF',
    `delivery_data_url` VARCHAR(500) DEFAULT NULL COMMENT '发货数据URL',
    `photos_urls` JSON DEFAULT NULL COMMENT '现场照片URLs',
    `signature_url` VARCHAR(500) DEFAULT NULL COMMENT '签字图片',
    `settlement_pdf_url` VARCHAR(500) DEFAULT NULL COMMENT '结算单PDF',
    `blockchain_hash` VARCHAR(200) DEFAULT NULL COMMENT '区块链存证哈希',
    `evidence_status` TINYINT DEFAULT 0 COMMENT '状态: 0-生成中 1-已归档 2-已存证',
    `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_evidence_package_pickup_order_id` (`pickup_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证据包';

-- ----------------------------
-- 10. progress_event - 进度事件
-- ----------------------------
DROP TABLE IF EXISTS `progress_event`;
CREATE TABLE `progress_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pickup_order_id` BIGINT DEFAULT NULL COMMENT '提货单ID',
    `contract_id` BIGINT DEFAULT NULL COMMENT '合同ID',
    `event_type` VARCHAR(30) DEFAULT NULL COMMENT '事件类型',
    `event_title` VARCHAR(200) DEFAULT NULL COMMENT '事件标题',
    `event_detail` JSON DEFAULT NULL COMMENT '事件详情',
    `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
    `notify_targets` VARCHAR(200) DEFAULT NULL COMMENT '通知对象',
    `notified` TINYINT DEFAULT 0 COMMENT '是否已通知',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_progress_event_pickup_order_id` (`pickup_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='进度事件';

-- ----------------------------
-- 11. authorized_pickup_person - 授权提货人
-- ----------------------------
DROP TABLE IF EXISTS `authorized_pickup_person`;
CREATE TABLE `authorized_pickup_person` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `buyer_id` BIGINT NOT NULL COMMENT '客户(买方)企业ID',
    `contract_id` BIGINT DEFAULT NULL COMMENT '关联合同ID(可空=适用所有合同)',
    `person_name` VARCHAR(50) DEFAULT NULL COMMENT '提货人姓名',
    `id_card_no` VARCHAR(100) DEFAULT NULL COMMENT '身份证号(加密)',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
    `max_pickup_weight` DECIMAL(18,4) DEFAULT NULL COMMENT '最大单次提货重量',
    `registered_by` TINYINT DEFAULT NULL COMMENT '登记方式: 1-客户自行 2-销售代录 3-仓库现场采集',
    `buyer_confirmed` TINYINT DEFAULT 0 COMMENT '客户是否确认: 0-待确认 1-已确认',
    `buyer_confirmed_at` DATETIME DEFAULT NULL COMMENT '客户确认时间',
    `valid_from` DATE DEFAULT NULL COMMENT '有效期开始',
    `valid_until` DATE DEFAULT NULL COMMENT '有效期截止',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-有效 0-已停用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_authorized_pickup_buyer_id` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='授权提货人';

-- ----------------------------
-- 12. pickup_verification - 提货确权记录
-- ----------------------------
DROP TABLE IF EXISTS `pickup_verification`;
CREATE TABLE `pickup_verification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pickup_order_id` BIGINT DEFAULT NULL COMMENT '提货单ID',
    `contract_id` BIGINT DEFAULT NULL COMMENT '合同ID',
    `buyer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
    `driver_name` VARCHAR(50) DEFAULT NULL COMMENT '司机姓名',
    `driver_phone` VARCHAR(20) DEFAULT NULL COMMENT '司机手机号',
    `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
    `is_pre_registered` TINYINT DEFAULT 0 COMMENT '司机是否预登记: 0-否 1-是',
    `authorized_person_id` BIGINT DEFAULT NULL COMMENT '匹配到的授权提货人ID',
    `sms_sent` TINYINT DEFAULT 0 COMMENT '确权短信是否已发',
    `sms_sent_at` DATETIME DEFAULT NULL COMMENT '短信发送时间',
    `sms_delivered` TINYINT DEFAULT 0 COMMENT '短信是否送达',
    `sms_delivery_receipt` VARCHAR(200) DEFAULT NULL COMMENT '运营商送达回执',
    `sms_link_clicked` TINYINT DEFAULT 0 COMMENT '客户是否点击了短信链接',
    `sms_link_clicked_at` DATETIME DEFAULT NULL COMMENT '点击时间',
    `phone_call_made` TINYINT DEFAULT 0 COMMENT '是否触发了电话确认',
    `phone_call_result` TINYINT DEFAULT 0 COMMENT '电话结果: 0-未接 1-确认 2-拒绝',
    `phone_call_recording_url` VARCHAR(500) DEFAULT NULL COMMENT '通话录音URL',
    `phone_call_at` DATETIME DEFAULT NULL COMMENT '通话时间',
    `driver_id_photo_url` VARCHAR(500) DEFAULT NULL COMMENT '司机身份证照片',
    `driver_face_photo_url` VARCHAR(500) DEFAULT NULL COMMENT '司机人脸照片',
    `driver_license_photo_url` VARCHAR(500) DEFAULT NULL COMMENT '驾驶证照片',
    `verification_level` TINYINT DEFAULT 2 COMMENT '确权强度: 1-轻度 2-标准 3-加强 4-严格 5-最严格',
    `verification_result` TINYINT DEFAULT 0 COMMENT '确权结果: 1-通过 2-拒绝 3-待人工处理',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_pickup_verification_pickup_order_id` (`pickup_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提货确权记录';

-- ----------------------------
-- 13. trading_habit_record - 交易习惯记录
-- ----------------------------
DROP TABLE IF EXISTS `trading_habit_record`;
CREATE TABLE `trading_habit_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `buyer_id` BIGINT NOT NULL COMMENT '客户ID',
    `driver_name` VARCHAR(50) DEFAULT NULL COMMENT '司机姓名',
    `driver_phone` VARCHAR(20) NOT NULL COMMENT '司机手机号',
    `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
    `total_pickups` INT DEFAULT 0 COMMENT '总提货次数',
    `total_weight` DECIMAL(18,4) DEFAULT 0 COMMENT '累计提货重量',
    `total_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '累计提货金额',
    `paid_pickups` INT DEFAULT 0 COMMENT '客户已付款的提货次数',
    `denied_pickups` INT DEFAULT 0 COMMENT '客户否认的提货次数',
    `last_pickup_at` DATETIME DEFAULT NULL COMMENT '最近提货时间',
    `first_pickup_at` DATETIME DEFAULT NULL COMMENT '首次提货时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trading_habit_buyer_driver` (`buyer_id`, `driver_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易习惯记录';

-- ----------------------------
-- 14. warehouse - 仓库
-- ----------------------------
DROP TABLE IF EXISTS `warehouse`;
CREATE TABLE `warehouse` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `enterprise_id` BIGINT DEFAULT NULL COMMENT '企业ID',
    `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '仓库名称',
    `warehouse_code` VARCHAR(50) DEFAULT NULL COMMENT '仓库编码',
    `address` VARCHAR(300) DEFAULT NULL COMMENT '地址',
    `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `gps_lat` DECIMAL(10,7) DEFAULT NULL COMMENT 'GPS纬度',
    `gps_lng` DECIMAL(10,7) DEFAULT NULL COMMENT 'GPS经度',
    `default_delivery_mode` TINYINT DEFAULT 2 COMMENT '默认发货模式',
    `backup_delivery_mode` TINYINT DEFAULT NULL COMMENT '备用发货模式',
    `has_wms` TINYINT DEFAULT 0 COMMENT '是否有WMS: 0-否 1-是',
    `wms_type` TINYINT DEFAULT NULL COMMENT 'WMS类型',
    `third_party_wms_config` JSON DEFAULT NULL COMMENT '第三方WMS配置',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库';

-- ----------------------------
-- 15. carrier - 承运公司
-- ----------------------------
DROP TABLE IF EXISTS `carrier`;
CREATE TABLE `carrier` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `enterprise_id` BIGINT DEFAULT NULL COMMENT '企业ID',
    `carrier_name` VARCHAR(100) DEFAULT NULL COMMENT '承运公司名称',
    `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='承运公司';

-- ----------------------------
-- 16. supplement_record - 事后补录记录
-- ----------------------------
DROP TABLE IF EXISTS `supplement_record`;
CREATE TABLE `supplement_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pickup_order_id` BIGINT DEFAULT NULL COMMENT '提货单ID',
    `submitted_by` BIGINT DEFAULT NULL COMMENT '提交人ID',
    `submit_reason` VARCHAR(500) DEFAULT NULL COMMENT '补录原因',
    `delivery_detail` JSON DEFAULT NULL COMMENT '发货明细',
    `document_urls` JSON DEFAULT NULL COMMENT '单据URLs',
    `approval_status` TINYINT DEFAULT 0 COMMENT '审批状态: 0-待审批 1-已通过 2-已拒绝',
    `approved_by` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `approved_at` DATETIME DEFAULT NULL COMMENT '审批时间',
    `approval_comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_supplement_record_pickup_order_id` (`pickup_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事后补录记录';

-- ----------------------------
-- 17. notification_log - 通知日志
-- ----------------------------
DROP TABLE IF EXISTS `notification_log`;
CREATE TABLE `notification_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `pickup_order_id` BIGINT DEFAULT NULL COMMENT '提货单ID',
    `contract_id` BIGINT DEFAULT NULL COMMENT '合同ID',
    `buyer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
    `channel` TINYINT DEFAULT NULL COMMENT '渠道: 1-sms 2-wechat 3-dingtalk 4-websocket',
    `recipient_phone` VARCHAR(20) DEFAULT NULL COMMENT '接收人手机号',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
    `sent_at` DATETIME DEFAULT NULL COMMENT '发送时间',
    `delivered` TINYINT DEFAULT 0 COMMENT '是否送达',
    `delivery_receipt` VARCHAR(200) DEFAULT NULL COMMENT '送达回执',
    `link_clicked` TINYINT DEFAULT 0 COMMENT '链接是否被点击',
    `link_clicked_at` DATETIME DEFAULT NULL COMMENT '链接点击时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_notification_log_pickup_order_id` (`pickup_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知日志';

-- ----------------------------
-- 18. confirm_timeout_config - 确认时效配置
-- ----------------------------
DROP TABLE IF EXISTS `confirm_timeout_config`;
CREATE TABLE `confirm_timeout_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `enterprise_id` BIGINT DEFAULT NULL COMMENT '企业ID',
    `buyer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
    `scenario` VARCHAR(30) DEFAULT NULL COMMENT '场景',
    `timeout_value` INT DEFAULT NULL COMMENT '超时值',
    `timeout_unit` VARCHAR(10) DEFAULT NULL COMMENT '超时单位',
    `reminder_before_hours` INT DEFAULT 6 COMMENT '提前提醒小时数',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='确认时效配置';

SET FOREIGN_KEY_CHECKS = 1;
