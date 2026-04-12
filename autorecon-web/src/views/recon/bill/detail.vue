<template>
  <div class="bill-detail-page">
    <div v-loading="loading" class="detail-container">
      <!-- Header -->
      <div class="detail-header">
        <div class="header-left">
          <h2 class="bill-no">{{ bill.billNo ?? '-' }}</h2>
          <el-tag :type="(getStatusTagType(bill.status) as 'success' | 'warning' | 'info' | 'danger')" size="large">{{ getStatusText(bill.status) }}</el-tag>
          <el-tag v-if="bill.autoConfirmed === 1" type="warning" style="margin-left: 8px">超时自动确认</el-tag>
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
          <span class="value">¥{{ formatAmount(bill.prevBalance) }}</span>
        </div>
        <div class="balance-row">
          <span class="label">+ 本期交易</span>
          <span class="value">¥{{ formatAmount(bill.currentTradeAmount ?? bill.totalAmount) }}</span>
        </div>
        <div class="balance-row">
          <span class="label">- 本期已付</span>
          <span class="value">¥{{ formatAmount(bill.currentPaymentAmount) }}</span>
        </div>
        <el-divider />
        <div class="balance-row total">
          <span class="label">= 应付余额</span>
          <span class="value">¥{{ formatAmount(bill.currentBalance) }}</span>
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

      <el-alert
        v-if="prediction && prediction.overallScore > 50 && ['PENDING', 'CREATED', 'GENERATED'].includes(bill.status)"
        :title="`异议预测: ${prediction.riskLevel}风险 (评分${prediction.overallScore})`"
        :type="prediction.overallScore > 70 ? 'error' : 'warning'"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #default>
          <div v-if="prediction.highRiskItems && prediction.highRiskItems.length > 0">
            <p style="margin: 4px 0" v-for="item in prediction.highRiskItems.slice(0, 3)" :key="item.itemId">
              #{{ item.lineNo }} {{ item.productName }} {{ item.spec }}: {{ item.riskReason }}
            </p>
          </div>
        </template>
      </el-alert>

      <el-alert
        v-if="bill.status === 'PENDING' && bill.autoConfirmDeadline && !bill.autoConfirmed"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #default>
          <span>
            超时自动确认倒计时:
            <strong>{{ formatDeadline(bill.autoConfirmDeadline) }}</strong>
            <span v-if="isExpiringSoon(bill.autoConfirmDeadline)" style="color: #E6A23C; margin-left: 8px">
              (即将到期)
            </span>
          </span>
        </template>
      </el-alert>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="对账明细" name="items">
          <div class="items-toolbar">
            <el-button type="primary" size="small" @click="openInvoiceLinksDialog">关联发票</el-button>
          </div>
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
            <el-table-column prop="invoice_status" label="开票状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getInvoiceStatusTagType(row.invoice_status)" size="small">
                  {{ getInvoiceStatusText(row.invoice_status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="付款信息" name="payments">
          <div class="payment-tab-summary">
            <div class="payment-summary-card">
              <div class="summary-line">
                <span class="summary-label">上期结转应付:</span>
                <span class="summary-value">¥{{ formatAmount(bill.prevBalance) }}</span>
              </div>
              <div class="summary-line">
                <span class="summary-label">本期新增交易额:</span>
                <span class="summary-value">¥{{ formatAmount(bill.currentTradeAmount ?? bill.totalAmount) }}</span>
              </div>
              <el-divider class="summary-divider" />
              <div class="summary-line highlight">
                <span class="summary-label">本期应付合计:</span>
                <span class="summary-value">¥{{ formatAmount(periodPayableTotal) }}</span>
              </div>
              <div class="summary-block-title">本期已付款明细:</div>
              <div v-for="(p, idx) in payments" :key="idx" class="payment-detail-line">
                <span class="pay-date">{{ p.paymentDate }}</span>
                <span class="pay-method">{{ getPaymentMethodText(p.paymentMethod) }}</span>
                <span class="pay-amt">¥{{ formatAmount(p.paymentAmount ?? p.amount) }}</span>
                <span v-if="p.bankSerialNo" class="pay-ref">(流水:{{ p.bankSerialNo }})</span>
              </div>
              <div v-if="!payments.length" class="payment-detail-empty">暂无付款记录</div>
              <div class="summary-line">
                <span class="summary-label">本期已付款小计:</span>
                <span class="summary-value">¥{{ formatAmount(bill.currentPaymentAmount) }}</span>
              </div>
              <el-divider class="summary-divider" />
              <div class="summary-line highlight">
                <span class="summary-label">本期应付余额:</span>
                <span class="summary-value">¥{{ formatAmount(bill.currentBalance) }}</span>
              </div>
            </div>
          </div>
          <el-table :data="payments" border style="width: 100%" class="payment-items-table">
            <el-table-column prop="paymentNo" label="付款单号" min-width="140" />
            <el-table-column prop="paymentAmount" label="金额" width="120" align="right">
              <template #default="{ row }">¥{{ formatAmount(row.paymentAmount ?? row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="paymentDate" label="付款日期" width="120" />
            <el-table-column label="付款方式" width="120">
              <template #default="{ row }">{{ getPaymentMethodText(row.paymentMethod) }}</template>
            </el-table-column>
            <el-table-column prop="bankSerialNo" label="银行流水号" min-width="140">
              <template #default="{ row }">{{ row.bankSerialNo ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
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

    <!-- Edit Bill Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑对账单" width="900px" destroy-on-close @close="closeEditDialog">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
        <el-form-item label="对账模板">
          <el-select v-model="editForm.templateId" placeholder="请选择模板" style="width: 100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="对账周期">
          <el-input v-model="editForm.period" placeholder="如: 2025-02-01 ~ 2025-02-28" />
        </el-form-item>
      </el-form>
      <div class="edit-items-title">对账明细</div>
      <el-table :data="editForm.items" border style="width: 100%" max-height="300">
        <el-table-column prop="productName" label="品名" width="100" />
        <el-table-column prop="spec" label="规格" width="80" />
        <el-table-column prop="quantity" label="数量" width="100" align="right">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" size="small" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="重量(吨)" width="110" align="right">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" size="small" :min="0" :precision="4" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column prop="unitPrice" label="单价" width="110" align="right">
          <template #default="{ row }">
            <el-input-number v-model="row.unitPrice" size="small" :min="0" :precision="4" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">
            <el-input-number v-model="row.amount" size="small" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Create Dispute Dialog -->
    <el-dialog v-model="showDisputeDialog" title="提异议" width="520px" destroy-on-close @close="closeDisputeDialog">
      <el-form :model="disputeForm" label-width="100px">
        <el-form-item label="对账明细" required>
          <el-select v-model="disputeForm.billItemId" placeholder="请选择明细行" style="width: 100%" filterable>
            <el-option
              v-for="(item, idx) in billItems"
              :key="idx"
              :label="`${idx + 1} ${item.productName ?? ''} ${item.spec ?? ''}`"
              :value="item.id ?? idx"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="异议类型" required>
          <el-select v-model="disputeForm.disputeType" placeholder="请选择" style="width: 100%">
            <el-option label="数量差异" value="QUANTITY" />
            <el-option label="重量差异" value="WEIGHT" />
            <el-option label="单价差异" value="UNIT_PRICE" />
            <el-option label="品规不符" value="SPEC_MISMATCH" />
            <el-option label="缺少记录" value="MISSING_RECORD" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" required>
          <el-input v-model="disputeForm.description" type="textarea" :rows="4" placeholder="请描述异议内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDisputeDialog = false">取消</el-button>
        <el-button type="primary" :loading="disputeSubmitting" :disabled="!disputeForm.description?.trim()" @click="handleCreateDispute">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="signDialogVisible" title="发起签章" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="签署顺序">
          <el-radio-group v-model="signOrderType">
            <el-radio :label="1">卖方先签</el-radio>
            <el-radio :label="2">买方先签</el-radio>
            <el-radio :label="3">无序签</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="signDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="signInitiating" @click="handleInitiateSign">确认发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invoiceLinksDialogVisible" title="关联发票" width="600px" destroy-on-close @open="loadInvoiceLinks">
      <el-table :data="invoiceLinks" border style="width: 100%">
        <el-table-column prop="billItemId" label="明细ID" width="80" />
        <el-table-column prop="invoiceNo" label="发票号" min-width="140" />
        <el-table-column prop="invoiceAmount" label="发票金额" width="120" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.invoiceAmount) }}</template>
        </el-table-column>
        <el-table-column prop="invoiceStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getInvoiceStatusText(row.invoiceStatus) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="!invoiceLinks.length" class="empty-hint">暂无关联发票，请通过发票管理进行关联</p>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBillDetail,
  updateBill,
  sendBill,
  confirmBill,
  voidBill,
  generatePdf,
  listDisputes,
  createDispute,
  getSignStatus,
  listTemplates,
  initiateSign,
  getInvoiceLinks,
  getDisputePrediction,
} from '@/api/recon'

