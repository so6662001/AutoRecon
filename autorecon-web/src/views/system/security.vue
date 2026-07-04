<template>
  <div class="page-container">
    <h2>数据安全</h2>
    <p class="page-desc">数据隔离、加密与访问审计概览</p>

    <el-row :gutter="16" class="status-row">
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover">
          <div class="status-item">
            <el-icon class="ok" :size="28"><CircleCheck /></el-icon>
            <div>
              <div class="label">数据隔离</div>
              <div class="value">{{ status?.dataIsolation ? '已启用' : '未启用' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover">
          <div class="status-item">
            <div>
              <div class="label">存储加密</div>
              <div class="value">{{ status?.encryptionLevel ?? '—' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <el-card shadow="hover">
          <div class="status-item">
            <div>
              <div class="label">传输加密</div>
              <div class="value">{{ status?.transferEncryption ?? '—' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card" shadow="hover">
      <template #header>备份与用量</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="备份频率">{{ status?.backupFrequency ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="上次备份时间">{{ formatTime(status?.lastBackupAt) }}</el-descriptions-item>
        <el-descriptions-item label="累计访问次数">{{ status?.totalAccessCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="数据导出">
          <el-button type="primary" size="small" :loading="exporting" @click="doExport">导出全部数据</el-button>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="section-card" shadow="hover">
      <template #header>数据使用报告</template>
      <el-descriptions v-if="usageReport" :column="1" border>
        <el-descriptions-item label="用途说明">{{ usageReport.dataUsage }}</el-descriptions-item>
        <el-descriptions-item label="存储占用">{{ usageReport.storageUsed }}</el-descriptions-item>
        <el-descriptions-item label="最近访问时间">{{ usageReport.lastAccessed }}</el-descriptions-item>
        <el-descriptions-item label="数据保留（天）">{{ usageReport.dataRetentionDays }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无报告" />
    </el-card>

    <el-card shadow="hover">
      <template #header>访问日志</template>
      <el-table :data="logRows" v-loading="logLoading" stripe>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import {
  getSecurityStatus,
  getSecurityAccessLog,
  exportAllSecurityData,
  getSecurityUsageReport,
} from '@/api/system'

interface SecurityStatus {
  dataIsolation?: boolean
  encryptionLevel?: string
  transferEncryption?: string
  lastBackupAt?: string
  backupFrequency?: string
  totalAccessCount?: number
}

interface AuditRow {
  createdAt?: string
  userName?: string
  module?: string
  action?: string
  ipAddress?: string
  detail?: string
}

const status = ref<SecurityStatus | null>(null)
const usageReport = ref<Record<string, string | number> | null>(null)
const exporting = ref(false)

const logLoading = ref(false)
const logRows = ref<AuditRow[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

function unwrap<T>(res: unknown): T | undefined {
  if (res && typeof res === 'object' && 'data' in res) {
    return (res as { data: T }).data
  }
  return res as T
}

function formatTime(v?: string) {
  if (!v) return '—'
  return v.replace('T', ' ').slice(0, 19)
}

async function loadStatus() {
  try {
    const res = await getSecurityStatus()
    status.value = unwrap<SecurityStatus>(res) ?? null
  } catch {
    status.value = null
  }
}

async function loadUsage() {
  try {
    const res = await getSecurityUsageReport()
    const data = unwrap<Record<string, string | number>>(res)
    usageReport.value = data ?? null
  } catch {
    usageReport.value = null
  }
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await getSecurityAccessLog({
      pageNum: page.value,
      pageSize: pageSize.value,
    })
    const data = unwrap<{
      records?: AuditRow[]
      total?: number
    }>(res)
    logRows.value = data?.records ?? []
    total.value = Number(data?.total ?? 0)
  } catch {
    logRows.value = []
    total.value = 0
  } finally {
    logLoading.value = false
  }
}

async function doExport() {
  exporting.value = true
  try {
    const res = await exportAllSecurityData()
    const taskId = unwrap<string>(res)
    ElMessage.success(taskId ? `导出任务已提交：${taskId}` : '导出任务已提交')
  } catch {
    /* */
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadStatus()
  loadUsage()
  loadLogs()
})
</script>

<style scoped lang="scss">
.page-desc {
  color: #999;
  margin-bottom: 20px;
}
.status-row {
  margin-bottom: 16px;
}
.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 48px;
}
.status-item .ok {
  color: #67c23a;
}
.label {
  font-size: 13px;
  color: #909399;
}
.value {
  font-size: 16px;
  font-weight: 600;
  margin-top: 4px;
}
.section-card {
  margin-bottom: 16px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
