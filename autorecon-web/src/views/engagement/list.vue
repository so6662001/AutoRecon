<template>
  <div class="engagement-page">
    <h2 class="page-title">买方引导</h2>

    <el-card class="funnel-card">
      <template #header>转化漏斗</template>
      <div class="funnel-chart">
        <div v-for="step in funnelSteps" :key="step.key" class="funnel-step">
          <div class="step-bar" :style="{ width: step.percent + '%' }" />
          <div class="step-label">{{ step.label }}</div>
          <div class="step-value">{{ step.count }} {{ step.suffix }}</div>
        </div>
      </div>
    </el-card>

    <el-card v-if="alerts.length > 0" class="alert-card">
      <template #header>需关注</template>
      <el-row :gutter="16">
        <el-col v-for="a in alerts" :key="a.key" :span="8">
          <div class="alert-item">
            <span class="alert-desc">{{ a.desc }}</span>
            <el-tag type="warning" size="small">{{ a.count }}家</el-tag>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <div class="action-bar">
      <el-button type="primary" @click="fetchBuyers">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="buyers" stripe>
      <el-table-column prop="companyName" label="公司名称" min-width="160" />
      <el-table-column prop="level" label="等级" width="100">
        <template #default="{ row }">
          <el-tag :type="getLevelTagType(row.level) as 'success' | 'primary' | 'warning' | 'info' | 'danger'" size="small">
            {{ row.level }} - {{ getLevelDesc(row.level) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastActiveAt" label="最近活跃" width="120" />
      <el-table-column prop="billsSent" label="已发送" width="90" align="right" />
      <el-table-column prop="billsConfirmed" label="已确认" width="90" align="right" />
      <el-table-column prop="billsIgnored" label="已忽略" width="90" align="right" />
      <el-table-column prop="suggestedAction" label="建议操作" min-width="120" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.canInvite"
            type="primary"
            link
            size="small"
            @click="handleInvite(row)"
          >
            发送邀请
          </el-button>
          <el-button type="primary" link size="small" @click="handleViewDetail(row)">
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchBuyers"
        @current-change="fetchBuyers"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listEngagementBuyers, getEngagementFunnel, sendEngagementInvite } from '@/api/system'

interface FunnelData {
  sent?: number
  opened?: number
  confirmed?: number
  registered?: number
  uploaded?: number
  sealed?: number
}

interface BuyerItem {
  id: number
  companyName: string
  level: string
  lastActiveAt?: string
  billsSent?: number
  billsConfirmed?: number
  billsIgnored?: number
  suggestedAction?: string
  canInvite?: boolean
}

const loading = ref(false)
const funnel = ref<FunnelData | null>(null)
const buyers = ref<BuyerItem[]>([])
const alerts = ref<Array<{ key: string; desc: string; count: number }>>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const funnelSteps = computed(() => {
  const f = funnel.value
  const sent = f?.sent ?? 0
  const opened = f?.opened ?? 0
  const confirmed = f?.confirmed ?? 0
  const registered = f?.registered ?? 0
  const uploaded = f?.uploaded ?? 0
  const sealed = f?.sealed ?? 0
  const max = Math.max(sent, 1)
  return [
    { key: 'sent', label: '已发送链接', count: sent, percent: 100, suffix: '' },
    { key: 'opened', label: '已打开', count: opened, percent: (opened / max) * 100, suffix: `(${sent ? ((opened / sent) * 100).toFixed(0) : 0}%)` },
    { key: 'confirmed', label: '已确认', count: confirmed, percent: (confirmed / max) * 100, suffix: `(${opened ? ((confirmed / opened) * 100).toFixed(0) : 0}%)` },
    { key: 'registered', label: '已注册', count: registered, percent: (registered / max) * 100, suffix: `(${confirmed ? ((registered / confirmed) * 100).toFixed(0) : 0}%)` },
    { key: 'uploaded', label: '已上传数据', count: uploaded, percent: (uploaded / max) * 100, suffix: '' },
    { key: 'sealed', label: '已签章认证', count: sealed, percent: (sealed / max) * 100, suffix: '' },
  ]
})

function getLevelTagType(level: string) {
  const map: Record<string, string> = {
    L0: 'info',
    L1: '',
    L2: 'success',
    L3: 'warning',
    L4: 'danger',
  }
  return map[level] ?? 'info'
}

function getLevelDesc(level: string) {
  const map: Record<string, string> = {
    L0: '未接触',
    L1: '已发送',
    L2: '已打开',
    L3: '已确认',
    L4: '已签章',
  }
  return map[level] ?? ''
}

async function fetchFunnel() {
  try {
    funnel.value = (await getEngagementFunnel()) as FunnelData
    const f = funnel.value
    const notOpened = (f?.sent ?? 0) - (f?.opened ?? 0)
    const openedNotConfirmed = (f?.opened ?? 0) - (f?.confirmed ?? 0)
    alerts.value = []
    if (notOpened > 0) {
      alerts.value.push({ key: 'notOpened', desc: '链接未打开', count: notOpened })
    }
    if (openedNotConfirmed > 0) {
      alerts.value.push({ key: 'notConfirmed', desc: '已打开未确认>7天', count: openedNotConfirmed })
    }
  } catch {
    funnel.value = null
    alerts.value = []
  }
}

async function fetchBuyers() {
  loading.value = true
  try {
    const res = await listEngagementBuyers({
      page: pagination.page,
      pageSize: pagination.pageSize,
    }) as { list?: BuyerItem[]; total?: number }
    buyers.value = res?.list ?? []
    pagination.total = res?.total ?? 0
  } catch {
    buyers.value = []
  } finally {
    loading.value = false
  }
}

async function handleInvite(row: BuyerItem) {
  try {
    await sendEngagementInvite(row.id)
    ElMessage.success('邀请已发送')
    fetchBuyers()
    fetchFunnel()
  } catch {
    ElMessage.error('发送失败')
  }
}

function handleViewDetail(row: BuyerItem) {
  ElMessage.info(`查看 ${row.companyName} 详情`)
}

onMounted(() => {
  fetchFunnel()
  fetchBuyers()
})
</script>

<style lang="scss" scoped>
.engagement-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .funnel-card {
    margin-bottom: 20px;
  }

  .funnel-chart {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .funnel-step {
    display: flex;
    align-items: center;
    gap: 12px;

    .step-bar {
      height: 24px;
      min-width: 20px;
      background: linear-gradient(90deg, var(--el-color-primary), var(--el-color-primary-light-3));
      border-radius: 4px;
      transition: width 0.3s;
    }

    .step-label {
      min-width: 100px;
      font-size: 14px;
    }

    .step-value {
      font-size: 13px;
      color: #606266;
    }
  }

  .alert-card {
    margin-bottom: 20px;
  }

  .alert-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px;
    background: #fdf6ec;
    border-radius: 6px;

    .alert-desc {
      font-size: 14px;
    }
  }

  .action-bar {
    margin-bottom: 16px;
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
