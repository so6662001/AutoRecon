# 页面埋点与数据分析系统 — 设计方案

> 版本: v1.0  
> 日期: 2026-03-17  
> 适用范围: 对账通 (AutoRecon) + 提货通 (PickupExpress) 双系统  
> 定位: 产品运营数据基础设施，用于了解用户使用习惯、衡量页面价值、驱动产品迭代决策

---

## 一、设计目标

### 1.1 核心目标

| 目标 | 说明 |
|------|------|
| **用户行为画像** | 了解不同角色（卖方管理员/操作员、买方、仓库员、司机）的使用路径和习惯 |
| **页面价值评估** | 量化每个页面/功能的实际使用频率、停留时长、转化效果 |
| **新功能验证** | 新上线的 12 项高级功能是否被使用、使用深度如何 |
| **运营决策支持** | 产出可视化报表，支撑产品迭代、功能裁剪、资源投入决策 |
| **问题发现** | 通过异常行为数据发现 UX 瓶颈（高跳出、短停留、异常操作路径） |

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| **最小侵入** | 埋点 SDK 以插件形式接入，不改变现有业务代码结构 |
| **隐私合规** | 不采集敏感数据（密码、身份证号等），符合《个人信息保护法》 |
| **性能无感** | 异步批量上报，不阻塞主线程，不影响页面加载和交互性能 |
| **灵活扩展** | 支持声明式（自动）和命令式（手动）两种埋点方式 |
| **跨系统统一** | 两个系统共享同一套埋点 SDK 和数据模型，报表可分系统/合并查看 |

---

## 二、整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端采集层                                │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │ 自动埋点     │  │ 手动埋点     │  │ 性能埋点               │ │
│  │ (路由切换    │  │ (按钮点击    │  │ (FCP/LCP/FID           │ │
│  │  页面可见性  │  │  表单提交    │  │  接口耗时              │ │
│  │  停留时长)   │  │  业务操作)   │  │  页面加载)             │ │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬─────────────┘ │
│         └─────────────────┴──────────────────────┘              │
│                           ↓                                     │
│              ┌─────────────────────────┐                        │
│              │ Analytics SDK (共享)     │                        │
│              │ · 事件队列 + 批量上报    │                        │
│              │ · 会话管理              │                        │
│              │ · 用户/设备指纹         │                        │
│              │ · 采样控制              │                        │
│              └───────────┬─────────────┘                        │
└──────────────────────────┼──────────────────────────────────────┘
                           ↓ HTTP POST (批量, gzip)
┌──────────────────────────┼──────────────────────────────────────┐
│                      后端接收层                                  │
│              ┌───────────┴─────────────┐                        │
│              │ Analytics Gateway       │                        │
│              │ · 数据校验 + 清洗       │                        │
│              │ · 写入消息队列          │                        │
│              └───────────┬─────────────┘                        │
│                          ↓                                      │
│              ┌───────────┴─────────────┐                        │
│              │ MQ (RabbitMQ/Kafka)     │                        │
│              └───────────┬─────────────┘                        │
│                          ↓                                      │
│              ┌───────────┴─────────────┐                        │
│              │ Analytics Worker        │                        │
│              │ · 实时聚合 → Redis      │                        │
│              │ · 离线归档 → ClickHouse │                        │
│              └───────────┬─────────────┘                        │
│                          ↓                                      │
│         ┌────────────────┴────────────────────┐                 │
│         │            存储层                    │                 │
│  ┌──────┴──────┐  ┌─────────────┐  ┌─────────┴──────┐         │
│  │ ClickHouse  │  │ Redis       │  │ MySQL          │         │
│  │ (明细/聚合) │  │ (实时计数)  │  │ (报表配置)     │         │
│  └─────────────┘  └─────────────┘  └────────────────┘         │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌──────────────────────────┼──────────────────────────────────────┐
│                     分析与展示层                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │ 实时看板     │  │ 多维报表     │  │ 自动告警               │ │
│  │ (今日概览    │  │ (页面价值    │  │ (异常流量              │ │
│  │  在线人数    │  │  用户路径    │  │  跳出率飙升            │ │
│  │  热力图)     │  │  留存分析)   │  │  功能使用下降)         │ │
│  └──────────────┘  └──────────────┘  └────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、事件模型设计

### 3.1 统一事件结构

所有埋点事件遵循统一的 JSON 结构：

```json
{
  "eventId": "uuid-v4",
  "eventType": "page_view | click | action | api_call | performance | error",
  "eventName": "recon_bill_list_view",
  "timestamp": 1710648000000,
  "system": "autorecon | pickup_express",
  "session": {
    "sessionId": "sid-xxxx",
    "startTime": 1710647000000,
    "pageCount": 5
  },
  "user": {
    "userId": 1001,
    "enterpriseId": 100,
    "roleType": 1,
    "isGuest": false
  },
  "device": {
    "deviceId": "fingerprint-hash",
    "platform": "web | h5 | mini_program",
    "os": "iOS 17.4",
    "browser": "Chrome 122",
    "screenWidth": 1920,
    "screenHeight": 1080,
    "isMobile": false,
    "userAgent": "Mozilla/5.0 ..."
  },
  "page": {
    "path": "/recon/bills",
    "name": "billList",
    "title": "对账单列表",
    "module": "recon",
    "referrer": "/dashboard",
    "duration": 45200,
    "isFirstVisit": false
  },
  "action": {
    "element": "button",
    "elementId": "btn-send-bill",
    "elementText": "发送",
    "category": "bill_operation",
    "label": "send_bill",
    "value": null,
    "extra": {}
  },
  "performance": {
    "fcp": 820,
    "lcp": 1240,
    "fid": 12,
    "cls": 0.05,
    "ttfb": 120,
    "domReady": 980,
    "loadComplete": 1800
  },
  "network": {
    "type": "4g",
    "downlink": 10.5
  }
}
```

### 3.2 事件类型定义

