-- Pickup Express - Demo data (steel industry)
-- Sellers: 1, 2 | Buyers: 3, 4, 5 | Warehouses: 1, 2 | Carriers: 1, 2

-- Warehouses (2)
INSERT INTO warehouse (id, enterprise_id, warehouse_name, warehouse_code, address, contact_name, contact_phone, status, deleted) VALUES
(1, 1, '日照钢铁上海仓库', 'RZ-SH-01', '上海市宝山区富锦路1000号', '王经理', '021-12345678', 1, 0),
(2, 1, '沙钢无锡仓库', 'SG-WX-01', '江苏省无锡市惠山区洛社镇', '李主管', '0510-87654321', 1, 0);

-- Carriers (2)
INSERT INTO carrier (id, enterprise_id, carrier_name, contact_name, contact_phone, status, deleted) VALUES
(1, 1, '华东物流有限公司', '张调度', '13800001111', 1, 0),
(2, 1, '长三角运输公司', '刘经理', '13900002222', 1, 0);

-- Contracts (3: 1 reserved=READY, 1 order=SIGNED, 1 order=PICKING)
INSERT INTO contract (id, contract_no, erp_contract_id, contract_type, seller_id, buyer_id, buyer_contact_name, buyer_contact_phone, total_quantity, total_weight, total_amount, picked_weight, picked_amount, warehouse_id, warehouse_name, sign_status, signed_pdf_url, status, created_at, updated_at, deleted) VALUES
(1, 'HT202403150001', 'ERP-C001', 1, 1, 3, '陈经理', '13700003333', 100, 100.0000, 450000.00, 0, 0, 1, '日照钢铁上海仓库', 0, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'HT202403160001', 'ERP-C002', 2, 1, 4, '林总', '13600004444', 50, 50.0000, 235000.00, 0, 0, 1, '日照钢铁上海仓库', 1, 'https://example.com/contract-signed-2.pdf', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'HT202403170001', 'ERP-C003', 2, 2, 5, '周主任', '13500005555', 80, 80.0000, 380000.00, 25.5, 121125.00, 2, '沙钢无锡仓库', 1, 'https://example.com/contract-signed-3.pdf', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Contract items (steel products)
INSERT INTO contract_item (id, contract_id, line_no, product_name, spec, material, origin, quantity, weight, unit_price, amount, picked_quantity, picked_weight, created_at, updated_at, deleted) VALUES
(1, 1, 1, '螺纹钢', 'Φ20', 'HRB400', '日照钢铁', 50, 50.0000, 4500.00, 225000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 2, '盘螺', 'Φ10', 'Q235', '日照钢铁', 50, 50.0000, 4500.00, 225000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 2, 1, '螺纹钢Φ20 HRB400', 'Φ20', 'HRB400', '日照钢铁', 50, 50.0000, 4700.00, 235000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 3, 1, '盘螺Φ10 Q235', 'Φ10', 'Q235', '沙钢', 40, 40.0000, 4725.00, 189000.00, 25.5, 25.5000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 3, 2, '高线Φ8 HPB300', 'Φ8', 'HPB300', '沙钢', 40, 40.0000, 4775.00, 191000.00, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Pickup orders (3: 1 COMPLETED, 1 DELIVERING, 1 READY)
INSERT INTO pickup_order (id, pickup_no, contract_id, contract_no, buyer_id, pickup_code, pickup_code_status, dispatch_mode, dispatch_status, customer_confirmed, vehicle_plate, driver_name, driver_phone, warehouse_id, warehouse_name, total_lifts, total_pieces, total_weight, total_amount, delivery_status, settlement_status, status, created_at, updated_at, deleted) VALUES
(1, 'TH202403170001', 3, 'HT202403170001', 5, 'A1B2C3', 2, 1, 2, 1, '苏E12345', '赵师傅', '13400006666', 2, '沙钢无锡仓库', 4, 40, 25.500, 121125.00, 2, 1, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'TH202403170002', 3, 'HT202403170001', 5, 'D4E5F6', 0, 1, 2, 1, '苏E67890', '钱师傅', '13300007777', 2, '沙钢无锡仓库', 2, 20, 12.800, 0, 1, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'TH202403170003', 2, 'HT202403160001', 4, 'G7H8I9', 0, 2, 2, 1, '沪A11111', '孙师傅', '13200008888', 1, '日照钢铁上海仓库', 0, 0, 0, 0, 0, 0, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Lift records for completed pickup (4 lifts)
INSERT INTO lift_record (id, pickup_order_id, lift_seq, product_name, spec, material, heat_no, batch_no, pieces, theoretical_weight, actual_weight, operator_id, operator_name, data_source, uploaded_at, created_at, updated_at, deleted) VALUES
(1, 1, 1, '盘螺Φ10 Q235', 'Φ10', 'Q235', 'H202403001', 'B20240317001', 10, 6.350, 6.320, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 2, '盘螺Φ10 Q235', 'Φ10', 'Q235', 'H202403001', 'B20240317002', 10, 6.350, 6.380, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 3, '盘螺Φ10 Q235', 'Φ10', 'Q235', 'H202403002', 'B20240317003', 10, 6.400, 6.410, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 1, 4, '盘螺Φ10 Q235', 'Φ10', 'Q235', 'H202403002', 'B20240317004', 10, 6.400, 6.390, 'OP001', '仓库张', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Delivery confirm + 3 photos for completed pickup
INSERT INTO delivery_confirm (id, pickup_order_id, operator_id, operator_name, signature_url, total_lifts, total_pieces, total_weight, confirmed_at, created_at, updated_at, deleted) VALUES
(1, 1, 'OP001', '仓库张', 'https://example.com/signature-1.png', 4, 40, 25.500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO delivery_photo (id, pickup_order_id, photo_type, photo_url, taken_at, created_at, updated_at, deleted) VALUES
(1, 1, 1, 'https://example.com/photo-loading-1.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 2, 'https://example.com/photo-goods-1.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 3, 'https://example.com/photo-plate-1.jpg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Settlement order
INSERT INTO settlement_order (id, settlement_no, pickup_order_id, contract_id, contract_no, buyer_id, total_weight, total_amount, tax_amount, total_with_tax, deducted_prepayment, receivable_amount, customer_viewed, status, created_at, updated_at, deleted) VALUES
(1, 'JS202403170001', 1, 3, 'HT202403170001', 5, 25.500, 121125.00, 0, 121125.00, 0, 121125.00, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Evidence package
INSERT INTO evidence_package (id, pickup_order_id, contract_id, package_hash, contract_pdf_url, photos_urls, signature_url, settlement_pdf_url, evidence_status, archived_at, created_at, updated_at, deleted) VALUES
(1, 1, 3, 'demo_hash_abc123def456', 'https://example.com/contract-signed-3.pdf', 'https://example.com/photo-loading-1.jpg,https://example.com/photo-goods-1.jpg,https://example.com/photo-plate-1.jpg', 'https://example.com/signature-1.png', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Update pickup_order 1 with evidence_package_id
UPDATE pickup_order SET evidence_package_id = 1 WHERE id = 1;

-- Authorized pickup persons (3)
INSERT INTO authorized_pickup_person (id, buyer_id, contract_id, person_name, phone, vehicle_plate, buyer_confirmed, status, created_at, updated_at, deleted) VALUES
(1, 3, NULL, '陈经理', '13700003333', NULL, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 4, 2, '林总', '13600004444', '沪A11111', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 5, 3, '周主任', '13500005555', '苏E12345', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Progress events (5)
INSERT INTO progress_event (id, pickup_order_id, contract_id, event_type, event_title, operator, notified, created_at, updated_at, deleted) VALUES
(1, 1, 3, 'DISPATCH', '派车确认', '系统', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 3, 'ARRIVE', '司机到达仓库', '赵师傅', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 3, 'LIFT_START', '开始装车', '仓库张', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 1, 3, 'DELIVERY_DONE', '发货完成', '仓库张', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 2, 3, 'LIFT_UPLOAD', '第2吊上传', '仓库李', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Trading habit records (2)
INSERT INTO trading_habit_record (id, buyer_id, driver_name, driver_phone, vehicle_plate, total_pickups, total_weight, total_amount, paid_pickups, last_pickup_at, first_pickup_at, created_at, updated_at, deleted) VALUES
(1, 5, '赵师傅', '13400006666', '苏E12345', 5, 120.500, 572000.00, 4, CURRENT_TIMESTAMP, TIMESTAMP '2024-01-15 10:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 4, '孙师傅', '13200008888', '沪A11111', 2, 35.000, 164500.00, 2, TIMESTAMP '2024-02-20 14:30:00', TIMESTAMP '2024-02-10 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Confirm timeout configs (2)
INSERT INTO confirm_timeout_config (id, enterprise_id, buyer_id, scenario, timeout_value, timeout_unit, reminder_before_hours, status, created_at, updated_at, deleted) VALUES
(1, 1, 3, 'DISPATCH_CONFIRM', 24, 'HOUR', 6, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 4, 'SETTLEMENT_VIEW', 72, 'HOUR', 12, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
