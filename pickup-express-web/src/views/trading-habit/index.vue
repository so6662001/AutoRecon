<template>
  <div class="page-container trading-habit">
    <div class="action-bar">
      <el-input
        v-model="searchForm.buyerName"
        placeholder="搜索客户"
        clearable
        style="width: 200px"
        @keyup.enter="loadData"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="showReportDialog = true">生成交易习惯报告</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="buyerName" label="客户" min-width="120" />
      <el-table-column prop="driverName" label="司机姓名" width="100" />
      <el-table-column prop="driverPhone" label="司机电话" width="120" />
      <el-table-column prop="plateNo" label="车牌" width="100" />
      <el-table-column prop="pickupCount" label="总提货次数" width="110" align="right" />
      <el-table-column prop="totalWeight" label="总重量" width="100" align="right">
        <template #default="{ row }">{{ formatNum(row.totalWeight) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="总金额" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="paidCount" label="已付次数" width="100" align="right" />
      <el-table-column prop="denyCount" label="否认次数" width="100" align="right" />
      <el-table-column prop="firstPickupAt" label="首次提货时间" width="120">
        <template #default="{ row }">{{ formatDate(row.firstPickupAt) }}</template>
      </el-table-column>
      <el-table-column prop="lastPickupAt" label="最近提货时间" width="120">
        <template #default="{ row }">{{ formatDate(row.lastPickupAt) }}</template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showReportDialog" title="交易习惯报告" width="600px">
      <el-select v-model="reportBuyerId" placeholder="选择客户" filterable style="width: 100%; margin-bottom: 16px">
        <el-option
          v-for="b in buyerOptions"
          :key="b.id"
          :label="b.name"
          :value="b.id"
        />
      </el-select>
      <div v-loading="reportLoading" class="report-content">
        <pre>{{ reportText }}</pre>
      </div>
      <template #footer>
        <el-button @click="showReportDialog = false">关闭</el-button>
        <el-button type="primary" :loading="reportLoading" @click="generateReport">生成报告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { listTradingHabits, getTradingHabitReport, queryBuyers } from '@/api/evidence'

const loading = ref(false)
const reportLoading = ref(false)
const showReportDialog = ref(false)
const reportBuyerId = ref<number | undefined>()
const reportText = ref('')
const tableData = ref<any[]>([])
const buyerOptions = ref<Array<{ id: number; name: string }>>([])

const searchForm = reactive({ buyerName: '' })

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
    const params: any = {}
    if (searchForm.buyerName) params.buyerName = searchForm.buyerName
    const res = (await listTradingHabits(params)) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data ?? []
    tableData.value = Array.isArray(list)
      ? list.map((r: any) => ({
          buyerId: r.buyerId ?? r.buyer_id,
          buyerName: r.buyerName ?? r.buyer_name ?? '-',
          driverName: r.driverName ?? r.driver_name ?? '-',
          driverPhone: r.driverPhone ?? r.driver_phone ?? '-',
          plateNo: r.plateNo ?? r.plate_no ?? '-',
          pickupCount: r.pickupCount ?? r.pickup_count ?? 0,
          totalWeight: r.totalWeight ?? r.total_weight ?? 0,
          totalAmount: r.totalAmount ?? r.total_amount ?? 0,
          paidCount: r.paidCount ?? r.paid_count ?? 0,
          denyCount: r.denyCount ?? r.deny_count ?? 0,
          firstPickupAt: r.firstPickupAt ?? r.first_pickup_at,
          lastPickupAt: r.lastPickupAt ?? r.last_pickup_at,
        }))
      : []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function loadBuyers() {
  try {
    const res = (await queryBuyers({})) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data ?? []
    buyerOptions.value = Array.isArray(list)
      ? list.map((b: any) => ({ id: b.id, name: b.name ?? b.buyerName ?? '-' }))
      : []
    if (buyerOptions.value.length && !reportBuyerId.value) {
      reportBuyerId.value = buyerOptions.value[0].id
    }
  } catch {
    buyerOptions.value = []
  }
}

async function generateReport() {
  if (!reportBuyerId.value) {
    ElMessage.warning('请选择客户')
    return
  }
  reportLoading.value = true
  try {
    const res = (await getTradingHabitReport(reportBuyerId.value)) as any
    const data = res?.data ?? res
    reportText.value = typeof data === 'string' ? data : data?.report ?? JSON.stringify(data, null, 2)
  } catch {
    reportText.value = '生成报告失败'
  } finally {
    reportLoading.value = false
  }
}

onMounted(() => {
  loadBuyers()
  loadData()
})
</script>

<style lang="scss" scoped>
.trading-habit {
  .action-bar {
    margin-bottom: 20px;
    display: flex;
    gap: 12px;
  }

  .report-content {
    min-height: 200px;
    max-height: 400px;
    overflow: auto;

    pre {
      white-space: pre-wrap;
      font-size: 13px;
      margin: 0;
    }
  }
}
</style>
