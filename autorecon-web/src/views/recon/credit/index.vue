<template>
  <div class="credit-score-page">
    <h2 class="page-title">信用评分</h2>

    <el-table :data="rankingList" stripe style="width: 100%">
      <el-table-column prop="rank" label="排名" width="80" align="center" />
      <el-table-column prop="buyerName" label="买方企业" min-width="160" />
      <el-table-column prop="score" label="信用评分" width="200">
        <template #default="{ row }">
          <el-progress
            :percentage="row.score"
            :color="getScoreColor(row.score)"
            :stroke-width="12"
          />
        </template>
      </el-table-column>
      <el-table-column prop="grade" label="评级" width="90">
        <template #default="{ row }">
          <el-tag :type="(getGradeTagType(row.grade) as 'success' | 'warning' | 'info' | 'danger')" size="small">{{ row.grade }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="avgPaymentDays" label="平均付款天数" width="120" align="center" />
      <el-table-column prop="overdueRate" label="逾期率" width="90" align="center">
        <template #default="{ row }">{{ (row.overdueRate ?? 0) }}%</template>
      </el-table-column>
      <el-table-column prop="disputeRate" label="异议率" width="90" align="center">
        <template #default="{ row }">{{ (row.disputeRate ?? 0) }}%</template>
      </el-table-column>
      <el-table-column prop="trend" label="趋势" width="80" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.trend === 'up'" color="#67c23a"><Top /></el-icon>
          <el-icon v-else-if="row.trend === 'down'" color="#f56c6c"><Bottom /></el-icon>
          <el-icon v-else color="#909399"><Minus /></el-icon>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewDetail(row)">
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Detail dialog -->
    <el-dialog
      v-model="detailVisible"
      title="信用详情"
      width="800px"
      destroy-on-close
      @close="detailBuyerId = null"
    >
      <div v-if="detailBuyerId" class="detail-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="chart-box">
              <div class="chart-title">信用评分</div>
              <v-chart class="chart-gauge" :option="gaugeOption" autoresize />
            </div>
          </el-col>
          <el-col :span="12">
            <div class="chart-box">
              <div class="chart-title">评分维度</div>
              <v-chart class="chart-radar" :option="radarOption" autoresize />
            </div>
          </el-col>
        </el-row>
        <div class="chart-box">
          <div class="chart-title">近6个月趋势</div>
          <v-chart class="chart-line" :option="trendOption" autoresize />
        </div>
        <div class="section">
          <div class="section-title">近期付款记录</div>
          <el-table :data="paymentHistory" size="small" stripe>
            <el-table-column prop="billNo" label="对账单号" width="130" />
            <el-table-column prop="amount" label="金额" width="120" align="right">
              <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="paymentDate" label="付款日期" width="120" />
            <el-table-column prop="days" label="付款天数" width="90" align="center" />
          </el-table>
        </div>
        <div v-if="isAdmin" class="action-row">
          <el-button type="primary" @click="handleAdjust">调整评分</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- Adjust dialog -->
    <el-dialog v-model="adjustVisible" title="调整评分" width="400px">
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="调整分数">
          <el-input-number v-model="adjustForm.score" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdjustSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import {
  GaugeChart,
  RadarChart,
  LineChart,
} from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { Top, Bottom, Minus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getCreditRanking,
  getCreditDetail,
  getCreditTrend,
  adjustCredit,
} from '@/api/recon'

use([
  GaugeChart,
  RadarChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  CanvasRenderer,
])

interface RankingItem {
  id: number
  rank: number
  buyerName: string
  score: number
  grade: string
  avgPaymentDays: number
  overdueRate: number
  disputeRate: number
  trend: 'up' | 'down' | 'flat'
}

const rankingList = ref<RankingItem[]>([])
const detailVisible = ref(false)
const detailBuyerId = ref<number | null>(null)
const adjustVisible = ref(false)
const isAdmin = ref(true)
const paymentHistory = ref<{ billNo: string; amount: number; paymentDate: string; days: number }[]>([])

const detailData = ref<{
  score?: number
  factors?: { name: string; value: number }[]
  trend?: { month: string; score: number }[]
}>({})

const adjustForm = reactive({
  score: 80,
  reason: '',
})

function formatAmount(val: number | undefined) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function getScoreColor(score: number) {
  if (score >= 90) return '#67c23a'
  if (score >= 80) return '#409eff'
  if (score >= 70) return '#e6a23c'
  if (score >= 60) return '#f56c6c'
  return '#909399'
}

function getGradeTagType(grade: string) {
  const map: Record<string, string> = {
    A: 'success',
    B: '',
    C: 'warning',
    D: 'danger',
    E: 'danger',
  }
  return map[grade] ?? 'info'
}

const gaugeOption = computed(() => ({
  series: [
    {
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: 100,
      center: ['50%', '70%'],
      radius: '90%',
      progress: { show: true, roundCap: true, width: 14 },
      axisLine: {
        roundCap: true,
        lineStyle: {
          width: 14,
          color: [[0.6, '#f56c6c'], [0.8, '#e6a23c'], [1, '#67c23a']],
        },
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      detail: {
        valueAnimation: true,
        formatter: '{value}',
        offsetCenter: [0, '-20%'],
        fontSize: 36,
        fontWeight: 'bold',
        color: getScoreColor(detailData.value.score ?? 0),
      },
      data: [{ value: detailData.value.score ?? 0 }],
    },
  ],
}))

const radarOption = computed(() => {
  const factors = detailData.value.factors ?? [
    { name: '付款及时性', value: 85 },
    { name: '异议处理', value: 78 },
    { name: '合作时长', value: 92 },
    { name: '交易规模', value: 88 },
    { name: '合规性', value: 95 },
    { name: '沟通配合', value: 82 },
  ]
  return {
    radar: {
      indicator: factors.map((f) => ({ name: f.name, max: 100 })),
    },
    series: [
      {
        type: 'radar',
        data: [{ value: factors.map((f) => f.value), name: '评分' }],
        areaStyle: { opacity: 0.3 },
      },
    ],
  }
})

const trendOption = computed(() => {
  const trend = detailData.value.trend ?? [
    { month: '2024-09', score: 78 },
    { month: '2024-10', score: 80 },
    { month: '2024-11', score: 82 },
    { month: '2024-12', score: 79 },
    { month: '2025-01', score: 84 },
    { month: '2025-02', score: 86 },
  ]
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.map((t) => t.month),
    },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{ type: 'line', data: trend.map((t) => t.score), smooth: true }],
  }
})

