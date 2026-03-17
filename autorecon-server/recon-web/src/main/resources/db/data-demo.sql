-- AutoRecon Demo Data - Steel Industry
-- Demo login: admin/admin123, seller1/123456, buyer1/123456, buyer2/123456
-- BCrypt hashes: admin123, 123456
-- H2: Enable explicit ID insert for tables with AUTO_INCREMENT

SET REFERENTIAL_INTEGRITY FALSE;

-- Enterprises: 1 seller (XX钢铁有限公司), 2 buyers (YY建设集团, ZZ贸易公司)
INSERT INTO enterprise (id, company_name, unified_credit_code, contact_name, contact_phone, enterprise_type, status, deleted) VALUES
(1, 'XX钢铁有限公司', '91310000MA1FL2XX01', '张经理', '13800138001', 1, 1, 0),
(2, 'YY建设集团', '91320100MA1XX3XX03', '李主任', '13800138003', 2, 1, 0),
(3, 'ZZ贸易公司', '91310115MA1XX4XX04', '王经理', '13800138004', 2, 1, 0);

-- Users: admin (platform), seller1 (卖方管理员-张三), buyer1 (YY建设买方-李四), buyer2 (ZZ贸易买方-王五)
-- BCrypt: admin123=$2b$12$/6jo9hfBIOoNXI/W3XS6pOGkTbEpDTFdJK36n3rXvM/Rd3NHkhNRC, 123456=$2b$12$UEi4x6E8gLg2G1ab.UlE5uccPInfgfgrGqDXAqxDs/n6vBrSrbqEe
-- role_type: 1-seller_admin 2-seller_operator 3-seller_finance 4-buyer_admin 5-buyer_operator 6-platform_admin
INSERT INTO sys_user (id, enterprise_id, username, password, real_name, role_type, status, deleted) VALUES
(1, NULL, 'admin', '$2b$12$/6jo9hfBIOoNXI/W3XS6pOGkTbEpDTFdJK36n3rXvM/Rd3NHkhNRC', '平台管理员', 6, 1, 0),
(2, 1, 'seller1', '$2b$12$UEi4x6E8gLg2G1ab.UlE5uccPInfgfgrGqDXAqxDs/n6vBrSrbqEe', '张三', 1, 1, 0),
(3, 2, 'buyer1', '$2b$12$UEi4x6E8gLg2G1ab.UlE5uccPInfgfgrGqDXAqxDs/n6vBrSrbqEe', '李四', 4, 1, 0),
(4, 3, 'buyer2', '$2b$12$UEi4x6E8gLg2G1ab.UlE5uccPInfgfgrGqDXAqxDs/n6vBrSrbqEe', '王五', 4, 1, 0);

-- Templates (seller 1): 标准对账单, 简易对账单, 框架协议对账单
INSERT INTO recon_template (id, enterprise_id, template_name, template_type, is_default, status, deleted) VALUES
(1, 1, '标准对账单', 1, 1, 1, 0),
(2, 1, '简易对账单', 2, 0, 1, 0),
(3, 1, '框架协议对账单', 3, 0, 1, 0);

-- Bills: 8 bills across all statuses
-- DZ20260301001: PENDING (YY建设, 45万, 2月份对账)
-- DZ20260301002: SIGNED (ZZ贸易, 82万, 已签章)
-- DZ20260301003: DISPUTED (YY建设, 215万, 有3条异议)
-- DZ20260315001: CREATED (ZZ贸易, 56万, 刚创建)
-- DZ20260315002: TO_SIGN (YY建设, 93万, 待签章)
-- DZ20260301004: COLLECTING (ZZ贸易, 120万, 催收中)
-- DZ20260201001: COMPLETED (YY建设, 68万, 已完成)
-- DZ20260201002: VOID (ZZ贸易, 15万, 已作废)
INSERT INTO recon_bill (id, bill_no, seller_id, buyer_id, template_id, period_start, period_end, total_amount, total_quantity, total_weight, currency, status, seller_sign_status, buyer_sign_status, source_type, created_by, deleted) VALUES
(1, 'DZ20260301001', 1, 2, 1, '2026-02-01', '2026-02-28', 450000.00, 180.0000, 178.5000, 'CNY', 'PENDING', 0, 0, 1, 2, 0),
(2, 'DZ20260301002', 1, 3, 1, '2026-02-01', '2026-02-28', 820000.00, 328.0000, 326.2000, 'CNY', 'SIGNED', 2, 2, 1, 2, 0),
(3, 'DZ20260301003', 1, 2, 1, '2026-02-01', '2026-02-28', 2150000.00, 860.0000, 855.0000, 'CNY', 'DISPUTED', 0, 0, 1, 2, 0),
(4, 'DZ20260315001', 1, 3, 1, '2026-03-01', '2026-03-31', 560000.00, 224.0000, 222.5000, 'CNY', 'CREATED', 0, 0, 1, 2, 0),
(5, 'DZ20260315002', 1, 2, 1, '2026-03-01', '2026-03-31', 930000.00, 372.0000, 369.8000, 'CNY', 'TO_SIGN', 1, 0, 1, 2, 0),
(6, 'DZ20260301004', 1, 3, 1, '2026-02-01', '2026-02-28', 1200000.00, 480.0000, 477.0000, 'CNY', 'COLLECTING', 2, 2, 1, 2, 0),
(7, 'DZ20260201001', 1, 2, 1, '2026-01-01', '2026-01-31', 680000.00, 272.0000, 270.5000, 'CNY', 'COMPLETED', 2, 2, 1, 2, 0),
(8, 'DZ20260201002', 1, 3, 1, '2026-01-01', '2026-01-31', 150000.00, 60.0000, 59.5000, 'CNY', 'VOID', 0, 0, 1, 2, 0);

