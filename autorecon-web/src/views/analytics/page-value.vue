<template>
  <div class="page-value-page">
    <h2 class="page-title">页面价值排行</h2>

    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 260px" @change="fetchData" />
      <el-select v-model="system" placeholder="全部系统" clearable style="width: 130px" @change="fetchData">
        <el-option label="autorecon" value="autorecon" />
        <el-option label="pickup" value="pickup" />
      </el-select>
      <el-select v-model="module" placeholder="全部模块" clearable style="width: 130px" @change="fetchData">
        <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
      </el-select>
    </div>

    <div class="table-card">
      <el-table :data="pagedData" stripe style="width: 100%" :default-sort="{ prop: 'pv', order: 'descending' }"
        @sort-change="handleSort">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="pageName" label="页面名称" min-width="180" sortable="custom" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="pv" label="PV" width="100" sortable="custom" />
        <el-table-column prop="uv" label="UV" width="100" sortable="custom" />
        <el-table-column prop="avgStay" label="平均停留(s)" width="120" sortable="custom" />
        <el-table-column prop="bounceRate" label="跳出率" width="100" sortable="custom">
          <template #default="{ row }">{{ row.bounceRate }}%</template>
        </el-table-column>
        <el-table-column prop="score" label="价值评分" width="100" sortable="custom" />
        <el-table-column label="趋势" width="100">
          <template #default="{ row }">
            <div class="mini-bars">
              <span v-for="(v, i) in row.trend" :key="i" class="mini-bar" :style="{ height: v + '%' }" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="80">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" size="small" effect="dark">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" v-model:current-page="page" v-model:page-size="pageSize"
        :total="sortedData.length" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getPageRanking } from '@/api/analytics'

interface PageRow {
  pageName: string; module: string; pv: number; uv: number
  avgStay: number; bounceRate: number; score: number; level: string
  trend: number[]
}

const dateRange = ref<string[]>([])
const system = ref('')
const module = ref('')
const page = ref(1)
const pageSize = ref(10)
const sortProp = ref('pv')
const sortOrder = ref<'ascending' | 'descending'>('descending')
const modules = ['对账管理', '异议处理', '签章管理', '财务管理', '系统设置', '买方引导']

const tableData = ref<PageRow[]>([])

function levelType(level: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    S: 'success', A: 'primary', B: 'warning', C: 'warning', D: 'danger',
  }
  return map[level] ?? 'info'
}

function generateMockData(): PageRow[] {
  const pages = [
    { pageName: '对账单列表', module: '对账管理' },
    { pageName: '对账单详情', module: '对账管理' },
    { pageName: '发起对账', module: '对账管理' },
    { pageName: '比对结果', module: '对账管理' },
    { pageName: '批量对账', module: '对账管理' },
    { pageName: '异议列表', module: '异议处理' },
    { pageName: '异议详情', module: '异议处理' },
    { pageName: '待签章', module: '签章管理' },
    { pageName: '印章管理', module: '签章管理' },
    { pageName: '付款管理', module: '财务管理' },
    { pageName: '发票管理', module: '财务管理' },
    { pageName: '催收管理', module: '财务管理' },
    { pageName: '模板管理', module: '对账管理' },
    { pageName: 'ERP配置', module: '系统设置' },
    { pageName: '用户管理', module: '系统设置' },
    { pageName: '合同管理', module: '财务管理' },
    { pageName: '买方数据上传', module: '对账管理' },
    { pageName: '引导概览', module: '买方引导' },
    { pageName: '对账日历', module: '对账管理' },
    { pageName: '自动对账', module: '对账管理' },
  ]
  const levels = ['S', 'A', 'A', 'B', 'B', 'B', 'C', 'C', 'D', 'D']
  return pages.map((p, i) => ({
    ...p,
    pv: 5000 - i * 200 + Math.floor(Math.random() * 100),
    uv: 2000 - i * 80 + Math.floor(Math.random() * 50),
    avgStay: +(60 + Math.random() * 120).toFixed(0),
    bounceRate: +(15 + Math.random() * 40).toFixed(1),
    score: +(95 - i * 4 + Math.random() * 5).toFixed(0),
    level: levels[Math.min(i, levels.length - 1)],
    trend: Array.from({ length: 7 }, () => 20 + Math.floor(Math.random() * 80)),
  }))
}

const sortedData = computed(() => {
  let data = [...tableData.value]
  if (module.value) data = data.filter(d => d.module === module.value)
  const prop = sortProp.value as keyof PageRow
  const dir = sortOrder.value === 'ascending' ? 1 : -1
  data.sort((a, b) => {
    const va = a[prop], vb = b[prop]
    if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
    return String(va).localeCompare(String(vb)) * dir
  })
  return data
})

const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return sortedData.value.slice(start, start + pageSize.value)
})

function handleSort({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sortProp.value = prop || 'pv'
  sortOrder.value = order || 'descending'
}

async function fetchData() {
  try {
    const res = await getPageRanking({
      system: system.value || undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      module: module.value || undefined,
    }) as Record<string, unknown>
    const data = (res && 'data' in res ? res.data : res) as PageRow[] | undefined
    if (Array.isArray(data) && data.length) tableData.value = data
  } catch {
    // mock data fallback handled by onMounted
  }
}

onMounted(() => {
  tableData.value = generateMockData()
  fetchData()
})
</script>

<style lang="scss" scoped>
.page-value-page {
  .page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
  .filter-bar {
    display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;
  }
  .table-card {
    background: #fff; border-radius: 8px; padding: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  }
  .pagination { margin-top: 16px; justify-content: flex-end; }
  .mini-bars {
    display: flex; align-items: flex-end; gap: 2px; height: 28px;
  }
  .mini-bar {
    display: inline-block; width: 4px; background: #409eff; border-radius: 1px; min-height: 2px;
  }
}

@media (max-width: 768px) {
  .page-value-page {
    .filter-bar { flex-direction: column; }
    .table-card { padding: 12px; overflow-x: auto; }
  }
}
</style>
