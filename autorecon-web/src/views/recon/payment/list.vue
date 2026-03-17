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
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="(getStatusTagType(row.status) as 'success' | 'warning' | 'info')" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleAllocate(row)">分配</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { listPayments, createPayment } from '@/api/recon'

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
}

const loading = ref(false)
const tableData = ref<PaymentItem[]>([])
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()

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

function getStatusTagType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'warning',
    PARTIAL: '',
    ALLOCATED: 'success',
  }
  return map[status] ?? 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待分配',
    PARTIAL: '部分分配',
    ALLOCATED: '已分配',
  }
  return map[status] ?? status
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

async function fetchPayments() {
  loading.value = true
  try {
    const res = await listPayments(buildParams()) as { list?: PaymentItem[]; total?: number }
    tableData.value = res?.list ?? []
    pagination.total = res?.total ?? 0
    if (tableData.value.length === 0 && pagination.total === 0) {
      tableData.value = [
        {
          id: 1,
          paymentNo: 'PM202503170001',
          payer: '某某贸易有限公司',
          payee: '某某钢铁有限公司',
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

function handleAllocate(_row: PaymentItem) {
  ElMessage.info('分配功能：选择对账单进行分配')
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
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
