# 对账通 v2.0 — 往来账对账系统设计方案

> 版本: v2.1（第 1 轮确认修订）  
> 日期: 2026-07-04  
> 分支: `autorecon-v2/reconciliation-redesign`  
> 状态: 设计评审中，待最终确认后编码

## 更新记录

| 版本 | 日期 | 变更 |
|------|------|------|
| v2.0 | 2026-07-04 | 首次输出，含 10 个待确认决策 |
| v2.1 | 2026-07-04 | 第 1 轮确认修订：科目过滤支持通配符；催收体系升级为"客户标签化差异化催收 + 部分/整单催 + AI 电话即时跟单"；AI 外呼首期对接阿里云；滚动周期节假日可配置顺延 |

---

## 一、需求分析与核心变化

### 1.1 v1 → v2 核心差异对照

| 维度 | v1（原系统） | v2（新需求） | 变化影响 |
|------|-------------|-------------|---------|
| **对账模型** | 按"贸易明细"对账（品名/规格/重量/金额逐条核对） | 按"往来流水"对账（类似银行对账单：日期/单号/摘要/应收/实收/余额） | **数据模型重构**——从"横向逐条比对"变为"纵向流水累计" |
| **对账单结构** | 一张对账单 = 明细列表 | 一份账套 = 往来流水 + 应收明细 | **新增"账套"概念**，流水与明细分离 |
| **周期模式** | 手动选择周期范围 | 固定周期（月度）+ 滚动周期（按天数） + 自动触发 | **新增调度引擎**，支持定时自动生成 |
| **科目过滤** | 无 | 按会计科目过滤不发送的项目，需保留余额快照 | **新增过滤规则引擎 + 余额快照机制** |
| **确认方式** | 支持超时自动确认 | **禁止自动确认**，仅支持主动确认或"视同确认"（状态不变） | **状态机调整**——去掉自动确认，新增"视同确认"状态 |
| **催收体系** | 单级催收（信用评分 + 手动催收） | 三阶段智能催收（通知 → AI外呼 → 转人工）+ 客户标签化 + 跟进闭环 | **催收引擎重构**——从"评分建议"升级为"执行闭环" |
| **提醒机制** | 提醒订阅（被动） | 多端同步主动推送 + 状态可视化（最近对账时间/状态） | **增强消息中心** |
| **数据来源** | ERP 拉取 + 在线录入 + Excel + OCR | ERP 对接 + Excel 模板导入（简化，但流程一致） | **简化输入模式**，聚焦 ERP + Excel |

### 1.2 新需求的核心设计挑战

| 挑战 | 分析 | 设计建议 |
|------|------|---------|
| **余额快照机制** | 科目过滤会导致"发给客户的余额"与"内部实际余额"不一致，必须维护两条平行的余额线。这是本次需求中最复杂的业务逻辑。 | 引入"双轨余额"模型：`internalBalance`（含过滤项）和 `externalBalance`（不含过滤项），每笔流水记录时计算两个余额，过滤规则变更时可回溯重算。 |
| **禁止自动确认 vs 视同确认** | 看似矛盾——财务上不能自动确认，但法律上又需要"视同无异议"。需要在系统状态和法律效力之间做区分。 | 状态机设计为 `PENDING`（待确认）→ `CONFIRMED`（已确认）或 `DEEMED_CONFIRMED`（视同确认），两者法律效力相同但系统标记不同。`DEEMED_CONFIRMED` 由定时任务标记但**不改变财务状态**——即不触发自动签章或结算。 |
| **滚动周期调度** | "每5天对账一次"意味着不同客户的对账周期可能完全不同，且周期起止点随时间滚动。 | 每个客户维护独立的 `nextReconDate`，调度器每天扫描并触发到期客户的对账任务。 |
| **三阶段催收** | 需要与外部系统（短信网关、AI 外呼平台）集成，且需要跟进状态闭环。 | 设计"催收工单"模型，每笔逾期应收生成独立工单，工单状态独立流转（通知→外呼→人工→核销），与对账单解耦。 |

---

## 二、系统架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                       前端 (Vue3)                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │ 对账中心 │ │ 催收中心 │ │ 消息中心 │ │ 系统配置   │ │
│  │ ·往来流水│ │ ·催收工单│ │ ·待办提醒│ │ ·科目过滤  │ │
│  │ ·对账单  │ │ ·跟进记录│ │ ·多端同步│ │ ·周期配置  │ │
│  │ ·确认签章│ │ ·催收策略│ │ ·状态看板│ │ ·ERP对接   │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘ │
└──────────────────────┬──────────────────────────────────┘
                       │ REST API
┌──────────────────────┴──────────────────────────────────┐
│                    后端 (Spring Boot)                     │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐ │
│  │                    核心业务层                         │ │
│  │  ┌────────────┐ ┌────────────┐ ┌──────────────────┐ │ │
│  │  │ 账套引擎   │ │ 调度引擎   │ │ 催收引擎         │ │ │
│  │  │ ·流水管理  │ │ ·固定周期  │ │ ·阶梯催收        │ │ │
│  │  │ ·科目过滤  │ │ ·滚动周期  │ │ ·AI外呼对接      │ │ │
│  │  │ ·余额快照  │ │ ·自动触发  │ │ ·人工转派        │ │ │
│  │  │ ·双轨余额  │ │ ·手动触发  │ │ ·跟进闭环        │ │ │
│  │  └────────────┘ └────────────┘ └──────────────────┘ │ │
│  │  ┌────────────┐ ┌────────────┐ ┌──────────────────┐ │ │
│  │  │ 对账单引擎 │ │ 确认引擎   │ │ 消息引擎         │ │ │
│  │  │ ·生成渲染  │ │ ·电子签章  │ │ ·多端推送        │ │ │
│  │  │ ·PDF导出   │ │ ·手机验证  │ │ ·消息中心        │ │ │
│  │  │ ·异议处理  │ │ ·视同确认  │ │ ·状态可视化      │ │ │
│  │  └────────────┘ └────────────┘ └──────────────────┘ │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ ERP 适配器   │  │ Excel 导入器 │  │ 外部集成       │  │
│  │ ·REST/DB/WS │  │ ·模板下载    │  │ ·短信网关      │  │
│  │ ·字段映射    │  │ ·数据校验    │  │ ·AI外呼平台    │  │
│  │ ·增量同步    │  │ ·余额校验    │  │ ·电子签章CA    │  │
│  └──────────────┘  └──────────────┘  └───────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 2.2 核心数据模型

```
┌──────────────────────────────────────────────────────────┐
│ 客户往来账 (customer_ledger)                              │
│  企业 ── 客户 ── 科目过滤规则                              │
│      └── 周期配置 (固定/滚动)                              │
│      └── 催收策略配置                                      │
│                                                          │
│ 往来流水 (ledger_transaction)                             │
│  日期 · 单号 · 摘要 · 结算数量                              │
│  本期应收 · 本期实收 · 应收余额                              │
│  科目编码 · 是否过滤 · 过滤前余额(内部) · 过滤后余额(外部)    │
│                                                          │
│ 对账单 (recon_statement)                                  │
│  账套 = 往来流水快照 + 应收明细                              │
│  周期 · 状态 · 期初余额 · 期末余额                           │
│  发送时间 · 确认时间 · 确认方式                               │
│                                                          │
│ 催收工单 (collection_order)                               │
│  关联对账单 · 逾期金额 · 当前阶段                            │
│  催收记录 · 跟进记录 · 预计回款 · 核销状态                     │
└──────────────────────────────────────────────────────────┘
```

---

## 三、数据模型详细设计

### 3.1 客户往来账配置

```sql
-- 客户往来账配置（每个企业×客户一条）
CREATE TABLE customer_ledger_config (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id       BIGINT NOT NULL COMMENT '卖方企业ID',
    customer_id         BIGINT NOT NULL COMMENT '客户(买方)ID',
    customer_name       VARCHAR(100) NOT NULL COMMENT '客户名称',

    -- 对账周期配置
    recon_mode          VARCHAR(20) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED=固定周期, ROLLING=滚动周期',
    fixed_day           INT DEFAULT 25 COMMENT '固定日(每月X日对上月)',
    rolling_days        INT DEFAULT 5 COMMENT '滚动天数',
    next_recon_date     DATE COMMENT '下次对账日期',
    last_recon_date     DATE COMMENT '最近一次对账日期',
    last_recon_status   VARCHAR(30) DEFAULT '' COMMENT '最近对账状态',

    -- 发起模式
    trigger_mode        VARCHAR(20) NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO=自动发起, MANUAL=仅手动',

    -- 科目过滤
    filter_subjects     JSON COMMENT '过滤科目编码列表 ["HK001","NB002"]',

    -- 视同确认天数
    deemed_confirm_days INT DEFAULT 0 COMMENT '0=不启用视同确认; >0=发出后X天视同确认',

    -- 催收策略
    collection_enabled  TINYINT DEFAULT 1 COMMENT '是否启用自动催收',
    collection_profile  VARCHAR(30) DEFAULT 'DEFAULT' COMMENT '催收策略配置名',

    -- 状态
    enabled             TINYINT DEFAULT 1,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,

    UNIQUE INDEX uk_enterprise_customer (enterprise_id, customer_id),
    INDEX idx_next_recon (next_recon_date, enabled)
) COMMENT '客户往来账配置';
```

### 3.2 往来流水