| 事件类型 | 触发时机 | 采集方式 | 示例 |
|---------|---------|---------|------|
| `page_view` | 路由切换/页面进入 | **自动**（路由守卫） | 进入对账单列表页 |
| `page_leave` | 路由离开/页面关闭 | **自动**（路由守卫 + visibilitychange） | 离开对账单列表页（含停留时长） |
| `click` | 按钮/链接点击 | **自动**（data-track 指令）+ 手动 | 点击"发送对账单"按钮 |
| `action` | 业务操作完成 | **手动**（API 调用后埋点） | 成功创建对账单、签章完成 |
| `search` | 搜索/筛选操作 | **手动** | 使用状态筛选对账单 |
| `api_call` | 接口调用 | **自动**（Axios 拦截器） | POST /api/recon/bills 耗时 320ms |
| `performance` | 页面加载完成 | **自动**（PerformanceObserver） | FCP=820ms, LCP=1240ms |
| `error` | JS 错误/接口错误 | **自动**（window.onerror + 拦截器） | TypeError at bill/list.vue:123 |
| `exposure` | 元素进入可视区 | **自动**（IntersectionObserver + data-track-exposure） | 融资入口 Banner 曝光 |
| `scroll_depth` | 页面滚动深度 | **自动** | 对账单详情页滚动到 75% |
| `tab_switch` | Tab/折叠面板切换 | **手动** | 提货详情页切换到"结算单"Tab |
| `form_submit` | 表单提交 | **手动** | 提交对账单创建表单 |

### 3.3 字段采集规则

#### 隐私保护 — 不采集字段

| 字段 | 原因 |
|------|------|
| 用户密码 | 敏感信息 |
| 手机号明文 | PII，仅采集脱敏格式 138****0000 |
| 身份证号 | PII |
| 银行账号 | PII |
| 对账单具体金额 | 商业机密 |
| 合同具体条款 | 商业机密 |
| 输入框具体内容 | 仅采集字段名和操作类型，不采集内容 |

#### 脱敏规则

- 手机号: `138****0000`
- 企业名: 保留（非敏感）
- 金额: 仅采集金额区间标签（如 `0-10万`, `10-50万`, `50-100万`, `100万+`）

---

## 四、埋点清单

### 4.1 对账通 (AutoRecon) 埋点清单

#### 4.1.1 页面浏览（自动采集）

| 序号 | 页面路径 | 页面名称 | 模块 | 关注指标 |
|------|---------|---------|------|---------|
| A01 | `/login` | 登录页 | auth | 登录成功率、填写时长 |
| A02 | `/guest/:token` | 买方免注册查看 | guest | 验证通过率、确认率、跳出率 |
| A03 | `/dashboard` | 工作台 | core | 日活、首屏时间、待办点击率 |
| A04 | `/recon/bills` | 对账单列表 | recon | 搜索使用率、筛选分布、翻页深度 |
| A05 | `/recon/bills/create` | 发起对账 | recon | 创建完成率、表单放弃率、平均耗时 |
| A06 | `/recon/bills/:id` | 对账单详情 | recon | 停留时长、各Tab查看率、操作分布 |
| A07 | `/recon/batch` | 批量对账 | recon | 使用频率、批量规模分布、成功率 |
| A08 | `/recon/match/:billId` | 比对结果 | recon | 查看时长、异议发起率 |
| A09 | `/recon/disputes` | 异议列表 | dispute | 查看频率、处理时效 |
| A10 | `/recon/disputes/:id` | 异议详情 | dispute | 处理时长、回复次数 |
| A11 | `/recon/sign/pending` | 待签章 | sign | 签章完成率、等待时长 |
| A12 | `/recon/sign/seals` | 印章管理 | sign | 使用频率 |
| A13 | `/recon/templates` | 模板管理 | template | 模板数量、编辑频率 |
| A14 | `/recon/payments` | 付款管理 | finance | 使用频率、分配操作率 |
| A15 | `/recon/collection` | 催收管理 | collection | 使用频率、催收操作率 |
| A16 | `/recon/credit` | 信用评分 | collection | 查看频率、评分分布查看 |
| A17 | `/recon/invoices` | 发票管理 | finance | 使用频率 |
| A18 | `/recon/tri-match/:billId` | 账票款匹配 | finance | 使用频率、匹配成功率 |
| A19 | `/recon/contracts` | 合同管理 | contract | 使用频率 |
| A20 | `/recon/finance` | 融资管理 | finance | 使用频率、申请率 |
| A21 | `/recon/auto-plans` | 自动对账 | auto | 计划创建数、执行成功率 |
| A22 | `/recon/calendar` | 对账日历 | auto | 使用频率、事件查看率 |
| A23 | `/recon/data/upload` | 买方数据上传 | recon | 上传模式分布、成功率 |
| A24 | `/engagement` | 买方引导 | engagement | 使用频率 |
| A25 | `/engagement/materials` | 话术与物料 | engagement | 使用频率、复制率 |
| A26 | `/system/erp` | ERP配置 | system | 配置完成率 |
| A27 | `/system/buyer-config` | 买方配置 | system | 使用频率 |
| A28 | `/system/users` | 用户管理 | system | 使用频率 |
| A29 | `/system/subscriptions` | 提醒订阅 | system | 订阅率、渠道分布 |
| A30 | `/system/billing` | 计费管理 | system | 查看频率 |
| A31 | `/system/enterprise` | 企业信息 | system | 完善率 |
| A32 | `/system/timeout-rules` | 超时确认规则 | system | 配置修改率 |
| A33 | `/system/tolerance` | 容差优化 | system | 建议采纳率 |
| A34 | `/system/recon-rules` | 对账规则 | system | 配置修改率 |
| A35 | `/system/notifications` | 通知模板 | system | 自定义率 |
| A36 | `/system/security` | 数据安全 | system | 查看频率 |
| A37 | `/system/data-auth` | 数据授权 | system | 授权率 |
| A38 | `/system/agreements` | 协议管理 | system | 使用频率 |

#### 4.1.2 关键业务操作（手动埋点）

