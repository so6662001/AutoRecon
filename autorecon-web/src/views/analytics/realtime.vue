<template>
  <div class="realtime-page">
    <div class="page-header">
      <h2 class="page-title">实时概览</h2>
      <div class="header-actions">
        <el-select v-model="system" placeholder="全部系统" clearable style="width: 140px" @change="fetchData">
          <el-option label="autorecon" value="autorecon" />
          <el-option label="pickup" value="pickup" />
        </el-select>
        <el-switch v-model="autoRefresh" active-text="自动刷新" inactive-text="" style="margin-left: 12px" />
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4" :lg="4" v-for="s in statCards" :key="s.label">
        <div class="stat-card" :style="{ borderTop: `3px solid ${s.color}` }">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="chart-card">
      <div class="chart-title">24小时 PV 趋势</div>
      <v-chart class="chart" :option="pvTrendOption" autoresize />
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">热门页面 Top10</div>
          <v-chart class="chart" :option="hotPagesOption" autoresize />
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">热门操作 Top10</div>
          <v-chart class="chart" :option="hotActionsOption" autoresize />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">系统分布</div>
          <v-chart class="chart-sm" :option="systemPieOption" autoresize />
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">终端分布</div>
          <v-chart class="chart-sm" :option="terminalPieOption" autoresize />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getRealtimeOverview, getHotPages } from '@/api/analytics'

use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const system = ref('')
const autoRefresh = ref(true)
let timer: ReturnType<typeof setInterval> | null = null

const overview = ref({
  todayPV: 12580,
  todayUV: 3420,
  onlineUsers: 186,
  todayEvents: 45230,
  avgStay: '4:32',
})

const pvTrend = ref<number[]>([])
const hotPages = ref<{ name: string; pv: number }[]>([])
const hotActions = ref<{ name: string; count: number }[]>([])

const statCards = computed(() => [
  { label: '今日PV', value: overview.value.todayPV.toLocaleString(), color: '#409eff' },
  { label: '今日UV', value: overview.value.todayUV.toLocaleString(), color: '#67c23a' },
  { label: '当前在线', value: overview.value.onlineUsers.toLocaleString(), color: '#e6a23c' },
  { label: '今日事件', value: overview.value.todayEvents.toLocaleString(), color: '#9c27b0' },
  { label: '平均停留', value: overview.value.avgStay, color: '#f56c6c' },
])

function generateMockPVTrend() {
  return Array.from({ length: 24 }, (_, i) => {
    const base = i >= 9 && i <= 18 ? 800 : 200
    return base + Math.floor(Math.random() * 400)
  })
}

function generateMockHotPages() {
  const pages = ['工作台', '对账单列表', '对账单详情', '发起对账', '比对结果', '异议列表', '付款管理', '签章管理', '模板管理', '催收管理']
  return pages.map((name, i) => ({ name, pv: 1200 - i * 100 + Math.floor(Math.random() * 50) }))
}

function generateMockHotActions() {
  const actions = ['查看详情', '导出Excel', '发起对账', '确认金额', '提交异议', '签章', '催收', '上传数据', '筛选', '打印']
  return actions.map((name, i) => ({ name, count: 980 - i * 80 + Math.floor(Math.random() * 40) }))
}

const pvTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`),
    boundaryGap: false,
  },
  yAxis: { type: 'value' },
  series: [{
    type: 'line',
    data: pvTrend.value,
    smooth: true,
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }] } },
    lineStyle: { color: '#409eff', width: 2 },
    itemStyle: { color: '#409eff' },
  }],
}))

const hotPagesOption = computed(() => {
  const data = [...hotPages.value].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(d => d.name) },
    series: [{ type: 'bar', data: data.map(d => d.pv), itemStyle: { color: '#409eff' } }],
  }
})

const hotActionsOption = computed(() => {
  const data = [...hotActions.value].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(d => d.name) },
    series: [{ type: 'bar', data: data.map(d => d.count), itemStyle: { color: '#67c23a' } }],
  }
})

const systemPieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: '5%' },
  series: [{
    type: 'pie', radius: ['40%', '65%'],
    data: [
      { value: 7860, name: 'autorecon', itemStyle: { color: '#409eff' } },
      { value: 4720, name: 'pickup', itemStyle: { color: '#67c23a' } },
    ],
    label: { formatter: '{b}: {d}%' },
  }],
}))

const terminalPieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: '5%' },
  series: [{
    type: 'pie', radius: ['40%', '65%'],
    data: [
      { value: 6800, name: 'PC', itemStyle: { color: '#409eff' } },
      { value: 3900, name: 'H5', itemStyle: { color: '#e6a23c' } },
      { value: 1880, name: '小程序', itemStyle: { color: '#67c23a' } },
    ],
    label: { formatter: '{b}: {d}%' },
  }],
}))

async function fetchData() {
  try {
    const res = await getRealtimeOverview(system.value || undefined) as Record<string, unknown>
    const data = (res && 'data' in res ? res.data : res) as typeof overview.value | undefined
    if (data) overview.value = data
    const pagesRes = await getHotPages(system.value || undefined) as Record<string, unknown>
    const pData = (pagesRes && 'data' in pagesRes ? pagesRes.data : pagesRes) as typeof hotPages.value | undefined
    if (pData) hotPages.value = pData
  } catch {
    pvTrend.value = generateMockPVTrend()
    hotPages.value = generateMockHotPages()
    hotActions.value = generateMockHotActions()
  }
}

onMounted(() => {
  pvTrend.value = generateMockPVTrend()
  hotPages.value = generateMockHotPages()
  hotActions.value = generateMockHotActions()
  fetchData()
  timer = setInterval(() => {
    if (autoRefresh.value) {
      pvTrend.value = generateMockPVTrend()
      overview.value = {
        ...overview.value,
        onlineUsers: overview.value.onlineUsers + Math.floor(Math.random() * 10) - 5,
      }
    }
  }, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.realtime-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 12px;
  }
  .page-title { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
  .header-actions { display: flex; align-items: center; }
  .stat-row { margin-bottom: 16px; }
  .stat-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px; text-align: center;
  }
  .stat-value { font-size: 28px; font-weight: 700; color: #303133; }
  .stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
  .chart-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
  }
  .chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
  .chart { height: 280px; }
  .chart-sm { height: 240px; }
}

@media (max-width: 768px) {
  .realtime-page {
    .stat-value { font-size: 20px; }
    .chart { height: 220px; }
    .chart-sm { height: 200px; }
  }
}
</style>
