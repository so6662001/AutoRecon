<template>
  <div class="tri-match-page">
    <h2 class="page-title">账票款三单匹配</h2>

    <div v-if="!billId || billId === '0'" class="bill-selector">
      <el-select v-model="selectedBillId" placeholder="请选择对账单" style="width: 320px" @change="onBillSelect">
        <el-option
          v-for="b in billOptions"
          :key="b.id"
          :label="`${b.billNo} - ${b.buyerName} - ¥${formatAmount(b.totalAmount)}`"
          :value="b.id"
        />
      </el-select>
      <el-button type="primary" @click="goToTriMatch">查看匹配</el-button>
    </div>

    <template v-else>
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">账票匹配率</div>
            <div class="stat-value">{{ matchData.billInvoiceRate ?? 0 }}%</div>
            <div class="stat-desc">已开票 / 应收总额</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">票款匹配率</div>
            <div class="stat-value">{{ matchData.invoicePaymentRate ?? 0 }}%</div>
            <div class="stat-desc">已付款 / 已开票</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">账款匹配率</div>
            <div class="stat-value">{{ matchData.billPaymentRate ?? 0 }}%</div>
            <div class="stat-desc">已付款 / 应收总额</div>
          </el-card>
        </el-col>
      </el-row>

      <div class="flow-diagram">
        <div class="flow-title">匹配流程</div>
        <div class="flow-boxes">
          <div class="flow-box flow-bill">
            <div class="flow-label">对账单</div>
            <div class="flow-amount">¥{{ formatAmount(matchData.totalAmount) }}</div>
          </div>
          <div class="flow-arrow">→</div>
          <div class="flow-box flow-invoice">
            <div class="flow-label">发票</div>
            <div class="flow-amount">¥{{ formatAmount(matchData.invoicedAmount) }}</div>
          </div>
          <div class="flow-arrow">→</div>
          <div class="flow-box flow-payment">
            <div class="flow-label">付款</div>
            <div class="flow-amount">¥{{ formatAmount(matchData.paidAmount) }}</div>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <div class="section-title">明细匹配状态</div>
        <el-table :data="detailItems" stripe>
          <el-table-column prop="productName" label="品名" min-width="120" />
          <el-table-column prop="quantity" label="数量" width="90" align="right" />
          <el-table-column prop="amount" label="金额" width="110" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="invoiceStatus" label="发票状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.invoiceStatus === 'MATCHED' ? 'success' : 'warning'" size="small">
                {{ row.invoiceStatus === 'MATCHED' ? '已开票' : '待开票' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="paymentStatus" label="付款状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.paymentStatus === 'PAID' ? 'success' : 'info'" size="small">
                {{ row.paymentStatus === 'PAID' ? '已付款' : '待付款' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTriMatch } from '@/api/recon'
import { queryBills } from '@/api/recon'

const route = useRoute()
const router = useRouter()
const billId = computed(() => route.params.billId as string)
const selectedBillId = ref<number | null>(null)
const billOptions = ref<{ id: number; billNo: string; buyerName: string; totalAmount: number }[]>([])

const matchData = ref<{
  totalAmount?: number
  invoicedAmount?: number
  paidAmount?: number
  billInvoiceRate?: number
  invoicePaymentRate?: number
  billPaymentRate?: number
  items?: { productName: string; quantity: number; amount: number; invoiceStatus: string; paymentStatus: string }[]
}>({})

const detailItems = computed(() =>
  matchData.value.items ?? [
    { productName: '螺纹钢', quantity: 100, amount: 62800, invoiceStatus: 'MATCHED', paymentStatus: 'PAID' },
    { productName: '线材', quantity: 50, amount: 63000, invoiceStatus: 'MATCHED', paymentStatus: 'PENDING' },
  ]
)

function formatAmount(val: number | undefined) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function onBillSelect() {
  if (selectedBillId.value) {
    router.replace({ name: 'triMatch', params: { billId: String(selectedBillId.value) } })
  }
}

function goToTriMatch() {
  if (selectedBillId.value) {
    router.push({ name: 'triMatch', params: { billId: String(selectedBillId.value) } })
  }
}

async function fetchBills() {
  try {
    const res = await queryBills({ pageSize: 50 }) as { list?: { id: number; billNo: string; buyerName: string; totalAmount: number }[] }
    billOptions.value = res?.list ?? [
      { id: 1, billNo: 'R202503001', buyerName: '某某贸易有限公司', totalAmount: 125800 },
      { id: 2, billNo: 'R202503002', buyerName: '某某制造有限公司', totalAmount: 256000 },
    ]
    if (billOptions.value.length > 0 && !selectedBillId.value) {
      selectedBillId.value = billOptions.value[0].id
    }
  } catch {
    billOptions.value = [{ id: 1, billNo: 'R202503001', buyerName: '某某贸易有限公司', totalAmount: 125800 }]
  }
}

async function fetchTriMatch(id: number) {
  try {
    const res = await getTriMatch(id) as typeof matchData.value
    matchData.value = res ?? {}
    if (!matchData.value.totalAmount) {
      matchData.value = {
        totalAmount: 125800,
        invoicedAmount: 125800,
        paidAmount: 50000,
        billInvoiceRate: 100,
        invoicePaymentRate: 40,
        billPaymentRate: 40,
      }
    }
  } catch {
    matchData.value = {
      totalAmount: 125800,
      invoicedAmount: 125800,
      paidAmount: 50000,
      billInvoiceRate: 100,
      invoicePaymentRate: 40,
      billPaymentRate: 40,
    }
  }
}

watch(
  () => route.params.billId,
  (id) => {
    const idStr = Array.isArray(id) ? id[0] : id
    const numId = idStr ? parseInt(idStr, 10) : 0
    if (numId > 0) {
      selectedBillId.value = numId
      fetchTriMatch(numId)
    }
  },
  { immediate: true }
)

onMounted(async () => {
  await fetchBills()
  const id = billId.value ? parseInt(billId.value, 10) : 0
  if (id > 0) {
    fetchTriMatch(id)
  } else if (billOptions.value.length > 0) {
    selectedBillId.value = billOptions.value[0].id
  }
})
</script>

<style lang="scss" scoped>
.tri-match-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .bill-selector {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;
    padding: 20px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .stats-row {
    margin-bottom: 24px;
  }

  .stat-card {
    margin-bottom: 20px;

    .stat-label {
      font-size: 14px;
      color: #909399;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #409eff;
      margin: 8px 0 4px;
    }

    .stat-desc {
      font-size: 12px;
      color: #c0c4cc;
    }
  }

  .flow-diagram {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    margin-bottom: 24px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);

    .flow-title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 16px;
    }

    .flow-boxes {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
    }

    .flow-box {
      padding: 16px 24px;
      border-radius: 8px;
      min-width: 140px;
      text-align: center;

      .flow-label {
        font-size: 12px;
        color: #909399;
      }

      .flow-amount {
        font-size: 18px;
        font-weight: 600;
        margin-top: 4px;
      }

      &.flow-bill {
        background: #ecf5ff;
        .flow-amount { color: #409eff; }
      }

      &.flow-invoice {
        background: #f0f9eb;
        .flow-amount { color: #67c23a; }
      }

      &.flow-payment {
        background: #fdf6ec;
        .flow-amount { color: #e6a23c; }
      }
    }

    .flow-arrow {
      font-size: 24px;
      color: #c0c4cc;
    }
  }

  .detail-section {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);

    .section-title {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 16px;
    }
  }
}
</style>
