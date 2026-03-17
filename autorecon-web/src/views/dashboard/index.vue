<template>
  <div class="dashboard-page">
    <h2 class="page-title">工作台</h2>

    <!-- Top row: 5 stat cards -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :md="8" :lg="8" :xl="24/5">
        <div class="stat-card stat-pending">
          <div class="stat-icon">
            <el-icon :size="32"><Clock /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.pendingCount ?? 0 }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="8" :xl="24/5">
        <div class="stat-card stat-disputed">
          <div class="stat-icon">
            <el-icon :size="32"><Warning /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.disputedCount ?? 0 }}</div>
            <div class="stat-label">异议中</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="8" :xl="24/5">
        <div class="stat-card stat-sign">
          <div class="stat-icon">
            <el-icon :size="32"><Stamp /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.toSignCount ?? 0 }}</div>
            <div class="stat-label">待签章</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="8" :xl="24/5">
        <div class="stat-card stat-collecting">
          <div class="stat-icon">
            <el-icon :size="32"><Bell /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.collectingCount ?? 0 }}</div>
            <div class="stat-label">催收中</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="8" :xl="24/5">
        <div class="stat-card stat-completed">
          <div class="stat-icon">
            <el-icon :size="32"><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.completedCount ?? 0 }}</div>
            <div class="stat-label">本月完成</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Middle row: 2 charts -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">月度对账完成率</div>
          <v-chart
            class="chart-gauge"
            :option="gaugeOption"
            autoresize
          />
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">应收账款趋势</div>
          <v-chart
            class="chart-bar"
            :option="barOption"
            autoresize
          />
        </div>
      </el-col>
    </el-row>

    <!-- Bottom: Recent todos table -->
    <div class="todos-section">
      <div class="section-header">
        <h3>待办事项</h3>
      </div>
      <el-table
        :data="todos"
        stripe
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="billNo" label="对账单号" min-width="140" />
        <el-table-column prop="buyerName" label="客户名称" min-width="160" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="(getStatusTagType(row.status) as 'success' | 'warning' | 'info' | 'danger')" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="['CREATED', 'GENERATED', 'PENDING'].includes(row.status)"
              type="primary"
              link
              size="small"
              @click.stop="handleAction(row, 'handle')"
            >
              处理
            </el-button>
            <el-button
              v-if="row.status === 'TO_SIGN'"
              type="primary"
              link
              size="small"
              @click.stop="handleAction(row, 'sign')"
            >
              签章
            </el-button>
            <el-button
              v-if="['SIGNED', 'COLLECTING'].includes(row.status)"
              type="warning"
              link
              size="small"
              @click.stop="handleAction(row, 'collect')"
            >
              催收
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { GaugeChart, BarChart } from 'echarts/charts'
import { GridComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getDashboard } from '@/api/recon'
import { Clock, Warning, Stamp, Bell, CircleCheck } from '@element-plus/icons-vue'

use([GaugeChart, BarChart, GridComponent, TitleComponent, TooltipComponent, CanvasRenderer])

interface DashboardData {
  pendingCount?: number
  disputedCount?: number
  toSignCount?: number
  collectingCount?: number
  completedCount?: number
  completionRate?: number
  receivablesTrend?: { month: string; amount: number }[]
  todos?: TodoItem[]
}

interface TodoItem {
  id: number
  billNo: string
  buyerName: string
  status: string
  updatedAt: string
}

const router = useRouter()
const dashboardData = ref<DashboardData>({})
const loading = ref(false)

const completionRate = computed(() => dashboardData.value.completionRate ?? 78)