```sql
-- 往来流水（银行对账单式）
CREATE TABLE ledger_transaction (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id       BIGINT NOT NULL,
    customer_id         BIGINT NOT NULL,

    -- 流水核心字段
    txn_date            DATE NOT NULL COMMENT '交易日期',
    doc_no              VARCHAR(50) NOT NULL COMMENT '单号(销售单/收款单/调整单等)',
    doc_type            VARCHAR(30) NOT NULL COMMENT '单据类型: SALES/RECEIPT/ADJUST/REFUND/OTHER',
    summary             VARCHAR(200) NOT NULL COMMENT '摘要',
    subject_code        VARCHAR(30) DEFAULT '' COMMENT '会计科目编码',

    -- 金额字段
    settle_qty          DECIMAL(14,4) DEFAULT 0 COMMENT '结算数量',
    period_receivable   DECIMAL(14,2) DEFAULT 0 COMMENT '本期应收(正=应收增加)',
    period_received     DECIMAL(14,2) DEFAULT 0 COMMENT '本期实收(正=实收增加)',

    -- 双轨余额
    internal_balance    DECIMAL(14,2) NOT NULL COMMENT '应收余额(内部,含过滤项)',
    external_balance    DECIMAL(14,2) NOT NULL COMMENT '应收余额(外部,不含过滤项)',
    is_filtered         TINYINT DEFAULT 0 COMMENT '是否被科目过滤(不发送给客户)',

    -- 数据来源
    source_type         VARCHAR(20) DEFAULT 'ERP' COMMENT 'ERP/EXCEL/MANUAL',
    source_ref          VARCHAR(100) DEFAULT '' COMMENT '来源系统单据ID',
    erp_sync_batch      VARCHAR(50) DEFAULT '' COMMENT 'ERP同步批次号',

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,

    INDEX idx_enterprise_customer (enterprise_id, customer_id),
    INDEX idx_txn_date (txn_date),
    INDEX idx_doc_no (doc_no),
    INDEX idx_sync_batch (erp_sync_batch)
) COMMENT '往来流水';
```

### 3.3 余额快照

```sql
-- 余额快照（科目过滤变更时 & 每次对账时自动生成）
CREATE TABLE balance_snapshot (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id       BIGINT NOT NULL,
    customer_id         BIGINT NOT NULL,
    snapshot_date       DATE NOT NULL COMMENT '快照日期',
    snapshot_type       VARCHAR(20) NOT NULL COMMENT 'RECON=对账时/FILTER_CHANGE=过滤变更时/MANUAL=手动',

    internal_balance    DECIMAL(14,2) NOT NULL COMMENT '内部余额(含过滤项)',
    external_balance    DECIMAL(14,2) NOT NULL COMMENT '外部余额(不含过滤项)',
    filtered_amount     DECIMAL(14,2) DEFAULT 0 COMMENT '已过滤金额累计',

    txn_count           INT DEFAULT 0 COMMENT '截至该快照的流水条数',
    last_txn_id         BIGINT COMMENT '最后一条流水ID',

    recon_statement_id  BIGINT COMMENT '关联的对账单ID(如果是对账快照)',
    remark              VARCHAR(200) DEFAULT '',

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_enterprise_customer (enterprise_id, customer_id),
    INDEX idx_snapshot_date (snapshot_date)
) COMMENT '余额快照';
```

### 3.4 对账单

```sql
-- 对账单（一份账套）
CREATE TABLE recon_statement (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    statement_no        VARCHAR(30) NOT NULL COMMENT '对账单号',
    enterprise_id       BIGINT NOT NULL,
    customer_id         BIGINT NOT NULL,

    -- 周期
    period_start        DATE NOT NULL COMMENT '对账周期开始',
    period_end          DATE NOT NULL COMMENT '对账周期结束',
    recon_mode          VARCHAR(20) COMMENT 'FIXED/ROLLING',
    trigger_mode        VARCHAR(20) COMMENT 'AUTO/MANUAL',

    -- 余额信息
    opening_balance     DECIMAL(14,2) DEFAULT 0 COMMENT '期初余额(外部)',
    period_receivable   DECIMAL(14,2) DEFAULT 0 COMMENT '本期应收合计',
    period_received     DECIMAL(14,2) DEFAULT 0 COMMENT '本期实收合计',
    closing_balance     DECIMAL(14,2) DEFAULT 0 COMMENT '期末余额(外部)',
    internal_closing    DECIMAL(14,2) DEFAULT 0 COMMENT '期末余额(内部,含过滤项)',
    filtered_count      INT DEFAULT 0 COMMENT '被过滤的流水条数',
    filtered_amount     DECIMAL(14,2) DEFAULT 0 COMMENT '被过滤的金额',

    -- 流水数据
    txn_count           INT DEFAULT 0 COMMENT '往来流水条数(发送给客户的)',
    snapshot_id         BIGINT COMMENT '关联余额快照ID',

    -- 状态
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        COMMENT 'DRAFT=草稿, SENT=已发送, PENDING=待确认, CONFIRMED=已确认, DEEMED_CONFIRMED=视同确认, DISPUTED=异议中, REVISED=已修订, VOIDED=已作废',
    sent_at             DATETIME COMMENT '发送时间',
    confirmed_at        DATETIME COMMENT '确认时间',
    confirm_method      VARCHAR(30) COMMENT 'E_SEAL=电子签章, SMS=手机验证, DEEMED=视同确认',
    deemed_confirm_date DATE COMMENT '视同确认生效日期',

    -- 签章
    pdf_url             VARCHAR(500) DEFAULT '',
    signed_pdf_url      VARCHAR(500) DEFAULT '',

    -- 关联
    revised_from_id     BIGINT COMMENT '修订来源(异议后重新生成时关联原单)',

    remark              VARCHAR(500) DEFAULT '',
    created_by          BIGINT,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,

    UNIQUE INDEX uk_statement_no (statement_no),
    INDEX idx_enterprise_customer (enterprise_id, customer_id),
    INDEX idx_status (status),
    INDEX idx_period (period_start, period_end),
    INDEX idx_deemed_date (deemed_confirm_date)
) COMMENT '对账单';
```

### 3.5 对账单流水明细（快照）

```sql
-- 对账单包含的流水明细（发送时的快照副本）
CREATE TABLE recon_statement_item (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    statement_id        BIGINT NOT NULL COMMENT '对账单ID',
    txn_id              BIGINT NOT NULL COMMENT '原始流水ID',

    -- 快照字段（发送时复制，不随原始流水变化）
    txn_date            DATE NOT NULL,
    doc_no              VARCHAR(50) NOT NULL,
    doc_type            VARCHAR(30) NOT NULL,
    summary             VARCHAR(200) NOT NULL,
    settle_qty          DECIMAL(14,4) DEFAULT 0,
    period_receivable   DECIMAL(14,2) DEFAULT 0,
    period_received     DECIMAL(14,2) DEFAULT 0,
    running_balance     DECIMAL(14,2) NOT NULL COMMENT '滚动余额(外部)',

    sort_order          INT DEFAULT 0 COMMENT '排序序号',

    INDEX idx_statement (statement_id),
    INDEX idx_txn (txn_id)
) COMMENT '对账单流水明细(快照)';
```

### 3.6 异议记录

```sql
CREATE TABLE recon_dispute (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    statement_id        BIGINT NOT NULL COMMENT '对账单ID',
    enterprise_id       BIGINT NOT NULL,
    customer_id         BIGINT NOT NULL,

    dispute_type        VARCHAR(30) DEFAULT 'AMOUNT' COMMENT 'AMOUNT=金额异议, MISSING=缺失单据, OTHER=其他',
    description         TEXT NOT NULL COMMENT '异议描述',
    disputed_items      JSON COMMENT '争议明细项ID列表',
    disputed_amount     DECIMAL(14,2) DEFAULT 0 COMMENT '争议金额',

    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PROCESSING/RESOLVED/CLOSED',
    resolution          TEXT COMMENT '解决方案',
    resolved_by         BIGINT,
    resolved_at         DATETIME,

    -- 修订后的新对账单
    revised_statement_id BIGINT COMMENT '修订后生成的新对账单ID',

    created_by          BIGINT,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,

    INDEX idx_statement (statement_id),
    INDEX idx_status (status)
) COMMENT '对账异议';
```

### 3.7 催收工单

```sql
-- 催收工单（每笔逾期应收一个工单）
CREATE TABLE collection_order (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no            VARCHAR(30) NOT NULL COMMENT '催收工单号',
    enterprise_id       BIGINT NOT NULL,
    customer_id         BIGINT NOT NULL,
    statement_id        BIGINT COMMENT '关联对账单ID',

    -- 逾期信息
    overdue_amount      DECIMAL(14,2) NOT NULL COMMENT '逾期金额',
    overdue_days        INT DEFAULT 0 COMMENT '逾期天数',
    due_date            DATE COMMENT '到期日',

    -- 催收阶段
    stage               VARCHAR(20) NOT NULL DEFAULT 'STAGE_1'
        COMMENT 'STAGE_1=通知提醒, STAGE_2=AI外呼, STAGE_3=人工跟进, CLOSED=已核销',
    stage_changed_at    DATETIME COMMENT '阶段变更时间',

    -- 自动催收次数
    notify_count        INT DEFAULT 0 COMMENT '通知发送次数',
    call_count          INT DEFAULT 0 COMMENT 'AI外呼次数',
    last_notify_at      DATETIME COMMENT '最后通知时间',
    last_call_at        DATETIME COMMENT '最后外呼时间',
    call_enabled        TINYINT DEFAULT 1 COMMENT '是否启用AI外呼',

    -- 人工跟进
    assigned_to         BIGINT COMMENT '分配给(销售人员ID)',
    assigned_at         DATETIME,
    expected_pay_date   DATE COMMENT '预计回款日期',

    -- 核销
    collected_amount    DECIMAL(14,2) DEFAULT 0 COMMENT '已收回金额',
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/CLOSED',
    closed_at           DATETIME,
    close_reason        VARCHAR(100) COMMENT '关闭原因: PAID/WRITE_OFF/CANCELLED',

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,

    UNIQUE INDEX uk_order_no (order_no),
    INDEX idx_enterprise_customer (enterprise_id, customer_id),
    INDEX idx_stage (stage),
    INDEX idx_status (status)
) COMMENT '催收工单';
```

