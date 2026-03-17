export const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === 'true' && import.meta.env.MODE !== 'production'

export const demoUsers = [
  { username: 'admin', password: 'admin123', realName: '系统管理员', role: '平台运营' },
  { username: 'seller1', password: '123456', realName: '张三(卖方)', role: '卖方管理员' },
  { username: 'buyer1', password: '123456', realName: '李四(买方)', role: '买方管理员' },
  { username: 'buyer2', password: '123456', realName: '王五(买方)', role: '买方管理员' },
]

export const mockDashboard = {
  code: 200,
  data: {
    contractCount: 5,
    pickingCount: 2,
    deliveringCount: 1,
    pendingSettlement: 3,
    totalReceivable: 1580000,
    monthPickupWeight: 520.5,
    recentEvents: [
      { id: '1', eventType: 'DELIVERY_COMPLETED', title: 'TH20260301001 发货完成 60吨', createdAt: '2026-03-01 15:30:00' },
      { id: '2', eventType: 'SETTLEMENT_CREATED', title: 'HT2026-003 结算单已生成 ¥252,000', createdAt: '2026-03-01 16:00:00' },
      { id: '3', eventType: 'PICKUP_ORDER_CREATED', title: 'TH20260310001 提货单已创建', createdAt: '2026-03-10 09:00:00' },
    ]
  }
}

export const mockContracts = {
  code: 200,
  data: {
    records: [
      { id: 1, contractNo: 'HT2026-001', contractType: 1, buyerName: 'YY建设集团', totalWeight: 500, pickedWeight: 0, totalAmount: 2100000, paidAmount: 500000, status: 0, createdAt: '2026-01-15' },
      { id: 2, contractNo: 'HT2026-002', contractType: 2, buyerName: 'ZZ贸易公司', totalWeight: 300, pickedWeight: 0, totalAmount: 1290000, paidAmount: 0, status: 2, createdAt: '2026-02-01' },
      { id: 3, contractNo: 'HT2026-003', contractType: 1, buyerName: 'YY建设集团', totalWeight: 200, pickedWeight: 120, totalAmount: 840000, paidAmount: 504000, status: 3, createdAt: '2026-02-15' },
    ],
    total: 3, size: 20, current: 1, pages: 1
  }
}