| 序号 | 事件名称 | 触发时机 | 关键属性 |
|------|---------|---------|---------|
| B01 | `login_success` | 登录成功 | method(密码/SSO), duration |
| B02 | `login_fail` | 登录失败 | reason, attempt_count |
| B03 | `guest_verify` | 买方验证手机号 | success, duration |
| B04 | `guest_confirm` | 买方确认对账单 | action(confirm/dispute), bill_id_hash |
| B05 | `bill_create` | 创建对账单 | item_count, has_contract, buyer_count |
| B06 | `bill_send` | 发送对账单 | send_method(manual/auto), batch_size |
| B07 | `bill_confirm` | 确认对账单 | role(seller/buyer), duration_since_send |
| B08 | `bill_dispute` | 提出异议 | reason_category, amount_range |
| B09 | `bill_sign` | 签章操作 | sign_method(seal/face/sms), duration |
| B10 | `bill_void` | 作废对账单 | reason |
| B11 | `match_execute` | 执行比对 | match_result(full/partial/none), diff_count |
| B12 | `payment_allocate` | 分配付款 | strategy(fifo/manual/proportional), amount_range |
| B13 | `batch_recon_start` | 发起批量对账 | buyer_count, total_items |
| B14 | `auto_plan_create` | 创建自动对账计划 | frequency(daily/weekly/monthly) |
| B15 | `template_create` | 创建模板 | field_count |
| B16 | `template_preview` | 预览模板 | - |
| B17 | `credit_score_view` | 查看信用评分 | buyer_count_viewed |
| B18 | `collection_action` | 执行催收 | method(sms/call/email), buyer_credit_level |
| B19 | `finance_apply` | 融资申请 | amount_range |
| B20 | `invoice_link` | 发票关联 | link_method(manual/auto), count |
| B21 | `erp_pull` | ERP数据拉取 | adapter_type, item_count, success |
| B22 | `data_upload` | 买方上传数据 | mode(excel/online/ocr), row_count, success |
| B23 | `subscription_toggle` | 订阅开关切换 | channel(sms/email/wechat), event_type, enabled |
| B24 | `tolerance_adopt` | 采纳容差建议 | dimension, old_value, new_value |
| B25 | `calendar_event_click` | 日历事件点击 | event_type |
| B26 | `pdf_download` | 下载PDF | source(bill/evidence), role |
| B27 | `search_execute` | 执行搜索 | page, filters_used, result_count |
| B28 | `export_data` | 导出数据 | type(excel/pdf), record_count |

### 4.2 提货通 (PickupExpress) 埋点清单

#### 4.2.1 页面浏览（自动采集）

| 序号 | 页面路径 | 页面名称 | 模块 | 关注指标 |
|------|---------|---------|------|---------|
| P01 | `/login` | 登录页 | auth | 登录成功率 |
| P02 | `/buyer/:token` | 买方提货通知 | guest | 验证率、确认率、停留时长 |
| P03 | `/dashboard` | 工作台 | core | 日活、事件点击率 |
| P04 | `/contract/list` | 合同列表 | contract | 搜索使用率 |
| P05 | `/contract/:id` | 合同详情 | contract | 停留时长 |
| P06 | `/pickup/list` | 提货单列表 | pickup | 筛选分布、翻页 |
| P07 | `/pickup/monitor` | 实时监控 | pickup | 使用频率、刷新频率、停留时长 |
| P08 | `/pickup/:id` | 提货单详情 | pickup | 各Tab查看率、停留时长 |
| P09 | `/pickup/:id/delivery` | 发货进度 | pickup | 刷新频率、停留时长 |
| P10 | `/dispatch/manage` | 派车管理 | dispatch | 操作频率 |
| P11 | `/settlement/list` | 结算列表 | settlement | 使用频率 |
| P12 | `/settlement/:id` | 结算详情 | settlement | 停留时长 |
| P13 | `/evidence/:id` | 证据包查看 | evidence | 使用频率、下载率 |
| P14 | `/warehouse/manage` | 仓库管理 | system | 使用频率 |
| P15 | `/carrier/manage` | 承运公司管理 | system | 使用频率 |
| P16 | `/template/manage` | 合同模板管理 | system | 使用频率 |
| P17 | `/authorized-persons` | 授权提货人 | verification | 使用频率、登记数量 |
| P18 | `/timeout-config` | 确认时效配置 | system | 配置修改率 |
| P19 | `/trading-habits` | 交易习惯 | verification | 查看频率 |
| P20 | `/supplement/manage` | 事后补录 | system | 使用频率、补录量 |
| P21 | `/verification/records` | 确权记录 | verification | 查看频率 |
| P22 | `/system/data-auth` | 数据授权 | system | 使用频率 |

#### 4.2.2 关键业务操作（手动埋点）

| 序号 | 事件名称 | 触发时机 | 关键属性 |
|------|---------|---------|---------|
| Q01 | `contract_create` | 创建合同 | type(reserved/order), item_count |
| Q02 | `contract_sign` | 签约操作 | method(online/offline), duration |
| Q03 | `pickup_create` | 创建提货单 | dispatch_mode(customer/sales/carrier), has_code |
| Q04 | `pickup_dispatch` | 派车分配 | auto_approve, driver_assign_duration |
| Q05 | `pickup_code_verify` | 提货码验证 | success, attempt_count |
| Q06 | `delivery_lift_upload` | 上传吊装数据 | data_source(wms/h5/driver/manual), lift_count |
| Q07 | `delivery_complete` | 发货完成 | total_lifts, total_weight, duration |
| Q08 | `delivery_photo_upload` | 上传现场照片 | photo_type(loading/cargo/plate/weigh), count |
| Q09 | `settlement_generate` | 生成结算单 | amount_range, has_variance |
| Q10 | `evidence_archive` | 证据归档 | item_count, hash_verified |
| Q11 | `evidence_download` | 下载证据包 | format(pdf/zip) |
| Q12 | `buyer_confirm` | 买方确认提货 | from_notification, duration |
| Q13 | `supplement_create` | 事后补录 | record_type, has_photo |
| Q14 | `authorized_person_add` | 添加授权人 | has_id_photo |
| Q15 | `monitor_refresh` | 监控页刷新 | auto_or_manual, interval |
| Q16 | `template_edit` | 编辑合同模板 | template_type, field_count |
| Q17 | `warehouse_delivery_initiate` | 仓库发起发货 | delivery_mode |
| Q18 | `timeout_config_change` | 修改确认时效 | scenario, old_days, new_days |

---

## 五、前端 SDK 设计

### 5.1 SDK 结构

```
src/analytics/
├── index.ts              # 入口 — 初始化 + 导出 API
├── core/
│   ├── tracker.ts        # 核心追踪器 — 事件队列 + 批量上报
│   ├── session.ts        # 会话管理 — 30min 超时刷新
│   ├── device.ts         # 设备信息采集 — UA 解析 + 屏幕
│   └── identity.ts       # 用户身份绑定
├── collectors/
│   ├── page-view.ts      # 页面浏览自动采集（路由守卫）
│   ├── page-leave.ts     # 页面离开 + 停留时长
│   ├── click.ts          # 点击采集（v-track 指令 + 自动）
│   ├── performance.ts    # 性能数据（Web Vitals）
│   ├── api.ts            # 接口调用监控（Axios 拦截器）
│   ├── error.ts          # 错误采集
│   ├── exposure.ts       # 曝光采集（IntersectionObserver）
│   └── scroll.ts         # 滚动深度采集
├── plugins/
│   ├── vue-plugin.ts     # Vue 插件（app.use 安装）
│   └── router-plugin.ts  # Router 插件（自动 PV）
├── directives/
│   └── v-track.ts        # 自定义指令 v-track
├── transport/
│   ├── sender.ts         # HTTP 上报（批量 + gzip + 重试）
│   └── beacon.ts         # sendBeacon 兜底（页面关闭时）
└── types.ts              # TypeScript 类型定义
```