### 3.8 催收跟进记录

```sql
CREATE TABLE collection_follow_up (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT NOT NULL COMMENT '催收工单ID',

    action_type         VARCHAR(30) NOT NULL
        COMMENT 'SMS=短信, WECHAT=微信通知, AI_CALL=AI外呼, MANUAL_CALL=人工电话, VISIT=上门, NOTE=备注, STAGE_UP=升级阶段, PAYMENT=到款',
    action_detail       TEXT COMMENT '操作详情/通话记录/备注内容',
    action_result       VARCHAR(30) COMMENT 'SUCCESS/NO_ANSWER/PROMISED/REFUSED/PARTIAL_PAY',

    -- AI外呼相关
    call_duration       INT COMMENT '通话时长(秒)',
    call_record_url     VARCHAR(500) COMMENT '通话录音URL',

    -- 到款相关
    payment_amount      DECIMAL(14,2) COMMENT '本次到款金额',
    expected_pay_date   DATE COMMENT '承诺回款日期',

    operator_id         BIGINT COMMENT '操作人(0=系统自动)',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_order (order_id),
    INDEX idx_action_type (action_type)
) COMMENT '催收跟进记录';
```

### 3.9 催收策略配置

```sql
CREATE TABLE collection_profile (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id       BIGINT NOT NULL,
    profile_name        VARCHAR(50) NOT NULL COMMENT '策略名称',
    is_default          TINYINT DEFAULT 0,

    -- 阶段1: 通知提醒
    stage1_start_day    INT DEFAULT 1 COMMENT '逾期第X天开始',
    stage1_interval     INT DEFAULT 3 COMMENT '提醒间隔天数',
    stage1_max_count    INT DEFAULT 3 COMMENT '最多提醒次数',
    stage1_channels     VARCHAR(100) DEFAULT 'SMS,WECHAT' COMMENT '通知渠道',

    -- 阶段2: AI外呼
    stage2_start_day    INT DEFAULT 30 COMMENT '逾期第X天升级',
    stage2_interval     INT DEFAULT 7 COMMENT '外呼间隔天数',
    stage2_max_count    INT DEFAULT 2 COMMENT '最多外呼次数',
    stage2_script_style VARCHAR(30) DEFAULT 'FRIENDLY' COMMENT 'FRIENDLY=友好, FORMAL=正式, URGENT=紧急',
    stage2_enabled      TINYINT DEFAULT 1 COMMENT '是否启用AI外呼',

    -- 阶段3: 人工跟进
    stage3_start_day    INT DEFAULT 60 COMMENT '逾期第X天转人工',
    stage3_auto_assign  TINYINT DEFAULT 1 COMMENT '是否自动分配给客户负责销售',

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,

    INDEX idx_enterprise (enterprise_id)
) COMMENT '催收策略配置';
```

### 3.10 消息通知记录

```sql
CREATE TABLE notification_record (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id       BIGINT NOT NULL,
    target_user_id      BIGINT COMMENT '目标用户ID(内部用户)',
    target_customer_id  BIGINT COMMENT '目标客户ID(外部客户)',
    target_phone        VARCHAR(20) COMMENT '目标手机号(脱敏存储)',

    scene               VARCHAR(50) NOT NULL
        COMMENT 'STATEMENT_SENT/STATEMENT_REMIND/DISPUTE_NOTIFY/OVERDUE_REMIND/COLLECTION_NOTIFY/PAYMENT_RECEIVED',
    channel             VARCHAR(20) NOT NULL COMMENT 'SMS/WECHAT/APP/EMAIL/AI_CALL',
    title               VARCHAR(100),
    content             TEXT,
    ref_type            VARCHAR(30) COMMENT '关联类型: STATEMENT/COLLECTION/PAYMENT',
    ref_id              BIGINT COMMENT '关联ID',

    send_status         VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED/READ',
    sent_at             DATETIME,
    read_at             DATETIME,
    fail_reason         VARCHAR(200),

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_target_user (target_user_id),
    INDEX idx_target_customer (target_customer_id),
    INDEX idx_scene (scene),
    INDEX idx_send_status (send_status)
) COMMENT '消息通知记录';
```

---

## 四、核心流程设计

### 4.1 对账单状态机

```
                  ┌─────────────────────────────────────────────┐
                  │                                             │
   ┌──────┐  生成  ┌──────┐  发送  ┌─────────┐  客户主动确认  ┌──────────┐
   │ DRAFT├──────→│ SENT ├──────→│ PENDING ├──────────────→│CONFIRMED │
   └──┬───┘       └──────┘       └────┬────┘              └──────────┘
      │                               │
      │ 作废                           │ 客户提异议
      ↓                               ↓
   ┌──────┐                      ┌──────────┐  解决  ┌─────────┐  重新确认
   │VOIDED│                      │ DISPUTED ├──────→│ REVISED ├──────→ PENDING
   └──────┘                      └──────────┘       └─────────┘
                                       │
                                       │ 超过X天未反馈
                                       ↓
                                ┌───────────────┐
                                │DEEMED_CONFIRMED│
                                │ (视同确认)      │
                                └───────────────┘
                                 ⚠ 状态标记为"视同确认"
                                   但不等同于"已确认"
                                   不触发后续自动流程
```

**状态说明**：

| 状态 | 含义 | 可执行操作 |
|------|------|-----------|
| `DRAFT` | 草稿，尚未发送 | 编辑、发送、作废 |
| `SENT` | 已发送，等待打开 | - |
| `PENDING` | 待确认（客户已查看） | 客户确认、客户提异议 |
| `CONFIRMED` | 已确认（客户主动确认+签章） | 下载签章PDF、触发催收（如逾期） |
| `DEEMED_CONFIRMED` | 视同确认（超X天未反馈） | 仅标记状态，**不触发自动签章** |
| `DISPUTED` | 异议中 | 处理异议、修订对账单 |
| `REVISED` | 已修订（异议处理后重新生成） | 重新发送 → 回到 PENDING |
| `VOIDED` | 已作废 | 无 |

**关键设计决策**：

> ⚠️ **`DEEMED_CONFIRMED`（视同确认）不等于 `CONFIRMED`（已确认）**
>
> - `CONFIRMED` = 客户主动确认 + 电子签章/手机验证，法律效力最强
> - `DEEMED_CONFIRMED` = 根据协议条款"发出后 X 天未反馈视为无异议"
> - 系统**不会**将 `DEEMED_CONFIRMED` 自动升级为 `CONFIRMED`
> - 在应收报表中，`DEEMED_CONFIRMED` 单独显示，供财务判断是否需要人工跟进
> - 企业可在"视同确认"后选择手动催促客户补签

### 4.2 对账生成流程

```
触发源
├── 自动触发: 调度器检查 next_recon_date ≤ 今天
└── 手动触发: 销售人员点击"发起对账"

     ↓

Step 1: 确定对账范围
  · 获取客户配置 (customer_ledger_config)
  · 计算周期: period_start ~ period_end
  · 获取上一次对账的余额快照 → opening_balance

     ↓

Step 2: 拉取往来流水
  · 从 ledger_transaction 查询该客户该周期内的所有流水
  · 或从 ERP 增量同步后查询
  · 或从 Excel 导入后查询

     ↓

Step 3: 应用科目过滤
  · 读取 filter_subjects 配置
  · 标记匹配的流水为 is_filtered = 1
  · 计算双轨余额:
    - internal_balance: 所有流水累计
    - external_balance: 仅非过滤流水累计

     ↓

Step 4: 生成余额快照
  · 记录 balance_snapshot (type=RECON)
  · 保存 internal_balance, external_balance, filtered_amount

     ↓

Step 5: 生成对账单
  · 创建 recon_statement (status=DRAFT)
  · 复制非过滤的流水到 recon_statement_item（快照副本）
  · 计算滚动余额 (running_balance)
  · 生成 PDF

     ↓

Step 6: 发送（自动或手动）
  · AUTO 模式: 直接发送 → status = SENT
  · MANUAL 模式: 停在 DRAFT → 等待人工确认后发送

     ↓

Step 7: 更新配置
  · 更新 last_recon_date = period_end
  · 更新 last_recon_status = 'SENT'
  · 计算 next_recon_date（固定模式：下月X日；滚动模式：+N天）
```

### 4.3 双轨余额计算示例

