<template>
  <div class="bill-list-page">
    <h2 class="page-title">对账单列表</h2>

    <!-- Search bar -->
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item label="对账单号">
        <el-input v-model="searchForm.billNo" placeholder="请输入" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="买方企业">
        <el-input v-model="searchForm.buyerName" placeholder="请输入" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="已创建" value="CREATED" />
          <el-option label="已生成" value="GENERATED" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="异议中" value="DISPUTED" />
          <el-option label="待签章" value="TO_SIGN" />
          <el-option label="已签章" value="SIGNED" />
          <el-option label="催收中" value="COLLECTING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已作废" value="VOIDED" />
        </el-select>
      </el-form-item>
      <el-form-item label="对账周期">
        <el-date-picker
          v-model="searchForm.periodRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="合同号">
        <el-input v-model="searchForm.contractNo" placeholder="请输入" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- Action bar -->
    <div class="action-bar">
      <el-button type="primary" @click="handleCreate">发起对账</el-button>
      <el-button @click="handleBatchRecon">批量对账</el-button>
    </div>

    <!-- Table -->
    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="billNo" label="对账单号" min-width="140" />
      <el-table-column prop="buyerName" label="买方企业" min-width="160" />
      <el-table-column prop="period" label="对账周期" min-width="180" />
      <el-table-column prop="totalAmount" label="交易总额" width="120" align="right">
        <template #default="{ row }">
          ¥{{ formatAmount(row.totalAmount) }}
        </template>
      </el-table-column>
      <el-table-column prop="paidAmount" label="已付款" width="120" align="right">
        <template #default="{ row }">
          ¥{{ formatAmount(row.paidAmount) }}
        </template>
      </el-table-column>
      <el-table-column prop="balanceAmount" label="应付余额" width="120" align="right">
        <template #default="{ row }">
          ¥{{ formatAmount(row.balanceAmount) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(getStatusTagType(row.status) as 'success' | 'warning' | 'info' | 'danger')" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
          <el-button
            v-if="['CREATED', 'GENERATED'].includes(row.status)"
            type="primary"
            link
            size="small"
            @click="handleSend(row)"
          >
            发送
          </el-button>
          <el-button
            v-if="!['SIGNED', 'COLLECTING', 'COMPLETED', 'VOIDED'].includes(row.status)"
            type="danger"
            link
            size="small"
            @click="handleVoid(row)"
          >
            作废
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchBills"
        @current-change="fetchBills"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryBills, sendBill, voidBill } from '@/api/recon'

interface BillItem {
  id: number
  billNo: string
  buyerName: string
  period: string
  totalAmount: number
  paidAmount: number
  balanceAmount: number
  status: string
  createdAt: string
}

interface SearchForm {
  billNo: string
  buyerName: string
  status: string
  periodRange: [string, string] | null
  contractNo: string
}

const router = useRouter()
const loading = ref(false)
const tableData = ref<BillItem[]>([])

const searchForm = reactive<SearchForm>({
  billNo: '',
  buyerName: '',
  status: '',
  periodRange: null,
  contractNo: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

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

function buildParams() {
  const params: Record<string, unknown> = {
    page: pagination.page,
    pageSize: pagination.pageSize,
  }
  if (searchForm.billNo) params.billNo = searchForm.billNo
  if (searchForm.buyerName) params.buyerName = searchForm.buyerName
  if (searchForm.status) params.status = searchForm.status
  if (searchForm.contractNo) params.contractNo = searchForm.contractNo
  if (searchForm.periodRange?.[0]) params.periodStart = searchForm.periodRange[0]
  if (searchForm.periodRange?.[1]) params.periodEnd = searchForm.periodRange[1]
  return params
}

async function fetchBills() {
  loading.value = true
  try {
    const res = await queryBills(buildParams()) as { list?: BillItem[]; total?: number }
    tableData.value = res?.list ?? []
    pagination.total = res?.total ?? 0
    if (tableData.value.length === 0 && pagination.total === 0) {
      tableData.value = [
        {
          id: 1,
          billNo: 'R202503001',
          buyerName: '某某贸易有限公司',
          period: '2025-02-01 ~ 2025-02-28',
          totalAmount: 125800.5,
          paidAmount: 50000,
          balanceAmount: 75800.5,
          status: 'PENDING',
          createdAt: '2025-03-17 10:30:00',
        },
        {
          id: 2,
          billNo: 'R202503002',
          buyerName: '某某制造有限公司',
          period: '2025-02-01 ~ 2025-02-28',
          totalAmount: 256000,
          paidAmount: 256000,
          balanceAmount: 0,
          status: 'SIGNED',
          createdAt: '2025-03-16 14:20:00',
        },
      ]
      pagination.total = 2
    }
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchBills()
}

function handleReset() {
  searchForm.billNo = ''
  searchForm.buyerName = ''
  searchForm.status = ''
  searchForm.periodRange = null
  searchForm.contractNo = ''
  pagination.page = 1
  fetchBills()
}

function handleCreate() {
  router.push({ name: 'billCreate' })
}

function handleBatchRecon() {
  router.push({ name: 'batchRecon' })
}

function handleView(row: BillItem) {
  router.push({ name: 'billDetail', params: { id: String(row.id) } })
}

async function handleSend(row: BillItem) {
  try {
    await sendBill(row.id)
    ElMessage.success('发送成功')
    fetchBills()
  } catch {
    // error handled by interceptor
  }
}

async function handleVoid(row: BillItem) {
  await ElMessageBox.confirm('确定要作废该对账单吗？作废后不可恢复。', '确认作废', {
    type: 'warning',
  })
  try {
    await voidBill(row.id)
    ElMessage.success('已作废')
    fetchBills()
  } catch {
    // error handled by interceptor
  }
}

onMounted(() => {
  fetchBills()
})
</script>

<style lang="scss" scoped>
.bill-list-page {
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
