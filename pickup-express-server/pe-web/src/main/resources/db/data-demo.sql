-- Pickup Express - Demo data (steel industry)
-- Same enterprises as AutoRecon: 1=XX钢铁(seller), 2=YY建设(buyer), 3=ZZ贸易(buyer)
-- contract_type: 1=留货, 2=订货
-- contract status: 0=READY, 1=PENDING_SIGN, 2=SIGNED, 3=PICKING, 4=PICKED, 5=SETTLED
-- pickup_order status: 1=DISPATCH_PENDING, 2=READY, 5=DELIVERING, 6=COMPLETED

-- Warehouses (2): 华东自有仓-有WMS, 华南堆场-无WMS
INSERT INTO warehouse (id, enterprise_id, warehouse_name, warehouse_code, address, contact_name, contact_phone, has_wms, status, deleted) VALUES
(1, 1, '华东自有仓', 'HD-ZY-01', '上海市宝山区富锦路1000号', '王经理', '021-12345678', 1, 1, 0),
(2, 1, '华南堆场', 'HN-DC-01', '广东省广州市黄埔区港前路200号', '李主管', '020-87654321', 0, 1, 0);

-- Carriers (2): 顺通物流, 安达运输
INSERT INTO carrier (id, enterprise_id, carrier_name, contact_name, contact_phone, status, deleted) VALUES
(1, 1, '顺通物流', '张调度', '13800001111', 1, 0),
(2, 1, '安达运输', '刘经理', '13900002222', 1, 0);

