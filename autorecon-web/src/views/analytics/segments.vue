<template>
  <div class="segments-page">
    <h2 class="page-title">用户分群</h2>

    <el-row :gutter="16" class="segment-cards">
      <el-col :xs="12" :sm="8" :md="6" v-for="seg in segments" :key="seg.id">
        <div class="segment-card" :class="{ active: selectedId === seg.id }" @click="selectSegment(seg)">
          <div class="seg-name">{{ seg.name }}</div>
          <div class="seg-count">{{ seg.userCount.toLocaleString() }}</div>
          <div class="seg-meta">
            <span class="seg-pct">占比 {{ seg.percentage }}%</span>
            <span class="seg-trend" :class="seg.trend > 0 ? 'up' : 'down'">
              {{ seg.trend > 0 ? '↑' : '↓' }}{{ Math.abs(seg.trend) }}%
            </span>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="chart-card">
      <div class="chart-title">分群对比</div>
      <v-chart class="chart" :option="barOption" autoresize />
    </div>

    <el-dialog v-model="dialogVisible" :title="`${selectedSegment?.name} - 用户列表`" width="680px" destroy-on-close>
      <el-table :data="segmentUsers" stripe size="small" style="width: 100%">
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="company" label="企业" min-width="180" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="lastActive" label="最近活跃" width="140" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getUserSegments, getSegmentUsers as fetchSegUsers } from '@/api/analytics'

use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

interface Segment {
  id: string; name: string; userCount: number; percentage: number; trend: number
}
interface UserRow {
  userId: string; name: string; company: string; role: string; lastActive: string
}

const segments = ref<Segment[]>([])
const selectedId = ref('')
const selectedSegment = ref<Segment | null>(null)
const dialogVisible = ref(false)
const segmentUsers = ref<UserRow[]>([])

function generateMockSegments(): Segment[] {
  return [
    { id: 's1', name: '高活跃卖方', userCount: 1280, percentage: 18.5, trend: 3.2 },
    { id: 's2', name: '低活跃卖方', userCount: 860, percentage: 12.4, trend: -1.8 },
    { id: 's3', name: '高活跃买方', userCount: 2150, percentage: 31.1, trend: 5.6 },
    { id: 's4', name: '低活跃买方', userCount: 1420, percentage: 20.5, trend: -2.3 },
    { id: 's5', name: '新注册用户', userCount: 380, percentage: 5.5, trend: 12.1 },
    { id: 's6', name: '流失预警', userCount: 290, percentage: 4.2, trend: -8.4 },
    { id: 's7', name: '高价值客户', userCount: 420, percentage: 6.1, trend: 1.5 },
    { id: 's8', name: '待激活用户', userCount: 120, percentage: 1.7, trend: -3.6 },
  ]
}

function generateMockUsers(): UserRow[] {
  const names = ['张三', '李四', '王五', '赵六', '钱七', '孙八', '周九', '吴十']
  const companies = ['某某贸易有限公司', '某某制造有限公司', '某某科技股份有限公司', '某某建材有限公司']
  const roles = ['管理员', '操作员', '审核员', '查看者']
  return names.map((name, i) => ({
    userId: `U${1000 + i}`,
    name,
    company: companies[i % companies.length],
    role: roles[i % roles.length],
    lastActive: `2026-04-${String(13 - i).padStart(2, '0')} ${10 + i}:30`,
  }))
}

function selectSegment(seg: Segment) {
  selectedId.value = seg.id
  selectedSegment.value = seg
  segmentUsers.value = generateMockUsers()
  dialogVisible.value = true
  fetchSegmentUsers(seg.id)
}

async function fetchSegmentUsers(segId: string) {
  try {
    const res = await fetchSegUsers(segId, { page: 1, pageSize: 20 }) as Record<string, unknown>
    const data = (res && 'data' in res ? res.data : res) as UserRow[] | undefined
    if (Array.isArray(data) && data.length) segmentUsers.value = data
  } catch {
    // mock fallback
  }
}

const barOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
  xAxis: { type: 'category', data: segments.value.map(s => s.name), axisLabel: { rotate: 30 } },
  yAxis: { type: 'value', name: '用户数' },
  series: [{
    type: 'bar',
    data: segments.value.map(s => s.userCount),
    itemStyle: {
      color: (params: { dataIndex: number }) => {
        const colors = ['#409eff', '#79bbff', '#67c23a', '#95d475', '#e6a23c', '#f56c6c', '#9c27b0', '#b0bec5']
        return colors[params.dataIndex % colors.length]
      },
    },
  }],
}))

async function fetchData() {
  try {
    const res = await getUserSegments({ system: undefined }) as Record<string, unknown>
    const data = (res && 'data' in res ? res.data : res) as Segment[] | undefined
    if (Array.isArray(data) && data.length) segments.value = data
  } catch {
    // mock fallback
  }
}

onMounted(() => {
  segments.value = generateMockSegments()
  fetchData()
})
</script>

<style lang="scss" scoped>
.segments-page {
  .page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
  .segment-cards { margin-bottom: 16px; }
  .segment-card {
    background: #fff; border-radius: 8px; padding: 20px; text-align: center; cursor: pointer;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
    border: 2px solid transparent; transition: all 0.2s;
    &:hover { border-color: #409eff; transform: translateY(-2px); }
    &.active { border-color: #409eff; background: #ecf5ff; }
  }
  .seg-name { font-size: 14px; color: #606266; margin-bottom: 8px; }
  .seg-count { font-size: 28px; font-weight: 700; color: #303133; margin-bottom: 6px; }
  .seg-meta { display: flex; justify-content: center; gap: 12px; font-size: 13px; }
  .seg-pct { color: #909399; }
  .seg-trend { &.up { color: #67c23a; } &.down { color: #f56c6c; } }
  .chart-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
  }
  .chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
  .chart { height: 300px; }
}

@media (max-width: 768px) {
  .segments-page {
    .seg-count { font-size: 22px; }
    .chart { height: 240px; }
  }
}
</style>