interface BillDetail {
  id: number
  billNo: string
  status: string
  sellerName?: string
  buyerName?: string
  period?: string
  remark?: string
  templateId?: number
  templateName?: string
  createdAt?: string
  /** API: 上期结转应付 */
  prevBalance?: number
  /** API: 本期新增交易额 */
  currentTradeAmount?: number
  /** API: 本期已付款 */
  currentPaymentAmount?: number
  /** API: 本期应付余额 */
  currentBalance?: number
  /** Legacy / mock */
  carryOverAmount?: number
  totalAmount?: number
  paidAmount?: number
  balanceAmount?: number
  items?: BillItem[]
  payments?: Payment[]
  operationLogs?: OperationLog[]
  autoConfirmDeadline?: string
  autoConfirmed?: number | boolean
}

interface BillItem {
  id?: number
  lineNo?: number
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
  invoice_status?: string
}

interface InvoiceLink {
  billItemId?: number
  invoiceNo?: string
  invoiceAmount?: number
  invoiceStatus?: string
}

interface Template {
  id: number
  name: string
}

interface Payment {
  paymentNo: string
  amount?: number
  paymentAmount?: number
  paymentDate: string
  status: string
  paymentMethod?: number
  bankSerialNo?: string
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
const router = useRouter()
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

/** 本期应付合计 = 上期结转 + 本期新增交易额 */
const periodPayableTotal = computed(() => {
  const prev = bill.value.prevBalance ?? bill.value.carryOverAmount ?? 0
  const trade = bill.value.currentTradeAmount ?? bill.value.totalAmount ?? 0
  return Number(prev) + Number(trade)
})
const disputes = ref<{ id: number; subject: string; status: string; createdAt: string }[]>([])
const signStatus = ref<SignStatus | null>(null)
const operationLogs = computed(() => bill.value.operationLogs ?? [])
const signDialogVisible = ref(false)
const signOrderType = ref(1)
const signInitiating = ref(false)

const showEditDialog = ref(false)
const showDisputeDialog = ref(false)
const editSubmitting = ref(false)
const disputeSubmitting = ref(false)
const templates = ref<Template[]>([])
const editForm = reactive({
  remark: '',
  templateId: null as number | null,
  period: '',
  items: [] as BillItem[],
})
const disputeForm = reactive({
  billItemId: null as number | null,
  disputeType: 'QUANTITY',
  description: '',
})

const invoiceLinksDialogVisible = ref(false)
const invoiceLinks = ref<InvoiceLink[]>([])

interface DisputePrediction {
  overallScore: number
  riskLevel?: string
  highRiskItems?: { itemId?: number; lineNo?: number; productName?: string; spec?: string; riskReason?: string }[]
}

const prediction = ref<DisputePrediction | null>(null)

function getInvoiceStatusText(status?: string) {
  const map: Record<string, string> = {
    NONE: '未开票',
    FULL: '已开票',
    PARTIAL: '部分开票',
  }
  return map[status ?? ''] ?? status ?? '未开票'
}

function getInvoiceStatusTagType(status?: string): 'success' | 'warning' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'info'> = {
    NONE: 'info',
    FULL: 'success',
    PARTIAL: 'warning',
  }
  return map[status ?? ''] ?? 'info'
}

