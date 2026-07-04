<template>
  <div class="page-container pickup-monitor">
    <div class="toolbar">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="仓库">
          <el-select
            v-model="filters.warehouseId"
            placeholder="全部仓库"
            clearable
            style="width: 180px"
            @change="applyFilters"
          >
            <el-option
              v-for="w in warehouses"
              :key="w.id"
              :label="w.name"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" style="width: 140px" @change="applyFilters">
            <el-option label="全部" value="all" />
            <el-option label="发货中" value="delivering" />
            <el-option label="待提货" value="ready" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="refresh">刷新</el-button>
        </el-form-item>
      </el-form>
      <div class="last-updated">最后更新: {{ lastUpdatedText }}</div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.delivering }}</div>
          <div class="stat-label">正在发货</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.ready }}</div>
          <div class="stat-label">待提货</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.completedToday }}</div>
          <div class="stat-label">今日完成</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="displayRows" stripe style="width: 100%">
      <el-table-column prop="pickupOrderNo" label="提货单号" width="150" />
      <el-table-column prop="contractNo" label="合同号" width="130" />
      <el-table-column prop="buyerName" label="客户" min-width="120" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="driverName" label="驾驶员" width="100" />
      <el-table-column prop="plateNo" label="车牌" width="110" />
      <el-table-column label="发货进度" min-width="160">
        <template #default="{ row }">
          <el-progress
            :percentage="progressPercent(row)"
            :stroke-width="14"
            :format="() => `${row.loadedLifts}/${row.totalLifts}吊`"
          />
        </template>
      </el-table-column>
      <el-table-column label="已装重量(吨)" width="120" align="right">
        <template #default="{ row }">{{ formatNum(row.loadedWeight) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.statusCode)" size="small">{{ row.statusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="170">
        <template #default="{ row }">{{ formatDate(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="持续时间" width="110">
        <template #default="{ row }">{{ row.durationText }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="goDetail(row.id)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { queryPickupOrders, listWarehouses } from '@/api/evidence'

const router = useRouter()

const loading = ref(false)
const lastUpdated = ref<dayjs.Dayjs | null>(null)
const rawList = ref<any[]>([])
const warehouses = ref<{ id: number; name: string }[]>([])

const filters = reactive({
  warehouseId: undefined as number | undefined,
  status: 'all' as 'all' | 'delivering' | 'ready',
})

let timer: ReturnType<typeof setInterval> | null = null

const lastUpdatedText = computed(() =>
  lastUpdated.value ? lastUpdated.value.format('HH:mm:ss') : '—'
)

const STATUS = {
  DELIVERING: 5,
  READY: 2,
  COMPLETED: 6,
} as const

function statusLabel(code: number | undefined): string {
  const map: Record<number, string> = {
    1: '待派车',
    2: '待提货',
    3: '已接单',
    4: '已到达',
    5: '发货中',
    6: '已完成',
    7: '已取消',
  }
  return code != null ? map[code] ?? String(code) : '-'
}

function statusTagType(code: number | undefined): 'success' | 'warning' | 'info' | 'danger' {
  if (code === STATUS.COMPLETED) return 'success'
  if (code === 7) return 'danger'
  if (code === STATUS.DELIVERING) return 'warning'
  return 'info'
}

function formatNum(val: number | string | undefined) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

function formatDate(val: string | undefined) {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm:ss')
}