### 5.2 初始化配置

```typescript
// main.ts
import { createAnalytics } from '@/analytics'

const analytics = createAnalytics({
  system: 'autorecon',           // 'autorecon' | 'pickup_express'
  endpoint: '/api/analytics/collect',
  batchSize: 20,                  // 累积 20 条后批量上报
  flushInterval: 10000,           // 或每 10 秒上报一次
  sampleRate: 1.0,                // 采样率 1.0 = 100%
  enablePerformance: true,        // 是否采集 Web Vitals
  enableApiMonitor: true,         // 是否监控接口调用
  enableErrorCapture: true,       // 是否采集 JS 错误
  enableScrollDepth: true,        // 是否采集滚动深度
  enableExposure: true,           // 是否采集曝光
  debug: import.meta.env.DEV,     // 开发模式打印日志
  privacyMode: {
    maskPhone: true,              // 手机号脱敏
    noInputCapture: true,         // 不采集输入内容
    amountAsRange: true,          // 金额转区间
  },
})

app.use(analytics.vuePlugin)
router.use(analytics.routerPlugin)
```

### 5.3 自动埋点机制

#### 5.3.1 页面浏览（PV）— 路由守卫自动采集

```typescript
// routerPlugin 内部逻辑（示意）
router.afterEach((to, from) => {
  // 上报 page_leave（from）
  tracker.track('page_leave', {
    page: { path: from.path, name: from.name, duration: calcDuration() }
  })
  // 上报 page_view（to）
  tracker.track('page_view', {
    page: {
      path: to.path,
      name: to.name as string,
      title: to.meta.title as string,
      module: inferModule(to.path),  // 从路径推断模块
      referrer: from.path,
    }
  })
})
```

#### 5.3.2 点击追踪 — `v-track` 指令

```html
<!-- 声明式用法 -->
<el-button
  v-track="{ event: 'bill_send', category: 'bill_operation', label: 'send' }"
  type="primary"
  @click="handleSend"
>
  发送对账单
</el-button>

<!-- 简写用法（自动取 innerText 作 label） -->
<el-button v-track:bill_void type="danger" @click="handleVoid">
  作废
</el-button>
```

#### 5.3.3 曝光追踪 — `v-track-exposure` 指令

```html
<!-- 元素进入可视区时触发曝光事件 -->
<div v-track-exposure="{ event: 'finance_banner_show', once: true }">
  保理融资入口 Banner
</div>
```

### 5.4 手动埋点 API

```typescript
import { useAnalytics } from '@/analytics'

// Composition API 用法
const analytics = useAnalytics()

// 追踪业务操作
analytics.track('bill_create', {
  item_count: items.length,
  has_contract: !!contractNo,
  buyer_count: 1,
})

// 追踪搜索
analytics.trackSearch('billList', {
  filters: ['status', 'period'],
  resultCount: total,
})

// 追踪表单提交
analytics.trackFormSubmit('bill_create_form', {
  success: true,
  duration: Date.now() - formStartTime,
  fieldCount: 8,
})

// 设置用户属性（登录后调用）
analytics.identify({
  userId: user.id,
  enterpriseId: user.enterpriseId,
  roleType: user.roleType,
})
```

### 5.5 上报策略

| 策略 | 说明 |
|------|------|
| **批量上报** | 事件先进入内存队列，累积 20 条或 10 秒定时器触发批量上报 |
| **即时上报** | `action` 类型事件（如签章完成）和 `error` 事件立即上报 |
| **页面关闭** | `visibilitychange` + `beforeunload` 使用 `sendBeacon` 发送剩余队列 |
| **失败重试** | 上报失败暂存 `localStorage`，下次打开页面自动重发（最多存 200 条） |
| **数据压缩** | 批量上报时对 JSON 进行 gzip 压缩（Content-Encoding: gzip） |
| **采样控制** | 支持全局采样率和按事件类型采样率（如 performance 事件采样 10%） |
| **去重** | 使用 eventId（UUID）去重，防止 sendBeacon 和正常上报重复 |

---

## 六、后端设计

### 6.1 数据接收 API

```
POST /api/analytics/collect
Content-Type: application/json
Content-Encoding: gzip (可选)

Request Body:
{
  "events": [ ... ]   // 事件数组，最大 50 条/请求
}

Response:
{
  "code": 0,
  "received": 20
}
```

#### 接口特性

| 特性 | 说明 |
|------|------|
| 无需认证 | Guest 用户也能上报，使用 deviceId 标识 |
| 限流 | 单 IP 60 次/分钟（每批最多 50 条 = 3000 事件/分钟/IP） |
| 异步处理 | 接收后立即写入 MQ，返回 200 |
| 数据校验 | 校验 eventType、timestamp（±24h）、字段长度 |
| IP 地理定位 | 从请求 IP 解析省/市信息 |

### 6.2 数据存储方案

#### 6.2.1 ClickHouse 表结构