```
假设客户有以下流水（2025年3月）:

流水1: 3月1日 | 销售单 S001 | 应收 +10,000 | 科目: 正常
流水2: 3月5日 | 回扣    HK01 | 应收 -500   | 科目: HK (被过滤)
流水3: 3月10日| 收款单 R001 | 实收 +8,000  | 科目: 正常
流水4: 3月15日| 内部调整 NB01| 应收 -200   | 科目: NB (被过滤)
流水5: 3月20日| 销售单 S002 | 应收 +15,000 | 科目: 正常

期初余额(上期快照): 内部=20,000 / 外部=20,500

计算过程:
┌────┬────────┬──────────┬──────────┬──────────┬──────────┬────────┐
│序号│ 摘要    │ 应收     │ 实收     │ 内部余额  │ 外部余额  │ 过滤？ │
├────┼────────┼──────────┼──────────┼──────────┼──────────┼────────┤
│    │ 期初    │          │          │ 20,000   │ 20,500   │        │
│ 1  │ S001   │ +10,000  │          │ 30,000   │ 30,500   │ ❌     │
│ 2  │ HK01   │ -500     │          │ 29,500   │ 30,500   │ ✅     │
│ 3  │ R001   │          │ +8,000   │ 21,500   │ 22,500   │ ❌     │
│ 4  │ NB01   │ -200     │          │ 21,300   │ 22,500   │ ✅     │
│ 5  │ S002   │ +15,000  │          │ 36,300   │ 37,500   │ ❌     │
└────┴────────┴──────────┴──────────┴──────────┴──────────┴────────┘

发送给客户的对账单只包含流水 1、3、5:
  期初余额: ¥20,500 (外部)
  期末余额: ¥37,500 (外部)

内部系统记录完整的 ¥36,300（含回扣和内部调整）

差异 = 37,500 - 36,300 = ¥1,200（已过滤金额累计）
→ 记录在余额快照中，下次对账以 ¥37,500 为外部期初
```

### 4.4 催收工单流程

```
触发条件: 对账单状态为 CONFIRMED 且应收余额 > 0 且到期未收款

     ↓

┌──────────────────────────────────────────────────────────┐
│ 阶段1: 通知提醒 (STAGE_1)                                 │
│                                                          │
│  逾期第 1 天: 发送短信/微信提醒                              │
│  逾期第 4 天: 发送第 2 次提醒                                │
│  逾期第 7 天: 发送第 3 次提醒                                │
│                                                          │
│  若达到 stage1_max_count → 升级到阶段2                      │
└──────────────────────┬───────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────────┐
│ 阶段2: AI 外呼 (STAGE_2)                                  │
│                                                          │
│  前提: stage2_enabled = true                              │
│                                                          │
│  逾期第 30 天: 第 1 次 AI 外呼                              │
│    · 话术风格: FRIENDLY/FORMAL/URGENT (按配置)             │
│    · 记录通话结果: 接听/未接/承诺付款/拒绝                    │
│  逾期第 37 天: 第 2 次 AI 外呼                              │
│                                                          │
│  若达到 stage2_max_count 且无反馈 → 升级到阶段3              │
│  若客户承诺付款 → 记录预计回款日期，暂停催收                    │
└──────────────────────┬───────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────────┐
│ 阶段3: 人工跟进 (STAGE_3)                                  │
│                                                          │
│  系统自动分配给客户负责的销售人员                              │
│  发送站内消息 + 短信通知销售人员                               │
│                                                          │
│  销售人员需要:                                              │
│  · 录入跟进情况 (电话/拜访/备注)                             │
│  · 录入预计回款时间                                         │
│  · 直到款项到账核销                                         │
│                                                          │
│  款项到账 → collection_order.status = CLOSED               │
│          → 停止所有催收提醒                                  │
└──────────────────────────────────────────────────────────┘
```

### 4.5 消息中心与状态可视化

```
消息中心设计:

┌─────────────────────────────────────────────┐
│ 消息中心 (Header 右上角红点徽标)              │
│                                             │
│ 分类 Tab:                                   │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐       │
│ │ 全部 │ │ 对账 │ │ 催收 │ │ 系统 │       │
│ │ (15) │ │ (8)  │ │ (5)  │ │ (2)  │       │
│ └──────┘ └──────┘ └──────┘ └──────┘       │
│                                             │
│ 消息列表:                                    │
│ 🔴 某某贸易 对账单未确认 (已发送3天)    10分钟前│
│ 🟡 某某建材 逾期款项 ¥67,200 (逾期15天) 1小时前│
│ 🟢 某某制造 已确认对账单 R202503002     2小时前│
│ 🔴 某某科技 提出异议: 金额不符          昨天   │
│ ...                                         │
└─────────────────────────────────────────────┘

客户状态看板（在应收余额表中集成）:

| 客户名称    | 应收余额   | 最近对账日 | 对账状态   | 逾期天数 | 催收阶段 |
|------------|-----------|-----------|-----------|---------|---------|
| 某某贸易    | ¥75,800  | 03-17     | 🟡待确认  | -       | -       |
| 某某建材    | ¥67,200  | 03-15     | 🟢已确认  | 15天    | 🔴阶段1 |
| 某某科技    | ¥156,300 | 03-16     | 🔴异议中  | -       | -       |
| 某某物流    | ¥0       | 03-15     | 🟢已完成  | -       | -       |
| 某某建筑    | ¥98,600  | 03-14     | ⚪视同确认 | 5天     | -       |
```

---

## 五、前端页面设计

### 5.1 页面清单

| 序号 | 路径 | 页面名称 | 功能 |
|------|------|---------|------|
| 1 | `/dashboard` | 工作台 | 统计概览 + 待办 + 对账状态看板 |
| 2 | `/ledger/customers` | 客户往来账 | 客户列表 + 对账状态 + 余额 + 配置入口 |
| 3 | `/ledger/customers/:id` | 客户往来明细 | 往来流水表 + 双轨余额 + 历史对账单 |
| 4 | `/ledger/config/:id` | 客户对账配置 | 周期/过滤/催收策略配置 |
| 5 | `/statements` | 对账单列表 | 全部对账单 + 筛选 + 状态 |
| 6 | `/statements/create` | 发起对账 | 选择客户 → 拉取数据 → 预览 → 发送 |
| 7 | `/statements/:id` | 对账单详情 | 流水明细 + 余额 + 确认状态 + 异议 + 操作日志 |
| 8 | `/guest/:token` | 买方查看(Guest) | 验证 → 查看流水 → 确认/异议 |
| 9 | `/collection/orders` | 催收工单列表 | 工单列表 + 阶段 + 逾期天数 |
| 10 | `/collection/orders/:id` | 催收工单详情 | 跟进记录 + 操作(记录/升级/核销) |
| 11 | `/collection/profiles` | 催收策略配置 | 三阶段策略配置 |
| 12 | `/messages` | 消息中心 | 分类消息 + 已读/未读 + 状态 |
| 13 | `/reports/receivable` | 应收报表 | 应收余额表 + 对账状态 + 账龄分析 |
| 14 | `/system/erp` | ERP配置 | 对接/同步/映射 |
| 15 | `/system/subjects` | 科目过滤管理 | 配置哪些科目不发送 |
| 16 | `/system/templates` | 对账单模板 | 模板/样式配置 |
| 17 | `/system/users` | 用户管理 | 角色/权限 |
| 18 | `/system/enterprise` | 企业设置 | 企业信息/签章 |
| 19 | `/login` | 登录 | - |

### 5.2 客户往来账页面设计

```
┌─────────────────────────────────────────────────────────────┐
│ 客户往来账                                                    │
│                                                             │
│ 搜索: [客户名称    ] [状态 ▼] [查询] [重置]                   │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │客户名称    │应收余额   │最近对账  │对账状态│逾期 │催收  │操作│ │
│ ├───────────┼─────────┼────────┼──────┼────┼────┼────┤ │
│ │某某贸易    │¥75,800  │03-17   │🟡待确认│ -  │ -  │查看│ │
│ │某某建材    │¥67,200  │03-15   │🟢已确认│15天│🔴1 │查看│ │
│ │某某科技    │¥156,300 │03-16   │🔴异议中│ -  │ -  │查看│ │
│ │某某制造    │¥89,500  │03-14   │🟢已确认│ -  │ -  │查看│ │
│ │某某物流    │¥0       │03-15   │🟢已完成│ -  │ -  │查看│ │
│ │某某建筑    │¥98,600  │03-12   │⚪视同  │5天 │ -  │查看│ │
│ │某某钢构    │¥45,000  │ -      │⚪未对账│ -  │ -  │发起│ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ [手动发起对账] [批量发起] [导出应收表]                          │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 客户往来明细页面

```
┌─────────────────────────────────────────────────────────────┐
│ ← 返回  某某贸易有限公司 · 往来明细                            │
│                                                             │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐               │
│ │ 应收余额    │ │ 本月应收   │ │ 本月实收   │               │
│ │ ¥75,800    │ │ ¥125,800  │ │ ¥50,000   │               │
│ └────────────┘ └────────────┘ └────────────┘               │
│                                                             │
│ Tab: [往来流水] [历史对账单] [对账配置]                         │
│                                                             │
│ 往来流水（2025年3月）              [从ERP同步] [导入Excel]     │
│ ┌──────┬──────┬────────┬──────┬──────┬──────┬────────┬───┐ │
│ │ 日期  │ 单号  │ 摘要    │ 数量  │ 应收  │ 实收  │ 余额    │🔒│ │
│ ├──────┼──────┼────────┼──────┼──────┼──────┼────────┼───┤ │
│ │03-01 │S001  │螺纹钢   │100   │+10,000│     │30,500 │  │ │
│ │03-05 │HK01  │回扣     │     │-500  │     │(过滤)  │🔒│ │
│ │03-10 │R001  │银行收款  │     │     │+8,000│22,500 │  │ │
│ │03-15 │NB01  │内部调整  │     │-200  │     │(过滤)  │🔒│ │
│ │03-20 │S002  │盘螺     │50   │+15,000│     │37,500 │  │ │
│ └──────┴──────┴────────┴──────┴──────┴──────┴────────┴───┘ │
│ 🔒 = 科目过滤(不发送给客户)  内部余额: ¥36,300               │
│                                                             │
│ [发起本期对账]                                                │
└─────────────────────────────────────────────────────────────┘
```

### 5.4 买方 Guest 页面

```
┌─────────────────────────────────────────────────────────────┐
│ 🔒 本页面由对账通提供技术支持，数据加密传输                      │
│                                                             │
│        ┌────────────────────────────────────┐               │
│        │        对 账 单                     │               │
│        │                                    │               │
│        │ 卖方: 某某钢铁有限公司               │               │
│        │ 买方: 某某贸易有限公司               │               │
│        │ 对账周期: 2025-03-01 ~ 2025-03-31  │               │
│        │                                    │               │
│        │ ════════════════════════════════    │               │
│        │ 期初余额:          ¥20,500.00       │               │
│        │ ────────────────────────────────   │               │
│        │ 日期  │单号 │摘要     │应收   │实收  │               │
│        │ 03-01│S001│螺纹钢   │+10,000│     │               │
│        │ 03-10│R001│银行收款  │      │+8,000│               │
│        │ 03-20│S002│盘螺     │+15,000│     │               │
│        │ ────────────────────────────────   │               │
│        │ 本期应收:    ¥25,000.00             │               │
│        │ 本期实收:    ¥8,000.00              │               │
│        │ ════════════════════════════════    │               │
│        │ 期末应收余额: ¥37,500.00 ◀          │               │
│        │                                    │               │
│        │ [下载PDF]  [查看应收明细]            │               │
│        │                                    │               │
│        │ ┌──────────────────────────────┐   │               │
│        │ │   ✅ 确认无异议               │   │               │
│        │ └──────────────────────────────┘   │               │
│        │ ┌──────────────────────────────┐   │               │
│        │ │   ❌ 我有异议                 │   │               │
│        │ └──────────────────────────────┘   │               │
│        │                                    │               │
│        └────────────────────────────────────┘               │
│                                                             │
│ 🔒安全加密  📋合规存证  📞7×24支持                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、技术方案建议