-- Contracts (5)
-- HT2026-001: 留货合同, READY (YY建设, 螺纹钢500吨)
-- HT2026-002: 订货合同, SIGNED (ZZ贸易, 盘螺300吨)
-- HT2026-003: 留货合同, PICKING (YY建设, 线材200吨, 已提120吨)
-- HT2026-004: 订货合同, PICKED (ZZ贸易, 螺纹钢150吨, 全部提完)
-- HT2026-005: 留货合同, SETTLED (YY建设, 盘螺100吨, 已结清)
INSERT INTO contract (id, contract_no, erp_contract_id, contract_type, seller_id, buyer_id, buyer_contact_name, buyer_contact_phone, total_quantity, total_weight, total_amount, picked_weight, picked_amount, warehouse_id, warehouse_name, sign_status, signed_pdf_url, status, created_at, updated_at, deleted) VALUES
(1, 'HT2026-001', 'ERP-C001', 1, 1, 2, '李四', '13800138003', 500, 500.0000, 1925000.00, 0, 0, 1, '华东自有仓', 1, 'https://example.com/contract-1.pdf', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'HT2026-002', 'ERP-C002', 2, 1, 3, '王五', '13800138004', 300, 300.0000, 1176000.00, 0, 0, 1, '华东自有仓', 1, 'https://example.com/contract-2.pdf', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'HT2026-003', 'ERP-C003', 1, 1, 2, '李四', '13800138003', 200, 200.0000, 732000.00, 120.0000, 439200.00, 1, '华东自有仓', 1, 'https://example.com/contract-3.pdf', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 'HT2026-004', 'ERP-C004', 2, 1, 3, '王五', '13800138004', 150, 150.0000, 577500.00, 150.0000, 577500.00, 1, '华东自有仓', 1, 'https://example.com/contract-4.pdf', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 'HT2026-005', 'ERP-C005', 1, 1, 2, '李四', '13800138003', 100, 100.0000, 392000.00, 100.0000, 392000.00, 2, '华南堆场', 1, 'https://example.com/contract-5.pdf', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Contract items (15+): 日照钢铁/沙钢/永钢/首钢
INSERT INTO contract_item (id, contract_id, line_no, product_name, spec, material, origin, quantity, weight, unit_price, amount, picked_quantity, picked_weight, created_at, updated_at, deleted) VALUES
(1, 1, 1, '螺纹钢', 'Φ20 HRB400', 'HRB400', '日照钢铁', 200, 200.0000, 3850.00, 770000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 2, '螺纹钢', 'Φ25 HRB400', 'HRB400', '日照钢铁', 150, 150.0000, 3820.00, 573000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 3, '盘螺', 'Φ10 Q235', 'Q235', '沙钢', 150, 150.0000, 3920.00, 588000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 2, 1, '盘螺', 'Φ8 HRB400', 'HRB400', '沙钢', 150, 150.0000, 3880.00, 582000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 2, 2, '盘螺', 'Φ10 Q235', 'Q235', '永钢', 150, 150.0000, 3960.00, 594000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(6, 3, 1, '线材', 'Φ6.5 Q235', 'Q235', '永钢', 100, 100.0000, 3650.00, 365000.00, 60, 60.0000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(7, 3, 2, '线材', 'Φ8 Q235', 'Q235', '首钢', 100, 100.0000, 3670.00, 367000.00, 60, 60.0000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(8, 4, 1, '螺纹钢', 'Φ20 HRB400', 'HRB400', '日照钢铁', 150, 150.0000, 3850.00, 577500.00, 150, 150.0000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(9, 5, 1, '盘螺', 'Φ10 HRB400', 'HRB400', '沙钢', 100, 100.0000, 3920.00, 392000.00, 100, 100.0000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(10, 1, 4, '线材', 'Φ6 Q195', 'Q195', '永钢', 80, 80.0000, 3650.00, 292000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(11, 2, 3, '螺纹钢', 'Φ16 HRB400', 'HRB400', '首钢', 100, 100.0000, 3890.00, 389000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(12, 3, 3, '螺纹钢', 'Φ22 HRB400', 'HRB400', '日照钢铁', 50, 50.0000, 3840.00, 192000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Pickup orders (6)
-- TH20260301001: COMPLETED (HT2026-003, 60吨, 已完成)
-- TH20260305001: COMPLETED (HT2026-003, 60吨, 已完成)
-- TH20260310001: DELIVERING (HT2026-001, 100吨, 发货中)
-- TH20260315001: READY (HT2026-001, 80吨, 待提货)
-- TH20260315002: DISPATCH_PENDING (HT2026-002, 150吨, 待派车)
-- TH20260301002: COMPLETED (HT2026-004, 150吨, 已完成)
INSERT INTO pickup_order (id, pickup_no, contract_id, contract_no, buyer_id, pickup_code, pickup_code_status, dispatch_mode, dispatch_status, customer_confirmed, vehicle_plate, driver_name, driver_phone, warehouse_id, warehouse_name, total_lifts, total_pieces, total_weight, total_amount, delivery_status, settlement_status, status, created_at, updated_at, deleted) VALUES
(1, 'TH20260301001', 3, 'HT2026-003', 2, 'A1B2C3', 2, 1, 2, 1, '苏E12345', '赵师傅', '13400006666', 1, '华东自有仓', 4, 40, 60.000, 219600.00, 2, 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'TH20260305001', 3, 'HT2026-003', 2, 'D4E5F6', 2, 1, 2, 1, '苏E67890', '钱师傅', '13300007777', 1, '华东自有仓', 4, 40, 60.000, 219600.00, 2, 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'TH20260310001', 1, 'HT2026-001', 2, 'G7H8I9', 2, 1, 2, 1, '沪A11111', '孙师傅', '13200008888', 1, '华东自有仓', 6, 60, 100.000, 385000.00, 1, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 'TH20260315001', 1, 'HT2026-001', 2, 'J0K1L2', 2, 1, 2, 1, NULL, NULL, NULL, 1, '华东自有仓', 0, 0, 80.000, 308000.00, 0, 0, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 'TH20260315002', 2, 'HT2026-002', 3, NULL, 0, 2, 0, 0, NULL, NULL, NULL, 1, '华东自有仓', 0, 0, 150.000, 588000.00, 0, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(6, 'TH20260301002', 4, 'HT2026-004', 3, 'M3N4O5', 2, 1, 2, 1, '沪B22222', '周师傅', '13100009999', 1, '华东自有仓', 8, 80, 150.000, 577500.00, 2, 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Lift records (10+) for completed orders
INSERT INTO lift_record (id, pickup_order_id, lift_seq, product_name, spec, material, heat_no, batch_no, pieces, theoretical_weight, actual_weight, operator_id, operator_name, data_source, uploaded_at, created_at, updated_at, deleted) VALUES
(1, 1, 1, '线材Φ6.5 Q235', 'Φ6.5', 'Q235', 'H202603001', 'B20260301001', 10, 15.000, 14.950, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 2, '线材Φ6.5 Q235', 'Φ6.5', 'Q235', 'H202603001', 'B20260301002', 10, 15.000, 15.020, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 3, '线材Φ8 Q235', 'Φ8', 'Q235', 'H202603002', 'B20260301003', 10, 15.000, 14.980, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 1, 4, '线材Φ8 Q235', 'Φ8', 'Q235', 'H202603002', 'B20260301004', 10, 15.000, 15.050, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 2, 1, '线材Φ6.5 Q235', 'Φ6.5', 'Q235', 'H202603003', 'B20260305001', 10, 15.000, 14.990, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(6, 2, 2, '线材Φ6.5 Q235', 'Φ6.5', 'Q235', 'H202603003', 'B20260305002', 10, 15.000, 15.010, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(7, 2, 3, '线材Φ8 Q235', 'Φ8', 'Q235', 'H202603004', 'B20260305003', 10, 15.000, 14.970, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(8, 2, 4, '线材Φ8 Q235', 'Φ8', 'Q235', 'H202603004', 'B20260305004', 10, 15.000, 15.030, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(9, 6, 1, '螺纹钢Φ20 HRB400', 'Φ20', 'HRB400', 'H202603005', 'B20260301005', 10, 18.750, 18.720, 'OP002', '仓库李', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(10, 6, 2, '螺纹钢Φ20 HRB400', 'Φ20', 'HRB400', 'H202603005', 'B20260301006', 10, 18.750, 18.780, 'OP002', '仓库李', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(11, 6, 3, '螺纹钢Φ20 HRB400', 'Φ20', 'HRB400', 'H202603006', 'B20260301007', 10, 18.750, 18.690, 'OP002', '仓库李', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(12, 6, 4, '螺纹钢Φ20 HRB400', 'Φ20', 'HRB400', 'H202603006', 'B20260301008', 10, 18.750, 18.810, 'OP002', '仓库李', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Delivery confirms (2) + photos (6)
INSERT INTO delivery_confirm (id, pickup_order_id, operator_id, operator_name, signature_url, total_lifts, total_pieces, total_weight, confirmed_at, created_at, updated_at, deleted) VALUES
(1, 1, 'OP001', '仓库张', 'https://example.com/signature-1.png', 4, 40, 60.000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 2, 'OP001', '仓库张', 'https://example.com/signature-2.png', 4, 40, 60.000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO delivery_photo (id, pickup_order_id, photo_type, photo_url, taken_at, created_at, updated_at, deleted) VALUES
(1, 1, 1, 'https://example.com/photo-loading-1.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 2, 'https://example.com/photo-goods-1.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 3, 'https://example.com/photo-plate-1.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 2, 1, 'https://example.com/photo-loading-2.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 2, 2, 'https://example.com/photo-goods-2.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(6, 2, 3, 'https://example.com/photo-plate-2.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Settlement orders (3)
INSERT INTO settlement_order (id, settlement_no, pickup_order_id, contract_id, contract_no, buyer_id, total_weight, total_amount, tax_amount, total_with_tax, deducted_prepayment, receivable_amount, customer_viewed, status, created_at, updated_at, deleted) VALUES
(1, 'JS20260301001', 1, 3, 'HT2026-003', 2, 60.000, 219600.00, 0, 219600.00, 0, 219600.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'JS20260305001', 2, 3, 'HT2026-003', 2, 60.000, 219600.00, 0, 219600.00, 0, 219600.00, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'JS20260301002', 6, 4, 'HT2026-004', 3, 150.000, 577500.00, 0, 577500.00, 0, 577500.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Evidence packages (2)
INSERT INTO evidence_package (id, pickup_order_id, contract_id, package_hash, contract_pdf_url, photos_urls, signature_url, settlement_pdf_url, evidence_status, archived_at, created_at, updated_at, deleted) VALUES
(1, 1, 3, 'demo_hash_abc123def456', 'https://example.com/contract-3.pdf', 'https://example.com/photo-loading-1.jpg,https://example.com/photo-goods-1.jpg,https://example.com/photo-plate-1.jpg', 'https://example.com/signature-1.png', 'https://example.com/settlement-1.pdf', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 6, 4, 'demo_hash_xyz789ghi012', 'https://example.com/contract-4.pdf', 'https://example.com/photo-loading-6.jpg,https://example.com/photo-goods-6.jpg', 'https://example.com/signature-6.png', 'https://example.com/settlement-3.pdf', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

UPDATE pickup_order SET evidence_package_id = 1 WHERE id = 1;
UPDATE pickup_order SET evidence_package_id = 2 WHERE id = 6;

-- Authorized pickup persons (4)
INSERT INTO authorized_pickup_person (id, buyer_id, contract_id, person_name, phone, vehicle_plate, buyer_confirmed, status, created_at, updated_at, deleted) VALUES
(1, 2, NULL, '李四', '13800138003', NULL, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 2, 1, '李采购', '13800138033', '苏E12345', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 3, NULL, '王五', '13800138004', NULL, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 3, 2, '王经理', '13800138044', '沪B22222', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Progress events (8)
INSERT INTO progress_event (id, pickup_order_id, contract_id, event_type, event_title, operator, notified, created_at, updated_at, deleted) VALUES
(1, 1, 3, 'DISPATCH', '派车确认', '系统', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 3, 'ARRIVE', '司机到达仓库', '赵师傅', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 3, 'LIFT_START', '开始装车', '仓库张', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 1, 3, 'DELIVERY_DONE', '发货完成', '仓库张', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 2, 3, 'LIFT_UPLOAD', '第2吊上传', '仓库李', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(6, 3, 1, 'DISPATCH', '派车确认', '系统', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(7, 3, 1, 'ARRIVE', '司机到达仓库', '孙师傅', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(8, 6, 4, 'DELIVERY_DONE', '发货完成', '仓库李', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Trading habit records (3)
INSERT INTO trading_habit_record (id, buyer_id, driver_name, driver_phone, vehicle_plate, total_pickups, total_weight, total_amount, paid_pickups, last_pickup_at, first_pickup_at, created_at, updated_at, deleted) VALUES
(1, 2, '赵师傅', '13400006666', '苏E12345', 5, 120.500, 439200.00, 4, CURRENT_TIMESTAMP, TIMESTAMP '2026-01-15 10:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 2, '钱师傅', '13300007777', '苏E67890', 3, 80.000, 292800.00, 3, CURRENT_TIMESTAMP, TIMESTAMP '2026-02-10 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 3, '周师傅', '13100009999', '沪B22222', 2, 150.000, 577500.00, 2, CURRENT_TIMESTAMP, TIMESTAMP '2026-02-20 14:30:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Confirm timeout configs (3)
INSERT INTO confirm_timeout_config (id, enterprise_id, buyer_id, scenario, timeout_value, timeout_unit, reminder_before_hours, status, created_at, updated_at, deleted) VALUES
(1, 1, 2, 'DISPATCH_CONFIRM', 24, 'HOUR', 6, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 3, 'SETTLEMENT_VIEW', 72, 'HOUR', 12, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 2, 'SETTLEMENT_VIEW', 48, 'HOUR', 8, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Pickup verifications (2)
INSERT INTO pickup_verification (id, pickup_order_id, contract_id, buyer_id, driver_name, driver_phone, vehicle_plate, verification_result, created_at, updated_at, deleted) VALUES
(1, 1, 3, 2, '赵师傅', '13400006666', '苏E12345', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 6, 4, 3, '周师傅', '13100009999', '沪B22222', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