-- Bill items: 30+ items with real steel products (螺纹钢Φ20 HRB400 日照钢铁, 盘螺Φ10 Q235 沙钢, etc.)
INSERT INTO recon_bill_item (id, bill_id, line_no, contract_no, product_name, spec, material, origin, quantity, weight, unit_price, amount, total_amount, match_status, deleted) VALUES
(1, 1, 1, 'HT2026-001', '螺纹钢', 'Φ20 HRB400', 'HRB400', '日照钢铁', 50.0000, 49.5000, 3850.00, 190575.00, 190575.00, 1, 0),
(2, 1, 2, 'HT2026-001', '盘螺', 'Φ10 Q235', 'Q235', '沙钢', 45.0000, 44.8000, 3920.00, 175616.00, 175616.00, 1, 0),
(3, 1, 3, 'HT2026-001', '线材', 'Φ6 Q195', 'Q195', '永钢', 42.0000, 41.7000, 3650.00, 152205.00, 152205.00, 1, 0),
(4, 2, 1, 'HT2026-002', '螺纹钢', 'Φ25 HRB400', 'HRB400', '日照钢铁', 80.0000, 79.2000, 3820.00, 302544.00, 302544.00, 1, 0),
(5, 2, 2, 'HT2026-002', '盘螺', 'Φ8 HRB400', 'HRB400', '沙钢', 100.0000, 99.5000, 3880.00, 386060.00, 386060.00, 1, 0),
(6, 2, 3, 'HT2026-002', '线材', 'Φ6.5 Q235', 'Q235', '永钢', 74.0000, 73.5000, 3640.00, 267540.00, 267540.00, 1, 0),
(7, 3, 1, 'HT2026-003', '螺纹钢', 'Φ20 HRB400', 'HRB400', '日照钢铁', 300.0000, 298.0000, 3860.00, 1150280.00, 1150280.00, 2, 0),
(8, 3, 2, 'HT2026-003', '盘螺', 'Φ10 HRB400', 'HRB400', '沙钢', 280.0000, 278.5000, 3910.00, 1088935.00, 1088935.00, 2, 0),
(9, 3, 3, 'HT2026-003', '线材', 'Φ8 Q235', 'Q235', '永钢', 280.0000, 278.5000, 3680.00, 1024880.00, 1024880.00, 0, 0),
(10, 4, 1, 'HT2026-004', '螺纹钢', 'Φ22 HRB400', 'HRB400', '首钢', 100.0000, 99.5000, 3840.00, 382080.00, 382080.00, 1, 0),
(11, 4, 2, 'HT2026-004', '盘螺', 'Φ8 HRB400', 'HRB400', '沙钢', 65.0000, 64.5000, 3870.00, 249615.00, 249615.00, 1, 0),
(12, 4, 3, 'HT2026-004', '线材', 'Φ6.5 Q235', 'Q235', '永钢', 59.0000, 58.5000, 3640.00, 212940.00, 212940.00, 1, 0),
(13, 5, 1, 'HT2026-005', '螺纹钢', 'Φ16 HRB400', 'HRB400', '日照钢铁', 120.0000, 119.0000, 3890.00, 462910.00, 462910.00, 1, 0),
(14, 5, 2, 'HT2026-005', '盘螺', 'Φ10 Q235', 'Q235', '沙钢', 130.0000, 129.0000, 3920.00, 505680.00, 505680.00, 1, 0),
(15, 5, 3, 'HT2026-005', '线材', 'Φ6 Q195', 'Q195', '永钢', 122.0000, 121.0000, 3650.00, 441650.00, 441650.00, 1, 0),
(16, 6, 1, 'HT2026-006', '螺纹钢', 'Φ20 HRB400', 'HRB400', '日照钢铁', 200.0000, 198.5000, 3850.00, 764225.00, 764225.00, 1, 0),
(17, 6, 2, 'HT2026-006', '盘螺', 'Φ10 HRB400', 'HRB400', '沙钢', 150.0000, 149.0000, 3910.00, 582590.00, 582590.00, 1, 0),
(18, 6, 3, 'HT2026-006', '线材', 'Φ8 Q235', 'Q235', '永钢', 130.0000, 129.5000, 3680.00, 476560.00, 476560.00, 1, 0),
(19, 7, 1, 'HT2026-007', '螺纹钢', 'Φ20 HRB400', 'HRB400', '日照钢铁', 100.0000, 99.5000, 3850.00, 383075.00, 383075.00, 1, 0),
(20, 7, 2, 'HT2026-007', '盘螺', 'Φ8 HRB400', 'HRB400', '沙钢', 95.0000, 94.5000, 3880.00, 366660.00, 366660.00, 1, 0),
(21, 7, 3, 'HT2026-007', '线材', 'Φ6.5 Q235', 'Q235', '永钢', 77.0000, 76.5000, 3640.00, 278460.00, 278460.00, 1, 0),
(22, 8, 1, 'HT2026-008', '螺纹钢', 'Φ25 HRB400', 'HRB400', '日照钢铁', 30.0000, 29.8000, 3820.00, 113836.00, 113836.00, 1, 0),
(23, 8, 2, 'HT2026-008', '盘螺', 'Φ10 Q235', 'Q235', '沙钢', 30.0000, 29.7000, 3920.00, 116424.00, 116424.00, 1, 0);