### 6.1 后端架构

| 模块 | 建议 |
|------|------|
| **调度引擎** | XXL-JOB 定时任务，每天凌晨扫描 `next_recon_date`，触发到期客户的对账单生成 |
| **余额计算** | 事务内完成：插入流水 → 计算双轨余额 → 更新余额，使用数据库行锁保证一致性 |
| **科目过滤** | 规则引擎采用简单的科目编码匹配（JSON 数组 `filter_subjects`），无需复杂规则引擎 |
| **PDF 生成** | iText/FlyingSaucer，模板化生成"银行对账单"样式的 PDF |
| **AI 外呼** | 对接第三方 AI 外呼平台（如百度智能外呼/阿里云语音），通过 SPI 接口抽象，可替换 |
| **电子签章** | 复用 v1 的 e签宝/法大大对接 |
| **消息推送** | RabbitMQ 异步发送，支持 SMS/微信模板消息/站内消息三通道 |

### 6.2 前端架构

| 项目 | 建议 |
|------|------|
| **技术栈** | Vue3 + TypeScript + Vite + Element Plus + ECharts（与 v1 一致） |
| **状态管理** | Pinia |
| **路由** | Vue Router，根据角色动态菜单 |
| **消息中心** | WebSocket 实时推送 + 轮询兜底 |
| **移动端** | 继续 H5 响应式适配方案 |

### 6.3 与 v1 的代码复用

| 可复用 | 需重写 | 新增 |
|--------|--------|------|
| 用户认证体系 | 对账单数据模型 | 往来流水模型 |
| 电子签章对接 | 对账单生成逻辑 | 科目过滤引擎 |
| ERP 适配器框架 | 状态机（去掉自动确认） | 余额快照机制 |
| 通知发送基础设施 | 催收模块（从评分变为工单） | 催收工单模型 |
| 前端 Layout/Auth | 对账单详情页 | AI 外呼对接 |
| Analytics SDK | Guest 页面（从明细变为流水） | 调度引擎 |
| 安全体系(TenantUtil等) | Dashboard | 消息中心 |

---

## 七、实施建议

### 7.1 分期路线

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| **P0 — 核心对账** | 往来流水模型 + 对账单生成 + 科目过滤 + 双轨余额 + 发送/确认 | 🔴 最高 |
| **P1 — 调度与周期** | 固定/滚动周期配置 + 自动生成调度器 | 🔴 最高 |
| **P2 — 确认与异议** | 电子签章 + 手机验证 + 视同确认标记 + 异议处理 + 修订 | 🟡 高 |
| **P3 — 催收体系** | 催收工单 + 阶梯催收 + 通知提醒 + 跟进记录 + 核销 | 🟡 高 |
| **P4 — AI 外呼** | 外呼平台对接 + 话术配置 + 通话记录 | 🟢 中 |
| **P5 — 消息中心** | 多端消息推送 + 消息中心页面 + WebSocket | 🟢 中 |
| **P6 — 报表与看板** | 应收报表 + 账龄分析 + 对账状态看板 | 🔵 低 |

### 7.2 关键设计决策确认清单

请在编码前确认以下决策：

| # | 决策点 | 当前方案 | 需确认 |
|---|--------|---------|--------|
| 1 | 余额精度 | DECIMAL(14,2)（最大千亿级，精确到分） | 是否足够？ |
| 2 | 科目过滤粒度 | 按科目编码匹配（精确匹配） | 是否需要支持通配符？ |
| 3 | 视同确认触发 | 定时任务每天检查，超过 X 天标记 | X 天由企业自定义，默认 0（不启用）？ |
| 4 | 催收工单与对账单关系 | 1 个对账单可产生 1 个催收工单 | 是否需要支持部分催收（1 对多）？ |
| 5 | AI 外呼集成 | 预留 SPI 接口，首期不实现 | 首期是否需要对接？如需要，对接哪家平台？ |
| 6 | 滚动周期计算 | 从上次对账结束日 + N 天 | 遇到周末/节假日是否顺延？ |
| 7 | Excel 导入模板 | 统一模板（日期/单号/摘要/数量/应收/实收） | 是否需要支持多种模板？ |
| 8 | 历史数据迁移 | 首次使用时通过 Excel 批量导入历史流水 | 是否需要从 v1 数据迁移工具？ |
| 9 | 多币种 | 当前仅支持人民币 | 是否需要多币种支持？ |
| 10 | Guest 页面确认方式 | 电子签章优先，备选手机验证码 | 是否需要支持微信小程序确认？ |

---

## 八、与 v1 的兼容性

### 8.1 独立部署 vs 升级

**建议：v2 作为独立系统部署，不在 v1 基础上改造。**

理由：
1. 数据模型差异太大（"明细比对"vs"流水累计"），无法平滑升级
2. 状态机逻辑有根本性差异（去掉自动确认）
3. 催收体系从"评分建议"变为"工单执行"，架构不同
4. v1 的客户如果已在使用，不应被影响

### 8.2 共享基础设施

以下模块可从 v1 代码中提取为独立库复用：
- `auth-common`：认证/授权/TenantUtil
- `sign-sdk`：电子签章对接
- `erp-adapter`：ERP 适配器框架
- `notification-sdk`：通知发送
- `analytics-sdk`：前端埋点 SDK

---

---

## 九、第 1 轮确认后的详细设计增补（v2.1）

### 9.1 决策确认结果

| # | 决策点 | 最终决策 |
|---|--------|---------|
| 1 | 余额精度 | ✅ 保持 `DECIMAL(14,2)` |
| 2 | 科目过滤 | ✅ **需要支持通配符** — 详见 9.2 |
| 3 | 视同确认默认天数 | ✅ 默认 `0`（不启用），由企业自行配置 |
| 4 | 催收工单粒度 | ✅ **需要按客户差异化 + 支持整单催和部分催** — 详见 9.3 ~ 9.5 |
| 5 | AI 外呼平台 | ✅ **首期即对接阿里云智能外呼** — 详见 9.6 |
| 6 | 滚动周期节假日 | ✅ **可配置，默认顺延** — 详见 9.7 |
| 7 | Excel 模板 | ✅ 首期统一模板 |
| 8 | v1 数据迁移 | ✅ **不需要** |
| 9 | 多币种 | 首期仅人民币，v2.x 再考虑 |
| 10 | Guest 页面确认方式 | 电子签章优先 + 手机验证码兜底 |

---

### 9.2 科目过滤：通配符支持

#### 9.2.1 匹配规则

| 语法 | 含义 | 示例 |
|------|------|------|
| 精确匹配 | 完全相同 | `HK001` 只匹配 HK001 |
| `*` 通配符 | 匹配任意数量字符 | `HK*` 匹配 HK001、HK002、HK-回扣 |
| `?` 通配符 | 匹配单个字符 | `HK00?` 匹配 HK001~HK009 |
| `!` 前缀 | 排除（黑名单中的白名单） | `!HK998` 表示 HK998 不过滤（即使其他规则命中） |
| 组合 | 多条规则 OR 逻辑 | `["HK*", "NB*", "!NB001"]` |

#### 9.2.2 规则求值优先级

```
对每笔流水的科目编码进行匹配:
  1. 若命中 "!XXX" 排除规则 → 不过滤（发送给客户）
  2. 否则若命中任何过滤规则 → 过滤（不发送）
  3. 否则 → 不过滤
```

#### 9.2.3 数据库字段调整

`customer_ledger_config.filter_subjects` 字段从 `JSON 数组` 存储规则字符串列表：

```json
["HK*", "NB*", "!NB001", "ZJ-回扣", "调整-*"]
```

