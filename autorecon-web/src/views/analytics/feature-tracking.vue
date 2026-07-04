<template>
  <div class="feature-tracking-page">
    <h2 class="page-title">功能追踪</h2>

    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 260px" @change="fetchData" />
      <el-select v-model="system" placeholder="全部系统" clearable style="width: 130px" @change="fetchData">
        <el-option label="autorecon" value="autorecon" />
        <el-option label="pickup" value="pickup" />
      </el-select>
    </div>

    <el-row :gutter="16" class="feature-cards">
      <el-col :xs="12" :sm="8" :md="6" v-for="feat in features" :key="feat.name">
        <div class="feature-card">
          <div class="feat-name">{{ feat.name }}</div>
          <div class="feat-adoption">
            <span class="feat-rate">{{ feat.adoptionRate }}%</span>
            <el-progress :percentage="feat.adoptionRate" :stroke-width="6" :show-text="false"
              :color="feat.adoptionRate >= 60 ? '#67c23a' : feat.adoptionRate >= 30 ? '#e6a23c' : '#f56c6c'" />
          </div>
          <div class="feat-meta">
            <span>使用量: {{ feat.usageCount.toLocaleString() }}</span>
          </div>
          <div class="feat-sparkline">
            <span v-for="(v, i) in feat.trend" :key="i" class="spark-bar" :style="{ height: v + '%' }" />
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">功能采用率气泡图</div>
          <v-chart class="chart" :option="bubbleOption" autoresize />
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">功能采用率趋势</div>
          <div class="trend-select">
            <el-select v-model="selectedFeatures" multiple placeholder="选择功能" collapse-tags
              collapse-tags-tooltip style="width: 100%">
              <el-option v-for="f in features" :key="f.name" :label="f.name" :value="f.name" />
            </el-select>
          </div>
          <v-chart class="chart" :option="trendOption" autoresize />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { ScatterChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getFeatureAdoption } from '@/api/analytics'

use([ScatterChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

interface Feature {
  name: string; adoptionRate: number; usageCount: number; enterprises: number
  avgFrequency: number; trend: number[]
}

const dateRange = ref<string[]>([])
const system = ref('')
const selectedFeatures = ref<string[]>(['发起对账', '比对引擎', '电子签章'])

const features = ref<Feature[]>([])

function generateMockFeatures(): Feature[] {
  const names = ['发起对账', '比对引擎', '电子签章', '批量对账', '异议处理', '自动对账',
    '催收管理', '发票管理', '合同管理', '融资管理', '买方引导', '数据上传']
  return names.map(name => ({
    name,
    adoptionRate: +(20 + Math.random() * 70).toFixed(0),
    usageCount: Math.floor(500 + Math.random() * 8000),
    enterprises: Math.floor(10 + Math.random() * 200),
    avgFrequency: +(1 + Math.random() * 15).toFixed(1),
    trend: Array.from({ length: 7 }, () => 20 + Math.floor(Math.random() * 80)),
  }))
}

const bubbleOption = computed(() => ({
  tooltip: {
    formatter: (p: { data: number[]; seriesName: string }) =>
      `${p.seriesName}<br/>企业数: ${p.data[0]}<br/>人均频次: ${p.data[1]}<br/>总使用量: ${p.data[2]}`,
  },
  grid: { left: '3%', right: '8%', bottom: '3%', top: '5%', containLabel: true },
  xAxis: { type: 'value', name: '企业数' },
  yAxis: { type: 'value', name: '人均频次' },
  series: features.value.map(f => ({
    name: f.name,
    type: 'scatter',
    symbolSize: (data: number[]) => Math.sqrt(data[2]) * 1.5,
    data: [[f.enterprises, f.avgFrequency, f.usageCount]],
  })),
}))

const trendOption = computed(() => {
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() - 6 + i)
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#9c27b0', '#00bcd4']
  const selected = features.value.filter(f => selectedFeatures.value.includes(f.name))
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: '3%' },
    grid: { left: '3%', right: '4%', bottom: '14%', top: '5%', containLabel: true },
    xAxis: { type: 'category', data: days },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    series: selected.map((f, i) => ({
      name: f.name, type: 'line', smooth: true,
      data: f.trend.map(v => +(f.adoptionRate * v / 100).toFixed(0)),
      lineStyle: { color: colors[i % colors.length] },
      itemStyle: { color: colors[i % colors.length] },
    })),
  }
})

async function fetchData() {
  try {
    const res = await getFeatureAdoption({
      system: system.value || undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    }) as Record<string, unknown>
    const data = (res && 'data' in res ? res.data : res) as Feature[] | undefined
    if (Array.isArray(data) && data.length) features.value = data
  } catch {
    // mock fallback
  }
}

onMounted(() => {
  features.value = generateMockFeatures()
  fetchData()
})
</script>

<style lang="scss" scoped>
.feature-tracking-page {
  .page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
  .filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
  .feature-cards { margin-bottom: 16px; }
  .feature-card {
    background: #fff; border-radius: 8px; padding: 16px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
  }
  .feat-name { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px; }
  .feat-adoption { margin-bottom: 8px; }
  .feat-rate { font-size: 20px; font-weight: 700; color: #303133; }
  .feat-meta { font-size: 12px; color: #909399; margin-bottom: 8px; }
  .feat-sparkline { display: flex; align-items: flex-end; gap: 2px; height: 24px; }
  .spark-bar {
    display: inline-block; width: 4px; min-height: 2px;
    background: #409eff; border-radius: 1px;
  }
  .chart-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
  }
  .chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
  .trend-select { margin-bottom: 12px; }
  .chart { height: 320px; }
}

@media (max-width: 768px) {
  .feature-tracking-page {
    .filter-bar { flex-direction: column; }
    .chart { height: 260px; }
  }
}
</style>
