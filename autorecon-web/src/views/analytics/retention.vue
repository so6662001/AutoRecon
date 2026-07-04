<template>
  <div class="retention-page">
    <h2 class="page-title">留存分析</h2>

    <div class="filter-bar">
      <el-select v-model="system" placeholder="全部系统" clearable style="width: 130px" @change="fetchData">
        <el-option label="autorecon" value="autorecon" />
        <el-option label="pickup" value="pickup" />
      </el-select>
      <el-radio-group v-model="mode" @change="refreshData">
        <el-radio-button value="day">日留存</el-radio-button>
        <el-radio-button value="week">周留存</el-radio-button>
      </el-radio-group>
    </div>

    <div class="table-card">
      <div class="chart-title">留存矩阵</div>
      <div class="matrix-scroll">
        <el-table :data="matrixData" border size="small" style="width: 100%">
          <el-table-column prop="cohort" :label="mode === 'day' ? '日期' : '周'" width="110" fixed />
          <el-table-column prop="newUsers" label="新增用户" width="90" />
          <el-table-column v-for="d in periodCols" :key="d" :label="mode === 'day' ? `Day${d}` : `Week${d}`" width="75">
            <template #default="{ row }">
              <div class="cell-rate" :style="{ background: cellColor(row.rates[d]) }">
                {{ row.rates[d] != null ? row.rates[d] + '%' : '-' }}
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="chart-card">
      <div class="chart-title">留存趋势</div>
      <v-chart class="chart" :option="trendOption" autoresize />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getRetentionData } from '@/api/analytics'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const system = ref('')
const mode = ref<'day' | 'week'>('day')

interface MatrixRow {
  cohort: string
  newUsers: number
  rates: Record<number, number | null>
}

const matrixData = ref<MatrixRow[]>([])
const periodCols = computed(() => mode.value === 'day' ? [1, 2, 3, 5, 7, 14, 30] : [1, 2, 3, 4, 8, 12])

function cellColor(rate: number | null) {
  if (rate == null) return 'transparent'
  const intensity = Math.min(rate / 100, 1)
  const r = Math.round(255 - intensity * (255 - 64))
  const g = Math.round(255 - intensity * (255 - 158))
  const b = 255
  return `rgba(${r}, ${g}, ${b}, 0.6)`
}

function generateMockMatrix(): MatrixRow[] {
  const rows: MatrixRow[] = []
  const today = new Date()
  const count = mode.value === 'day' ? 10 : 8
  for (let i = 0; i < count; i++) {
    const d = new Date(today)
    if (mode.value === 'day') {
      d.setDate(d.getDate() - i - 1)
    } else {
      d.setDate(d.getDate() - (i + 1) * 7)
    }
    const label = mode.value === 'day'
      ? `${d.getMonth() + 1}/${d.getDate()}`
      : `第${count - i}周`
    const newUsers = 200 + Math.floor(Math.random() * 300)
    const rates: Record<number, number | null> = {}
    const cols = periodCols.value
    let prev = 100
    for (const col of cols) {
      const maxPeriod = mode.value === 'day' ? i + 1 : (i + 1)
      if (col > maxPeriod * (mode.value === 'day' ? 1 : 4)) {
        rates[col] = null
      } else {
        prev = +(prev * (0.5 + Math.random() * 0.4)).toFixed(1)
        rates[col] = Math.max(+(prev).toFixed(1), 1)
      }
    }
    rows.push({ cohort: label, newUsers, rates })
  }
  return rows
}

const trendOption = computed(() => {
  const keyDays = mode.value === 'day' ? [1, 7, 30] : [1, 4, 12]
  const labels = mode.value === 'day' ? ['Day1', 'Day7', 'Day30'] : ['Week1', 'Week4', 'Week12']
  const colors = ['#409eff', '#67c23a', '#e6a23c']
  const series = keyDays.map((day, idx) => ({
    name: labels[idx],
    type: 'line' as const,
    smooth: true,
    data: matrixData.value.map(r => r.rates[day] ?? 0),
    lineStyle: { color: colors[idx] },
    itemStyle: { color: colors[idx] },
  }))
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: '3%' },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '5%', containLabel: true },
    xAxis: { type: 'category', data: matrixData.value.map(r => r.cohort) },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    series,
  }
})

function refreshData() {
  matrixData.value = generateMockMatrix()
  fetchData()
}

async function fetchData() {
  try {
    const res = await getRetentionData({
      system: system.value || undefined,
      days: mode.value === 'day' ? 30 : 84,
    }) as Record<string, unknown>
    const data = (res && 'data' in res ? res.data : res) as MatrixRow[] | undefined
    if (Array.isArray(data) && data.length) matrixData.value = data
  } catch {
    // mock fallback
  }
}

onMounted(() => {
  matrixData.value = generateMockMatrix()
  fetchData()
})
</script>

<style lang="scss" scoped>
.retention-page {
  .page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
  .filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
  .table-card, .chart-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
  }
  .chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
  .matrix-scroll { overflow-x: auto; }
  .cell-rate {
    text-align: center; font-size: 12px; padding: 4px 0; border-radius: 2px; min-width: 50px;
  }
  .chart { height: 280px; }
}

@media (max-width: 768px) {
  .retention-page {
    .filter-bar { flex-direction: column; align-items: flex-start; }
    .chart { height: 220px; }
  }
}
</style>
