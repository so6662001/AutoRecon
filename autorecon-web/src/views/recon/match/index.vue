<template>
  <div class="match-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">比对结果</h2>
        <span class="bill-no">{{ billNo || `对账单号 #${billId}` }}</span>
      </div>
      <div class="header-actions">
        <el-button type="primary" :loading="executing" @click="handleExecute">
          <el-icon><RefreshRight /></el-icon>
          发起比对
        </el-button>
        <el-button :loading="loading" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          重新比对
        </el-button>
      </div>
    </div>

    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="stat-card stat-matched">
          <div class="stat-icon">✓</div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.matched }}</div>
            <div class="stat-label">匹配</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="stat-card stat-diff">
          <div class="stat-icon">⚠</div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.diff }}</div>
            <div class="stat-label">差异</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="stat-card stat-seller-extra">
          <div class="stat-icon">←</div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.sellerExtra }}</div>
            <div class="stat-label">卖方多出</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="stat-card stat-buyer-extra">
          <div class="stat-icon">→</div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.buyerExtra }}</div>
            <div class="stat-label">买方多出</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="stat-card stat-rate">
          <div class="stat-content">
            <div class="stat-value">{{ matchRate }}%</div>
            <div class="stat-label">匹配率</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="filter-tabs">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="仅差异" name="diff" />
      <el-tab-pane label="卖方多出" name="sellerExtra" />
      <el-tab-pane label="买方多出" name="buyerExtra" />
    </el-tabs>

    <div class="table-card">
      <el-table
        :data="filteredItems"
        stripe
        highlight-current-row
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="contractNo" label="合同号" min-width="120" />
        <el-table-column prop="productName" label="品名" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="100" />
        <el-table-column prop="sellerWeight" label="卖方重量" width="100" align="right">
          <template #default="{ row }">
            <span :class="{ 'diff-highlight': row.weightDiff !== 0 }">{{ row.sellerWeight ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="buyerWeight" label="买方重量" width="100" align="right">
          <template #default="{ row }">
            <span :class="{ 'diff-highlight': row.weightDiff !== 0 }">{{ row.buyerWeight ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="weightDiff" label="重量差异" width="100" align="right">
          <template #default="{ row }">
            <span :class="{ 'diff-highlight': row.weightDiff !== 0 }">{{ formatDiff(row.weightDiff) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sellerAmount" label="卖方金额" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'diff-highlight': row.amountDiff !== 0 }">{{ formatAmount(row.sellerAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="buyerAmount" label="买方金额" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'diff-highlight': row.amountDiff !== 0 }">{{ formatAmount(row.buyerAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="amountDiff" label="金额差异" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'diff-highlight': row.amountDiff !== 0 }">{{ formatAmount(row.amountDiff) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'matched'" type="success" size="small">✓ 匹配</el-tag>
            <el-tag v-else-if="row.status === 'diff'" type="warning" size="small">⚠ 差异</el-tag>
            <el-tag v-else-if="row.status === 'seller_extra'" type="info" size="small">← 卖方多出</el-tag>
            <el-tag v-else-if="row.status === 'buyer_extra'" type="danger" size="small">→ 买方多出</el-tag>
            <el-tag v-else type="info" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="action-bar">
      <el-button type="primary" :loading="disputeSubmitting" @click="handleConfirmAll">全部确认</el-button>
      <el-button type="warning" @click="handleOpenDisputeDialog">标记差异为异议</el-button>
      <el-button @click="handleExportReport">导出比对报告</el-button>
    </div>

    <el-dialog v-model="disputeDialogVisible" title="标记差异为异议" width="700px" destroy-on-close>
      <p class="dialog-hint">请选择要创建异议的差异项：</p>
      <el-table
        :data="diffItemsForDispute"
        max-height="300"
        :row-key="(row: MatchItem): string => String(row.id ?? `${row.contractNo}-${row.productName}-${row.spec}`)"
        @selection-change="handleDisputeSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="contractNo" label="合同号" width="120" />
        <el-table-column prop="productName" label="品名" min-width="100" />
        <el-table-column prop="weightDiff" label="重量差异" width="90" align="right">
          <template #default="{ row }">{{ formatDiff(row.weightDiff) }}</template>
        </el-table-column>
        <el-table-column prop="amountDiff" label="金额差异" width="100" align="right">
          <template #default="{ row }">{{ formatAmount(row.amountDiff) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="disputeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="disputeSubmitting" :disabled="selectedDiffItems.length === 0" @click="handleCreateDisputes">
          创建异议 ({{ selectedDiffItems.length }})
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="明细详情" width="600px" destroy-on-close>
      <div v-if="selectedItem" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="合同号">{{ selectedItem.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="品名">{{ selectedItem.productName }}</el-descriptions-item>
          <el-descriptions-item label="规格">{{ selectedItem.spec }}</el-descriptions-item>
          <el-descriptions-item label="卖方重量">{{ selectedItem.sellerWeight ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="买方重量">{{ selectedItem.buyerWeight ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="重量差异">{{ formatDiff(selectedItem.weightDiff) }}</el-descriptions-item>
          <el-descriptions-item label="卖方金额">{{ formatAmount(selectedItem.sellerAmount) }}</el-descriptions-item>
          <el-descriptions-item label="买方金额">{{ formatAmount(selectedItem.buyerAmount) }}</el-descriptions-item>
          <el-descriptions-item label="金额差异">{{ formatAmount(selectedItem.amountDiff) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getStatusText(selectedItem.status) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Refresh } from '@element-plus/icons-vue'
import { executeMatch, getMatchResult, getDiffItems, confirmBill, createDispute } from '@/api/recon'

interface MatchItem {
  id?: number
  contractNo: string
  productName: string
  spec: string
  sellerWeight?: number
  buyerWeight?: number
  weightDiff?: number
  sellerAmount?: number
  buyerAmount?: number
  amountDiff?: number
  status: string
  matchStatus?: number
}

interface MatchResult {
  billNo?: string
  matched?: number
  diff?: number
  sellerExtra?: number
  buyerExtra?: number
  total?: number
  items?: MatchItem[]
}

const route = useRoute()
const router = useRouter()
const billId = computed(() => String(route.params.billId))

const loading = ref(false)
const executing = ref(false)
const billNo = ref('')
const stats = ref({
  matched: 0,
  diff: 0,
  sellerExtra: 0,
  buyerExtra: 0,
  total: 0,
})
const items = ref<MatchItem[]>([])
const activeTab = ref('all')
const detailVisible = ref(false)
const selectedItem = ref<MatchItem | null>(null)
const disputeDialogVisible = ref(false)
const selectedDiffItems = ref<MatchItem[]>([])
const disputeSubmitting = ref(false)

const matchRate = computed(() => {
  const total = stats.value.total
  if (!total) return 0
  return Math.round((stats.value.matched / total) * 100)
})

const filteredItems = computed(() => {
  const list = items.value
  if (activeTab.value === 'all') return list
  if (activeTab.value === 'diff') return list.filter((i) => i.status === 'diff')
  if (activeTab.value === 'sellerExtra') return list.filter((i) => i.status === 'seller_extra')
  if (activeTab.value === 'buyerExtra') return list.filter((i) => i.status === 'buyer_extra')
  return list
})

const diffItemsForDispute = computed(() =>
  items.value.filter((i) => i.status !== 'matched')
)

function formatDiff(val?: number): string {
  if (val == null) return '-'
  if (val > 0) return `+${val}`
  return String(val)
}

function formatAmount(val?: number): string {
  if (val == null) return '-'
  return `¥${Number(val).toLocaleString()}`
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    matched: '匹配',
    diff: '差异',
    seller_extra: '卖方多出',
    buyer_extra: '买方多出',
  }
  return map[status] ?? status
}

function handleRowClick(row: MatchItem) {
  selectedItem.value = row
  detailVisible.value = true
}

async function fetchData() {
  loading.value = true
  try {
    const [resultRes, diffRes] = await Promise.all([
      getMatchResult(Number(billId.value)) as Promise<MatchResult>,
      getDiffItems(Number(billId.value)) as Promise<MatchItem[]>,
    ])
    billNo.value = resultRes?.billNo ?? ''
    stats.value = {
      matched: resultRes?.matched ?? 0,
      diff: resultRes?.diff ?? 0,
      sellerExtra: resultRes?.sellerExtra ?? 0,
      buyerExtra: resultRes?.buyerExtra ?? 0,
      total: resultRes?.total ?? 0,
    }
    items.value = (diffRes && Array.isArray(diffRes) ? diffRes : resultRes?.items ?? []).map((item) => ({
      ...item,
      weightDiff: item.weightDiff ?? (item.sellerWeight != null && item.buyerWeight != null
        ? (item.sellerWeight - item.buyerWeight) : 0),
      amountDiff: item.amountDiff ?? (item.sellerAmount != null && item.buyerAmount != null
        ? (item.sellerAmount - item.buyerAmount) : 0),
    }))
  } catch {
    items.value = []
    stats.value = { matched: 0, diff: 0, sellerExtra: 0, buyerExtra: 0, total: 0 }
  } finally {
    loading.value = false
  }
}

async function handleExecute() {
  executing.value = true
  try {
    await executeMatch(Number(billId.value))
    ElMessage.success('比对已发起')
    await fetchData()
  } catch {
    ElMessage.error('发起比对失败')
  } finally {
    executing.value = false
  }
}

function handleRefresh() {
  fetchData()
}

function handleDisputeSelectionChange(rows: MatchItem[]) {
  selectedDiffItems.value = rows
}

function detectDisputeType(item: MatchItem): number {
  if (item.status === 'seller_extra' || item.status === 'buyer_extra') return 4
  const hasWeight = (item.weightDiff ?? 0) !== 0
  const hasAmount = (item.amountDiff ?? 0) !== 0
  if (hasWeight) return 2
  if (hasAmount) return 3
  return 2
}

async function handleConfirmAll() {
  try {
    await ElMessageBox.confirm('确认所有匹配项无异议？', '确认', {
      type: 'warning',
    })
    disputeSubmitting.value = true
    await confirmBill(Number(billId.value))
    ElMessage.success('全部确认成功')
    router.push({ name: 'billDetail', params: { id: billId.value } })
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('确认失败')
  } finally {
    disputeSubmitting.value = false
  }
}

function handleOpenDisputeDialog() {
  if (diffItemsForDispute.value.length === 0) {
    ElMessage.warning('暂无差异项可标记')
    return
  }
  selectedDiffItems.value = []
  disputeDialogVisible.value = true
}

async function handleCreateDisputes() {
  if (selectedDiffItems.value.length === 0) return
  disputeSubmitting.value = true
  try {
    let count = 0
    for (const item of selectedDiffItems.value) {
      await createDispute({
        billId: Number(billId.value),
        matchItemId: item.id,
        disputeType: detectDisputeType(item),
      })
      count++
    }
    ElMessage.success(`已创建 ${count} 条异议`)
    disputeDialogVisible.value = false
    await fetchData()
  } catch {
    ElMessage.error('创建异议失败')
  } finally {
    disputeSubmitting.value = false
  }
}

function handleExportReport() {
  const headers = ['合同号', '品名', '规格', '卖方重量', '买方重量', '重量差异', '卖方金额', '买方金额', '金额差异', '状态']
  const rows = items.value.map((r) => [
    r.contractNo,
    r.productName,
    r.spec,
    r.sellerWeight ?? '',
    r.buyerWeight ?? '',
    formatDiff(r.weightDiff),
    r.sellerAmount ?? '',
    r.buyerAmount ?? '',
    formatAmount(r.amountDiff),
    getStatusText(r.status),
  ])
  const csvContent = '\uFEFF' + [headers.join(','), ...rows.map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(','))].join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `比对报告_${billNo.value || billId.value}_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.match-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    flex-wrap: wrap;
    gap: 16px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .page-title {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
        color: #303133;
      }

      .bill-no {
        font-size: 14px;
        color: #909399;
      }
    }
  }

  .stat-row {
    margin-bottom: 20px;
  }

  .stat-card {
    display: flex;
    align-items: center;
    padding: 16px 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
    margin-bottom: 20px;
    background: #fff;

    .stat-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;
      font-size: 20px;
      font-weight: bold;
      color: #fff;
    }

    .stat-content {
      flex: 1;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: #303133;
    }

    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }

    &.stat-matched .stat-icon {
      background: #67c23a;
    }

    &.stat-diff .stat-icon {
      background: #e6a23c;
    }

    &.stat-seller-extra .stat-icon {
      background: #409eff;
    }

    &.stat-buyer-extra .stat-icon {
      background: #f56c6c;
    }

    &.stat-rate .stat-content .stat-value {
      color: #409eff;
    }
  }

  .filter-tabs {
    margin-bottom: 16px;

    :deep(.el-tabs__header) {
      margin-bottom: 0;
    }
  }

  .table-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
    margin-bottom: 20px;
  }

  .diff-highlight {
    color: #f56c6c;
    font-weight: 500;
  }

  .action-bar {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  .detail-content {
    padding: 8px 0;
  }

  .dialog-hint {
    margin: 0 0 16px;
    color: #606266;
    font-size: 14px;
  }
}
</style>
