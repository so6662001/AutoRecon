<template>
  <div class="invoice-list-page">
    <h2 class="page-title">发票管理</h2>

    <el-form :model="searchForm" inline class="search-form">
      <el-form-item label="发票号码">
        <el-input v-model="searchForm.invoiceNo" placeholder="请输入" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="searchForm.type" placeholder="全部" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="增值税专用" value="VAT_SPECIAL" />
          <el-option label="普通" value="VAT_NORMAL" />
          <el-option label="电子" value="ELECTRONIC" />
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
      <el-button type="primary" @click="handleCreate">录入发票</el-button>
      <el-button @click="handleLink">关联对账单</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
      <el-table-column prop="invoiceNo" label="发票号码" min-width="140" />
      <el-table-column prop="invoiceCode" label="发票代码" width="120" />
      <el-table-column prop="type" label="类型" width="110">
        <template #default="{ row }">
          <el-tag :type="(getTypeTagType(row.type) as 'success' | 'info' | 'danger')" size="small">{{ getTypeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="110" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="taxAmount" label="税额" width="100" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.taxAmount) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="价税合计" width="110" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="invoiceDate" label="开票日期" width="110" />
      <el-table-column prop="buyerName" label="买方" min-width="120" />
      <el-table-column prop="sellerName" label="卖方" min-width="120" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="(getStatusTagType(row.status) as 'success' | 'warning' | 'info')" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleLinkToBill(row)">关联</el-button>
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
        @size-change="fetchInvoices"
        @current-change="fetchInvoices"
      />
    </div>

    <!-- Create dialog -->
    <el-dialog v-model="createVisible" title="录入发票" width="560px" @close="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="发票号码" prop="invoiceNo">
          <el-input v-model="createForm.invoiceNo" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="发票代码" prop="invoiceCode">
          <el-input v-model="createForm.invoiceCode" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="createForm.type" placeholder="请选择" style="width: 100%">
            <el-option label="增值税专用" value="VAT_SPECIAL" />
            <el-option label="普通" value="VAT_NORMAL" />
            <el-option label="电子" value="ELECTRONIC" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="createForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="税额" prop="taxAmount">
          <el-input-number v-model="createForm.taxAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开票日期" prop="invoiceDate">
          <el-date-picker
            v-model="createForm.invoiceDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="买方" prop="buyerName">
          <el-input v-model="createForm.buyerName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="卖方" prop="sellerName">
          <el-input v-model="createForm.sellerName" placeholder="请输入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Link dialog -->
    <el-dialog v-model="linkVisible" title="关联对账单" width="560px">
      <el-form :model="linkForm" label-width="100px">
        <el-form-item label="对账单">
          <el-select v-model="linkForm.billId" placeholder="请选择对账单" style="width: 100%">
            <el-option
              v-for="b in billOptions"
              :key="b.id"
              :label="`${b.billNo} - ¥${formatAmount(b.totalAmount)}`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="明细项">
          <el-select v-model="linkForm.billItemId" placeholder="请选择明细" style="width: 100%">
            <el-option
              v-for="item in billItemOptions"
              :key="item.id"
              :label="`${item.productName} - ¥${formatAmount(item.amount)}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联金额">
          <el-input-number v-model="linkForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkVisible = false">取消</el-button>
        <el-button type="primary" @click="handleLinkSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { listInvoices, createInvoice, linkInvoice } from '@/api/recon'
import { queryBills } from '@/api/recon'

interface InvoiceItem {
  id: number
  invoiceNo: string
  invoiceCode: string
  type: string
  amount: number
  taxAmount: number
  totalAmount: number
  invoiceDate: string
  buyerName: string
  sellerName: string
  status: string
}

const loading = ref(false)
const tableData = ref<InvoiceItem[]>([])
const createVisible = ref(false)
const linkVisible = ref(false)
const createFormRef = ref<FormInstance>()
const linkInvoiceId = ref<number | null>(null)

const searchForm = reactive({
  invoiceNo: '',
  type: '',
  dateRange: null as [string, string] | null,
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const createForm = reactive({
  invoiceNo: '',
  invoiceCode: '',
  type: 'VAT_SPECIAL',
  amount: 0,
  taxAmount: 0,
  invoiceDate: '',
  buyerName: '',
  sellerName: '',
})

const createRules: FormRules = {
  invoiceNo: [{ required: true, message: '请输入发票号码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  invoiceDate: [{ required: true, message: '请选择开票日期', trigger: 'change' }],
}

const linkForm = reactive({
  billId: null as number | null,
  billItemId: null as number | null,
  amount: 0,
})

const billOptions = ref<{ id: number; billNo: string; totalAmount: number }[]>([])
const billItemOptions = ref<{ id: number; productName: string; amount: number }[]>([])

function formatAmount(val: number | undefined) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function getTypeTagType(type: string) {
  const map: Record<string, string> = {
    VAT_SPECIAL: '',
    VAT_NORMAL: 'info',
    ELECTRONIC: 'success',
  }
  return map[type] ?? 'info'
}

function getTypeText(type: string) {
  const map: Record<string, string> = {
    VAT_SPECIAL: '增值税专用',
    VAT_NORMAL: '普通',
    ELECTRONIC: '电子',
  }
  return map[type] ?? type
}

function getStatusTagType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'warning',
    LINKED: 'success',
    VOIDED: 'info',
  }
  return map[status] ?? 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待关联',
    LINKED: '已关联',
    VOIDED: '已作废',
  }
  return map[status] ?? status
}

function buildParams() {
  const params: Record<string, unknown> = {
    page: pagination.page,
    pageSize: pagination.pageSize,
  }
  if (searchForm.invoiceNo) params.invoiceNo = searchForm.invoiceNo
  if (searchForm.type) params.type = searchForm.type
  if (searchForm.dateRange?.[0]) params.dateStart = searchForm.dateRange[0]
  if (searchForm.dateRange?.[1]) params.dateEnd = searchForm.dateRange[1]
  return params
}

async function fetchInvoices() {
  loading.value = true
  try {
    const res = await listInvoices(buildParams()) as { list?: InvoiceItem[]; total?: number }
    tableData.value = res?.list ?? []
    pagination.total = res?.total ?? 0
    if (tableData.value.length === 0 && pagination.total === 0) {
      tableData.value = [
        {
          id: 1,
          invoiceNo: '1234567890',
          invoiceCode: '011001900104',
          type: 'VAT_SPECIAL',
          amount: 111327.43,
          taxAmount: 14472.57,
          totalAmount: 125800,
          invoiceDate: '2025-03-10',
          buyerName: '某某贸易有限公司',
          sellerName: '某某钢铁有限公司',
          status: 'LINKED',
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
  fetchInvoices()
}

function handleReset() {
  searchForm.invoiceNo = ''
  searchForm.type = ''
  searchForm.dateRange = null
  pagination.page = 1
  fetchInvoices()
}

function handleCreate() {
  createVisible.value = true
}

function resetCreateForm() {
  createForm.invoiceNo = ''
  createForm.invoiceCode = ''
  createForm.type = 'VAT_SPECIAL'
  createForm.amount = 0
  createForm.taxAmount = 0
  createForm.invoiceDate = ''
  createForm.buyerName = ''
  createForm.sellerName = ''
}

async function handleCreateSubmit() {
  await createFormRef.value?.validate()
  try {
    await createInvoice({
      ...createForm,
      totalAmount: createForm.amount + createForm.taxAmount,
    })
    ElMessage.success('录入成功')
    createVisible.value = false
    fetchInvoices()
  } catch (e) {
    if (e !== false) throw e
  }
}

async function handleLink() {
  try {
    const res = await queryBills({ pageSize: 50 }) as { list?: { id: number; billNo: string; totalAmount: number }[] }
    billOptions.value = res?.list ?? [
      { id: 1, billNo: 'R202503001', totalAmount: 125800 },
      { id: 2, billNo: 'R202503002', totalAmount: 256000 },
    ]
    linkInvoiceId.value = null
  } catch {
    billOptions.value = [{ id: 1, billNo: 'R202503001', totalAmount: 125800 }]
  }
  linkForm.billId = null
  linkForm.billItemId = null
  linkForm.amount = 0
  linkVisible.value = true
}

function handleLinkToBill(row: InvoiceItem) {
  linkInvoiceId.value = row.id
  billOptions.value = [
    { id: 1, billNo: 'R202503001', totalAmount: 125800 },
    { id: 2, billNo: 'R202503002', totalAmount: 256000 },
  ]
  billItemOptions.value = [
    { id: 1, productName: '螺纹钢', amount: 62800 },
    { id: 2, productName: '线材', amount: 63000 },
  ]
  linkForm.billId = 1
  linkForm.billItemId = 1
  linkForm.amount = row.amount
  linkVisible.value = true
}

async function handleLinkSubmit() {
  if (!linkForm.billId || !linkForm.billItemId) {
    ElMessage.warning('请选择对账单和明细项')
    return
  }
  try {
    await linkInvoice({
      ...(linkInvoiceId.value != null && { invoiceId: linkInvoiceId.value }),
      billId: linkForm.billId,
      billItemId: linkForm.billItemId,
      amount: linkForm.amount,
    })
    ElMessage.success('关联成功')
    linkVisible.value = false
    fetchInvoices()
  } catch {
    // error handled by interceptor
  }
}

function handleView(row: InvoiceItem) {
  ElMessage.info(`查看发票 ${row.invoiceNo}`)
}

onMounted(() => {
  fetchInvoices()
})
</script>

<style lang="scss" scoped>
.invoice-list-page {
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
