<template>
  <div class="page-container dashboard">
    <!-- Top row: 5 stat cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :md="8" :lg="24/5">
        <el-card shadow="hover" class="stat-card stat-blue">
          <div class="stat-value">{{ stats.contractCount }}</div>
          <div class="stat-label">合同总数</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="24/5">
        <el-card shadow="hover" class="stat-card stat-orange">
          <div class="stat-value">{{ stats.pickingCount }}</div>
          <div class="stat-label">提货中</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="24/5">
        <el-card shadow="hover" class="stat-card stat-purple">
          <div class="stat-value">{{ stats.deliveringCount }}</div>
          <div class="stat-label">发货中</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="24/5">
        <el-card shadow="hover" class="stat-card stat-red">
          <div class="stat-value">{{ stats.pendingSettlement }}</div>
          <div class="stat-label">待结算</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="24/5">
        <el-card shadow="hover" class="stat-card stat-green">
          <div class="stat-value">{{ stats.monthPickupWeight }}</div>
          <div class="stat-label">本月提货量(吨)</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Middle row: 2 charts -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>应收账款概览</span>
          </template>
          <v-chart
            v-if="gaugeOption"
            :option="gaugeOption"
            class="chart"
            autoresize
          />
          <div v-else class="chart-placeholder">暂无数据</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>近6月提货趋势</span>
          </template>
          <v-chart
            v-if="barOption"
            :option="barOption"
            class="chart"
            autoresize
          />
          <div v-else class="chart-placeholder">暂无数据</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Bottom: 最近动态 (el-timeline) -->
    <el-card shadow="hover" class="timeline-card">
      <template #header>
        <span>最近动态</span>
      </template>
      <el-timeline v-loading="eventsLoading">
        <el-timeline-item
          v-for="(evt, idx) in recentEvents"
          :key="evt.id || idx"
          :timestamp="evt.time"
          placement="top"
        >
          <el-tag size="small" class="mr-2">{{ evt.eventType }}</el-tag>
          {{ evt.title }}
          <el-button
            v-if="evt.contractId"
            type="primary"
            link
            size="small"
            class="ml-2"
            @click="viewEvent(evt)"
          >
            查看
          </el-button>
        </el-timeline-item>
        <el-empty v-if="!eventsLoading && !recentEvents.length" description="暂无动态" />
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { BarChart, GaugeChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getDashboard } from '@/api/evidence'