后端使用 `AntPathMatcher` 或简单正则实现通配符匹配。

#### 9.2.4 规则变更时的处理

> ⚠️ **规则变更是敏感操作**，会导致余额重算，必须记录审计日志。

变更流程：

```
用户修改过滤规则
    ↓
系统提示: "此变更将影响 XXX 客户的余额计算，是否重新生成余额快照？"
    ↓
选择"是" → 后台任务重新扫描所有历史流水 → 重新计算 internal_balance/external_balance
        → 生成新的余额快照 (type=FILTER_CHANGE)
选择"否" → 仅对未来新流水生效，历史流水维持原状
```

---

### 9.3 客户标签化管理（催收差异化的基础）

#### 9.3.1 客户标签体系

引入**三维客户画像**，作为催收策略匹配的依据：

| 维度 | 说明 | 取值示例 |
|------|------|---------|
| **客户类型** | 业务性质 | 大型终端、经销商、同行、加工厂、贸易商 |
| **客户等级** | 战略重要性 | A（战略客户）/ B（重点客户）/ C（普通客户）/ D（观察客户） |
| **客户标签** | 灵活自定义标签 | VIP、老客户、新客户、高风险、月结、季结、免催 …… |

#### 9.3.2 数据模型

```sql
-- 客户类型字典
CREATE TABLE customer_type_dict (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id   BIGINT NOT NULL,
    type_code       VARCHAR(30) NOT NULL COMMENT '类型编码',
    type_name       VARCHAR(50) NOT NULL COMMENT '类型名称',
    default_profile_id BIGINT COMMENT '默认催收策略ID',
    sort_order      INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_enterprise_code (enterprise_id, type_code)
) COMMENT '客户类型字典';

-- 客户标签字典
CREATE TABLE customer_tag_dict (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id   BIGINT NOT NULL,
    tag_code        VARCHAR(30) NOT NULL,
    tag_name        VARCHAR(50) NOT NULL,
    tag_color       VARCHAR(20) DEFAULT '#409eff',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_enterprise_tag (enterprise_id, tag_code)
) COMMENT '客户标签字典';

-- 扩展 customer_ledger_config，新增维度字段
ALTER TABLE customer_ledger_config
  ADD COLUMN customer_type    VARCHAR(30) DEFAULT '' COMMENT '客户类型编码',
  ADD COLUMN customer_level   VARCHAR(10) DEFAULT 'C' COMMENT 'A/B/C/D',
  ADD COLUMN customer_tags    JSON COMMENT '标签编码列表 ["VIP","MONTHLY"]',
  ADD COLUMN sales_owner_id   BIGINT COMMENT '负责销售员ID(转人工时分配)',
  ADD COLUMN send_call_along  TINYINT DEFAULT 0 COMMENT '发送对账单时是否同时AI外呼(0=否,1=是)';
```

#### 9.3.3 催收策略匹配规则

一个客户的催收策略按以下**优先级**决定（先命中的先生效）：

```
优先级 1: 客户级独立策略 (customer_ledger_config.collection_profile 非空)
优先级 2: 标签匹配策略 (customer_tags 命中的第一个绑定策略)
优先级 3: 类型+等级匹配策略 (客户类型 + 等级 组合)
优先级 4: 企业默认策略 (is_default=1 的策略)
```

---

### 9.4 催收策略配置（差异化重设计）

#### 9.4.1 新的策略配置模型

```sql
-- 催收策略配置（重设计版）
DROP TABLE IF EXISTS collection_profile;

CREATE TABLE collection_profile (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id       BIGINT NOT NULL,
    profile_name        VARCHAR(50) NOT NULL COMMENT '策略名称',
    profile_type        VARCHAR(20) NOT NULL DEFAULT 'TYPE_LEVEL'
        COMMENT 'DEFAULT=企业默认, TYPE_LEVEL=按类型+等级, TAG=按标签, CUSTOMER=客户独立',
    is_default          TINYINT DEFAULT 0,

    -- 适用范围（根据 profile_type 使用不同字段）
    match_customer_type VARCHAR(30) COMMENT 'TYPE_LEVEL 时使用',
    match_customer_level VARCHAR(10) COMMENT 'TYPE_LEVEL 时使用: A/B/C/D',
    match_tag_code      VARCHAR(30) COMMENT 'TAG 时使用',

    -- 催收开关
    collection_enabled  TINYINT DEFAULT 1 COMMENT '是否启用催收(为0=永不催收)',
    collection_mode     VARCHAR(20) DEFAULT 'PARTIAL'
        COMMENT 'FULL=整单催收, PARTIAL=按未收部分催收',

    -- 对账单发送时的即时外呼
    send_call_along     TINYINT DEFAULT 0 COMMENT '发送对账单时同时AI外呼',
    send_call_delay_min INT DEFAULT 30 COMMENT '发送后延迟X分钟外呼',
    send_call_script    VARCHAR(30) DEFAULT 'STATEMENT_NOTIFY'
        COMMENT '话术模板编码: STATEMENT_NOTIFY',

    -- 阶段1: 通知提醒
    stage1_start_day    INT DEFAULT 1 COMMENT '逾期第X天开始(0=对账确认次日)',
    stage1_interval     INT DEFAULT 3 COMMENT '提醒间隔天数',
    stage1_max_count    INT DEFAULT 3 COMMENT '最多提醒次数',
    stage1_channels     VARCHAR(100) DEFAULT 'SMS,WECHAT',
    stage1_quiet_hours  VARCHAR(20) DEFAULT '22:00-08:00' COMMENT '免打扰时段',

    -- 阶段2: AI 外呼
    stage2_enabled      TINYINT DEFAULT 1 COMMENT '是否启用AI外呼阶段',
    stage2_start_day    INT DEFAULT 30 COMMENT '逾期第X天升级',
    stage2_interval     INT DEFAULT 7,
    stage2_max_count    INT DEFAULT 2,
    stage2_script_code  VARCHAR(30) DEFAULT 'COLLECTION_STANDARD'
        COMMENT '话术模板编码',
    stage2_call_window  VARCHAR(20) DEFAULT '09:00-18:00' COMMENT '外呼时段',
    stage2_retry_no_answer INT DEFAULT 3 COMMENT '未接听重试次数',
    stage2_retry_interval INT DEFAULT 30 COMMENT '重试间隔分钟',

    -- 阶段3: 人工跟进
    stage3_start_day    INT DEFAULT 60,
    stage3_auto_assign  TINYINT DEFAULT 1 COMMENT '自动分配给负责销售',
    stage3_urgency      VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL/HIGH/URGENT',

    -- 特殊配置
    skip_holidays       TINYINT DEFAULT 1 COMMENT '节假日是否跳过',
    max_call_per_day    INT DEFAULT 1 COMMENT '每客户每天最多外呼次数',

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,

    INDEX idx_enterprise (enterprise_id),
    INDEX idx_profile_type (profile_type),
    INDEX idx_match (match_customer_type, match_customer_level)
) COMMENT '催收策略配置';
```

#### 9.4.2 典型策略示例

| 策略名 | 类型 | 匹配条件 | 收款开关 | 阶段1 | 阶段2(AI外呼) | 阶段3 | 备注 |
|--------|------|---------|---------|-------|-------------|-------|------|
| **战略大客户** | TYPE_LEVEL | 大型终端 + A | 关闭催收 | - | - | 逾期30天转销售 | 完全人工，不打扰 |
| **重点客户** | TYPE_LEVEL | 大型终端 + B | 开启 | 逾期7天短信 | ❌ 不外呼 | 逾期45天转销售 | 温和 |
| **普通经销商** | TYPE_LEVEL | 经销商 + C | 开启 | 逾期3天+每3天 | 逾期30天，间隔7天，2次 | 逾期60天转人工 | 标准 |
| **高风险客户** | TAG | 高风险 | 开启 | 逾期1天+每天 | 逾期7天，间隔2天，5次 | 逾期20天转人工 | 严格 |
| **同行客户** | TYPE_LEVEL | 同行 + * | 开启 | 逾期5天+每5天 | 逾期20天，间隔5天，3次 | 逾期45天转销售 | 中等 |
| **免催客户** | TAG | 免催 | 关闭 | - | - | - | 完全不催 |

---

### 9.5 催收工单：整单催 vs 部分催

#### 9.5.1 两种催收模式

| 模式 | 触发条件 | 工单粒度 | 催收金额 | 适用场景 |
|------|---------|---------|---------|---------|
| **整单催 (FULL)** | 对账确认后到期未全款 | 每张对账单 1 个工单 | 对账单余额 | 简单场景、A/B级客户 |
| **部分催 (PARTIAL)** | ERP 数据显示对账单中部分单据已收款 | 按未收款单据合并生成工单 | ∑(未收款单据应收) | 精细化管理、C/D级客户 |

#### 9.5.2 部分催的数据来源

关键：**部分催需要知道哪些应收单已经付款、哪些没付款**，这个信息来自 ERP。

设计方案：**流水与应收单关联，付款可反向匹配**

