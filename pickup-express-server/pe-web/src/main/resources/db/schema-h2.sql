-- H2 in-memory schema for demo mode
-- Minimal schema for application startup
CREATE TABLE IF NOT EXISTS contract (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contract_no VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pickup_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pickup_no VARCHAR(32),
    contract_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT DEFAULT 0
);