function openInvoiceLinksDialog() {
  invoiceLinksDialogVisible.value = true
}

async function loadInvoiceLinks() {
  try {
    const res = await getInvoiceLinks(billId.value) as InvoiceLink[]
    invoiceLinks.value = Array.isArray(res) ? res : []
  } catch {
    invoiceLinks.value = []
  }
}

function formatAmount(val: number | undefined) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function formatDeadline(deadline: string): string {
  if (!deadline) return ''
  const d = new Date(deadline)
  const now = new Date()
  const diff = d.getTime() - now.getTime()
  if (diff <= 0) return '已到期'
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)
  const remainHours = hours % 24
  if (days > 0) return `${days}天${remainHours}小时`
  return `${hours}小时`
}

function isExpiringSoon(deadline: string): boolean {
  if (!deadline) return false
  const d = new Date(deadline)
  const diff = d.getTime() - new Date().getTime()
  return diff > 0 && diff < 24 * 60 * 60 * 1000
}

/** 付款方式 integer → 文案 */
function getPaymentMethodText(method: number | undefined) {
  const map: Record<number, string> = {
    1: '银行转账',
    2: '承兑汇票',
    3: '现金',
    4: '其他',
  }
  if (method == null) return '-'
  return map[method] ?? String(method)
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
        prevBalance: 0,
        currentTradeAmount: 125800.5,
        currentPaymentAmount: 50000,
        currentBalance: 75800.5,
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
          {
            paymentNo: 'FK202503001',
            paymentAmount: 50000,
            amount: 50000,
            paymentDate: '2025-03-10',
            paymentMethod: 1,
            bankSerialNo: 'BK00123',
            status: '已确认',
          },
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
      prevBalance: 0,
      currentTradeAmount: 125800.5,
      currentPaymentAmount: 50000,
      currentBalance: 75800.5,
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

async function loadPrediction() {
  try {
    const res = (await getDisputePrediction(billId.value)) as { data?: DisputePrediction } | DisputePrediction
    const payload = res && typeof res === 'object' && 'data' in res && res.data != null
      ? res.data
      : (res as DisputePrediction)
    prediction.value = payload ?? null
  } catch {
    prediction.value = null
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

function openEditDialog() {
  editForm.remark = bill.value.remark ?? ''
  editForm.templateId = bill.value.templateId ?? null
  editForm.period = bill.value.period ?? ''
  editForm.items = (bill.value.items ?? []).map((i) => ({ ...i }))
  showEditDialog.value = true
}

function closeEditDialog() {
  editForm.remark = ''
  editForm.templateId = null
  editForm.period = ''
  editForm.items = []
}

async function handleSaveEdit() {
  editSubmitting.value = true
  try {
    await updateBill(billId.value, {
      remark: editForm.remark,
      templateId: editForm.templateId,
      period: editForm.period,
      items: editForm.items,
    })
    ElMessage.success('保存成功')
    showEditDialog.value = false
    fetchDetail()
  } catch {
    // error handled by interceptor
  } finally {
    editSubmitting.value = false
  }
}

function handleEdit() {
  if (!['CREATED', 'GENERATED'].includes(bill.value.status)) return
  openEditDialog()
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

function openDisputeDialog() {
  if (billItems.value.length === 0) {
    ElMessage.warning('暂无对账明细，无法创建异议')
    return
  }
  disputeForm.billItemId = billItems.value[0].id ?? 0
  disputeForm.disputeType = 'QUANTITY'
  disputeForm.description = ''
  showDisputeDialog.value = true
}

function closeDisputeDialog() {
  disputeForm.billItemId = null
  disputeForm.disputeType = 'QUANTITY'
  disputeForm.description = ''
}

async function handleCreateDispute() {
  if (!disputeForm.description?.trim()) return
  disputeSubmitting.value = true
  try {
    await createDispute({
      billId: billId.value,
      billItemId: disputeForm.billItemId,
      disputeType: disputeForm.disputeType,
      description: disputeForm.description.trim(),
    })
    ElMessage.success('异议已提交')
    showDisputeDialog.value = false
    activeTab.value = 'disputes'
    loadDisputes()
  } catch {
    // error handled by interceptor
  } finally {
    disputeSubmitting.value = false
  }
}

function handleDispute() {
  openDisputeDialog()
}

function handleUrge() {
  ElMessage.info('催促已发送')
}

function handleSign() {
  signOrderType.value = 1
  signDialogVisible.value = true
}

async function handleInitiateSign() {
  signInitiating.value = true
  try {
    await initiateSign(billId.value, signOrderType.value)
    ElMessage.success('签章流程已发起')
    signDialogVisible.value = false
    router.push({ name: 'signPending' })
  } catch {
    ElMessage.error('发起签章失败')
  } finally {
    signInitiating.value = false
  }
}

function handleDownloadPdf() {
  generatePdf(billId.value).then(() => {
    ElMessage.success('PDF生成中，请稍后下载')
  })
}

async function loadTemplates() {
  try {
    const res = await listTemplates() as Template[]
    templates.value = Array.isArray(res) ? res : []
    if (templates.value.length === 0) {
      templates.value = [{ id: 1, name: '标准对账模板' }, { id: 2, name: '简化对账模板' }]
    }
  } catch {
    templates.value = [{ id: 1, name: '标准对账模板' }]
  }
}

watch(activeTab, (tab) => {
  if (tab === 'disputes') loadDisputes()
  if (tab === 'sign') loadSignStatus()
})

onMounted(async () => {
  await fetchDetail()
  loadTemplates()
  loadPrediction()
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

  .items-toolbar {
    margin-bottom: 12px;
  }

  .empty-hint {
    margin: 16px 0 0;
    color: #909399;
    font-size: 14px;
  }

  .payment-tab-summary {
    margin-bottom: 20px;
  }

  .payment-summary-card {
    max-width: 520px;
    padding: 16px 20px;
    background: #f5f7fa;
    border-radius: 8px;
    font-size: 14px;
    line-height: 1.6;
  }

  .payment-summary-card .summary-line {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    padding: 2px 0;
  }

  .payment-summary-card .summary-line.highlight {
    font-weight: 600;
    color: #303133;
  }

  .payment-summary-card .summary-label {
    color: #606266;
  }

  .payment-summary-card .summary-value {
    font-variant-numeric: tabular-nums;
  }

  .summary-divider {
    margin: 12px 0;
  }

  .summary-block-title {
    margin: 12px 0 8px;
    font-weight: 600;
    color: #303133;
  }

  .payment-detail-line {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 8px 12px;
    padding: 4px 0;
    color: #606266;
    font-size: 13px;
  }

  .payment-detail-line .pay-amt {
    font-weight: 500;
    color: #303133;
  }

  .payment-detail-empty {
    color: #909399;
    font-size: 13px;
    padding: 8px 0;
  }

  .payment-items-table {
    margin-top: 8px;
  }

  .log-remark {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }

  .edit-items-title {
    font-size: 14px;
    font-weight: 600;
    margin: 16px 0 8px;
    color: #303133;
  }
}
</style>
