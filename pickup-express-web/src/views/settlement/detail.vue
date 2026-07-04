<template>
  <div v-loading="loading" class="page-container settlement-detail">
    <div class="detail-header">
      <div class="header-left">
        <span class="title-no">{{ detail.settlementNo || '-' }}</span>
        <el-tag size="small" class="ml-2">{{ settlementStatusLabel }}</el-tag>
      </div>
      <div class="header-actions">
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>

    <el-row :gutter="12" class="amount-cards">
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">结算重量(吨)</div>
          <div class="metric-value">{{ formatNum(detail.totalWeight) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">结算金额(¥)</div>
          <div class="metric-value">{{ formatMoney(detail.totalAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">税额(¥)</div>
          <div class="metric-value">{{ formatMoney(detail.taxAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">价税合计(¥)</div>
          <div class="metric-value">{{ formatMoney(detail.totalWithTax) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-card shadow="hover" class="metric-card highlight">
          <div class="metric-label">应收金额(¥)</div>
          <div class="metric-value">{{ formatMoney(receivableDisplay) }}</div>
          <div class="metric-hint">价税合计 − 已扣预付</div>
        </el-card>
      </el-col>
    </el-row>

    <el-descriptions :column="2" border class="info-block">
      <el-descriptions-item label="结算单号">{{ detail.settlementNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="合同号">{{ detail.contractNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="客户">{{ detail.buyerName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="提货单号">{{ pickupNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
      <el-descriptions-item label="客户已查看">
        <el-tag :type="detail.customerViewed === 1 ? 'success' : 'info'" size="small">
          {{ detail.customerViewed === 1 ? '是' : '否' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="查看时间" :span="2">{{ formatDate(detail.customerViewedAt) }}</el-descriptions-item>
    </el-descriptions>

    <div class="section-title">结算明细</div>
    <el-table :data="detailRows" stripe style="width: 100%">
      <el-table-column prop="liftSeq" label="吊序号" width="90" align="center" />
      <el-table-column prop="productName" label="品名" min-width="100" />
      <el-table-column prop="spec" label="规格" width="120" />
      <el-table-column prop="material" label="材质" width="100" />
      <el-table-column prop="pieces" label="件数" width="80" align="right" />
      <el-table-column label="重量(吨)" width="110" align="right">
        <template #default="{ row }">{{ formatNum(row.weight) }}</template>
      </el-table-column>
      <el-table-column label="单价(¥/吨)" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
      </el-table-column>
      <el-table-column label="金额(¥)" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="数据来源" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ row.dataSource || '-' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div class="section-title">关联提货单</div>
    <div v-if="detail.pickupOrderId" class="pickup-link">
      <router-link :to="{ name: 'pickupDetail', params: { id: String(detail.pickupOrderId) } }">
        {{ pickupNo ? `提货单 ${pickupNo}` : `提货单详情 (#${detail.pickupOrderId})` }}
      </router-link>
    </div>
    <div v-else class="text-muted">—</div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { getSettlement, getPickupOrderDetail } from '@/api/evidence'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const pickupNo = ref('')

const detail = reactive<{
  id?: number
  settlementNo?: string
  pickupOrderId?: number
  contractNo?: string
  buyerName?: string
  totalWeight?: number | string
  totalAmount?: number | string
  taxAmount?: number | string
  totalWithTax?: number | string
  deductedPrepayment?: number | string
  receivableAmount?: number | string
  settlementDetail?: string
  customerViewed?: number
  customerViewedAt?: string
  status?: number
  createdAt?: string
}>({})

const detailRows = ref<
  {
    liftSeq?: number
    productName?: string
    spec?: string
    material?: string
    pieces?: number
    weight?: number
    unitPrice?: number
    amount?: number
    dataSource?: string
  }[]
>([])

const settlementStatusLabel = computed(() => {
  const s = detail.status
  if (s === 1) return '已结算'
  if (s === 0) return '未结算'
  return s != null ? String(s) : '-'
})

const receivableDisplay = computed(() => {
  const tw = num(detail.totalWithTax)
  const prep = num(detail.deductedPrepayment)
  if (detail.receivableAmount != null && detail.receivableAmount !== '') {
    return detail.receivableAmount
  }
  return Math.max(0, tw - prep)
})

function num(v: unknown) {
  if (v == null || v === '') return 0
  return Number(v)
}

function formatNum(val: number | string | undefined) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

function formatMoney(val: number | string | undefined) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(val: string | undefined) {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm:ss')
}

function parseDetailJson(raw: string | undefined) {
  if (!raw || !raw.trim()) {
    detailRows.value = []
    return
  }
  try {
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) {
      detailRows.value = []
      return
    }
    detailRows.value = parsed.map((row: any) => ({
      liftSeq: row.liftSeq ?? row.lift_seq,
      productName: row.productName ?? row.product_name ?? '-',
      spec: row.spec ?? '-',
      material: row.material ?? '-',
      pieces: row.pieces ?? row.pieceCount ?? row.piece_count,
      weight: row.weight != null ? Number(row.weight) : undefined,
      unitPrice: row.unitPrice != null ? Number(row.unitPrice) : row.unit_price != null ? Number(row.unit_price) : undefined,
      amount: row.amount != null ? Number(row.amount) : undefined,
      dataSource: row.dataSource ?? row.data_source ?? '-',
    }))
  } catch {
    detailRows.value = []
  }
}

async function load() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  pickupNo.value = ''
  try {
    const res = (await getSettlement(id)) as any
    const data = res?.data ?? res
    Object.assign(detail, data ?? {})
    parseDetailJson(detail.settlementDetail)

    const pid = detail.pickupOrderId
    if (pid) {
      try {
        const pRes = (await getPickupOrderDetail(pid)) as any
        const p = pRes?.data ?? pRes
        pickupNo.value = p?.pickupNo ?? p?.pickupOrderNo ?? p?.pickup_no ?? ''
      } catch {
        pickupNo.value = ''
      }
    }
  } catch {
    Object.keys(detail).forEach((k) => delete (detail as any)[k])
    detailRows.value = []
    pickupNo.value = ''
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push({ name: 'settlementList' })
}

watch(
  () => route.params.id,
  () => {
    load()
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.settlement-detail {
  .detail-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  .title-no {
    font-size: 18px;
    font-weight: 600;
  }

  .ml-2 {
    margin-left: 8px;
  }

  .amount-cards {
    margin-bottom: 20px;
  }

  .metric-card {
    margin-bottom: 12px;
    text-align: center;

    &.highlight {
      border-color: var(--el-color-primary-light-5);
    }
  }

  .metric-label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin-bottom: 6px;
  }

  .metric-value {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-color-primary);
  }

  .metric-hint {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    margin-top: 4px;
  }

  .info-block {
    margin-bottom: 20px;
  }

  .section-title {
    font-size: 15px;
    font-weight: 600;
    margin: 16px 0 12px;
  }

  .pickup-link a {
    color: var(--el-color-primary);
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }

  .text-muted {
    color: var(--el-text-color-secondary);
  }
}
</style>