```sql
-- 应收明细表（对账单发送时冻结的应收明细，可与流水关联）
CREATE TABLE receivable_item (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id       BIGINT NOT NULL,
    customer_id         BIGINT NOT NULL,

    -- 应收源单信息
    source_doc_no       VARCHAR(50) NOT NULL COMMENT '源单号(销售单号)',
    source_doc_date     DATE NOT NULL,
    source_summary      VARCHAR(200),

    -- 金额
    original_amount     DECIMAL(14,2) NOT NULL COMMENT '原始应收金额',
    paid_amount         DECIMAL(14,2) DEFAULT 0 COMMENT '已付金额(从ERP同步)',
    remaining_amount    DECIMAL(14,2) NOT NULL COMMENT '剩余应收 = original - paid',

    -- 到期日
    due_date            DATE COMMENT '到期日',
    is_overdue          TINYINT GENERATED ALWAYS AS (CASE WHEN due_date < CURDATE() AND remaining_amount > 0 THEN 1 ELSE 0 END) STORED,

    -- 状态
    status              VARCHAR(20) DEFAULT 'PENDING'
        COMMENT 'PENDING/PARTIAL_PAID/PAID/WRITTEN_OFF',
    last_erp_sync_at    DATETIME COMMENT '最近ERP同步时间',

    -- 关联流水
    txn_id              BIGINT COMMENT '关联的初始应收流水ID',

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,

    INDEX idx_customer (enterprise_id, customer_id),
    INDEX idx_source_doc (source_doc_no),
    INDEX idx_due_date (due_date),
    INDEX idx_status (status)
) COMMENT '应收明细(可回溯付款状态)';

-- 催收工单表增加对应字段
ALTER TABLE collection_order
  ADD COLUMN mode              VARCHAR(20) DEFAULT 'FULL' COMMENT 'FULL=整单催, PARTIAL=部分催',
  ADD COLUMN receivable_items  JSON COMMENT '关联应收明细ID列表(部分催时使用)',
  ADD COLUMN profile_id        BIGINT COMMENT '关联的催收策略ID';
```

#### 9.5.3 催收工单生成流程

```
每日凌晨调度器扫描 (每小时增量扫描一次)
    ↓
从 ERP 增量同步应收付款状态 → 更新 receivable_item.paid_amount
    ↓
筛选所有 remaining_amount > 0 AND is_overdue = 1 的应收明细
    ↓
按 customer_id 分组
    ↓
对每个客户查询其对应的 collection_profile (按 9.3.3 匹配)
    ↓
如果策略 collection_enabled = 0 → 跳过
如果策略 collection_mode = FULL:
    → 查询该客户当前是否已有 ACTIVE 工单
    → 是 → 更新工单金额 = 所有未收应收之和
    → 否 → 创建新工单，金额 = 所有未收应收之和
如果策略 collection_mode = PARTIAL:
    → 为每笔未收应收单独关联到工单
    → 已存在工单则同步（新增/移除已还款的应收）
    ↓
根据逾期天数决定当前 stage (STAGE_1/2/3)
    ↓
根据策略执行对应阶段的动作（发通知/AI外呼/转人工）
```

#### 9.5.4 部分催的对账单说明

> 💡 **重要**：对账单本身仍然是"完整的应收明细"，部分催收只影响"催什么/催多少"，**不影响对账单的对外展示**。

客户仍然看到完整的应收余额 ¥100,000，但系统内部知道：
- 已付 ¥60,000（来自 ERP）
- 应催 ¥40,000（催收工单金额）

---

### 9.6 对账单发送时的即时 AI 外呼跟单

#### 9.6.1 场景

商家配置：某客户 / 某策略下，**发送对账单后 X 分钟内 AI 打电话通知客户**，"您好，您的对账单已发送，请查收并及时确认"。

这与"逾期催收"不同——这是**主动跟单**，用于提高对账单确认率。

#### 9.6.2 配置层级

`send_call_along` 支持在 3 个层级配置，从高到低优先级：

```
1. 客户级 (customer_ledger_config.send_call_along)
2. 策略级 (collection_profile.send_call_along)
3. 企业级默认 (enterprise_settings.default_send_call_along)
```

发送对账单时的实际决策：

```java
boolean shouldCallAlong = 
    customerConfig.sendCallAlong != null ? customerConfig.sendCallAlong :
    matchedProfile.sendCallAlong != null ? matchedProfile.sendCallAlong :
    enterpriseSettings.defaultSendCallAlong;
```

#### 9.6.3 执行流程

```
对账单 status: DRAFT → SENT
    ↓
若 shouldCallAlong = true:
    → 创建一次性外呼任务 (call_task)
    → 延迟 send_call_delay_min 分钟后触发
    → 使用 STATEMENT_NOTIFY 话术
    → 外呼结果记录到 collection_follow_up
    ↓
若外呼中客户表示"未收到" → 系统自动重发 SMS/微信
若客户表示"已收到会确认" → 记录，进入正常等待
若客户提出异议 → 引导线上提交
若无人接听 → 按 stage2_retry_no_answer 重试
```

---

### 9.7 AI 外呼系统设计（对接阿里云）

#### 9.7.1 阿里云智能外呼选型

| 产品 | 说明 | 选型 |
|------|------|------|
| **阿里云语音服务 (Voice Interaction Service)** | 提供 TTS + ASR + 对话流程编排 | ✅ 首选 |
| **阿里云智能对话分析 (SCA)** | 用于外呼后的通话质检和意图分析 | 可选，v2.1 后接 |
| **阿里云语音通知 (Voice Notification)** | 简单播报固定文本 | 备选，作为兜底 |

推荐方案：**智能外呼平台 (SDS - Smart Dialing System)**  
产品地址：`aliyun.com/product/vms-outbound`（智能外呼）

#### 9.7.2 集成架构

```
对账通催收引擎
    ↓ (下单调用)
CallService (SPI 抽象接口)
    ├── AliyunCallProvider   ← 首期实现
    ├── HuaweiCallProvider   ← 预留
    └── TencentCallProvider  ← 预留
    ↓
阿里云智能外呼 SDK
    ↓
执行呼叫 → 播报话术 → 用户回答 → ASR识别 → 意图匹配 → 挂机
    ↓
    ↓ (异步回调 Webhook)
CallCallbackController
    ├── 更新 call_task 状态
    ├── 记录 collection_follow_up
    ├── 更新工单 stage（如客户承诺付款）
    └── 下载录音存 MinIO
```

#### 9.7.3 数据模型

```sql
-- 外呼任务表
CREATE TABLE call_task (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_no             VARCHAR(30) NOT NULL COMMENT '任务号',
    enterprise_id       BIGINT NOT NULL,
    customer_id         BIGINT NOT NULL,

    -- 关联业务
    scene               VARCHAR(30) NOT NULL
        COMMENT 'STATEMENT_NOTIFY=对账通知, COLLECTION=催收, DISPUTE_FOLLOW=异议跟进',
    ref_type            VARCHAR(30) COMMENT 'STATEMENT/COLLECTION_ORDER',
    ref_id              BIGINT,

    -- 呼叫配置
    target_phone        VARCHAR(20) NOT NULL COMMENT '目标手机号(存储时脱敏)',
    target_name         VARCHAR(50) COMMENT '目标联系人',
    script_code         VARCHAR(30) NOT NULL COMMENT '话术模板编码',
    script_variables    JSON COMMENT '话术变量 {customer_name, amount, ...}',
    script_style        VARCHAR(20) DEFAULT 'FRIENDLY' COMMENT 'FRIENDLY/FORMAL/URGENT',

    -- 时间控制
    scheduled_at        DATETIME NOT NULL COMMENT '计划呼叫时间',
    call_window_start   TIME DEFAULT '09:00:00',
    call_window_end     TIME DEFAULT '18:00:00',
    max_retry           INT DEFAULT 3,
    retry_interval_min  INT DEFAULT 30,

    -- 执行状态
    status              VARCHAR(20) DEFAULT 'PENDING'
        COMMENT 'PENDING/CALLING/SUCCESS/NO_ANSWER/FAILED/CANCELLED',
    retry_count         INT DEFAULT 0,
    last_call_at        DATETIME,

    -- 阿里云对接字段
    provider            VARCHAR(20) DEFAULT 'ALIYUN',
    provider_task_id    VARCHAR(100) COMMENT '阿里云任务ID',
    provider_call_id    VARCHAR(100) COMMENT '阿里云通话ID',

    -- 结果字段
    call_duration       INT DEFAULT 0 COMMENT '通话时长(秒)',
    call_result_code    VARCHAR(30) COMMENT '结果代码',
    call_result_intent  VARCHAR(50) COMMENT '识别意图: PROMISE_PAY/REFUSED/NEED_INVOICE/NOT_ME',
    call_transcript     TEXT COMMENT '对话内容',
    record_url          VARCHAR(500) COMMENT '录音URL',

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT DEFAULT 0,

    UNIQUE INDEX uk_task_no (task_no),
    INDEX idx_scheduled (scheduled_at, status),
    INDEX idx_ref (ref_type, ref_id)
) COMMENT 'AI外呼任务';

-- 话术模板
CREATE TABLE call_script_template (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id       BIGINT COMMENT '企业ID(NULL=平台通用)',
    script_code         VARCHAR(30) NOT NULL COMMENT '话术编码',
    script_name         VARCHAR(100) NOT NULL,
    scene               VARCHAR(30) NOT NULL,
    style               VARCHAR(20) DEFAULT 'FRIENDLY',

    -- 阿里云流程 ID (在阿里云控制台配置好流程后，绑定ID)
    aliyun_flow_id      VARCHAR(100) COMMENT '阿里云智能外呼流程ID',

    -- 话术内容(用于本地展示/备份)
    greeting_text       TEXT COMMENT '开场白',
    main_text           TEXT COMMENT '主体话术',
    closing_text        TEXT COMMENT '结束语',
    intent_config       JSON COMMENT '意图分支配置',

    enabled             TINYINT DEFAULT 1,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX uk_enterprise_code (enterprise_id, script_code)
) COMMENT 'AI外呼话术模板';
```

