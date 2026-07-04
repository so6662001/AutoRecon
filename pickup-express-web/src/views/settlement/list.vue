<template>
  <div class="page-container settlement-list">
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item label="结算单号">
        <el-input v-model="searchForm.settlementNo" placeholder="请输入结算单号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="合同号">
        <el-input v-model="searchForm.contractNo" placeholder="请输入合同号" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="客户">
        <el-input v-model="searchForm.buyerName" placeholder="请输入客户" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="待结算" value="待结算" />
          <el-option label="已结算" value="已结算" />
          <el-option label="已对账" value="已对账" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="stats-row">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-value">{{ stats.pendingCount }}</div>
        <div class="stat-label">待结算数</div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-value">¥{{ formatMoney(stats.monthAmount) }}</div>
        <div class="stat-label">本月结算额</div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-value">¥{{ formatMoney(stats.totalReceivable) }}</div>
        <div class="stat-label">应收总额</div>
      </el-card>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
      <el-table-column prop="settlementNo" label="结算单号" width="140" />
      <el-table-column prop="contractNo" label="合同号" width="120" />
      <el-table-column prop="buyerName" label="客户" min-width="120" />
      <el-table-column prop="pickupNo" label="提货单号" width="120" />
      <el-table-column prop="weight" label="结算重量(吨)" width="110" align="right">
        <template #default="{ row }">{{ formatNum(row.weight) }}</template>
      </el-table-column>
      <el-table-column prop="amount" label="结算金额(¥)" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="taxAmount" label="税额" width="90" align="right">
        <template #default="{ row }">{{ formatMoney(row.taxAmount) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="价税合计" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="客户已查看" width="100">
        <template #default="{ row }">
          <el-tag :type="row.viewed ? 'success' : 'info'" size="small">{{ row.viewed ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="同步对账" width="100">
        <template #default="{ row }">
          <el-tag :type="row.synced ? 'success' : 'info'" size="small">{{ row.synced ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ row.status || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @size-change="loadData"
      @current-change="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { listSettlements } from '@/api/evidence'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])

const searchForm = reactive({
  settlementNo: '',
  contractNo: '',
  buyerName: '',
  status: '',
})

const stats = reactive({
  pendingCount: 0,
  monthAmount: 0,
  totalReceivable: 0,
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

function formatNum(val: number | string) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | string) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function formatDate(val: string | undefined) {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm')
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (searchForm.settlementNo) params.settlementNo = searchForm.settlementNo
    if (searchForm.contractNo) params.contractNo = searchForm.contractNo
    if (searchForm.buyerName) params.buyerName = searchForm.buyerName
    if (searchForm.status) params.status = searchForm.status

    const res = (await listSettlements(params)) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data ?? []
    const total = data?.total ?? list.length

    tableData.value = Array.isArray(list)
      ? list.map((r: any) => ({
          id: r.id,
          settlementNo: r.settlementNo ?? r.settlement_no ?? '-',
          contractNo: r.contractNo ?? r.contract_no ?? '-',
          buyerName: r.buyerName ?? r.buyer_name ?? '-',
          pickupNo: r.pickupNo ?? r.pickup_no ?? '-',
          weight: r.weight ?? r.settlementWeight ?? 0,
          amount: r.amount ?? r.settlementAmount ?? 0,
          taxAmount: r.taxAmount ?? r.tax_amount ?? 0,
          totalAmount: r.totalAmount ?? r.total_amount ?? 0,
          viewed: r.viewed ?? r.customerViewed ?? false,
          synced: r.synced ?? r.reconciled ?? false,
          status: r.status ?? '-',
          createdAt: r.createdAt ?? r.created_at,
        }))
      : []

    pagination.total = total

    stats.pendingCount = data?.pendingCount ?? tableData.value.filter((r) => r.status === '待结算').length
    stats.monthAmount = data?.monthAmount ?? 0
    stats.totalReceivable = data?.totalReceivable ?? tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.settlementNo = ''
  searchForm.contractNo = ''
  searchForm.buyerName = ''
  searchForm.status = ''
  pagination.page = 1
  loadData()
}

function viewDetail(row: any) {
  router.push({ name: 'settlementDetail', params: { id: String(row.id) } })
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.settlement-list {
  .search-form {
    margin-bottom: 16px;
  }

  .stats-row {
    display: flex;
    gap: 16px;
    margin-bottom: 20px;

    .stat-card {
      flex: 1;
      text-align: center;

      .stat-value {
        font-size: 24px;
        font-weight: 600;
        color: var(--el-color-primary);
      }

      .stat-label {
        font-size: 14px;
        color: var(--el-text-color-secondary);
        margin-top: 4px;
      }
    }
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
