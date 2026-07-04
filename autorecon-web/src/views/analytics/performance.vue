<template>
  <div class="performance-page">
    <h2 class="page-title">性能监控</h2>

    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 260px" @change="fetchData" />
      <el-select v-model="system" placeholder="全部系统" clearable style="width: 130px" @change="fetchData">
        <el-option label="autorecon" value="autorecon" />
        <el-option label="pickup" value="pickup" />
      </el-select>
    </div>

    <el-row :gutter="16" class="gauge-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="g in gauges" :key="g.name">
        <div class="chart-card">
          <div class="chart-title">{{ g.name }}</div>
          <v-chart class="chart-gauge" :option="g.option" autoresize />
          <div class="gauge-value" :style="{ color: g.color }">{{ g.value }}{{ g.unit }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="chart-card">
      <div class="chart-title">性能指标趋势 (7天)</div>
      <v-chart class="chart-line" :option="trendOption" autoresize />
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <div class="table-card">
          <div class="chart-title">页面性能排行</div>
          <el-table :data="pagePerf" stripe size="small" style="width: 100%">
            <el-table-column prop="page" label="页面" min-width="140" />
            <el-table-column prop="fcp" label="FCP(ms)" width="85" />
            <el-table-column prop="lcp" label="LCP(ms)" width="85" />
            <el-table-column prop="fid" label="FID(ms)" width="80" />
            <el-table-column prop="cls" label="CLS" width="70" />
            <el-table-column label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="perfStatus(row).type" size="small">{{ perfStatus(row).label }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="table-card">
          <div class="chart-title">慢接口 Top 10</div>
          <el-table :data="slowApis" stripe size="small" style="width: 100%">
            <el-table-column prop="endpoint" label="接口" min-width="180" />
            <el-table-column prop="avgMs" label="平均(ms)" width="90" />
            <el-table-column prop="p95" label="P95(ms)" width="90" />
            <el-table-column prop="count" label="调用量" width="80" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { GaugeChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getPerformanceOverview } from '@/api/analytics'

use([GaugeChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const dateRange = ref<string[]>([])
const system = ref('')

const metrics = ref({ fcp: 1200, lcp: 2100, fid: 45, cls: 0.08 })

interface PagePerf { page: string; fcp: number; lcp: number; fid: number; cls: number }
interface SlowApi { endpoint: string; avgMs: number; p95: number; count: number }

const pagePerf = ref<PagePerf[]>([])
const slowApis = ref<SlowApi[]>([])
const trendData = ref<{ fcp: number[]; lcp: number[]; fid: number[]; cls: number[] }>({
  fcp: [], lcp: [], fid: [], cls: [],
})

function gaugeColor(value: number, thresholds: [number, number]) {
  if (value <= thresholds[0]) return '#67c23a'
  if (value <= thresholds[1]) return '#e6a23c'
  return '#f56c6c'
}

function makeGaugeOption(value: number, max: number, thresholds: [number, number]) {
  const color = gaugeColor(value, thresholds)
  return {
    series: [{
      type: 'gauge',
      startAngle: 210, endAngle: -30, min: 0, max,
      center: ['50%', '60%'], radius: '85%',
      progress: { show: true, roundCap: true, width: 12, itemStyle: { color } },
      axisLine: { roundCap: true, lineStyle: { width: 12, color: [[1, '#e4e7ed']] } },
      axisTick: { show: false }, splitLine: { show: false },
      axisLabel: { show: false }, anchor: { show: false },
      pointer: { show: false },
      detail: { show: false },
      data: [{ value }],
    }],
  }
}

const gauges = computed(() => [
  {
    name: 'FCP', value: metrics.value.fcp, unit: 'ms', color: gaugeColor(metrics.value.fcp, [1800, 3000]),
    option: makeGaugeOption(metrics.value.fcp, 5000, [1800, 3000]),
  },
  {
    name: 'LCP', value: metrics.value.lcp, unit: 'ms', color: gaugeColor(metrics.value.lcp, [2500, 4000]),
    option: makeGaugeOption(metrics.value.lcp, 6000, [2500, 4000]),
  },
  {
    name: 'FID', value: metrics.value.fid, unit: 'ms', color: gaugeColor(metrics.value.fid, [100, 300]),
    option: makeGaugeOption(metrics.value.fid, 500, [100, 300]),
  },
  {
    name: 'CLS', value: metrics.value.cls, unit: '', color: gaugeColor(metrics.value.cls, [0.1, 0.25]),
    option: makeGaugeOption(metrics.value.cls * 1000, 500, [100, 250]),
  },
])

const trendOption = computed(() => {
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(); d.setDate(d.getDate() - 6 + i)
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: '3%' },
    grid: { left: '3%', right: '4%', bottom: '14%', top: '5%', containLabel: true },
    xAxis: { type: 'category', data: days },
    yAxis: [
      { type: 'value', name: 'ms', position: 'left' },
      { type: 'value', name: 'CLS', position: 'right', max: 0.5 },
    ],
    series: [
      { name: 'FCP', type: 'line', data: trendData.value.fcp, lineStyle: { color: '#409eff' }, itemStyle: { color: '#409eff' } },
      { name: 'LCP', type: 'line', data: trendData.value.lcp, lineStyle: { color: '#e6a23c' }, itemStyle: { color: '#e6a23c' } },
      { name: 'FID', type: 'line', data: trendData.value.fid, lineStyle: { color: '#67c23a' }, itemStyle: { color: '#67c23a' } },
      { name: 'CLS', type: 'line', yAxisIndex: 1, data: trendData.value.cls, lineStyle: { color: '#f56c6c' }, itemStyle: { color: '#f56c6c' } },
    ],
  }
})

function perfStatus(row: PagePerf): { type: 'success' | 'warning' | 'danger'; label: string } {
  if (row.lcp <= 2500 && row.fid <= 100 && row.cls <= 0.1) return { type: 'success', label: '优' }
  if (row.lcp <= 4000 && row.fid <= 300 && row.cls <= 0.25) return { type: 'warning', label: '中' }
  return { type: 'danger', label: '差' }
}

function initMock() {
  trendData.value = {
    fcp: Array.from({ length: 7 }, () => 800 + Math.floor(Math.random() * 800)),
    lcp: Array.from({ length: 7 }, () => 1500 + Math.floor(Math.random() * 1500)),
    fid: Array.from({ length: 7 }, () => 20 + Math.floor(Math.random() * 80)),
    cls: Array.from({ length: 7 }, () => +(Math.random() * 0.2).toFixed(3)),
  }
  pagePerf.value = [
    { page: '对账单列表', fcp: 980, lcp: 1800, fid: 32, cls: 0.05 },
    { page: '对账单详情', fcp: 1200, lcp: 2200, fid: 48, cls: 0.08 },
    { page: '比对结果', fcp: 1500, lcp: 2800, fid: 65, cls: 0.12 },
    { page: '工作台', fcp: 850, lcp: 1600, fid: 28, cls: 0.03 },
    { page: '发起对账', fcp: 1100, lcp: 2000, fid: 42, cls: 0.06 },
    { page: '批量对账', fcp: 1800, lcp: 3200, fid: 95, cls: 0.15 },
    { page: '异议列表', fcp: 1050, lcp: 1900, fid: 38, cls: 0.07 },
    { page: '付款管理', fcp: 1300, lcp: 2400, fid: 52, cls: 0.09 },
    { page: '签章管理', fcp: 2200, lcp: 4100, fid: 180, cls: 0.22 },
    { page: '催收管理', fcp: 1400, lcp: 2600, fid: 58, cls: 0.11 },
  ]
  slowApis.value = [
    { endpoint: '/api/recon/bills/export', avgMs: 2800, p95: 5200, count: 1200 },
    { endpoint: '/api/recon/match/compute', avgMs: 2400, p95: 4800, count: 3600 },
    { endpoint: '/api/recon/bills/batch', avgMs: 2100, p95: 4200, count: 800 },
    { endpoint: '/api/recon/reports/generate', avgMs: 1800, p95: 3500, count: 560 },
    { endpoint: '/api/recon/invoices/tri-match', avgMs: 1600, p95: 3200, count: 420 },
    { endpoint: '/api/recon/bills/list', avgMs: 1200, p95: 2800, count: 8500 },
    { endpoint: '/api/recon/sign/batch', avgMs: 1100, p95: 2400, count: 380 },
    { endpoint: '/api/recon/data/upload', avgMs: 980, p95: 2200, count: 1600 },
    { endpoint: '/api/recon/collection/send', avgMs: 850, p95: 1800, count: 920 },
    { endpoint: '/api/recon/finance/apply', avgMs: 780, p95: 1600, count: 340 },
  ]
}

async function fetchData() {
  try {
    const res = await getPerformanceOverview({
      system: system.value || undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    }) as Record<string, unknown>
    const data = res && 'data' in res ? res.data as Record<string, unknown> : null
    if (data?.metrics) metrics.value = data.metrics as typeof metrics.value
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
.performance-page {
  .page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
  .filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
  .gauge-row { margin-bottom: 0; }
  .chart-card, .table-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
  }
  .chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
  .chart-gauge { height: 160px; }
  .gauge-value { text-align: center; font-size: 22px; font-weight: 700; margin-top: -10px; }
  .chart-line { height: 280px; }
}

@media (max-width: 768px) {
  .performance-page {
    .filter-bar { flex-direction: column; }
    .chart-gauge { height: 130px; }
    .gauge-value { font-size: 18px; }
    .chart-line { height: 220px; }
    .table-card { overflow-x: auto; }
  }
}
</style>
