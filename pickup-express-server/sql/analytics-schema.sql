-- =============================================================================
-- Analytics Module - MySQL 8.0 Schema
-- =============================================================================

CREATE TABLE IF NOT EXISTS analytics_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id        VARCHAR(36) NOT NULL COMMENT '事件UUID',
    event_type      VARCHAR(30) NOT NULL COMMENT '事件类型',
    event_name      VARCHAR(100) NOT NULL COMMENT '事件名称',
    event_time      DATETIME(3) NOT NULL COMMENT '事件时间',
    system          VARCHAR(30) NOT NULL DEFAULT 'autorecon' COMMENT '系统标识',
    user_id         BIGINT DEFAULT 0,
    enterprise_id   BIGINT DEFAULT 0,
    role_type       TINYINT DEFAULT 0,
    is_guest        TINYINT DEFAULT 0,
    session_id      VARCHAR(36) DEFAULT '',
    device_id       VARCHAR(64) DEFAULT '',
    platform        VARCHAR(20) DEFAULT 'web',
    os              VARCHAR(30) DEFAULT '',
    browser         VARCHAR(30) DEFAULT '',
    screen_width    SMALLINT DEFAULT 0,
    screen_height   SMALLINT DEFAULT 0,
    is_mobile       TINYINT DEFAULT 0,
    page_path       VARCHAR(200) DEFAULT '',
    page_name       VARCHAR(50) DEFAULT '',
    page_title      VARCHAR(100) DEFAULT '',
    page_module     VARCHAR(30) DEFAULT '',
    referrer        VARCHAR(200) DEFAULT '',
    duration        INT DEFAULT 0 COMMENT '停留时长(ms)',
    action_category VARCHAR(50) DEFAULT '',
    action_label    VARCHAR(50) DEFAULT '',
    action_value    VARCHAR(200) DEFAULT '',
    action_extra    TEXT COMMENT 'JSON extra data',
    perf_fcp        SMALLINT DEFAULT 0,
    perf_lcp        SMALLINT DEFAULT 0,
    perf_fid        SMALLINT DEFAULT 0,
    perf_cls        FLOAT DEFAULT 0,
    ip              VARCHAR(45) DEFAULT '',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_time (event_time),
    INDEX idx_system (system),
    INDEX idx_enterprise (enterprise_id),
    INDEX idx_page_path (page_path),
    INDEX idx_event_type_name (event_type, event_name),
    UNIQUE INDEX uk_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析事件表';

CREATE TABLE IF NOT EXISTS analytics_page_daily (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date       DATE NOT NULL,
    system          VARCHAR(30) NOT NULL,
    page_path       VARCHAR(200) NOT NULL,
    page_name       VARCHAR(50) DEFAULT '',
    page_module     VARCHAR(30) DEFAULT '',
    pv              INT DEFAULT 0,
    uv              INT DEFAULT 0,
    sessions        INT DEFAULT 0,
    avg_duration    INT DEFAULT 0 COMMENT '平均停留ms',
    bounce_count    INT DEFAULT 0,
    avg_fcp         SMALLINT DEFAULT 0,
    avg_lcp         SMALLINT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_date_system_path (stat_date, system, page_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面日聚合';

CREATE TABLE IF NOT EXISTS analytics_action_daily (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date       DATE NOT NULL,
    system          VARCHAR(30) NOT NULL,
    event_name      VARCHAR(100) NOT NULL,
    action_category VARCHAR(50) DEFAULT '',
    action_count    INT DEFAULT 0,
    action_users    INT DEFAULT 0,
    action_enterprises INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_date_system_event (stat_date, system, event_name, action_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日聚合';

CREATE TABLE IF NOT EXISTS analytics_report_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name     VARCHAR(100) NOT NULL,
    report_type     VARCHAR(50) NOT NULL,
    system          VARCHAR(30),
    config_json     JSON,
    created_by      BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表配置';

CREATE TABLE IF NOT EXISTS analytics_funnel_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    funnel_name     VARCHAR(100) NOT NULL,
    system          VARCHAR(30),
    steps_json      JSON NOT NULL COMMENT '漏斗步骤JSON',
    window_hours    INT DEFAULT 24 COMMENT '转化窗口(小时)',
    created_by      BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='漏斗配置';

CREATE TABLE IF NOT EXISTS analytics_alert_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_name      VARCHAR(100) NOT NULL,
    metric          VARCHAR(100) NOT NULL,
    condition_type  VARCHAR(20) NOT NULL COMMENT 'gt/lt/change_rate',
    threshold       DECIMAL(10,2) NOT NULL,
    window_minutes  INT DEFAULT 60,
    notify_channels VARCHAR(200),
    enabled         TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则';
