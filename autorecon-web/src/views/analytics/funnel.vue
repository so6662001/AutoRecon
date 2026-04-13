<template>
  <div class="funnel-page">
    <h2 class="page-title">漏斗分析</h2>

    <div class="filter-bar">
      <el-radio-group v-model="activeFunnel" @change="onFunnelChange">
        <el-radio-button v-for="f in funnels" :key="f.id" :value="f.id">{{ f.name }}</el-radio-button>
      </el-radio-group>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 260px; margin-left: 12px" @change="fetchData" />
    </div>

    <div class="chart-card">
      <div class="chart-title">{{ currentFunnel?.name }} - 转化漏斗</div>
      <v-chart class="chart-funnel" :option="funnelOption" autoresize />
    </div>

    <div class="conversion-card">
      <div class="chart-title">各步骤转化率</div>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="6" v-for="(step, i) in funnelSteps" :key="i">
          <div class="step-card">
            <div class="step-name">{{ step.name }}</div>
            <div class="step-count">{{ step.count.toLocaleString() }}</div>
            <template v-if="i > 0">
              <div class="step-rate" :class="rateClass(step.rate)">
                转化率: {{ step.rate }}%
              </div>
              <el-progress :percentage="step.rate" :stroke-width="6" :show-text="false"
                :color="rateColor(step.rate)" />
            </template>
            <div v-else class="step-rate step-rate-start">起始步骤</div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { FunnelChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getFunnelData } from '@/api/analytics'

use([FunnelChart, TooltipComponent, LegendComponent, CanvasRenderer])

interface FunnelConfig { id: number; name: string; steps: string[] }
interface StepData { name: string; count: number; rate: number }

const funnels: FunnelConfig[] = [
  { id: 1, name: '对账全流程', steps: ['发起对账', '生成对账单', '比对完成', '签章确认', '付款完成'] },
  { id: 2, name: '买方转化', steps: ['邀请发送', '邀请打开', '注册完成', '首次登录', '首次确认'] },
  { id: 3, name: '提货流程', steps: ['创建提货单', '确认提货', '出库完成', '签收确认'] },
  { id: 4, name: '数据上传', steps: ['进入上传页', '选择文件', '上传完成', '数据校验', '匹配成功'] },
]

const activeFunnel = ref(1)
const dateRange = ref<string[]>([])
const funnelSteps = ref<StepData[]>([])

const currentFunnel = computed(() => funnels.find(f => f.id === activeFunnel.value))

function rateClass(rate: number) {
  if (rate >= 60) return 'rate-good'
  if (rate >= 30) return 'rate-warn'
  return 'rate-bad'
}

function rateColor(rate: number) {
  if (rate >= 60) return '#67c23a'
  if (rate >= 30) return '#e6a23c'
  return '#f56c6c'
}

const funnelOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: '5%' },
  series: [{
    type: 'funnel',
    left: '10%', top: 30, bottom: 60, width: '80%',
    min: 0, max: funnelSteps.value[0]?.count || 100,
    minSize: '0%', maxSize: '100%',
    sort: 'descending', gap: 2,
    label: { show: true, position: 'inside', formatter: '{b}\n{c}' },
    emphasis: { label: { fontSize: 16 } },
    data: funnelSteps.value.map(s => ({ name: s.name, value: s.count })),
  }],
}))

function onFunnelChange() {
  const config = currentFunnel.value
  if (config) {
    const base = 5000 + Math.floor(Math.random() * 2000)
    let count = base
    funnelSteps.value = config.steps.map((name, i) => {
      if (i > 0) count = Math.floor(count * (0.5 + Math.random() * 0.35))
      return { name, count, rate: i === 0 ? 100 : +((count / base) * 100).toFixed(1) }
    })
  }
  fetchData()
}

async function fetchData() {
  try {
    const res = await getFunnelData(activeFunnel.value, {
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    }) as Record<string, unknown>
    const data = (res && 'data' in res ? res.data : res) as StepData[] | undefined
    if (Array.isArray(data) && data.length) funnelSteps.value = data
  } catch {
    // mock fallback
  }
}

onMounted(() => {
  onFunnelChange()
})
</script>

<style lang="scss" scoped>
.funnel-page {
  .page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
  .filter-bar { display: flex; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
  .chart-card, .conversion-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
  }
  .chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
  .chart-funnel { height: 360px; }
  .step-card {
    text-align: center; padding: 16px; border: 1px solid #ebeef5; border-radius: 8px; margin-bottom: 12px;
  }
  .step-name { font-size: 14px; color: #606266; margin-bottom: 8px; }
  .step-count { font-size: 24px; font-weight: 700; color: #303133; margin-bottom: 8px; }
  .step-rate { font-size: 13px; margin-bottom: 8px;
    &.rate-good { color: #67c23a; }
    &.rate-warn { color: #e6a23c; }
    &.rate-bad { color: #f56c6c; }
    &.step-rate-start { color: #909399; }
  }
}

@media (max-width: 768px) {
  .funnel-page {
    .filter-bar { flex-direction: column; align-items: flex-start; }
    .chart-funnel { height: 280px; }
  }
}
</style>