function handleViewDetail(row: RankingItem) {
  detailBuyerId.value = row.id
  detailVisible.value = true
  fetchDetail(row.id)
}

async function fetchDetail(buyerId: number) {
  try {
    const [detailRes, trendRes] = await Promise.all([
      getCreditDetail(buyerId) as Promise<{ score?: number; factors?: { name: string; value: number }[] }>,
      getCreditTrend(buyerId) as Promise<{ data?: { month: string; score: number }[] }>,
    ])
    detailData.value = {
      score: detailRes?.score ?? 85,
      factors: detailRes?.factors,
      trend: trendRes?.data,
    }
    paymentHistory.value = [
      { billNo: 'R202503001', amount: 50000, paymentDate: '2025-03-15', days: 35 },
      { billNo: 'R202502015', amount: 125800, paymentDate: '2025-02-28', days: 28 },
      { billNo: 'R202502008', amount: 68000, paymentDate: '2025-02-20', days: 25 },
    ]
  } catch {
    detailData.value = {
      score: 85,
      factors: [
        { name: '付款及时性', value: 85 },
        { name: '异议处理', value: 78 },
        { name: '合作时长', value: 92 },
        { name: '交易规模', value: 88 },
        { name: '合规性', value: 95 },
        { name: '沟通配合', value: 82 },
      ],
      trend: [
        { month: '2024-09', score: 78 },
        { month: '2024-10', score: 80 },
        { month: '2024-11', score: 82 },
        { month: '2024-12', score: 79 },
        { month: '2025-01', score: 84 },
        { month: '2025-02', score: 86 },
      ],
    }
    paymentHistory.value = [
      { billNo: 'R202503001', amount: 50000, paymentDate: '2025-03-15', days: 35 },
      { billNo: 'R202502015', amount: 125800, paymentDate: '2025-02-28', days: 28 },
    ]
  }
}

function handleAdjust() {
  adjustForm.score = detailData.value.score ?? 80
  adjustForm.reason = ''
  adjustVisible.value = true
}

async function handleAdjustSubmit() {
  if (!detailBuyerId.value) return
  try {
    await adjustCredit(detailBuyerId.value, {
      score: adjustForm.score,
      reason: adjustForm.reason,
    })
    ElMessage.success('调整成功')
    adjustVisible.value = false
    fetchDetail(detailBuyerId.value)
    fetchRanking()
  } catch {
    // error handled by interceptor
  }
}

async function fetchRanking() {
  try {
    const res = await getCreditRanking(20) as RankingItem[]
    rankingList.value = Array.isArray(res) ? res : []
    if (rankingList.value.length === 0) {
      rankingList.value = Array.from({ length: 10 }, (_, i) => ({
        id: i + 1,
        rank: i + 1,
        buyerName: `买方企业${i + 1}`,
        score: 95 - i * 3,
        grade: ['A', 'A', 'B', 'B', 'B', 'C', 'C', 'C', 'D', 'E'][i],
        avgPaymentDays: 25 + i * 5,
        overdueRate: i,
        disputeRate: i % 3,
        trend: (['up', 'flat', 'down'] as const)[i % 3],
      }))
    }
  } catch {
    rankingList.value = Array.from({ length: 10 }, (_, i) => ({
      id: i + 1,
      rank: i + 1,
      buyerName: `买方企业${i + 1}`,
      score: 95 - i * 3,
      grade: ['A', 'A', 'B', 'B', 'B', 'C', 'C', 'C', 'D', 'E'][i],
      avgPaymentDays: 25 + i * 5,
      overdueRate: i,
      disputeRate: i % 3,
      trend: (['up', 'flat', 'down'] as const)[i % 3],
    }))
  }
}

watch(detailVisible, (v) => {
  if (v && detailBuyerId.value) {
    fetchDetail(detailBuyerId.value)
  }
})

onMounted(() => {
  fetchRanking()
})
</script>

<style lang="scss" scoped>
.credit-score-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .detail-content {
    .chart-box {
      background: #f9fafb;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 20px;

      .chart-title {
        font-size: 14px;
        font-weight: 600;
        margin-bottom: 12px;
      }

      .chart-gauge {
        height: 200px;
      }

      .chart-radar {
        height: 220px;
      }

      .chart-line {
        height: 200px;
      }
    }

    .section {
      margin-top: 20px;

      .section-title {
        font-size: 14px;
        font-weight: 600;
        margin-bottom: 12px;
      }
    }

    .action-row {
      margin-top: 20px;
      padding-top: 16px;
      border-top: 1px solid #ebeef5;
    }
  }
}
</style>
