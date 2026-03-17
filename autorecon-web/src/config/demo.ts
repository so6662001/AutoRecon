// Demo mode mock data for when backend is not available
// Cannot be enabled in production builds
export const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === 'true' && import.meta.env.MODE !== 'production'

export const demoUsers = [
  { username: 'admin', password: 'admin123', realName: '系统管理员', role: '平台运营' },
  { username: 'seller1', password: '123456', realName: '张三(卖方)', role: '卖方管理员' },
  { username: 'buyer1', password: '123456', realName: '李四(买方)', role: '买方管理员' },
]

// Mock dashboard data
export const mockDashboard = {
  code: 200,
  data: {
    pendingCount: 12,
    disputedCount: 3,
    toSignCount: 8,
    collectingCount: 6,
    completedCount: 45,
    totalReceivable: 2580000,
    totalOverdue: 320000,
    monthlyCompletionRate: 78.5,
    completionRate: 78.5,
    recentTodos: [
      { billNo: 'DZ20260315001', buyerName: 'YY建设集团', action: '提出异议(3项)', time: '2小时前', status: 'DISPUTED' },
      { billNo: 'DZ20260314002', buyerName: 'ZZ贸易公司', action: '等待签章', time: '1天前', status: 'TO_SIGN' },
      { billNo: 'DZ20260310003', buyerName: 'AA工程公司', action: '逾期未付款(7天)', time: '3天前', status: 'COLLECTING' },
    ],
  },
}

// Mock bill list
export const mockBillList = {
  code: 200,
  data: {
    records: [
      { id: 1, billNo: 'DZ20260301001', buyerName: 'YY建设集团', periodStart: '2026-02-01', periodEnd: '2026-02-28', totalAmount: 1480000, currentPaymentAmount: 500000, currentBalance: 980000, status: 'PENDING', createdAt: '2026-03-01 09:00:00' },
      { id: 2, billNo: 'DZ20260301002', buyerName: 'ZZ贸易公司', periodStart: '2026-02-01', periodEnd: '2026-02-28', totalAmount: 820000, currentPaymentAmount: 820000, currentBalance: 0, status: 'SIGNED', createdAt: '2026-03-01 09:30:00' },
      { id: 3, billNo: 'DZ20260301003', buyerName: 'AA工程公司', periodStart: '2026-02-01', periodEnd: '2026-02-28', totalAmount: 2150000, currentPaymentAmount: 1000000, currentBalance: 1150000, status: 'DISPUTED', createdAt: '2026-03-01 10:00:00' },
      { id: 4, billNo: 'DZ20260315001', buyerName: 'BB建材公司', periodStart: '2026-03-01', periodEnd: '2026-03-15', totalAmount: 560000, currentPaymentAmount: 0, currentBalance: 560000, status: 'CREATED', createdAt: '2026-03-15 14:00:00' },
      { id: 5, billNo: 'DZ20260315002', buyerName: 'CC钢构公司', periodStart: '2026-03-01', periodEnd: '2026-03-15', totalAmount: 930000, currentPaymentAmount: 200000, currentBalance: 730000, status: 'TO_SIGN', createdAt: '2026-03-15 15:00:00' },
    ],
    total: 5,
    size: 20,
    current: 1,
    pages: 1,
  },
}

// Mock credit ranking
export const mockCreditRanking = {
  code: 200,
  data: [
    { enterpriseId: 2, buyerName: 'YY建设集团', creditScore: 92.5, scoreLevel: 'A', avgPaymentDays: 12.3, overdueRate: 2.1, disputeRate: 5.0, trend: 1 },
    { enterpriseId: 3, buyerName: 'ZZ贸易公司', creditScore: 85.0, scoreLevel: 'B', avgPaymentDays: 18.5, overdueRate: 5.3, disputeRate: 8.2, trend: 2 },
    { enterpriseId: 4, buyerName: 'AA工程公司', creditScore: 68.2, scoreLevel: 'C', avgPaymentDays: 32.1, overdueRate: 15.8, disputeRate: 12.5, trend: 3 },
    { enterpriseId: 5, buyerName: 'BB建材公司', creditScore: 55.0, scoreLevel: 'D', avgPaymentDays: 45.0, overdueRate: 28.0, disputeRate: 18.0, trend: 3 },
    { enterpriseId: 6, buyerName: 'CC钢构公司', creditScore: 78.8, scoreLevel: 'B', avgPaymentDays: 20.5, overdueRate: 8.5, disputeRate: 6.0, trend: 1 },
  ],
}
