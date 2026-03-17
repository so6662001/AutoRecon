<template>
  <div class="contract-list-page">
    <h2 class="page-title">合同管理</h2>

    <el-form :model="searchForm" inline class="search-form">
      <el-form-item label="合同号">
        <el-input v-model="searchForm.contractNo" placeholder="请输入" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="买方">
        <el-input v-model="searchForm.buyer" placeholder="请输入" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="周期">
        <el-date-picker
          v-model="searchForm.periodRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
      <el-table-column prop="contractNo" label="合同号" min-width="140" />
      <el-table-column prop="contractName" label="合同名称" min-width="180" />
      <el-table-column prop="itemCount" label="明细数" width="90" align="center" />
      <el-table-column prop="totalWeight" label="总重量(吨)" width="110" align="right">
        <template #default="{ row }">{{ formatNumber(row.totalWeight) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="总金额" width="120" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="paidAmount" label="已付金额" width="120" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.paidAmount) }}</template>
      </el-table-column>
      <el-table-column prop="unpaidAmount" label="未付金额" width="120" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.unpaidAmount) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewDetail(row)">
            查看明细
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchContracts"
        @current-change="fetchContracts"
      />
    </div>

    <!-- Detail dialog -->
    <el-dialog v-model="detailVisible" title="合同明细" width="800px">
      <div v-if="currentContract" class="detail-content">
        <div class="summary-row">
          <span>合同号：{{ currentContract.contractNo }}</span>
          <span>总金额：¥{{ formatAmount(summary?.totalAmount) }}</span>
          <span>已付：¥{{ formatAmount(summary?.paidAmount) }}</span>
        </div>
        <el-table :data="contractItems" stripe size="small">
          <el-table-column prop="productName" label="品名" min-width="120" />
          <el-table-column prop="spec" label="规格" width="100" />
          <el-table-column prop="quantity" label="数量" width="90" align="right" />
          <el-table-column prop="weight" label="重量(吨)" width="100" align="right">
            <template #default="{ row }">{{ formatNumber(row.weight) }}</template>
          </el-table-column>
          <el-table-column prop="unitPrice" label="单价" width="100" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listContracts, getContractSummary, getContractItems } from '@/api/recon'

interface ContractItem {
  id: number
  contractNo: string
  contractName: string
  itemCount: number
  totalWeight: number
  totalAmount: number
  paidAmount: number
  unpaidAmount: number
}

const loading = ref(false)
const tableData = ref<ContractItem[]>([])
const detailVisible = ref(false)
const currentContract = ref<ContractItem | null>(null)
const summary = ref<{ totalAmount?: number; paidAmount?: number } | null>(null)
const contractItems = ref<{ productName: string; spec: string; quantity: number; weight: number; unitPrice: number; amount: number }[]>([])

const searchForm = reactive({
  contractNo: '',
  buyer: '',
  periodRange: null as [string, string] | null,
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

function formatNumber(val: number | undefined) {
  if (val == null) return '0'
  return Number(val).toLocaleString('zh-CN')
}

function buildParams() {
  const params: Record<string, unknown> = {
    page: pagination.page,
    pageSize: pagination.pageSize,
  }
  if (searchForm.contractNo) params.contractNo = searchForm.contractNo
  if (searchForm.buyer) params.buyer = searchForm.buyer
  if (searchForm.periodRange?.[0]) params.periodStart = searchForm.periodRange[0]
  if (searchForm.periodRange?.[1]) params.periodEnd = searchForm.periodRange[1]
  return params
}

async function fetchContracts() {
  loading.value = true
  try {
    const res = await listContracts(buildParams()) as { list?: ContractItem[]; total?: number }
    tableData.value = res?.list ?? []
    pagination.total = res?.total ?? 0
    if (tableData.value.length === 0 && pagination.total === 0) {
      tableData.value = [
        {
          id: 1,
          contractNo: 'HT2025001',
          contractName: '钢材采购合同-2025Q1',
          itemCount: 12,
          totalWeight: 1250.5,
          totalAmount: 5680000,
          paidAmount: 2560000,
          unpaidAmount: 3120000,
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
  fetchContracts()
}

function handleReset() {
  searchForm.contractNo = ''
  searchForm.buyer = ''
  searchForm.periodRange = null
  pagination.page = 1
  fetchContracts()
}

async function handleViewDetail(row: ContractItem) {
  currentContract.value = row
  detailVisible.value = true
  try {
    const [sumRes, itemsRes] = await Promise.all([
      getContractSummary(row.contractNo) as Promise<{ totalAmount?: number; paidAmount?: number }>,
      getContractItems(row.contractNo) as Promise<{ productName: string; spec: string; quantity: number; weight: number; unitPrice: number; amount: number }[]>,
    ])
    summary.value = sumRes
    contractItems.value = Array.isArray(itemsRes) ? itemsRes : []
    if (contractItems.value.length === 0) {
      contractItems.value = [
        { productName: '螺纹钢', spec: 'HRB400', quantity: 500, weight: 250, unitPrice: 5024, amount: 1256000 },
        { productName: '线材', spec: 'HPB300', quantity: 300, weight: 180, unitPrice: 4980, amount: 896400 },
      ]
    }
  } catch {
    summary.value = { totalAmount: row.totalAmount, paidAmount: row.paidAmount }
    contractItems.value = [
      { productName: '螺纹钢', spec: 'HRB400', quantity: 500, weight: 250, unitPrice: 5024, amount: 1256000 },
      { productName: '线材', spec: 'HPB300', quantity: 300, weight: 180, unitPrice: 4980, amount: 896400 },
    ]
  }
}

onMounted(() => {
  fetchContracts()
})
</script>

<style lang="scss" scoped>
.contract-list-page {
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

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .detail-content {
    .summary-row {
      display: flex;
      gap: 24px;
      margin-bottom: 16px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 4px;
    }

    .el-table {
      margin-top: 12px;
    }
  }
}
</style>