#### 9.7.4 话术模板示例

**场景 1：对账单通知 (STATEMENT_NOTIFY)**

```
[开场白 - FRIENDLY]
您好，我是 {enterprise_name} 的智能助理小对，
您本期的对账单已发送到您的手机，
金额是 {closing_balance} 元，请您注意查收。

[意图分支]
- 客户说"好的" → "感谢您的配合，方便的话请在 X 天内确认，祝生意兴隆。"
- 客户说"没收到" → "抱歉，我马上帮您重新发送。"
- 客户说"有问题/异议" → "好的，您可以点击短信链接在线提交异议，我们会尽快处理。"
- 客户说"打错了/不是本人" → "抱歉打扰，请问 {contact_name} 的联系电话是？"

[结束语]
祝您工作顺利，再见。
```

**场景 2：逾期催收 (COLLECTION_STANDARD)**

```
[开场白 - FORMAL]
您好，我是 {enterprise_name} 的对账系统助理，
根据我们的记录，您有一笔 {overdue_amount} 元的应收款项
已经逾期 {overdue_days} 天，希望您能及时安排付款。

[意图分支]
- "会尽快付" → 询问"预计什么时候能付款？" → 记录 expected_pay_date
- "已经付了" → "好的，请您把付款凭证发给我们的销售员核对。" → 提醒销售核实
- "有异议" → "您可以在对账系统中提交异议，我们会尽快处理。"
- "找错人了" → "抱歉打扰。" → 转人工核实

[结束语]
感谢您的理解和配合，再见。
```

#### 9.7.5 意图识别与后续动作

| 识别意图 | 系统自动动作 |
|---------|-------------|
| `PROMISE_PAY` | 记录 `expected_pay_date`，暂停催收提醒 3 天 |
| `ALREADY_PAID` | 通知销售员核实，暂停催收提醒 1 天 |
| `NEED_INVOICE` | 生成"发票申请"待办分配给销售 |
| `DISPUTE` | 提醒客户在线提交异议，同时通知销售 |
| `NOT_ME/WRONG_NUMBER` | 标记电话有误，转人工核实 |
| `REFUSED` | 直接升级到人工阶段（STAGE_3） |
| `NO_ANSWER` | 按重试策略重试 |

#### 9.7.6 合规与限制

| 项 | 要求 |
|----|------|
| **通话时段** | 严格遵守 09:00-18:00（可配置） |
| **免打扰** | 客户明确拒绝后，加入黑名单 30 天不再外呼 |
| **单日频次** | 单客户单日最多 1 通电话（可配置） |
| **录音合规** | 开场白必须告知"通话将被录音"（阿里云自动播报） |
| **数据存储** | 通话录音保留 6 个月，通话内容脱敏后可长期保留 |
| **敏感号码** | 政府/事业单位号段自动过滤，不外呼 |

#### 9.7.7 成本估算

阿里云智能外呼 计费参考（v2.1 撰写时的公开价格）：

| 费用项 | 单价 |
|--------|------|
| 通话费 | 约 0.10-0.15 元/分钟 |
| 平台服务费 | 约 0.05 元/次呼叫（含未接通） |
| ASR 识别 | 通常包含在通话费中 |

**估算单个客户催收成本**：
- 阶段2 平均 2 次外呼，每次 1-2 分钟
- 单个工单 AI 外呼成本约 ¥0.5-1.0
- 相比人工催收成本（人工电话约 ¥5-10/次），节省 80%+

---

### 9.8 滚动周期节假日处理

#### 9.8.1 配置项

在 `customer_ledger_config` 表增加：

```sql
ALTER TABLE customer_ledger_config
  ADD COLUMN holiday_policy VARCHAR(20) DEFAULT 'POSTPONE'
    COMMENT 'POSTPONE=顺延到下一工作日, ADVANCE=提前到上一工作日, KEEP=不调整';
```

#### 9.8.2 节假日日历

引入国家法定节假日日历表：

```sql
CREATE TABLE holiday_calendar (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_code    VARCHAR(10) DEFAULT 'CN',
    holiday_date    DATE NOT NULL,
    holiday_name    VARCHAR(50),
    day_type        VARCHAR(20) NOT NULL COMMENT 'HOLIDAY=法定假日, WORKDAY=调休上班, WEEKEND=周末',
    year            INT NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_country_date (country_code, holiday_date),
    INDEX idx_year (year)
) COMMENT '节假日日历';
```

日历数据来源：
- **首选**：每年年底手工导入国务院公布的次年节假日安排
- **备选**：对接第三方节假日 API（如聚合数据）
- **兜底**：仅按周末（周六周日）判断

#### 9.8.3 日期计算逻辑

```java
public LocalDate resolveNextReconDate(CustomerLedgerConfig config, LocalDate lastReconDate) {
    LocalDate rawNextDate;

    if (config.getReconMode() == FIXED) {
        // 固定周期：每月X日对上月
        rawNextDate = lastReconDate.plusMonths(1).withDayOfMonth(config.getFixedDay());
    } else {
        // 滚动周期：+N 天
        rawNextDate = lastReconDate.plusDays(config.getRollingDays());
    }

    switch (config.getHolidayPolicy()) {
        case "POSTPONE":
            return holidayService.nextWorkday(rawNextDate);
        case "ADVANCE":
            return holidayService.previousWorkday(rawNextDate);
        case "KEEP":
            return rawNextDate;
        default:
            return rawNextDate;
    }
}
```

---

### 9.9 v2.1 前端页面增补

在原 5.1 页面清单基础上新增：

| 序号 | 路径 | 页面名称 | 功能 |
|------|------|---------|------|
| 20 | `/customers/types` | 客户类型管理 | 类型字典 CRUD |
| 21 | `/customers/tags` | 客户标签管理 | 标签字典 CRUD |
| 22 | `/customers/batch-classify` | 客户批量分类 | 批量分配客户类型/等级/标签 |
| 23 | `/collection/profiles` | 催收策略管理 | 策略列表 + 差异化配置 |
| 24 | `/collection/profiles/:id/edit` | 编辑催收策略 | 三阶段详细配置向导 |
| 25 | `/collection/orders` | 催收工单列表 | 整单/部分催工单 |
| 26 | `/collection/call-tasks` | 外呼任务监控 | 实时外呼任务队列 |
| 27 | `/collection/call-records` | 外呼记录 | 通话记录 + 录音回放 |
| 28 | `/system/call-scripts` | AI话术管理 | 话术模板配置 |
| 29 | `/system/holiday-calendar` | 节假日日历 | 节假日导入/维护 |
| 30 | `/system/aliyun-config` | 阿里云外呼配置 | AK/SK + 号码池 |
| 31 | `/reports/collection` | 催收分析报表 | 催收成功率/回款率/成本 |

---

### 9.10 v2.1 实施路线更新

| 阶段 | 内容 | 依赖 |
|------|------|------|
| **P0 — 核心对账** | 往来流水 + 通配符科目过滤 + 双轨余额 + 对账单生成 | 无 |
| **P1 — 周期调度** | 固定/滚动周期 + 节假日日历 + 顺延逻辑 | P0 |
| **P2 — 确认异议** | 电子签章 + 视同确认标记 + 异议处理 | P0 |
| **P3 — 客户分类** | 客户类型/等级/标签字典 + 客户配置 | P0 |
| **P4 — 应收明细** | ERP 增量同步 receivable_item + 付款状态回写 | P0 |
| **P5 — 催收策略** | 差异化策略配置 + 策略匹配引擎 + 工单生成（整单/部分催） | P3, P4 |
| **P6 — AI 外呼基础** | 阿里云 SDK 集成 + 话术模板 + 外呼任务队列 | P5 |
| **P7 — 发单即时跟单** | 发送对账单时的即时外呼 | P6 |
| **P8 — 催收执行** | 阶段执行器 + Webhook 回调 + 意图识别 | P6 |
| **P9 — 消息中心** | 多端消息推送 + 消息中心页面 | P0 |
| **P10 — 报表看板** | 应收报表 + 催收分析 + 账龄分析 | P5, P8 |

---

### 9.11 待第 2 轮确认的问题

请再确认以下问题，之后我将开始编码：

| # | 问题 | 我的建议 |
|---|------|---------|
| A | 客户类型/等级/标签是否需要从 ERP 同步？ | 建议首期在平台维护，后续可从 ERP 同步 |
| B | ERP 中的付款数据同步频率？ | 建议 T+1 全量 + 每小时增量 |
| C | 阿里云外呼流程配置由谁完成？ | 建议：平台先配置好通用流程，企业可申请自定义流程 |
| D | 外呼失败/拒接是否自动升级到人工？ | 建议：达到重试上限自动升级 |
| E | 是否允许企业自建阿里云账号？ | 建议：首期使用平台账号，企业维度隔离；后续支持企业自建 |
| F | 通话录音存储位置与保留期？ | 建议：MinIO 存储 6 个月，之后仅保留脱敏文本 |
| G | 节假日日历的初始数据？ | 建议：内置 2026-2027 两年数据，之后每年更新 |
| H | 部分催的应收明细是否需要买方可见？ | 建议：仅内部可见，对客户仍展示完整对账单 |
| I | 商家能否临时暂停某个客户的所有催收？ | 建议：客户配置页增加"暂停催收 X 天"按钮 |
| J | AI 外呼命中"承诺付款"后是否给销售发通知？ | 建议：是，实时通知销售员 |

---

*设计文档 v2.1 结束 — 等待第 2 轮确认后开始编码*

