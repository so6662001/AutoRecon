<template>
  <div class="user-path-page">
    <h2 class="page-title">用户路径分析</h2>

    <div class="filter-bar">
      <el-select v-model="startPage" placeholder="起始页面" clearable style="width: 160px" @change="fetchData">
        <el-option v-for="p in pageList" :key="p" :label="p" :value="p" />
      </el-select>
      <el-select v-model="endPage" placeholder="目标页面" clearable style="width: 160px" @change="fetchData">
        <el-option v-for="p in pageList" :key="p" :label="p" :value="p" />
      </el-select>
      <el-select v-model="system" placeholder="全部系统" clearable style="width: 130px" @change="fetchData">
        <el-option label="autorecon" value="autorecon" />
        <el-option label="pickup" value="pickup" />
      </el-select>
    </div>

    <div class="chart-card">
      <div class="chart-title">页面流转桑基图</div>
      <v-chart class="chart-sankey" :option="sankeyOption" autoresize />
    </div>

    <div class="table-card">
      <div class="chart-title">Top 10 用户路径</div>
      <el-table :data="topPaths" stripe style="width: 100%">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="路径" min-width="400">
          <template #default="{ row }">
            <div class="path-flow">
              <span v-for="(step, i) in row.steps" :key="i">
                <el-tag size="small" effect="plain">{{ step }}</el-tag>
                <span v-if="Number(i) < row.steps.length - 1" class="path-arrow">→</span>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="users" label="用户数" width="100" />
        <el-table-column prop="sessions" label="会话数" width="100" />
        <el-table-column prop="avgDuration" label="平均时长(s)" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { SankeyChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getUserPaths } from '@/api/analytics'

use([SankeyChart, TooltipComponent, CanvasRenderer])

const system = ref('')
const startPage = ref('')
const endPage = ref('')

const pageList = ['登录', '工作台', '对账单列表', '对账单详情', '发起对账', '比对结果', '异议列表', '待签章', '付款管理', '买方数据上传']

interface SankeyNode { name: string }
interface SankeyLink { source: string; target: string; value: number }
interface PathRow { steps: string[]; users: number; sessions: number; avgDuration: number }

const nodes = ref<SankeyNode[]>([])
const links = ref<SankeyLink[]>([])
const topPaths = ref<PathRow[]>([])

function generateMockSankey() {
  const n: SankeyNode[] = [
    { name: '登录' }, { name: '工作台' }, { name: '对账单列表' },
    { name: '对账单详情' }, { name: '发起对账' }, { name: '比对结果' },
    { name: '异议列表' }, { name: '待签章' }, { name: '付款管理' },
    { name: '买方数据上传' },
  ]
  const l: SankeyLink[] = [
    { source: '登录', target: '工作台', value: 3200 },
    { source: '工作台', target: '对账单列表', value: 2400 },
    { source: '工作台', target: '发起对账', value: 600 },
    { source: '工作台', target: '异议列表', value: 300 },
    { source: '对账单列表', target: '对账单详情', value: 1800 },
    { source: '对账单列表', target: '发起对账', value: 400 },
    { source: '对账单详情', target: '比对结果', value: 1200 },
    { source: '对账单详情', target: '待签章', value: 500 },
    { source: '比对结果', target: '待签章', value: 800 },
    { source: '比对结果', target: '异议列表', value: 350 },
    { source: '待签章', target: '付款管理', value: 600 },
    { source: '工作台', target: '买方数据上传', value: 200 },
    { source: '发起对账', target: '对账单详情', value: 500 },
  ]
  return { nodes: n, links: l }
}

function generateMockPaths(): PathRow[] {
  return [
    { steps: ['登录', '工作台', '对账单列表', '对账单详情', '比对结果'], users: 1200, sessions: 1800, avgDuration: 245 },
    { steps: ['登录', '工作台', '对账单列表', '对账单详情', '待签章'], users: 860, sessions: 1100, avgDuration: 198 },
    { steps: ['登录', '工作台', '发起对账', '对账单详情'], users: 540, sessions: 680, avgDuration: 312 },
    { steps: ['登录', '工作台', '对账单列表', '对账单详情', '比对结果', '待签章'], users: 480, sessions: 520, avgDuration: 380 },
    { steps: ['登录', '工作台', '异议列表'], users: 360, sessions: 420, avgDuration: 156 },
    { steps: ['工作台', '对账单列表', '对账单详情'], users: 320, sessions: 400, avgDuration: 120 },
    { steps: ['登录', '工作台', '买方数据上传'], users: 280, sessions: 310, avgDuration: 95 },
    { steps: ['对账单列表', '对账单详情', '比对结果', '异议列表'], users: 220, sessions: 260, avgDuration: 210 },
    { steps: ['比对结果', '待签章', '付款管理'], users: 190, sessions: 230, avgDuration: 175 },
    { steps: ['登录', '工作台', '对账单列表'], users: 160, sessions: 200, avgDuration: 68 },
  ]
}

const sankeyOption = computed(() => ({
  tooltip: { trigger: 'item', triggerOn: 'mousemove' },
  series: [{
    type: 'sankey',
    data: nodes.value,
    links: links.value,
    emphasis: { focus: 'adjacency' },
    lineStyle: { color: 'gradient', curveness: 0.5 },
    label: { fontSize: 12 },
    layoutIterations: 32,
  }],
}))

async function fetchData() {
  try {
    const res = await getUserPaths({
      system: system.value || undefined,
      startPage: startPage.value || undefined,
      endPage: endPage.value || undefined,
    }) as Record<string, unknown>
    const data = res && 'data' in res ? res.data as { nodes?: SankeyNode[]; links?: SankeyLink[]; paths?: PathRow[] } : null
    if (data?.nodes) nodes.value = data.nodes
    if (data?.links) links.value = data.links
    if (data?.paths) topPaths.value = data.paths
  } catch {
    // mock fallback
  }
}

onMounted(() => {
  const mock = generateMockSankey()
  nodes.value = mock.nodes
  links.value = mock.links
  topPaths.value = generateMockPaths()
  fetchData()
})
</script>

<style lang="scss" scoped>
.user-path-page {
  .page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
  .filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
  .chart-card, .table-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin-bottom: 16px;
  }
  .chart-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
  .chart-sankey { height: 400px; }
  .path-flow { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
  .path-arrow { color: #909399; margin: 0 2px; }
}

@media (max-width: 768px) {
  .user-path-page {
    .filter-bar { flex-direction: column; }
    .chart-sankey { height: 300px; }
    .table-card { overflow-x: auto; }
  }
}
</style>