function mapRow(r: any) {
  const lifts = r.lifts ?? r.liftRecords ?? []
  const loadedLifts = Array.isArray(lifts) ? lifts.length : 0
  let loadedWeight = 0
  if (Array.isArray(lifts)) {
    for (const lift of lifts) {
      const w = lift.actualWeight ?? lift.actual_weight ?? lift.theoryWeight ?? lift.theoreticalWeight
      if (w != null) loadedWeight += Number(w)
    }
  }
  const totalLifts = r.totalLifts ?? r.total_lifts ?? 0
  const statusCode = r.status ?? r.statusCode
  const startRaw = r.actualArrivalAt ?? r.actual_arrival_at ?? r.createdAt ?? r.created_at
  const start = startRaw ? dayjs(startRaw) : null
  const durationText =
    start && start.isValid() ? formatDuration(dayjs().diff(start, 'second')) : '-'

  return {
    id: r.id,
    pickupOrderNo: r.pickupOrderNo ?? r.pickupNo ?? r.pickup_no ?? '-',
    contractNo: r.contractNo ?? r.contract_no ?? '-',
    buyerName: r.buyerName ?? r.buyer_name ?? '-',
    warehouseName: r.warehouseName ?? r.warehouse_name ?? '-',
    warehouseId: r.warehouseId ?? r.warehouse_id,
    driverName: r.driverName ?? r.driver_name ?? '-',
    plateNo: r.plateNo ?? r.vehiclePlate ?? r.vehicle_plate ?? '-',
    loadedLifts,
    totalLifts: totalLifts || loadedLifts,
    loadedWeight,
    statusCode,
    statusLabel: statusLabel(statusCode),
    startTime: startRaw,
    durationText,
  }
}

function formatDuration(totalSec: number) {
  if (totalSec < 0) totalSec = 0
  const h = Math.floor(totalSec / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  if (h > 0) return `${h}时${m}分`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

function progressPercent(row: { loadedLifts: number; totalLifts: number }) {
  const t = row.totalLifts || 0
  if (!t) return 0
  return Math.min(100, Math.round((row.loadedLifts / t) * 100))
}

function passesWarehouseFilter(r: any) {
  const wid = r.warehouseId ?? r.warehouse_id
  if (filters.warehouseId == null) return true
  return wid === filters.warehouseId
}

const stats = computed(() => {
  let delivering = 0
  let ready = 0
  let completedToday = 0

  for (const r of rawList.value) {
    if (!passesWarehouseFilter(r)) continue
    const code = r.status ?? r.statusCode
    if (code === STATUS.DELIVERING) delivering++
    if (code === STATUS.READY) ready++
    if (code === STATUS.COMPLETED) {
      const ts = r.updatedAt ?? r.updated_at ?? r.createdAt ?? r.created_at
      if (ts && dayjs(ts).isValid() && dayjs(ts).isSame(dayjs(), 'day')) {
        completedToday++
      }
    }
  }

  return { delivering, ready, completedToday }
})

const displayRows = computed(() => {
  return rawList.value
    .filter((r) => {
      if (!passesWarehouseFilter(r)) return false
      const code = r.status ?? r.statusCode
      if (code === 6 || code === 7) return false
      if (filters.status === 'delivering') return code === STATUS.DELIVERING
      if (filters.status === 'ready') return code === STATUS.READY
      return true
    })
    .map(mapRow)
})

function applyFilters() {
  /* reactive filters drive computed displayRows & stats */
}

async function loadWarehouses() {
  try {
    const res = (await listWarehouses()) as any
    const data = res?.data ?? res
    const list = Array.isArray(data) ? data : data?.list ?? []
    warehouses.value = list.map((w: any) => ({
      id: w.id,
      name: w.name ?? w.warehouseName ?? w.warehouse_name ?? `仓库${w.id}`,
    }))
  } catch {
    warehouses.value = []
  }
}

async function refresh() {
  loading.value = true
  try {
    const res = (await queryPickupOrders({
      pageNum: 1,
      pageSize: 500,
    })) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data?.rows ?? []
    rawList.value = Array.isArray(list) ? list : []
    lastUpdated.value = dayjs()
  } catch {
    rawList.value = []
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push({ name: 'pickupDetail', params: { id: String(id) } })
}

onMounted(async () => {
  await loadWarehouses()
  await refresh()
  timer = setInterval(() => {
    refresh()
  }, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.pickup-monitor {
  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;
  }

  .filter-form {
    margin-bottom: 0;
  }

  .last-updated {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .stats-row {
    margin-bottom: 20px;
  }

  .stat-card {
    text-align: center;
    margin-bottom: 12px;
  }

  .stat-value {
    font-size: 26px;
    font-weight: 600;
    color: var(--el-color-primary);
  }

  .stat-label {
    margin-top: 6px;
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}
</style>