const gaugeOption = computed(() => ({
  series: [
    {
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: 100,
      center: ['50%', '65%'],
      radius: '90%',
      progress: {
        show: true,
        roundCap: true,
        width: 14,
      },
      axisLine: {
        roundCap: true,
        lineStyle: { width: 14, color: [[1, '#e4e7ed']] },
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      title: {
        offsetCenter: [0, '30%'],
        fontSize: 14,
        color: '#909399',
      },
      detail: {
        valueAnimation: true,
        formatter: '{value}%',
        offsetCenter: [0, '-10%'],
        fontSize: 28,
        fontWeight: 'bold',
        color: '#409eff',
      },
      data: [{ value: completionRate.value, name: '完成率' }],
    },
  ],
}))

const barOption = computed(() => {
  const trend = dashboardData.value.receivablesTrend ?? [
    { month: '2024-09', amount: 1250 },
    { month: '2024-10', amount: 1380 },
    { month: '2024-11', amount: 1520 },
    { month: '2024-12', amount: 1680 },
    { month: '2025-01', amount: 1450 },
    { month: '2025-02', amount: 1620 },
  ]
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const p = Array.isArray(params) && params[0] ? params[0] as { axisValue?: string; data?: number[] } : null
        const name = p?.axisValue ?? ''
        const val = p?.data?.[0] ?? 0
        return `${name}<br/>应收账款: ¥${Number(val).toLocaleString()}万`
      },
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.map((t) => t.month.slice(-2) + '月'),
      axisLabel: { interval: 0 },
    },
    yAxis: {
      type: 'value',
      name: '万元',
      axisLabel: { formatter: '{value}' },
    },
    series: [
      {
        type: 'bar',
        data: trend.map((t) => t.amount),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#409eff' },
              { offset: 1, color: '#79bbff' },
            ],
          },
        },
      },
    ],
  }
})

const todos = computed(() => dashboardData.value.todos ?? [])

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

function handleRowClick(row: TodoItem) {
  router.push({ name: 'billDetail', params: { id: String(row.id) } })
}

function handleAction(row: TodoItem, action: string) {
  if (action === 'handle' || action === 'sign' || action === 'collect') {
    router.push({ name: 'billDetail', params: { id: String(row.id) } })
  }
}

async function fetchDashboard() {
  loading.value = true
  try {
    const res = await getDashboard() as DashboardData
    dashboardData.value = res ?? {}
  } catch {
    dashboardData.value = {
      pendingCount: 12,
      disputedCount: 3,
      toSignCount: 5,
      collectingCount: 8,
      completedCount: 42,
      completionRate: 78,
      todos: [
        { id: 1001, billNo: 'R202503001', buyerName: '某某贸易有限公司', status: 'PENDING', updatedAt: '2025-03-17 10:30' },
        { id: 1002, billNo: 'R202503002', buyerName: '某某制造有限公司', status: 'TO_SIGN', updatedAt: '2025-03-16 14:20' },
        { id: 1003, billNo: 'R202503003', buyerName: '某某科技股份有限公司', status: 'DISPUTED', updatedAt: '2025-03-16 09:15' },
        { id: 1004, billNo: 'R202503004', buyerName: '某某建材有限公司', status: 'COLLECTING', updatedAt: '2025-03-15 16:45' },
        { id: 1005, billNo: 'R202503005', buyerName: '某某物流有限公司', status: 'GENERATED', updatedAt: '2025-03-15 11:00' },
      ],
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .stat-row {
    margin-bottom: 20px;
  }

  .stat-card {
    display: flex;
    align-items: center;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
    margin-bottom: 20px;
    transition: transform 0.2s;

    &:hover {
      transform: translateY(-2px);
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;
      color: #fff;
    }

    .stat-content {
      flex: 1;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      line-height: 1.2;
      color: #303133;
    }

    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-top: 4px;
    }

    &.stat-pending .stat-icon {
      background: linear-gradient(135deg, #409eff, #66b1ff);
    }

    &.stat-disputed .stat-icon {
      background: linear-gradient(135deg, #e6a23c, #f0c78a);
    }

    &.stat-sign .stat-icon {
      background: linear-gradient(135deg, #9c27b0, #ba68c8);
    }

    &.stat-collecting .stat-icon {
      background: linear-gradient(135deg, #f56c6c, #f89898);
    }

    &.stat-completed .stat-icon {
      background: linear-gradient(135deg, #67c23a, #95d475);
    }
  }

  .chart-row {
    margin-bottom: 20px;
  }

  .chart-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
    margin-bottom: 20px;

    .chart-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 16px;
    }

    .chart-gauge {
      height: 220px;
    }

    .chart-bar {
      height: 220px;
    }
  }

  .todos-section {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);

    .section-header {
      margin-bottom: 16px;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}
</style>
