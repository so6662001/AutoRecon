<template>
  <div class="page-container">
    <h2>话术与物料</h2>
    <p class="page-desc">复制话术用于与买方沟通，下载物料用于线下推广</p>

    <h3 class="section-title">引导话术</h3>
    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="(s, i) in scripts" :key="i" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="script-card">
          <div class="scenario">{{ s.scenario }}</div>
          <p class="script-text">{{ s.script }}</p>
          <el-button type="primary" link @click="copyText(s.script)">
            <el-icon class="mr"><DocumentCopy /></el-icon>
            复制
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <h3 class="section-title mt">辅助物料</h3>
    <el-table :data="materials" stripe>
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type === 'video' ? '视频' : 'PDF' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openUrl(row.url)">打开链接</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import { getEngagementScripts, getEngagementMaterials } from '@/api/system'

interface ScriptRow {
  scenario?: string
  script?: string
}

interface MaterialRow {
  name?: string
  type?: string
  url?: string
}

const loading = ref(false)
const scripts = ref<ScriptRow[]>([])
const materials = ref<MaterialRow[]>([])

function unwrapArray<T>(res: unknown): T[] {
  const data =
    res && typeof res === 'object' && 'data' in res
      ? (res as { data: unknown }).data
      : res
  return Array.isArray(data) ? (data as T[]) : []
}

async function load() {
  loading.value = true
  try {
    const [s, m] = await Promise.all([getEngagementScripts(), getEngagementMaterials()])
    scripts.value = unwrapArray<ScriptRow>(s)
    materials.value = unwrapArray<MaterialRow>(m)
  } catch {
    scripts.value = []
    materials.value = []
  } finally {
    loading.value = false
  }
}

function copyText(text?: string) {
  if (!text) return
  navigator.clipboard.writeText(text).then(
    () => ElMessage.success('已复制到剪贴板'),
    () => ElMessage.error('复制失败')
  )
}

function openUrl(url?: string) {
  if (!url) return
  const abs = url.startsWith('http') ? url : `${window.location.origin}${url}`
  window.open(abs, '_blank')
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-desc {
  color: #999;
  margin-bottom: 20px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px;
}
.mt {
  margin-top: 28px;
}
.script-card {
  margin-bottom: 16px;
  min-height: 160px;
}
.scenario {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
.script-text {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  margin: 0 0 12px;
  min-height: 72px;
}
.mr {
  margin-right: 4px;
}
</style>