```sql
-- 事件明细表（按天分区，TTL 保留 180 天）
CREATE TABLE analytics_events (
    event_id       String,
    event_type     LowCardinality(String),
    event_name     LowCardinality(String),
    timestamp      DateTime64(3, 'Asia/Shanghai'),
    event_date     Date DEFAULT toDate(timestamp),

    -- 系统标识
    system         LowCardinality(String),  -- 'autorecon' | 'pickup_express'

    -- 用户维度
    user_id        UInt64 DEFAULT 0,
    enterprise_id  UInt64 DEFAULT 0,
    role_type      UInt8 DEFAULT 0,
    is_guest       UInt8 DEFAULT 0,

    -- 会话维度
    session_id     String,

    -- 设备维度
    device_id      String,
    platform       LowCardinality(String),
    os             LowCardinality(String),
    browser        LowCardinality(String),
    screen_width   UInt16 DEFAULT 0,
    screen_height  UInt16 DEFAULT 0,
    is_mobile      UInt8 DEFAULT 0,

    -- 页面维度
    page_path      String,
    page_name      LowCardinality(String),
    page_title     String,
    page_module    LowCardinality(String),
    referrer       String,
    duration       UInt32 DEFAULT 0,

    -- 操作维度
    action_element     String DEFAULT '',
    action_element_id  String DEFAULT '',
    action_category    LowCardinality(String) DEFAULT '',
    action_label       LowCardinality(String) DEFAULT '',
    action_value       String DEFAULT '',
    action_extra       String DEFAULT '',    -- JSON

    -- 性能维度
    perf_fcp       UInt16 DEFAULT 0,
    perf_lcp       UInt16 DEFAULT 0,
    perf_fid       UInt16 DEFAULT 0,
    perf_cls       Float32 DEFAULT 0,

    -- 地理维度
    ip             String DEFAULT '',
    country        LowCardinality(String) DEFAULT '',
    province       LowCardinality(String) DEFAULT '',
    city           LowCardinality(String) DEFAULT ''
)
ENGINE = MergeTree()
PARTITION BY event_date
ORDER BY (system, event_type, event_name, timestamp)
TTL event_date + INTERVAL 180 DAY
SETTINGS index_granularity = 8192;

-- 页面日聚合表（物化视图自动聚合）
CREATE MATERIALIZED VIEW analytics_page_daily_mv
ENGINE = SummingMergeTree()
PARTITION BY event_date
ORDER BY (system, event_date, page_path, page_module)
AS SELECT
    system,
    toDate(timestamp) AS event_date,
    page_path,
    page_name,
    page_module,
    countIf(event_type = 'page_view') AS pv,
    uniqExactIf(user_id, event_type = 'page_view' AND user_id > 0) AS uv,
    uniqExactIf(session_id, event_type = 'page_view') AS sessions,
    avgIf(duration, event_type = 'page_leave' AND duration > 0 AND duration < 3600000) AS avg_duration,
    countIf(event_type = 'page_leave' AND duration < 5000) AS bounce_count,
    avgIf(perf_fcp, perf_fcp > 0) AS avg_fcp,
    avgIf(perf_lcp, perf_lcp > 0) AS avg_lcp
FROM analytics_events
GROUP BY system, event_date, page_path, page_name, page_module;

-- 操作日聚合表
CREATE MATERIALIZED VIEW analytics_action_daily_mv
ENGINE = SummingMergeTree()
PARTITION BY event_date
ORDER BY (system, event_date, event_name, action_category)
AS SELECT
    system,
    toDate(timestamp) AS event_date,
    event_name,
    action_category,
    action_label,
    count() AS action_count,
    uniqExact(user_id) AS action_users,
    uniqExact(enterprise_id) AS action_enterprises
FROM analytics_events
WHERE event_type IN ('action', 'click', 'form_submit')
GROUP BY system, event_date, event_name, action_category, action_label;
```

#### 6.2.2 Redis 实时计数

```
# 实时在线（5分钟窗口）
analytics:online:{system}          → HyperLogLog (session_id)

# 今日 PV/UV
analytics:daily:pv:{system}:{date}  → Integer
analytics:daily:uv:{system}:{date}  → HyperLogLog (user_id)

# 页面实时热度（Sorted Set）
analytics:hot_pages:{system}:{date} → ZSET (page_path -> count)

# 功能使用计数
analytics:action:{system}:{date}:{event_name} → Integer
```

#### 6.2.3 MySQL 配置表

```sql
-- 报表配置
CREATE TABLE analytics_report_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name     VARCHAR(100) NOT NULL COMMENT '报表名称',
    report_type     VARCHAR(50) NOT NULL COMMENT '报表类型: overview|page_value|funnel|retention|path',
    system          VARCHAR(30) COMMENT '所属系统，NULL=全部',
    config_json     JSON COMMENT '报表配置(图表类型、维度、过滤)',
    created_by      BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) COMMENT '报表配置表';

-- 自定义看板
CREATE TABLE analytics_dashboard (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dashboard_name  VARCHAR(100) NOT NULL COMMENT '看板名称',
    layout_json     JSON COMMENT '看板布局',
    owner_id        BIGINT,
    is_public       TINYINT DEFAULT 0 COMMENT '是否公开',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) COMMENT '自定义看板';

-- 自动告警规则
CREATE TABLE analytics_alert_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_name      VARCHAR(100) NOT NULL,
    metric          VARCHAR(100) NOT NULL COMMENT '监控指标',
    condition_type  VARCHAR(20) NOT NULL COMMENT 'gt|lt|change_rate',
    threshold       DECIMAL(10,2) NOT NULL COMMENT '阈值',
    window_minutes  INT DEFAULT 60 COMMENT '检测窗口(分钟)',
    notify_channels VARCHAR(200) COMMENT '通知渠道: sms,email,webhook',
    enabled         TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) COMMENT '告警规则';
```

### 6.3 数据处理流程

```
事件上报 → Gateway 校验
              ↓
         写入 MQ 队列
              ↓
    ┌─────────┴─────────┐
    ↓                   ↓
  实时消费者          离线消费者
    ↓                   ↓
  更新 Redis         写入 ClickHouse
  (在线数/PV/UV      (事件明细)
   热力榜/计数)          ↓
                    物化视图自动聚合
                    (页面日报/操作日报)
                         ↓
                    定时任务 (每日凌晨)
                    · 生成日报/周报
                    · 计算留存/漏斗
                    · 检测告警规则
                    · 清理过期数据
```

---

## 七、分析报表设计

### 7.1 报表体系总览

```
数据分析中心 (新增左侧导航菜单)
├── 📊 实时概览 ................. 今日数据实时大屏
├── 📈 页面价值排行 ............. 全部页面 PV/UV/停留/跳出率排行
├── 🔄 用户路径分析 ............. 桑基图展示用户访问路径
├── 📉 漏斗分析 ................. 关键业务转化漏斗
├── 📅 留存分析 ................. 日/周留存率曲线
├── 🧑‍💻 用户分群 ................ 按角色/行为/活跃度分群
├── 🆕 新功能追踪 ............... 12项高级功能使用追踪
├── 📱 终端分析 ................. PC/H5/小程序分布
├── ⚡ 性能监控 ................. Web Vitals 趋势
└── ⚙️ 埋点管理 ................. 埋点列表/调试/配置
```

### 7.2 各报表详细设计

#### 7.2.1 实时概览

**页面路径**: `/analytics/realtime`

