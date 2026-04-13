# 用户培训手册总览

> 适用系统: 对账通 (AutoRecon) + 提货通 (PickupExpress)  
> 版本: v1.0  
> 更新日期: 2026-03-17

---

## 培训手册清单

### 对账通 (AutoRecon)

| 手册 | 适用角色 | 预计学习时间 | 文件 |
|------|---------|-------------|------|
| **卖方使用培训手册** | 卖方管理员、卖方操作员、卖方财务 | 60-90 分钟 | [autorecon-seller-guide.md](./autorecon-seller-guide.md) |
| **买方使用培训手册** | 买方管理员、买方操作员、免注册买方 | 30 分钟 | [autorecon-buyer-guide.md](./autorecon-buyer-guide.md) |

### 提货通 (PickupExpress)

| 手册 | 适用角色 | 预计学习时间 | 文件 |
|------|---------|-------------|------|
| **卖方使用培训手册** | 卖方管理员、卖方销售员、仓库操作员 | 60-90 分钟 | [pickup-express-seller-guide.md](./pickup-express-seller-guide.md) |
| **买方使用培训手册** | 买方/客户（H5 移动端为主） | 15 分钟 | [pickup-express-buyer-guide.md](./pickup-express-buyer-guide.md) |
| **驾驶员使用指南** | 提货驾驶员 | 10 分钟 | [pickup-express-driver-guide.md](./pickup-express-driver-guide.md) |

---

## 建议培训顺序

### 企业首次上线

```
第1天: 管理员完成系统配置
       ├── 对账通：阅读 卖方手册 第一章~第二章
       └── 提货通：阅读 卖方手册 第一章~第二章

第2天: 操作员培训
       ├── 对账通：卖方手册 第三章（日常对账）
       └── 提货通：卖方手册 第三章~第五章（合同/派车/发货）

第3天: 财务人员培训
       └── 对账通：卖方手册 第四章（财务管理）
       └── 提货通：卖方手册 第六章（结算与证据）

第4天: 买方引导
       ├── 对账通买方：发送手册链接，重点阅读第一章（免注册使用）
       └── 提货通买方：发送手册链接，重点阅读第一章（收到通知）

按需: 驾驶员引导
       └── 提货通驾驶员：发送简版指南，10分钟即可上手
```

### 日常答疑参考

遇到问题时，请先查阅对应手册的「常见问题与故障排除」章节，通常可以找到解答。

---

## 配套资源

| 资源 | 说明 | 文件 |
|------|------|------|
| 系统设计方案（对账通） | 产品与技术设计全文 | [reconciliation-system-design.md](../design/reconciliation-system-design.md) |
| 系统设计方案（提货通） | 产品与技术设计全文 | [evidence-chain-system-design.md](../design/evidence-chain-system-design.md) |
| 卖方销售工具箱（对账通） | 销售话术、FAQ、案例模板 | [seller-guidance-toolkit.md](../design/seller-guidance-toolkit.md) |
| 卖方销售工具箱（提货通） | 销售话术、FAQ、定价 | [pickup-express-toolkit.md](../design/pickup-express-toolkit.md) |
| 用户协议 | 平台使用条款 | [user-agreement.md](../legal/user-agreement.md) |
| 隐私政策 | 数据收集与使用说明 | [privacy-policy.md](../legal/privacy-policy.md) |
| 模块集成指南 | 开发者集成文档 | [module-integration-guide.md](../design/module-integration-guide.md) |
| 埋点分析设计 | 数据分析系统设计 | [analytics-tracking-design.md](../design/analytics-tracking-design.md) |
