<template>
  <div class="bill-detail-page">
    <div v-loading="loading" class="detail-container">
      <!-- Header -->
      <div class="detail-header">
        <div class="header-left">
          <h2 class="bill-no">{{ bill.billNo ?? '-' }}</h2>
          <el-tag :type="(getStatusTagType(bill.status) as 'success' | 'warning' | 'info' | 'danger')" size="large">{{ getStatusText(bill.status) }}</el-tag>
        </div>
        <div class="header-actions">
          <template v-if="['CREATED', 'GENERATED'].includes(bill.status)">
            <el-button type="primary" @click="handleSend">发送</el-button>
            <el-button @click="handleEdit">编辑</el-button>
            <el-button type="danger" @click="handleVoid">作废</el-button>
          </template>
          <template v-else-if="bill.status === 'PENDING'">
            <el-button type="primary" @click="handleConfirm">确认</el-button>
            <el-button type="warning" @click="handleDispute">提异议</el-button>
            <el-button @click="handleUrge">催促</el-button>
          </template>
          <template v-else-if="bill.status === 'TO_SIGN'">
            <el-button type="primary" @click="handleSign">签章</el-button>
          </template>
          <template v-else-if="['SIGNED', 'COLLECTING', 'COMPLETED'].includes(bill.status)">
            <el-button type="primary" @click="handleDownloadPdf">下载PDF</el-button>
          </template>
        </div>
      </div>

      <!-- Balance summary card -->
      <div class="balance-card">
        <div class="balance-row">
          <span class="label">上期结转</span>
          <span class="value">¥{{ formatAmount(bill.carryOverAmount) }}</span>
        </div>
        <div class="balance-row">
          <span class="label">+ 本期交易</span>
          <span class="value">¥{{ formatAmount(bill.totalAmount) }}</span>
        </div>
        <div class="balance-row">
          <span class="label">- 本期已付</span>
          <span class="value">¥{{ formatAmount(bill.paidAmount) }}</span>
        </div>
        <el-divider />
        <div class="balance-row total">
          <span class="label">= 应付余额</span>
          <span class="value">¥{{ formatAmount(bill.balanceAmount) }}</span>
        </div>
      </div>

      <!-- Info section -->
      <el-descriptions :column="2" border class="info-section">
        <el-descriptions-item label="卖方">{{ bill.sellerName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="买方">{{ bill.buyerName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="对账周期">{{ bill.period ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ bill.createdAt ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="模板">{{ bill.templateName ?? '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="对账明细" name="items">
          <el-table :data="billItems" border style="width: 100%" :span-method="spanMethod">
            <el-table-column prop="contractNo" label="合同号" width="120" />
            <el-table-column prop="orderNo" label="订单号" width="120" />
            <el-table-column prop="deliveryNo" label="发货单号" width="120" />
            <el-table-column prop="productName" label="品名" width="100" />
            <el-table-column prop="spec" label="规格" width="80" />
            <el-table-column prop="material" label="材质" width="80" />
            <el-table-column prop="quantity" label="数量" width="90" align="right" />
            <el-table-column prop="weight" label="重量(吨)" width="100" align="right" />
            <el-table-column prop="unitPrice" label="单价" width="100" align="right" />
            <el-table-column prop="amount" label="金额" width="120" align="right">
              <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="付款信息" name="payments">
          <el-table :data="payments" border style="width: 100%">
            <el-table-column prop="paymentNo" label="付款单号" min-width="140" />
            <el-table-column prop="amount" label="金额" width="120" align="right">
              <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="paymentDate" label="付款日期" width="120" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
          <div class="payment-summary">
            <span>已付款合计: ¥{{ formatAmount(bill.paidAmount) }}</span>
            <span>应付余额: ¥{{ formatAmount(bill.balanceAmount) }}</span>
          </div>
        </el-tab-pane>

        <el-tab-pane label="异议记录" name="disputes">
          <el-table :data="disputes" border style="width: 100%">
            <el-table-column prop="id" label="异议编号" width="100" />
            <el-table-column prop="subject" label="主题" min-width="160" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.status === 'OPEN' ? '待处理' : '已解决' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="签章信息" name="sign">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="卖方签章">
              {{ signStatus?.sellerSigned ? '已签章' : '待签章' }}
            </el-descriptions-item>
            <el-descriptions-item label="买方签章">
              {{ signStatus?.buyerSigned ? '已签章' : '待签章' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="操作日志" name="logs">
          <el-timeline>
            <el-timeline-item
              v-for="log in operationLogs"
              :key="log.id"
              :timestamp="log.createdAt"
              placement="top"
            >
              <div>{{ log.action }}</div>
              <div v-if="log.remark" class="log-remark">{{ log.remark }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBillDetail,
  sendBill,
  confirmBill,
  voidBill,
  generatePdf,
  listDisputes,
  getSignStatus,
} from '@/api/recon'

interface BillDetail {
  id: number
  billNo: string
  status: string
  sellerName?: string
  buyerName?: string
  period?: string
  createdAt?: string
  templateName?: string
  carryOverAmount?: number
  totalAmount?: number
  paidAmount?: number
  balanceAmount?: number
  items?: BillItem[]
  payments?: Payment[]
  operationLogs?: OperationLog[]
}

interface BillItem {
  contractNo: string
  orderNo: string
  deliveryNo: string
  productName: string
  spec: string
  material: string
  quantity: number
  weight: number
  unitPrice: number
  amount: number
}

interface Payment {
  paymentNo: string
  amount: number
  paymentDate: string
  status: string
}

interface OperationLog {
  id: number
  action: string
  remark?: string
  createdAt: string
}

interface SignStatus {
  sellerSigned?: boolean
  buyerSigned?: boolean
}

const route = useRoute()
const billId = computed(() => Number(route.params.id))
const loading = ref(false)
const bill = ref<BillDetail>({
  id: 0,
  billNo: '',
  status: 'CREATED',
})
const activeTab = ref('items')

const billItems = computed(() => bill.value.items ?? [])
const payments = computed(() => bill.value.payments ?? [])
const disputes = ref<{ id: number; subject: string; status: string; createdAt: string }[]>([])
const signStatus = ref<SignStatus | null>(null)
const operationLogs = computed(() => bill.value.operationLogs ?? [])

function formatAmount(val: number | undefined) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function getStatusTagType(status: string) {
  const map: Record<string, string> = {
    CREATED: 'info',
    GENERATED: 'info',
    PENDING: 'warning',
    DISPUTED: 'danger',
    TO_SIGN: '',
    SIGNED: 'success',
    COLLECTING: 'warning',
    COMPLETED: 'success',
    VOIDED: 'info',
  }
  return map[status] ?? 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    CREATED: '已创建',
    GENERATED: '已生成',
    PENDING: '待审核',
    DISPUTED: '异议中',
    TO_SIGN: '待签章',
    SIGNED: '已签章',
    COLLECTING: '催收中',
    COMPLETED: '已完成',
    VOIDED: '已作废',
  }
  return map[status] ?? status
}

function spanMethod({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) {
  if (columnIndex !== 0) return { rowspan: 1, colspan: 1 }
  const items = billItems.value
  if (items.length === 0) return { rowspan: 1, colspan: 1 }
  const contractNo = items[rowIndex]?.contractNo
  if (!contractNo) return { rowspan: 1, colspan: 1 }
  const firstIdx = items.findIndex((i) => i.contractNo === contractNo)
  if (firstIdx !== rowIndex) return { rowspan: 0, colspan: 0 }
  const count = items.filter((i) => i.contractNo === contractNo).length
  return { rowspan: count, colspan: 1 }
}

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getBillDetail(billId.value) as BillDetail
    bill.value = res ?? bill.value
    if (!bill.value.billNo) {
      bill.value = {
        id: billId.value,
        billNo: 'R202503001',
        status: 'PENDING',
        sellerName: '某某钢铁有限公司',
        buyerName: '某某贸易有限公司',
        period: '2025-02-01 ~ 2025-02-28',
        createdAt: '2025-03-17 10:30:00',
        templateName: '标准对账模板',
        carryOverAmount: 0,
        totalAmount: 125800.5,
        paidAmount: 50000,
        balanceAmount: 75800.5,
        items: [
          {
            contractNo: 'HT2025001',
            orderNo: 'DD202503001',
            deliveryNo: 'FH202503001',
            productName: '钢材',
            spec: 'Φ20',
            material: 'Q235',
            quantity: 100,
            weight: 2.5,
            unitPrice: 4500,
            amount: 450000,
          },
          {
            contractNo: 'HT2025001',
            orderNo: 'DD202503002',
            deliveryNo: 'FH202503002',
            productName: '钢材',
            spec: 'Φ25',
            material: 'Q235',
            quantity: 50,
            weight: 1.25,
            unitPrice: 4600,
            amount: 230000,
          },
        ],
        payments: [
          { paymentNo: 'FK202503001', amount: 50000, paymentDate: '2025-03-10', status: '已确认' },
        ],
        operationLogs: [
          { id: 1, action: '创建对账单', createdAt: '2025-03-17 10:30:00' },
          { id: 2, action: '发送给买方', createdAt: '2025-03-17 10:35:00' },
          { id: 3, action: '买方已查看', createdAt: '2025-03-17 11:00:00' },
        ],
      }
    }
  } catch {
    // use mock
    bill.value = {
      id: billId.value,
      billNo: 'R202503001',
      status: 'PENDING',
      sellerName: '某某钢铁有限公司',
      buyerName: '某某贸易有限公司',
      period: '2025-02-01 ~ 2025-02-28',
      createdAt: '2025-03-17 10:30:00',
      templateName: '标准对账模板',
      carryOverAmount: 0,
      totalAmount: 125800.5,
      paidAmount: 50000,
      balanceAmount: 75800.5,
      items: [],
      payments: [],
      operationLogs: [],
    }
  } finally {
    loading.value = false
  }
}

async function loadDisputes() {
  try {
    const res = await listDisputes({ billId: billId.value }) as { list?: { id: number; subject: string; status: string; createdAt: string }[] }
    disputes.value = res?.list ?? []
  } catch {
    disputes.value = []
  }
}

async function loadSignStatus() {
  try {
    const res = await getSignStatus(billId.value) as SignStatus
    signStatus.value = res
  } catch {
    signStatus.value = null
  }
}

function handleSend() {
  sendBill(billId.value).then(() => {
    ElMessage.success('发送成功')
    fetchDetail()
  })
}

function handleEdit() {
  ElMessage.info('编辑功能开发中')
}

async function handleVoid() {
  await ElMessageBox.confirm('确定要作废该对账单吗？作废后不可恢复。', '确认作废', {
    type: 'warning',
  })
  await voidBill(billId.value)
  ElMessage.success('已作废')
  fetchDetail()
}

function handleConfirm() {
  confirmBill(billId.value).then(() => {
    ElMessage.success('确认成功')
    fetchDetail()
  })
}

function handleDispute() {
  ElMessage.info('请前往异议列表创建异议')
}

function handleUrge() {
  ElMessage.info('催促已发送')
}

function handleSign() {
  ElMessage.info('请前往签章流程')
}

function handleDownloadPdf() {
  generatePdf(billId.value).then(() => {
    ElMessage.success('PDF生成中，请稍后下载')
  })
}

watch(activeTab, (tab) => {
  if (tab === 'disputes') loadDisputes()
  if (tab === 'sign') loadSignStatus()
})

onMounted(() => {
  fetchDetail()
})
</script>

<style lang="scss" scoped>
.bill-detail-page {
  .detail-container {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    flex-wrap: wrap;
    gap: 16px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .bill-no {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
        color: #303133;
      }
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .balance-card {
    background: #f5f7fa;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 24px;
    max-width: 400px;

    .balance-row {
      display: flex;
      justify-content: space-between;
      padding: 4px 0;

      &.total {
        font-weight: 600;
        font-size: 16px;
        color: #409eff;
      }
    }

    .label {
      color: #606266;
    }
  }

  .info-section {
    margin-bottom: 24px;
  }

  .detail-tabs {
    margin-top: 24px;
  }

  .payment-summary {
    margin-top: 16px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    display: flex;
    gap: 24px;
    font-weight: 500;
  }

  .log-remark {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>