-- Payments (5): bank transfer, acceptance bill
INSERT INTO payment (id, payment_no, payer_id, payee_id, payment_date, payment_amount, payment_method, allocated_amount, status, deleted) VALUES
(1, 'FK202603001', 2, 1, '2026-03-10', 200000.00, 1, 200000.00, 1, 0),
(2, 'FK202603002', 3, 1, '2026-03-12', 500000.00, 1, 500000.00, 1, 0),
(3, 'FK202603003', 2, 1, '2026-03-15', 300000.00, 2, 300000.00, 1, 0),
(4, 'FK202603004', 3, 1, '2026-03-18', 400000.00, 2, 400000.00, 1, 0),
(5, 'FK202603005', 2, 1, '2026-03-20', 250000.00, 1, 250000.00, 1, 0);

-- Disputes (5): weight diff, amount diff, spec mismatch
INSERT INTO dispute (id, bill_id, bill_item_id, dispute_type, description, raised_by, raised_by_side, status, deleted) VALUES
(1, 3, 7, 2, '重量差异：实际过磅298吨，系统记录300吨，差异2吨', 3, 2, 1, 0),
(2, 3, 8, 3, '单价异议：盘螺Φ10 HRB400 单价应为3890元/吨', 3, 2, 2, 0),
(3, 3, 9, 1, '规格不符：线材Φ8 实际为Φ8.5', 3, 2, 1, 0),
(4, 3, 7, 2, '金额差异：发票金额与对账单不符', 3, 2, 0, 0),
(5, 3, 8, 3, '材质异议：HRB400 需提供质保书', 3, 2, 1, 0);

-- Credit scores (3): A/B/C levels
INSERT INTO credit_score (id, enterprise_id, seller_id, credit_score, score_level, avg_payment_days, overdue_rate, total_trade_amount, deleted) VALUES
(1, 2, 1, 92.50, 'A', 28.50, 2.30, 8500000.00, 0),
(2, 3, 1, 88.00, 'B', 35.00, 5.20, 6200000.00, 0),
(3, 2, 1, 78.50, 'C', 45.00, 8.50, 2100000.00, 0);

-- Collection plans (2)
INSERT INTO collection_plan (id, bill_id, seller_id, buyer_id, receivable_amount, collected_amount, remaining_amount, due_date, strategy_level, status, current_stage, deleted) VALUES
(1, 3, 1, 2, 2150000.00, 0.00, 2150000.00, '2026-04-30', 'B', 1, 1, 0),
(2, 6, 1, 3, 1200000.00, 500000.00, 700000.00, '2026-04-15', 'A', 1, 2, 0);

-- Guest access token for demo: /api/v1/guest/view/DEMO_GUEST_TOKEN
INSERT INTO guest_access_token (id, token, bill_id, expire_at, phone_verified, opened_count, confirmed, deleted) VALUES
(1, 'DEMO_GUEST_TOKEN', 1, '2027-12-31 23:59:59', 1, 0, 0, 0);

SET REFERENTIAL_INTEGRITY TRUE;
