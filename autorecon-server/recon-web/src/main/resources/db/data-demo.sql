-- AutoRecon Demo Data - Steel Industry
-- Demo login: admin/admin123, seller1/123456, buyer1/123456
-- H2: Enable explicit ID insert for tables with AUTO_INCREMENT

-- Enterprises: 2 sellers (1,2), 3 buyers (3,4,5)
SET REFERENTIAL_INTEGRITY FALSE;
INSERT INTO enterprise (id, company_name, unified_credit_code, contact_name, contact_phone, enterprise_type, status, deleted) VALUES
(1, '华东钢铁贸易有限公司', '91310000MA1FL2XX01', '张经理', '13800138001', 1, 1, 0),
(2, '华北钢材贸易公司', '91110108MA01XX2X02', '李总', '13800138002', 1, 1, 0),
(3, '江苏建筑钢材有限公司', '91320100MA1XX3XX03', '王主任', '13800138003', 2, 1, 0),
(4, '上海钢铁加工厂', '91310115MA1XX4XX04', '陈厂长', '13800138004', 2, 1, 0),
(5, '杭州金属材料公司', '91330100MA1XX5XX05', '刘经理', '13800138005', 2, 1, 0);

-- Users: admin, seller1, seller2, buyer1, buyer2, buyer3
-- MD5: admin123=0192023a7bbd73250516f069df18b500, 123456=e10adc3949ba59abbe56e057f20f883e
-- role_type: 1-seller_admin 2-seller_operator 3-seller_finance 4-buyer_admin 5-buyer_operator 6-platform_admin
INSERT INTO sys_user (id, enterprise_id, username, password, real_name, role_type, status, deleted) VALUES
(1, 1, 'admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 6, 1, 0),
(2, 1, 'seller1', 'e10adc3949ba59abbe56e057f20f883e', '张销售', 1, 1, 0),
(3, 2, 'seller2', 'e10adc3949ba59abbe56e057f20f883e', '李销售', 1, 1, 0),
(4, 3, 'buyer1', 'e10adc3949ba59abbe56e057f20f883e', '王采购', 4, 1, 0),
(5, 4, 'buyer2', 'e10adc3949ba59abbe56e057f20f883e', '陈采购', 4, 1, 0),
(6, 5, 'buyer3', 'e10adc3949ba59abbe56e057f20f883e', '刘采购', 4, 1, 0);

-- Templates (seller 1 and 2)
INSERT INTO recon_template (id, enterprise_id, template_name, template_type, is_default, status, deleted) VALUES
(1, 1, '标准钢材对账模板', 1, 1, 1, 0),
(2, 2, '简易对账模板', 2, 1, 1, 0);

-- Bills: 5 bills - CREATED, PENDING, DISPUTED, TO_SIGN, SIGNED
INSERT INTO recon_bill (id, bill_no, seller_id, buyer_id, template_id, period_start, period_end, total_amount, total_quantity, total_weight, currency, status, seller_sign_status, buyer_sign_status, source_type, created_by, deleted) VALUES
(1, 'DZ202403001', 1, 3, 1, '2024-03-01', '2024-03-31', 1256800.00, 500.0000, 498.5000, 'CNY', 'CREATED', 0, 0, 1, 2, 0),
(2, 'DZ202403002', 1, 4, 1, '2024-03-01', '2024-03-31', 892500.00, 350.0000, 348.2000, 'CNY', 'PENDING', 0, 0, 1, 2, 0),
(3, 'DZ202403003', 1, 3, 1, '2024-03-01', '2024-03-31', 2156000.00, 860.0000, 855.0000, 'CNY', 'DISPUTED', 0, 0, 1, 2, 0),
(4, 'DZ202403004', 2, 5, 2, '2024-03-01', '2024-03-31', 1680000.00, 672.0000, 670.0000, 'CNY', 'TO_SIGN', 1, 0, 1, 3, 0),
(5, 'DZ202403005', 1, 3, 1, '2024-02-01', '2024-02-29', 986400.00, 392.0000, 390.5000, 'CNY', 'SIGNED', 2, 2, 1, 2, 0);

-- Bill items - steel products: 螺纹钢, 盘螺, 线材, HRB400, Q235, Φ20, Φ10
INSERT INTO recon_bill_item (id, bill_id, line_no, contract_no, product_name, spec, material, quantity, weight, unit_price, amount, total_amount, match_status, deleted) VALUES
(1, 1, 1, 'HT2024-001', '螺纹钢', 'Φ20 HRB400', 'HRB400', 100.0000, 99.5000, 3850.00, 383275.00, 383275.00, 1, 0),
(2, 1, 2, 'HT2024-001', '螺纹钢', 'Φ10 HRB400', 'HRB400', 80.0000, 79.2000, 3920.00, 310464.00, 310464.00, 1, 0),
(3, 1, 3, 'HT2024-002', '盘螺', 'Φ8 HRB400', 'HRB400', 120.0000, 119.8000, 3880.00, 464824.00, 464824.00, 1, 0),
(4, 2, 1, 'HT2024-003', '线材', 'Φ6.5 Q235', 'Q235', 150.0000, 149.5000, 3650.00, 545675.00, 545675.00, 1, 0),
(5, 2, 2, 'HT2024-003', '螺纹钢', 'Φ25 HRB400', 'HRB400', 100.0000, 99.2000, 3820.00, 378944.00, 378944.00, 0, 0),
(6, 3, 1, 'HT2024-004', '螺纹钢', 'Φ20 HRB400', 'HRB400', 300.0000, 298.0000, 3860.00, 1150280.00, 1150280.00, 2, 0),
(7, 3, 2, 'HT2024-004', '盘螺', 'Φ10 HRB400', 'HRB400', 280.0000, 278.5000, 3910.00, 1088935.00, 1088935.00, 2, 0),
(8, 3, 3, 'HT2024-005', '线材', 'Φ8 Q235', 'Q235', 280.0000, 278.5000, 3680.00, 1024880.00, 1024880.00, 0, 0),
(9, 4, 1, 'HT2024-006', '螺纹钢', 'Φ22 HRB400', 'HRB400', 250.0000, 248.5000, 3840.00, 954240.00, 954240.00, 1, 0),
(10, 4, 2, 'HT2024-006', '盘螺', 'Φ8 HRB400', 'HRB400', 200.0000, 199.5000, 3870.00, 772065.00, 772065.00, 1, 0),
(11, 4, 3, 'HT2024-007', '螺纹钢', 'Φ16 HRB400', 'HRB400', 222.0000, 221.0000, 3890.00, 859690.00, 859690.00, 1, 0),
(12, 5, 1, 'HT2024-008', '螺纹钢', 'Φ20 HRB400', 'HRB400', 150.0000, 149.0000, 3850.00, 573650.00, 573650.00, 1, 0),
(13, 5, 2, 'HT2024-008', '线材', 'Φ6.5 Q235', 'Q235', 120.0000, 119.5000, 3640.00, 434980.00, 434980.00, 1, 0),
(14, 5, 3, 'HT2024-009', '盘螺', 'Φ10 HRB400', 'HRB400', 122.0000, 121.0000, 3900.00, 471900.00, 471900.00, 1, 0);

-- Payments
INSERT INTO payment (id, payment_no, payer_id, payee_id, payment_date, payment_amount, allocated_amount, status, deleted) VALUES
(1, 'FK202403001', 3, 1, '2024-03-15', 500000.00, 500000.00, 1, 0),
(2, 'FK202403002', 4, 1, '2024-03-20', 300000.00, 300000.00, 1, 0),
(3, 'FK202403003', 5, 2, '2024-03-25', 800000.00, 800000.00, 1, 0);

-- Disputes
INSERT INTO dispute (id, bill_id, bill_item_id, dispute_type, description, raised_by, raised_by_side, status, deleted) VALUES
(1, 3, 6, 2, '重量差异：实际过磅298吨，系统记录300吨，差异2吨', 4, 2, 1, 0),
(2, 3, 7, 3, '单价异议：盘螺Φ10 HRB400 单价应为3890元/吨', 4, 2, 2, 0);

-- Credit scores (buyer enterprise, seller enterprise)
INSERT INTO credit_score (id, enterprise_id, seller_id, credit_score, score_level, avg_payment_days, overdue_rate, total_trade_amount, deleted) VALUES
(1, 3, 1, 92.50, 'A', 28.50, 2.30, 8500000.00, 0),
(2, 4, 1, 88.00, 'B', 35.00, 5.20, 3200000.00, 0),
(3, 5, 2, 95.00, 'A', 22.00, 0.50, 12000000.00, 0);

-- Collection plan
INSERT INTO collection_plan (id, bill_id, seller_id, buyer_id, receivable_amount, collected_amount, remaining_amount, due_date, strategy_level, status, current_stage, deleted) VALUES
(1, 3, 1, 3, 2156000.00, 0.00, 2156000.00, '2024-04-30', 'B', 1, 1, 0);
SET REFERENTIAL_INTEGRITY TRUE;