| 区域 | 内容 | 图表类型 |
|------|------|---------|
| 顶部指标卡 | 今日 PV、今日 UV、当前在线、今日事件数、平均停留时长 | 数字卡片（带环比箭头） |
| 实时流量曲线 | 过去 24 小时 PV 趋势（5 分钟粒度） | 折线图 |
| 页面热力榜 | 当前时段 Top 10 热门页面 | 横向柱状图 |
| 操作热力榜 | 当前时段 Top 10 高频操作 | 横向柱状图 |
| 系统分布 | 对账通 vs 提货通流量占比 | 环形图 |
| 终端分布 | PC / H5 / 小程序占比 | 环形图 |

**数据刷新**: 30 秒自动刷新（从 Redis 读取）

#### 7.2.2 页面价值排行

**页面路径**: `/analytics/page-value`

| 列 | 说明 | 排序 |
|----|------|------|
| 页面名称 | 路径 + 中文名 | - |
| 所属模块 | recon / pickup / system 等 | 筛选 |
| PV | 页面浏览量 | ↓ |
| UV | 独立访客数 | ↓ |
| 平均停留时长 | 秒 | ↓ |
| 跳出率 | 停留 <5 秒视为跳出 | ↓ |
| 转化率 | 进入该页后执行核心操作的比例 | ↓ |
| 使用企业数 | 有多少企业使用过 | ↓ |
| 趋势 | 7 日 PV 迷你图（sparkline） | - |
| 价值评分 | 综合评分（算法见下方） | ↓ |

**页面价值评分算法**:

```
价值评分 = 0.25 × UV 归一化
         + 0.20 × 平均停留时长归一化
         + 0.20 × 转化率归一化
         + 0.15 × 使用企业数归一化
         + 0.10 × (1 - 跳出率) 归一化
         + 0.10 × 回访率归一化
```

评分等级：

| 评分区间 | 等级 | 标签颜色 | 建议 |
|---------|------|---------|------|
| 80-100 | S (核心) | 🟢 绿色 | 持续优化性能和体验 |
| 60-79 | A (重要) | 🔵 蓝色 | 保持并适当推广 |
| 40-59 | B (一般) | 🟡 黄色 | 分析原因，考虑优化入口 |
| 20-39 | C (低频) | 🟠 橙色 | 评估是否需要调整定位或合并 |
| 0-19 | D (冷门) | 🔴 红色 | 考虑下线或整合到其他页面 |

支持筛选：日期范围、系统（对账通/提货通）、模块、角色

#### 7.2.3 用户路径分析

**页面路径**: `/analytics/user-path`

| 组件 | 说明 |
|------|------|
| 桑基图 | 展示用户从进入到离开的页面流转路径，节点大小代表流量 |
| 路径筛选 | 可选择起始页面、终止页面、经过页面 |
| 路径排行 | Top 20 最常见路径及占比 |
| 异常路径 | 与典型路径偏差较大的异常路径（可能是 UX 问题） |

典型路径分析视角：

- **卖方典型路径**: 登录 → 工作台 → 对账单列表 → 发起对账 → 对账单详情 → 签章
- **买方典型路径**: 通知链接 → Guest 验证 → 查看对账单 → 确认/异议
- **仓库员路径**: 登录 → 实时监控 → 提货单详情 → 发货进度

#### 7.2.4 漏斗分析

**页面路径**: `/analytics/funnel`

预置漏斗：

**漏斗 1: 对账全流程**

```
创建对账单 → 发送对账单 → 买方查看 → 买方确认 → 签章完成 → 催收(可选)
    100%        92%          78%        65%        58%         30%
```

**漏斗 2: 买方免注册转化**

```
打开 Guest 链接 → 手机验证通过 → 查看对账单 → 确认/异议 → 注册账号
     100%            85%           80%          68%          12%
```

**漏斗 3: 提货全流程**

```
创建合同 → 签约完成 → 派车分配 → 提货码验证 → 发货完成 → 结算生成
  100%      88%        82%        75%         70%        65%
```

**漏斗 4: 买方数据上传**

```
进入上传页 → 选择上传模式 → 开始上传 → 上传成功 → 数据确认
   100%         85%          72%        60%        55%
```

支持自定义漏斗（选择步骤 + 转化窗口）。

#### 7.2.5 留存分析

**页面路径**: `/analytics/retention`

| 维度 | 说明 |
|------|------|
| 日留存 | 第 1/3/7/14/30 天留存率 |
| 周留存 | 第 1/2/3/4 周留存率 |
| 按角色 | 卖方管理员 vs 操作员 vs 买方 |
| 按功能 | 使用某功能后的留存率变化（功能粘性） |

展示形式：留存矩阵热力图 + 留存曲线

#### 7.2.6 用户分群

**页面路径**: `/analytics/segments`

预置分群：

| 分群 | 定义 |
|------|------|
| 高活跃卖方 | 最近 7 天登录 ≥5 次，且有操作行为 |
| 低活跃卖方 | 最近 30 天登录 ≤3 次 |
| 高活跃买方 | 最近 7 天 Guest 访问 ≥3 次 或 登录 ≥2 次 |
| 沉默买方 | 收到通知但最近 30 天未访问 |
| 新注册用户 | 注册 ≤7 天 |
| 高级功能用户 | 使用过 ≥3 项高级功能（自动对账、信用评分、融资等） |
| 仅基础功能用户 | 仅使用对账单创建/查看/确认，未使用高级功能 |
| H5 移动端用户 | 最近 7 天 ≥50% 访问来自移动端 |

支持自定义分群条件（AND/OR 组合）。

#### 7.2.7 新功能追踪

**页面路径**: `/analytics/feature-tracking`

专门追踪 12 项高级功能的采纳情况：

| 功能 | 追踪指标 |
|------|---------|
| 智能催收评分 | 查看次数、使用企业数、评分后催收率 |
| 异议预测 | 预测展示次数、预测准确率（回溯） |
| 自动容差学习 | 建议生成次数、采纳率、采纳后异议率变化 |
| 重量差异归因 | 使用次数、归因分布 |
| 发票关联 | 关联次数、自动匹配率 |
| 保理融资 | 入口曝光次数、申请次数、转化率 |
| 超时自动确认 | 配置企业数、自动确认触发次数 |
| 批量对账 | 使用次数、平均批量规模 |
| 定期自动对账 | 计划创建数、执行次数、成功率 |
| 对账日历 | PV、事件点击率 |
| 移动端H5 | H5 PV 占比、H5 上操作占比 |
| 对账提醒订阅 | 订阅率、各渠道占比 |