use([BarChart, GaugeChart, GridComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const eventsLoading = ref(false)

const stats = reactive({
  contractCount: 0,
  pickingCount: 0,
  deliveringCount: 0,
  pendingSettlement: 0,
  monthPickupWeight: 0,
})

const receivableTotal = ref(0)
const receivableTarget = ref(1000000)
const monthlyWeightData = ref<{ date: string; weight: number }[]>([])
const recentEvents = ref<
  { id: string; time: string; eventType: string; title: string; contractNo: string; contractId?: number }[]
>([])

const gaugeOption = computed(() => {
  const percent = receivableTarget.value > 0
    ? Math.min(100, Math.round((receivableTotal.value / receivableTarget.value) * 100))
    : 0
  return {
    series: [
      {
        type: 'gauge',
        startAngle: 180,
        endAngle: 0,
        min: 0,
        max: 100,
        progress: { show: true, width: 18 },
        axisLine: { lineStyle: { width: 18 } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        anchor: { show: false },
        title: { show: false },
        detail: {
          valueAnimation: true,
          formatter: `¥${receivableTotal.value.toLocaleString('zh-CN')}`,
          offsetCenter: [0, '70%'],
          fontSize: 16,
        },
        data: [{ value: percent }],
      },
    ],
  }
})

const MOCK_MONTHLY_WEIGHT = [
  { date: '10月', weight: 245 },
  { date: '11月', weight: 312 },
  { date: '12月', weight: 288 },
  { date: '1月', weight: 356 },
  { date: '2月', weight: 198 },
  { date: '3月', weight: 420 },
]

const barOption = computed(() => {
  const data = monthlyWeightData.value.length ? monthlyWeightData.value : MOCK_MONTHLY_WEIGHT
  return {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.date),
    },
    yAxis: { type: 'value', name: '吨' },
    series: [
      {
        type: 'bar',
        data: data.map((d) => d.weight),
        itemStyle: { color: '#67c23a' },
      },
    ],
  }
})

const MOCK_STATS = {
  contractCount: 128,
  pickingCount: 23,
  deliveringCount: 12,
  pendingSettlement: 8,
  monthPickupWeight: 420,
}

const MOCK_EVENTS = [
  { id: '1', time: '2025-03-17 10:30', eventType: '提货', title: '提货单PO20250317001已发货', contractNo: 'HT202503001', contractId: 1 },
  { id: '2', time: '2025-03-17 09:15', eventType: '签约', title: '合同HT202503002双方已签', contractNo: 'HT202503002', contractId: 2 },
  { id: '3', time: '2025-03-16 16:20', eventType: '结算', title: '结算单已生成', contractNo: 'HT202503003', contractId: 3 },
  { id: '4', time: '2025-03-16 14:00', eventType: '派车', title: '派车单已分配驾驶员', contractNo: 'HT202503004', contractId: 4 },
  { id: '5', time: '2025-03-16 11:30', eventType: '提货', title: '提货单PO20250316002已完成', contractNo: 'HT202503005', contractId: 5 },
]

async function loadDashboard() {
  eventsLoading.value = true
  try {
    const res = (await getDashboard()) as any
    const data = res?.data ?? res
    if (data) {
      stats.contractCount = data.contractCount ?? data.contract_count ?? MOCK_STATS.contractCount
      stats.pickingCount = data.pickingCount ?? data.picking_count ?? MOCK_STATS.pickingCount
      stats.deliveringCount = data.deliveringCount ?? data.delivering_count ?? MOCK_STATS.deliveringCount
      stats.pendingSettlement = data.pendingSettlement ?? data.pending_settlement ?? MOCK_STATS.pendingSettlement
      stats.monthPickupWeight = data.monthPickupWeight ?? data.month_pickup_weight ?? MOCK_STATS.monthPickupWeight

      receivableTotal.value = data.receivableTotal ?? data.receivable_total ?? 1256800
      receivableTarget.value = data.receivableTarget ?? data.receivable_target ?? 2000000

      const monthly = data.monthlyWeight ?? data.monthly_weight ?? MOCK_MONTHLY_WEIGHT
      monthlyWeightData.value = Array.isArray(monthly)
        ? monthly.map((m: any) => ({
            date: m.date ?? m.day ?? '',
            weight: Number(m.weight ?? m.value ?? 0),
          }))
        : MOCK_MONTHLY_WEIGHT

      const events = data.recentEvents ?? data.recent_events ?? MOCK_EVENTS
      recentEvents.value = Array.isArray(events)
        ? events.map((e: any) => ({
            id: String(e.id ?? ''),
            time: e.time ?? e.createdAt ?? '',
            eventType: e.eventType ?? e.event_type ?? '进度',
            title: e.title ?? e.content ?? '',
            contractNo: e.contractNo ?? e.contract_no ?? '',
            contractId: e.contractId ?? e.contract_id,
          }))
        : MOCK_EVENTS
    } else {
      Object.assign(stats, MOCK_STATS)
      receivableTotal.value = 1256800
      receivableTarget.value = 2000000
      monthlyWeightData.value = MOCK_MONTHLY_WEIGHT
      recentEvents.value = MOCK_EVENTS
    }
  } catch {
    Object.assign(stats, MOCK_STATS)
    receivableTotal.value = 1256800
    receivableTarget.value = 2000000
    monthlyWeightData.value = MOCK_MONTHLY_WEIGHT
    recentEvents.value = MOCK_EVENTS
  } finally {
    eventsLoading.value = false
  }
}

function viewEvent(row: { contractId?: number; contractNo?: string }) {
  if (row.contractId) {
    router.push({ name: 'contractDetail', params: { id: String(row.contractId) } })
  }
}

onMounted(loadDashboard)
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-row {
    margin-bottom: 16px;
  }

  .stat-card {
    text-align: center;
    padding: 16px 0;

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      margin-bottom: 4px;
    }

    .stat-label {
      font-size: 14px;
      color: #909399;
    }

    &.stat-blue .stat-value { color: #409eff; }
    &.stat-orange .stat-value { color: #e6a23c; }
    &.stat-purple .stat-value { color: #9c27b0; }
    &.stat-red .stat-value { color: #f56c6c; }
    &.stat-green .stat-value { color: #67c23a; }
  }

  .chart-row {
    margin-bottom: 16px;
  }

  .chart-card {
    .chart {
      height: 280px;
      width: 100%;
    }

    .chart-placeholder {
      height: 280px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #909399;
    }
  }

  .timeline-card {
    margin-top: 16px;
  }

  .mr-2 { margin-right: 8px; }
  .ml-2 { margin-left: 8px; }
}

@media (max-width: 768px) {
  .dashboard {
    .stat-card {
      padding: 12px 0;

      .stat-value {
        font-size: 20px;
      }
    }

    .chart-card .chart {
      height: 200px;
    }

    .chart-card .chart-placeholder {
      height: 200px;
    }
  }
}
</style>
