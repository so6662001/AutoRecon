<template>
  <div class="payment-list-page">
    <h2 class="page-title">付款管理</h2>

    <el-form :model="searchForm" inline class="search-form">
      <el-form-item label="买方/卖方">
        <el-input
          v-model="searchForm.party"
          placeholder="付款方或收款方"
          clearable
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item label="付款方式">
        <el-select v-model="searchForm.method" placeholder="全部" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="银行转账" value="BANK" />
          <el-option label="承兑汇票" value="ACCEPTANCE" />
          <el-option label="现金" value="CASH" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="action-bar">
      <el-button type="primary" @click="handleCreate">登记付款</el-button>
      <el-select v-model="batchStrategy" style="width: 120px; margin-left: 12px" placeholder="策略">
        <el-option label="FIFO" :value="1" />
        <el-option label="按比例" :value="3" />
      </el-select>
      <el-button type="primary" plain style="margin-left: 8px" @click="handleBatchAutoAllocate">
        批量自动抵扣
      </el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
      <el-table-column prop="paymentNo" label="付款流水号" min-width="140" />
      <el-table-column prop="payer" label="付款方" min-width="140" />
      <el-table-column prop="payee" label="收款方" min-width="140" />
      <el-table-column prop="paymentDate" label="付款日期" width="120" />
      <el-table-column prop="amount" label="付款金额(¥)" width="120" align="right">
        <template #default="{ row }">
          ¥{{ formatAmount(row.amount) }}
        </template>
      </el-table-column>
      <el-table-column prop="method" label="付款方式" width="110">
        <template #default="{ row }">
          <el-tag :type="(getMethodTagType(row.method) as 'success' | 'warning' | 'info')" size="small">
            {{ getMethodText(row.method) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="allocatedAmount" label="已分配金额" width="120" align="right">
        <template #default="{ row }">
          ¥{{ formatAmount(row.allocatedAmount) }}
        </template>
      </el-table-column>
      <el-table-column prop="unallocatedAmount" label="未分配金额" width="120" align="right">
        <template #default="{ row }">
          ¥{{ formatAmount(row.unallocatedAmount) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag
            :type="(getPaymentStatusTagType(row) as 'success' | 'warning' | 'info' | 'primary')"
            size="small"
          >
            {{ getPaymentStatusText(row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openAllocateDialog(row)">手动分配</el-button>
          <template v-if="Number(row.unallocatedAmount) > 0">
            <el-button type="primary" link size="small" @click="handleRowAutoFifo(row)">FIFO抵扣</el-button>
            <el-button type="primary" link size="small" @click="handleRowAutoProportional(row)">按比例抵扣</el-button>
          </template>
          <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchPayments"
        @current-change="fetchPayments"
      />
    </div>

    <!-- Create payment dialog -->
    <el-dialog v-model="createVisible" title="登记付款" width="500px" @close="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="付款方" prop="payer">
          <el-input v-model="createForm.payer" placeholder="请输入付款方" />
        </el-form-item>
        <el-form-item label="收款方" prop="payee">
          <el-input v-model="createForm.payee" placeholder="请输入收款方" />
        </el-form-item>
        <el-form-item label="日期" prop="paymentDate">
          <el-date-picker
            v-model="createForm.paymentDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="createForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="方式" prop="method">
          <el-select v-model="createForm.method" placeholder="请选择" style="width: 100%">
            <el-option label="银行转账" value="BANK" />
            <el-option label="承兑汇票" value="ACCEPTANCE" />
            <el-option label="现金" value="CASH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="银行流水号">
          <el-input v-model="createForm.bankRef" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Manual allocation dialog -->
    <el-dialog
      v-model="allocateVisible"
      title="手动分配"
      width="720px"
      destroy-on-close
      @close="resetAllocateDialog"
    >
      <div v-loading="allocateLoading" class="allocate-dialog-body">
        <div v-if="allocatePaymentRef" class="allocate-payment-info">
          <div><strong>付款方：</strong>{{ allocatePaymentRef.payer }}</div>
          <div><strong>付款金额：</strong>¥{{ formatAmount(allocatePaymentRef.amount) }}</div>
          <div><strong>未分配：</strong>¥{{ formatAmount(allocatePaymentRef.unallocatedAmount) }}</div>
        </div>
        <el-table :data="allocateRows" border max-height="360" style="width: 100%">
          <el-table-column width="48" align="center">
            <template #header>
              <span />
            </template>
            <template #default="{ row }">
              <el-checkbox v-model="row.checked" />
            </template>
          </el-table-column>
          <el-table-column prop="billNo" label="对账单号" min-width="120" />
          <el-table-column prop="productName" label="品名" min-width="100" />
          <el-table-column prop="spec" label="规格" width="90" />
          <el-table-column label="未付金额" width="120" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.unpaidAmount) }}</template>
          </el-table-column>
          <el-table-column label="分配金额" width="140" align="right">
            <template #default="{ row }">
              <el-input-number
                v-model="row.allocateAmount"
                :min="0"
                :max="row.unpaidAmount"
                :precision="2"
                size="small"
                controls-position="right"
                style="width: 120px"
                :disabled="!row.checked"
              />
            </template>
          </el-table-column>
        </el-table>
        <p v-if="!allocateRows.length && !allocateLoading" class="allocate-empty">未找到未付明细，请确认买卖双方下存在未结清对账单。</p>
      </div>
      <template #footer>
        <el-button @click="allocateVisible = false">取消</el-button>
        <el-button type="primary" :loading="allocateSubmitting" @click="submitAllocate">提交分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listPayments,
  createPayment,
  allocatePayment,
  autoAllocateFIFO,
  autoAllocateProportional,
  batchAutoAllocate,
  queryBills,
  getBillDetail,
} from '@/api/recon'

interface PaymentItem {
  id: number
  paymentNo: string
  payer: string
  payee: string
  paymentDate: string
  amount: number
  method: string
  allocatedAmount: number
  unallocatedAmount: number
  status: string
  payerId?: number
  payeeId?: number
}

interface AllocateRow {
  billId: number
  billNo: string
  billItemId?: number
  productName: string
  spec: string
  unpaidAmount: number
  checked: boolean
  allocateAmount: number
}

const loading = ref(false)
const tableData = ref<PaymentItem[]>([])
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const batchStrategy = ref(1)

const allocateVisible = ref(false)
const allocateLoading = ref(false)
const allocateSubmitting = ref(false)
const allocatePaymentRef = ref<PaymentItem | null>(null)
const allocateRows = ref<AllocateRow[]>([])

const searchForm = reactive({
  party: '',
  method: '',
  dateRange: null as [string, string] | null,
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const createForm = reactive({
  payer: '',
  payee: '',
  paymentDate: '',
  amount: 0,
  method: 'BANK',
  bankRef: '',
  remark: '',
})

const createRules: FormRules = {
  payer: [{ required: true, message: '请输入付款方', trigger: 'blur' }],
  payee: [{ required: true, message: '请输入收款方', trigger: 'blur' }],
  paymentDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  method: [{ required: true, message: '请选择付款方式', trigger: 'change' }],
}

function formatAmount(val: number | undefined) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function getMethodTagType(method: string) {
  const map: Record<string, string> = {
    BANK: '',
    ACCEPTANCE: 'warning',
    CASH: 'success',
    OTHER: 'info',
  }
  return map[method] ?? 'info'
}

function getMethodText(method: string) {
  const map: Record<string, string> = {
    BANK: '银行转账',
    ACCEPTANCE: '承兑汇票',
    CASH: '现金',
    OTHER: '其他',
  }
  return map[method] ?? method
}

type DerivedPayStatus = 'PENDING' | 'PARTIAL' | 'ALLOCATED_FULL'

function resolvePaymentStatus(row: PaymentItem): DerivedPayStatus {
  const unallocated = Number(row.unallocatedAmount)
  const total = Number(row.amount)
  const allocated = total - unallocated
  if (unallocated <= 0 || total <= 0) return 'ALLOCATED_FULL'
  if (allocated <= 0) return 'PENDING'
  return 'PARTIAL'
}

function getPaymentStatusTagType(row: PaymentItem) {
  const s = resolvePaymentStatus(row)
  const map: Record<DerivedPayStatus, string> = {
    PENDING: 'warning',
    PARTIAL: 'primary',
    ALLOCATED_FULL: 'success',
  }
  return map[s] ?? 'info'
}

function getPaymentStatusText(row: PaymentItem) {
  const s = resolvePaymentStatus(row)
  const map: Record<DerivedPayStatus, string> = {
    PENDING: '待分配',
    PARTIAL: '部分分配',
    ALLOCATED_FULL: '已全部分配',
  }
  return map[s] ?? row.status
}

function buildParams() {
  const params: Record<string, unknown> = {
    page: pagination.page,
    pageSize: pagination.pageSize,
  }
  if (searchForm.party) params.party = searchForm.party
  if (searchForm.method) params.method = searchForm.method
  if (searchForm.dateRange?.[0]) params.dateStart = searchForm.dateRange[0]
  if (searchForm.dateRange?.[1]) params.dateEnd = searchForm.dateRange[1]
  return params
}

function unwrapListPayload(res: unknown): { list: Record<string, unknown>[]; total: number } {
  if (res == null) return { list: [], total: 0 }
  if (Array.isArray(res)) {
    return { list: res as Record<string, unknown>[], total: res.length }
  }
  const r = res as Record<string, unknown>
  const inner = (r.data as Record<string, unknown> | undefined) ?? r
  const rawList = inner.list ?? inner.records ?? inner.data
  const list = Array.isArray(rawList) ? (rawList as Record<string, unknown>[]) : []
  const total = Number(inner.total ?? inner.totalCount ?? list.length)
  return { list, total }
}

const methodNumToCode: Record<number, string> = {
  1: 'BANK',
  2: 'ACCEPTANCE',
  3: 'CASH',
  4: 'OTHER',
}

function normalizePaymentRow(raw: Record<string, unknown>): PaymentItem {
  const amount = Number(raw.paymentAmount ?? raw.amount ?? 0)
  const allocated = Number(raw.allocatedAmount ?? 0)
  const unallocated = Number(raw.unallocatedAmount ?? amount - allocated)
  let method = raw.method as string
  if (typeof raw.paymentMethod === 'number') {
    method = methodNumToCode[raw.paymentMethod] ?? String(raw.paymentMethod)
  }
  return {
    id: Number(raw.id),
    paymentNo: String(raw.paymentNo ?? ''),
    payer: String(raw.payerName ?? raw.payer ?? ''),
    payee: String(raw.payeeName ?? raw.payee ?? ''),
    paymentDate: String(raw.paymentDate ?? ''),
    amount,
    method,
    allocatedAmount: allocated,
    unallocatedAmount: unallocated,
    status: String(raw.status ?? ''),
    payerId: raw.payerId != null ? Number(raw.payerId) : undefined,
    payeeId: raw.payeeId != null ? Number(raw.payeeId) : undefined,
  }
}

async function fetchPayments() {
  loading.value = true
  try {
    const res = await listPayments(buildParams())
    const { list, total } = unwrapListPayload(res)
    tableData.value = list.map(normalizePaymentRow)
    pagination.total = total
    if (tableData.value.length === 0 && pagination.total === 0) {
      tableData.value = [
        {
          id: 1,
          paymentNo: 'PM202503170001',
          payer: '某某贸易有限公司',
          payee: '某某钢铁有限公司',
          payerId: 101,
          payeeId: 201,
          paymentDate: '2025-03-15',
          amount: 125800,
          method: 'BANK',
          allocatedAmount: 50000,
          unallocatedAmount: 75800,
          status: 'PARTIAL',
        },
      ]
      pagination.total = 1
    }
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchPayments()
}

function handleReset() {
  searchForm.party = ''
  searchForm.method = ''
  searchForm.dateRange = null
  pagination.page = 1
  fetchPayments()
}

function handleCreate() {
  createVisible.value = true
}

function resetCreateForm() {
  createForm.payer = ''
  createForm.payee = ''
  createForm.paymentDate = ''
  createForm.amount = 0
  createForm.method = 'BANK'
  createForm.bankRef = ''
  createForm.remark = ''
}

async function handleCreateSubmit() {
  await createFormRef.value?.validate()
  try {
    await createPayment({
      payer: createForm.payer,
      payee: createForm.payee,
      paymentDate: createForm.paymentDate,
      amount: createForm.amount,
      method: createForm.method,
      bankRef: createForm.bankRef || undefined,
      remark: createForm.remark || undefined,
    })
    ElMessage.success('登记成功')
    createVisible.value = false
    fetchPayments()
  } catch (e) {
    if (e !== false) throw e
  }
}

async function loadUnpaidRowsForPayment(payment: PaymentItem): Promise<AllocateRow[]> {
  const buyerId = payment.payerId
  const sellerId = payment.payeeId
  if (buyerId == null || sellerId == null) {
    ElMessage.warning('缺少买卖双方ID，无法加载未付明细')
    return []
  }
  const qRes = await queryBills({
    buyerId,
    sellerId,
    pageNum: 1,
    pageSize: 50,
  } as Record<string, unknown>)
  const { list: billRows } = unwrapListPayload(qRes)
  const rows: AllocateRow[] = []
  for (const br of billRows) {
    const billId = Number(br.id)
    const billNo = String(br.billNo ?? '')
    if (!billId) continue
    try {
      const detail = await getBillDetail(billId) as {
        items?: Array<{
          id?: number
          productName?: string
          spec?: string
          unpaidAmount?: number
        }>
      }
      const items = detail?.items ?? []
      for (const it of items) {
        const unpaid = Number(it.unpaidAmount ?? 0)
        if (unpaid <= 0) continue
        rows.push({
          billId,
          billNo,
          billItemId: it.id,
          productName: String(it.productName ?? ''),
          spec: String(it.spec ?? ''),
          unpaidAmount: unpaid,
          checked: false,
          allocateAmount: 0,
        })
      }
    } catch {
      // skip bill
    }
  }
  return rows
}

function resetAllocateDialog() {
  allocatePaymentRef.value = null
  allocateRows.value = []
}

async function openAllocateDialog(row: PaymentItem) {
  allocatePaymentRef.value = row
  allocateVisible.value = true
  allocateLoading.value = true
  allocateRows.value = []
  try {
    allocateRows.value = await loadUnpaidRowsForPayment(row)
  } catch {
    allocateRows.value = []
    ElMessage.error('加载未付明细失败')
  } finally {
    allocateLoading.value = false
  }
}

async function submitAllocate() {
  const payment = allocatePaymentRef.value
  if (!payment) return
  const selected = allocateRows.value.filter((r) => r.checked && Number(r.allocateAmount) > 0)
  if (selected.length === 0) {
    ElMessage.warning('请勾选并填写分配金额')
    return
  }
  const totalAlloc = selected.reduce((s, r) => s + Number(r.allocateAmount), 0)
  if (totalAlloc > Number(payment.unallocatedAmount) + 0.01) {
    ElMessage.warning('分配金额合计不能超过未分配金额')
    return
  }
  allocateSubmitting.value = true
  try {
    await allocatePayment(payment.id, {
      allocations: selected.map((r) => ({
        billId: r.billId,
        billItemId: r.billItemId,
        amount: Number(r.allocateAmount),
      })),
    })
    ElMessage.success('分配成功')
    allocateVisible.value = false
    fetchPayments()
  } catch {
    // interceptor
  } finally {
    allocateSubmitting.value = false
  }
}

async function handleBatchAutoAllocate() {
  try {
    await batchAutoAllocate(batchStrategy.value)
    ElMessage.success('批量自动抵扣已提交')
    fetchPayments()
  } catch {
    // interceptor
  }
}

async function handleRowAutoFifo(row: PaymentItem) {
  try {
    await autoAllocateFIFO(row.id)
    ElMessage.success('FIFO抵扣已执行')
    fetchPayments()
  } catch {
    // interceptor
  }
}

async function handleRowAutoProportional(row: PaymentItem) {
  try {
    await autoAllocateProportional(row.id)
    ElMessage.success('按比例抵扣已执行')
    fetchPayments()
  } catch {
    // interceptor
  }
}

function handleView(item: PaymentItem) {
  ElMessage.info(`查看付款 ${item.paymentNo}`)
}

onMounted(() => {
  fetchPayments()
})
</script>

<style lang="scss" scoped>
.payment-list-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .search-form {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 16px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .action-bar {
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .allocate-dialog-body {
    min-height: 120px;
  }

  .allocate-payment-info {
    margin-bottom: 16px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 6px;
    font-size: 14px;
    line-height: 1.8;
  }

  .allocate-empty {
    margin: 16px 0 0;
    color: #909399;
    font-size: 14px;
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