展示形式：功能采纳矩阵图（X 轴=使用企业数，Y 轴=人均使用频次，气泡大小=满意度）

#### 7.2.8 终端分析

**页面路径**: `/analytics/device`

| 维度 | 图表 |
|------|------|
| 平台分布 | PC / H5 / 小程序 — 环形图 |
| 操作系统 | Windows / macOS / iOS / Android — 柱状图 |
| 浏览器 | Chrome / Safari / Edge / Firefox / WeChat — 柱状图 |
| 屏幕分辨率 | 分辨率分布 — 表格 |
| 网络类型 | WiFi / 4G / 5G — 环形图 |
| 移动端 vs PC 趋势 | 7/30 天移动端占比趋势 — 折线图 |

#### 7.2.9 性能监控

**页面路径**: `/analytics/performance`

| 指标 | 说明 | 健康阈值 |
|------|------|---------|
| FCP (首次内容绘制) | 用户感知到页面开始加载 | ≤1.8s |
| LCP (最大内容绘制) | 页面主要内容加载完成 | ≤2.5s |
| FID (首次输入延迟) | 页面可交互性 | ≤100ms |
| CLS (累积布局偏移) | 视觉稳定性 | ≤0.1 |
| TTFB (首字节时间) | 服务器响应速度 | ≤600ms |
| 接口平均耗时 | 后端 API 响应时间 | ≤500ms |
| 接口成功率 | 非 5xx 响应占比 | ≥99.5% |

展示：各指标趋势折线图 + 页面维度性能排行 + 慢接口 Top 10

#### 7.2.10 埋点管理

**页面路径**: `/analytics/settings`

| 功能 | 说明 |
|------|------|
| 埋点列表 | 展示所有已注册埋点事件，含事件名、类型、状态、最近触发时间 |
| 埋点调试 | 实时查看上报的原始事件（开发/测试用） |
| 采样配置 | 全局采样率 + 按事件类型采样率 |
| 告警配置 | 设置指标异常告警规则 |
| 数据导出 | 按时间范围导出事件明细（CSV） |

---

## 八、关键分析场景

### 8.1 场景一：新功能 ROI 评估

**问题**: "自动容差学习"功能上线 2 个月了，投入值不值？

**分析路径**:

1. **功能采纳率** = 使用该功能的企业数 / 总活跃企业数
2. **使用深度** = 人均使用次数 / 月
3. **建议采纳率** = 采纳建议数 / 总建议数
4. **业务效果** = 采纳后异议率变化（对比采纳前后 30 天）
5. **结论** = 如果采纳率 >30%、建议采纳率 >50%、异议率下降 >10%，则功能 ROI 正向

### 8.2 场景二：买方转化优化

**问题**: 买方从收到通知到完成确认的转化率只有 58%，瓶颈在哪？

**分析路径**:

1. 查看**漏斗报表**「买方免注册转化」漏斗
2. 定位最大流失步骤（如 "手机验证通过 → 查看对账单" 只有 80%）
3. 用**路径分析**查看未转化用户的行为（是否在验证页停留过长？）
4. 用**终端分析**查看失败是否集中在某个浏览器/设备
5. 用**性能监控**查看 Guest 页面加载是否过慢
6. 得出优化方向（如简化验证流程、优化移动端体验）

### 8.3 场景三：页面精简决策

**问题**: 系统有 38 个页面（对账通），有些可能没人用，是否可以精简？

**分析路径**:

1. 查看**页面价值排行**，按「价值评分」升序
2. 筛选评分 D 级（0-19 分）的页面
3. 分析这些页面的 UV 趋势（是一直低还是在下降？）
4. 查看这些页面的「使用企业数」（是否只有个别企业在用？）
5. 与页面所属功能的业务价值交叉评估
6. 决策：下线 / 合并到其他页面 / 调整入口位置

### 8.4 场景四：H5 移动端价值验证

**问题**: 投入资源做了 H5 移动端适配，效果如何？

**分析路径**:

1. **终端分析** → H5 PV 占总 PV 比例趋势
2. **终端分析** → H5 UV / 总 UV 趋势
3. **页面价值** → 按终端筛选，对比 PC 和 H5 的停留时长、跳出率
4. **漏斗分析** → 对比 H5 和 PC 的转化率差异
5. **分群** → H5 用户的留存率 vs PC 用户
6. 结论：H5 用户占比、转化率、留存是否达到预期

---

## 九、API 接口设计

### 9.1 数据上报接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/analytics/collect` | 批量上报事件 |
| POST | `/api/analytics/heartbeat` | 心跳（更新在线状态） |

### 9.2 报表查询接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/analytics/realtime/overview` | 实时概览数据 |
| GET | `/api/analytics/realtime/online` | 当前在线人数 |
| GET | `/api/analytics/realtime/hot-pages` | 热门页面 Top N |
| GET | `/api/analytics/pages/ranking` | 页面价值排行 |
| GET | `/api/analytics/pages/:path/detail` | 单页面详细数据 |
| GET | `/api/analytics/pages/:path/trend` | 单页面趋势 |
| GET | `/api/analytics/funnel/list` | 漏斗列表 |
| GET | `/api/analytics/funnel/:id/data` | 漏斗数据 |
| POST | `/api/analytics/funnel` | 创建自定义漏斗 |
| GET | `/api/analytics/retention` | 留存分析数据 |
| GET | `/api/analytics/path/flow` | 用户路径桑基图数据 |
| GET | `/api/analytics/path/top` | Top N 路径 |
| GET | `/api/analytics/segments` | 用户分群列表 |
| GET | `/api/analytics/segments/:id/users` | 分群用户列表 |
| GET | `/api/analytics/features/adoption` | 功能采纳矩阵 |
| GET | `/api/analytics/features/:name/trend` | 功能使用趋势 |
| GET | `/api/analytics/device/distribution` | 终端分布 |
| GET | `/api/analytics/device/trend` | 终端趋势 |
| GET | `/api/analytics/performance/overview` | 性能概览 |
| GET | `/api/analytics/performance/pages` | 页面性能排行 |
| GET | `/api/analytics/performance/api-slow` | 慢接口 Top N |
| GET | `/api/analytics/events/debug` | 埋点调试（实时流） |
| GET | `/api/analytics/events/list` | 事件列表 |
| POST | `/api/analytics/events/export` | 导出事件明细 |
| GET | `/api/analytics/alerts` | 告警规则列表 |
| POST | `/api/analytics/alerts` | 创建告警规则 |
| GET | `/api/analytics/dashboard` | 看板配置 |
| PUT | `/api/analytics/dashboard` | 更新看板 |

