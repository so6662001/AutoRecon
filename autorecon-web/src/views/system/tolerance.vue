<template>
  <div class="page-container">
    <h2>容差优化建议</h2>
    <p class="page-desc">基于历史比对数据给出的容差调整建议，采纳后将更新对账匹配规则</p>

    <el-table :data="rows" stripe v-loading="loading" empty-text="暂无建议">
      <el-table-column prop="dimension" label="维度" width="120" />
      <el-table-column label="当前值" width="120">
        <template #default="{ row }">{{ formatNum(row.currentTolerance) }}</template>
      </el-table-column>
      <el-table-column label="建议值" width="120">
        <template #default="{ row }">{{ formatNum(row.suggestedTolerance) }}</template>
      </el-table-column>
      <el-table-column label="匹配率提升" min-width="140">
        <template #default="{ row }">
          <span v-if="improvementPct(row) != null" class="up">{{ improvementPct(row) }}%</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="sampleCount" label="样本数" width="90" />
      <el-table-column label="置信度" width="100">
        <template #default="{ row }">{{ formatPct(row.confidence) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :loading="actingId === row.id && action === 'adopt'"
            @click="adopt(row)"
          >
            采纳
          </el-button>
          <el-button
            link
            type="danger"
            :loading="actingId === row.id && action === 'reject'"
            @click="reject(row)"
          >
            拒绝
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getToleranceSuggestions, adoptToleranceSuggestion, rejectToleranceSuggestion } from '@/api/recon'

interface ToleranceRow {
  id: number
  dimension?: string
  currentTolerance?: number | string
  suggestedTolerance?: number | string
  matchRateCurrent?: number | string
  matchRateSuggested?: number | string
  sampleCount?: number
  confidence?: number | string
}

const loading = ref(false)
const rows = ref<ToleranceRow[]>([])
const actingId = ref<number | null>(null)
const action = ref<'adopt' | 'reject' | null>(null)

function unwrapList(res: unknown): ToleranceRow[] {
  const data =
    res && typeof res === 'object' && 'data' in res
      ? (res as { data: unknown }).data
      : res
  return Array.isArray(data) ? (data as ToleranceRow[]) : []
}

function formatNum(v: unknown) {
  if (v === null || v === undefined) return '—'
  return String(v)
}

function formatPct(v: unknown) {
  if (v === null || v === undefined) return '—'
  const n = Number(v)
  if (Number.isNaN(n)) return String(v)
  return `${(n * 100).toFixed(1)}%`
}

function improvementPct(row: ToleranceRow) {
  const a = row.matchRateCurrent != null ? Number(row.matchRateCurrent) : NaN
  const b = row.matchRateSuggested != null ? Number(row.matchRateSuggested) : NaN
  if (Number.isNaN(a) || Number.isNaN(b)) return null
  return ((b - a) * 100).toFixed(2)
}

async function load() {
  loading.value = true
  try {
    const res = await getToleranceSuggestions()
    rows.value = unwrapList(res)
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function adopt(row: ToleranceRow) {
  actingId.value = row.id
  action.value = 'adopt'
  try {
    await adoptToleranceSuggestion(row.id)
    ElMessage.success('已采纳建议')
    await load()
  } catch {
    /* */
  } finally {
    actingId.value = null
    action.value = null
  }
}

async function reject(row: ToleranceRow) {
  actingId.value = row.id
  action.value = 'reject'
  try {
    await rejectToleranceSuggestion(row.id)
    ElMessage.success('已拒绝')
    await load()
  } catch {
    /* */
  } finally {
    actingId.value = null
    action.value = null
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-desc {
  color: #999;
  margin-bottom: 20px;
}
.up {
  color: #67c23a;
  font-weight: 500;
}
</style>
