<template>
  <div class="device-page">
    <h2 class="page-title">终端分析</h2>

    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 260px" @change="fetchData" />
      <el-select v-model="system" placeholder="全部系统" clearable style="width: 130px" @change="fetchData">
        <el-option label="autorecon" value="autorecon" />
        <el-option label="pickup" value="pickup" />
      </el-select>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :md="6" v-for="chart in pieCharts" :key="chart.title">
        <div class="chart-card">
          <div class="chart-title">{{ chart.title }}</div>
          <v-chart class="chart-pie" :option="chart.option" autoresize />
        </div>
      </el-col>
    </el-row>

    <div class="chart-card">
      <div class="chart-title">移动端 vs PC端 趋势 (30天)</div>
      <v-chart class="chart-line" :option="trendOption" autoresize />
    </div>

    <div class="table-card">
      <div class="chart-title">屏幕分辨率 Top 10</div>
      <el-table :data="resolutions" stripe size="small" style="width: 100%">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="resolution" label="分辨率" width="160" />
        <el-table-column prop="sessions" label="会话数" width="120" />
        <el-table-column prop="percentage" label="占比" width="100">
          <template #default="{ row }">{{ row.percentage }}%</template>
        </el-table-column>
        <el-table-column label="占比条" min-width="200">
          <template #default="{ row }">
            <el-progress :percentage="row.percentage" :stroke-width="10" :show-text="false" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getDeviceDistribution } from '@/api/analytics'

use([PieChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const dateRange = ref<string[]>([])
const system = ref('')

interface DistData { name: string; value: number }

const platformData = ref<DistData[]>([])
const osData = ref<DistData[]>([])
const browserData = ref<DistData[]>([])
const networkData = ref<DistData[]>([])
const mobileTrend = ref<number[]>([])
const pcTrend = ref<number[]>([])

interface Resolution { resolution: string; sessions: number; percentage: number }
const resolutions = ref<Resolution[]>([])

function initMock() {
  platformData.value = [
    { name: 'PC', value: 5800 }, { name: 'H5', value: 3200 }, { name: '小程序', value: 1600 },
  ]
  osData.value = [
    { name: 'Windows', value: 4200 }, { name: 'macOS', value: 1800 },
    { name: 'Android', value: 2600 }, { name: 'iOS', value: 1800 }, { name: 'Linux', value: 200 },
  ]
  browserData.value = [
    { name: 'Chrome', value: 5600 }, { name: 'Safari', value: 1400 },
    { name: 'Edge', value: 1200 }, { name: 'Firefox', value: 600 }, { name: '其他', value: 800 },
  ]
  networkData.value = [
    { name: 'WiFi', value: 6200 }, { name: '4G', value: 2800 },
    { name: '5G', value: 1200 }, { name: '3G', value: 400 },
  ]
  mobileTrend.value = Array.from({ length: 30 }, () => 150 + Math.floor(Math.random() * 100))
  pcTrend.value = Array.from({ length: 30 }, () => 250 + Math.floor(Math.random() * 120))
  resolutions.value = [
    { resolution: '1920×1080', sessions: 3200, percentage: 30.2 },
    { resolution: '1366×768', sessions: 1800, percentage: 17.0 },
    { resolution: '2560×1440', sessions: 1200, percentage: 11.3 },
    { resolution: '375×812', sessions: 980, percentage: 9.2 },
    { resolution: '414×896', sessions: 860, percentage: 8.1 },
    { resolution: '1536×864', sessions: 720, percentage: 6.8 },
    { resolution: '390×844', sessions: 580, percentage: 5.5 },
    { resolution: '1440×900', sessions: 480, percentage: 4.5 },
    { resolution: '360×780', sessions: 420, percentage: 4.0 },
    { resolution: '1280×720', sessions: 360, percentage: 3.4 },
  ]
}

function makePieOption(data: DistData[], colors?: string[]) {
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: '2%', itemWidth: 10, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['38%', '62%'],
      data: data.map((d, i) => ({
        ...d,
        itemStyle: colors ? { color: colors[i % colors.length] } : undefined,
      })),
      label: { formatter: '{d}%', fontSize: 11 },
    }],
  }
}

const pieCharts = computed(() => [
  { title: '平台分布', option: makePieOption(platformData.value, ['#409eff', '#e6a23c', '#67c23a']) },
  { title: '操作系统', option: makePieOption(osData.value, ['#409eff', '#303133', '#67c23a', '#909399', '#e6a23c']) },
  { title: '浏览器', option: makePieOption(browserData.value, ['#409eff', '#5856d6', '#007aff', '#e6a23c', '#b0bec5']) },
  { title: '网络类型', option: makePieOption(networkData.value, ['#67c23a', '#409eff', '#9c27b0', '#e6a23c']) },
])

const trendOption = computed(() => {
  const days = Array.from({ length: 30 }, (_, i) => {
    const d = new Date(); d.setDate(d.getDate() - 29 + i)
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: '3%' },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '5%', containLabel: true },
    xAxis: { type: 'category', data: days, axisLabel: { interval: 4 } },
    yAxis: { type: 'value' },
    series: [
      { name: 'PC', type: 'line', smooth: true, data: pcTrend.value, lineStyle: { color: '#409eff' }, itemStyle: { color: '#409eff' } },
      { name: '移动端', type: 'line', smooth: true, data: mobileTrend.value, lineStyle: { color: '#67c23a' }, itemStyle: { color: '#67c23a' } },
    ],
  }
})

async function fetchData() {
  try {
    const res = await getDeviceDistribution({
      system: system.value || undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    }) as Record<string, unknown>
    const data = res && 'data' in res ? res.data as Record<string, unknown> : null
    if (data) {
      if (Array.isArray(data.platform)) platformData.value = data.platform as DistData[]
      if (Array.isArray(data.os)) osData.value = data.os as DistData[]
      if (Array.isArray(data.browser)) browserData.value = data.browser as DistData[]
      if (Array.isArray(data.network)) networkData.value = data.network as DistData[]
    }
  } catch {
    // mock fallback
  }
}

onMounted(() => {
  initMock()
  fetchData()
})
</script>

<style lang="scss" scoped>
.device-page {
  .page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
  .filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
  .chart-card, .table-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
  }
  .chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
  .chart-pie { height: 220px; }
  .chart-line { height: 280px; }
}

@media (max-width: 768px) {
  .device-page {
    .filter-bar { flex-direction: column; }
    .chart-pie { height: 200px; }
    .chart-line { height: 220px; }
    .table-card { overflow-x: auto; }
  }
}
</style>