### 9.3 通用查询参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `system` | string | `autorecon` / `pickup_express` / `all` |
| `startDate` | string | 开始日期 YYYY-MM-DD |
| `endDate` | string | 结束日期 YYYY-MM-DD |
| `granularity` | string | `hour` / `day` / `week` / `month` |
| `roleType` | int | 按角色筛选 |
| `platform` | string | `web` / `h5` / `mini_program` |
| `module` | string | 按模块筛选 |

---

## 十、前端报表页面清单

### 10.1 对账通新增页面

| 序号 | 路径 | 页面名称 | 角色限制 |
|------|------|---------|---------|
| 1 | `/analytics/realtime` | 实时概览 | 管理员、平台 |
| 2 | `/analytics/page-value` | 页面价值排行 | 管理员、平台 |
| 3 | `/analytics/user-path` | 用户路径分析 | 管理员、平台 |
| 4 | `/analytics/funnel` | 漏斗分析 | 管理员、平台 |
| 5 | `/analytics/retention` | 留存分析 | 管理员、平台 |
| 6 | `/analytics/segments` | 用户分群 | 管理员、平台 |
| 7 | `/analytics/feature-tracking` | 新功能追踪 | 管理员、平台 |
| 8 | `/analytics/device` | 终端分析 | 管理员、平台 |
| 9 | `/analytics/performance` | 性能监控 | 管理员、平台 |
| 10 | `/analytics/settings` | 埋点管理 | 平台 |

### 10.2 提货通新增页面

共享同一套报表页面，通过 `system` 参数区分数据。如果提货通独立部署，则将报表页面同样嵌入。

---

## 十一、实施计划

### 11.1 分期实施

| 阶段 | 内容 | 交付物 |
|------|------|--------|
| **P0 — 基础采集** | 前端 SDK（自动 PV + 停留时长 + 错误）+ 后端接收 + ClickHouse 存储 | SDK + Gateway + 明细表 |
| **P1 — 手动埋点** | 关键业务操作埋点（对账通 28 个 + 提货通 18 个） | v-track 指令 + 手动埋点代码 |
| **P2 — 核心报表** | 实时概览 + 页面价值排行 + 漏斗分析 | 3 个报表页面 |
| **P3 — 高级报表** | 路径分析 + 留存 + 分群 + 功能追踪 + 终端 + 性能 | 7 个报表页面 |
| **P4 — 智能化** | 自动告警 + 自定义看板 + 数据导出 | 告警系统 + 看板编辑器 |

### 11.2 技术依赖

| 组件 | 用途 | 是否已有 |
|------|------|---------|
| ClickHouse | 事件明细存储 + 聚合查询 | 新增 |
| Redis | 实时计数 + 在线状态 | 已有 |
| RabbitMQ/Kafka | 事件队列 | 已有 RabbitMQ |
| ECharts | 前端图表（Vue-ECharts） | 已有 |
| Axios | HTTP 上报 | 已有 |

### 11.3 数据量评估

| 指标 | 估算 |
|------|------|
| 日活用户 | 1,000 - 5,000 |
| 人均日 PV | 20 - 50 |
| 人均日事件 | 50 - 150（含自动事件） |
| 日事件总量 | 5 万 - 75 万条 |
| 单条事件大小 | ~500 bytes（压缩后 ~200 bytes） |
| 日存储增量 | 25 MB - 375 MB（ClickHouse 压缩后） |
| 月存储 | 0.75 GB - 11 GB |
| 180 天存储 | 4.5 GB - 67 GB（TTL 自动清理） |

---

## 十二、隐私与合规

### 12.1 法律依据

- 《中华人民共和国个人信息保护法》第 13 条 — 取得用户同意或为履行合同所必需
- 《数据安全法》— 数据分类分级保护
- GB/T 35273-2020《信息安全技术 个人信息安全规范》

### 12.2 合规措施

| 措施 | 实施方式 |
|------|---------|
| 用户知情同意 | 在用户协议和隐私政策中明确说明数据分析用途（已有条款） |
| 数据最小化 | 不采集与分析无关的个人信息 |
| 数据脱敏 | 手机号、金额等敏感字段脱敏处理 |
| 数据存储期限 | 事件明细保留 180 天，聚合数据保留 2 年 |
| 数据访问控制 | 报表页面仅管理员和平台角色可访问 |
| 用户退出权 | 提供关闭数据分析采集的选项（设置页） |
| 数据安全 | HTTPS 传输，ClickHouse 集群内网访问 |

### 12.3 与已有法律文件的衔接

本设计中的数据采集已被现有《用户协议》和《隐私政策》中的以下条款覆盖：

- 用户协议 §7.2「数据使用授权」— 第 5 项「商业洞察」
- 隐私政策 §2.2「信息使用目的」— 第 6 项「业务分析与优化」
- 隐私政策 §2.5「匿名化处理」— 数据去标识化后用于统计分析

---

## 十三、风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| ClickHouse 运维复杂 | 中 | 可先用 MySQL + 定时聚合替代，数据量大后迁移 |
| 埋点漏采/错采 | 高 | P0 阶段完成后进行埋点验证（debug 工具），建立变更 Review 机制 |
| 性能影响 | 中 | 异步批量上报 + 采样控制，上线前压测 |
| 数据量超预期 | 低 | 动态调整采样率 + TTL 缩短 + ClickHouse 水平扩展 |
| 隐私投诉 | 低 | 已有法律文件覆盖 + 提供用户退出机制 |
| 数据准确性 | 中 | 使用 UUID 去重 + 服务端校验 + 定期对账 |

---

## 十四、成功指标

上线 30 天后评估：

| 指标 | 目标 |
|------|------|
| 埋点覆盖率 | ≥95% 页面有 PV 数据 |
| 数据上报成功率 | ≥99.5% |
| 数据延迟 | 实时数据 ≤30 秒，报表数据 ≤5 分钟 |
| 报表加载时间 | ≤3 秒 |
| 产出可执行洞察 | ≥5 条产品优化建议 |
| 发现低价值页面 | 识别 ≥3 个 D 级页面并制定优化计划 |
| 新功能采纳率量化 | 12 项功能均有采纳率数据 |

---

*文档结束*
