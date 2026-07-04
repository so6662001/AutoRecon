<template>
  <div class="page-container contract-detail">
    <div v-loading="loading" class="detail-content">
      <!-- Header -->
      <div class="detail-header">
        <div class="header-left">
          <span class="contract-no">{{ contract.contractNo }}</span>
          <el-tag size="small" class="ml-2">{{ contract.contractType }}</el-tag>
          <el-tag :type="contractTagType(contract.status) as any" size="small" class="ml-2">
            {{ contract.status }}
          </el-tag>
        </div>
        <div class="header-actions">
          <el-button v-if="canInitiateSign" type="primary" @click="handleInitiateSign">
            发起签约
          </el-button>
          <el-button @click="handleViewPdf">查看签章PDF</el-button>
        </div>
      </div>

      <!-- Info section (el-descriptions) -->
      <el-descriptions :column="2" border class="info-section">
        <el-descriptions-item label="卖方">{{ contract.sellerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="买方">{{ contract.buyerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同类型">{{ contract.contractType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="签约状态">{{ contract.signStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="付款条件" :span="2">{{ contract.paymentTerms || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交货期限">{{ contract.deliveryDeadline || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提货仓库">{{ contract.warehouseName || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- Progress bars -->
      <el-card shadow="never" class="progress-card">
        <div class="progress-item">
          <span class="progress-label">已提/未提 (重量)</span>
          <el-progress
            :percentage="weightProgress"
            :stroke-width="16"
            :format="() => `${contract.pickedWeight ?? 0} / ${contract.totalWeight ?? 0} 吨`"
          />
        </div>
        <div class="progress-item">
          <span class="progress-label">已付/未付 (金额)</span>
          <el-progress
            :percentage="paymentProgress"
            :stroke-width="16"
            :format="() => `¥${formatMoney(contract.paidAmount)} / ¥${formatMoney(contract.totalAmount)}`"
          />
        </div>
      </el-card>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="合同明细" name="items">
          <el-table :data="contractItems" stripe>
            <el-table-column prop="productName" label="品名" />
            <el-table-column prop="spec" label="规格" />
            <el-table-column prop="material" label="材质" />
            <el-table-column prop="origin" label="产地" />
            <el-table-column prop="quantity" label="数量" align="right" />
            <el-table-column prop="weight" label="重量" align="right" />
            <el-table-column prop="unitPrice" label="单价" align="right" />
            <el-table-column prop="amount" label="金额" align="right" />
            <el-table-column prop="pickedWeight" label="已提重量" align="right" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="提货记录" name="pickups">
          <el-table :data="pickupOrders" stripe>
            <el-table-column prop="pickupOrderNo" label="提货单号" width="160" />
            <el-table-column prop="pickupDate" label="提货日期" width="120" />
            <el-table-column prop="totalWeight" label="重量" align="right" />
            <el-table-column prop="totalAmount" label="金额" align="right" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="viewPickup(row)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="结算记录" name="settlements">
          <el-table :data="settlements" stripe>
            <el-table-column prop="settlementNo" label="结算单号" width="160" />
            <el-table-column prop="settlementDate" label="日期" width="120" />
            <el-table-column prop="weight" label="重量" align="right" />
            <el-table-column prop="amount" label="金额" align="right" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="全流程时间线" name="timeline">
          <el-timeline>
            <el-timeline-item
              v-for="(evt, idx) in progressEvents"
              :key="idx"
              :timestamp="evt.time"
              placement="top"
            >
              <el-tag size="small" class="mr-2">{{ evt.eventType }}</el-tag>
              {{ evt.title }}
            </el-timeline-item>
            <el-empty v-if="!progressEvents.length" description="暂无进度" />
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getContractDetail,
  getContractProgress,
  listSettlementsByContract,
  initiateSign as apiInitiateSign,
} from '@/api/evidence'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('items')

const contract = reactive<Record<string, any>>({
  id: null,
  contractNo: '',
  contractType: '',
  status: '',
  signStatus: '',
  buyerName: '',
  sellerName: '',
  paymentTerms: '',
  deliveryDeadline: '',
  warehouseName: '',
  totalWeight: 0,
  pickedWeight: 0,
  totalAmount: 0,
  paidAmount: 0,
})

const contractItems = ref<any[]>([])
const pickupOrders = ref<any[]>([])
const settlements = ref<any[]>([])
const progressEvents = ref<{ time: string; eventType: string; title: string }[]>([])

const contractId = computed(() => Number(route.params.id))

const weightProgress = computed(() => {
  const total = Number(contract.totalWeight) || 0
  const picked = Number(contract.pickedWeight) || 0
  if (total <= 0) return 0
  return Math.round((picked / total) * 100)
})

const paymentProgress = computed(() => {
  const total = Number(contract.totalAmount) || 0
  const paid = Number(contract.paidAmount) || 0
  if (total <= 0) return 0
  return Math.round((paid / total) * 100)
})

const canInitiateSign = computed(
  () => contract.contractType === '订货' && contract.status === '待签约'
)

function contractTagType(s: string) {
  const map: Record<string, string> = {
    可提货: 'success',
    待签约: 'warning',
    提货中: '',
    已提完: 'info',
    已结清: 'info',
  }
  return map[s] ?? ''
}

function formatMoney(val: number | string) {
  if (val == null || val === '') return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

async function loadDetail() {
  if (!contractId.value) return
  loading.value = true
  try {
    const res = (await getContractDetail(contractId.value)) as any
    const data = res?.data ?? res ?? {}
    Object.assign(contract, {
      id: data.id ?? contractId.value,
      contractNo: data.contractNo ?? data.contract_no ?? '-',
      contractType: data.contractType ?? data.contract_type ?? '-',
      status: data.status ?? data.contractStatus ?? '-',
      signStatus: data.signStatus ?? data.sign_status ?? '未签',
      buyerName: data.buyerName ?? data.buyer_name ?? '-',
      sellerName: data.sellerName ?? data.seller_name ?? '-',
      paymentTerms: data.paymentTerms ?? data.payment_terms ?? '-',
      deliveryDeadline: data.deliveryDeadline ?? data.delivery_deadline ?? '-',
      warehouseName: data.warehouseName ?? data.warehouse_name ?? '-',
      totalWeight: data.totalWeight ?? data.total_weight ?? 0,
      pickedWeight: data.pickedWeight ?? data.picked_weight ?? 0,
      totalAmount: data.totalAmount ?? data.total_amount ?? 0,
      paidAmount: data.paidAmount ?? data.paid_amount ?? 0,
    })
    contractItems.value = (data.items ?? data.contractItems ?? []).map((i: any) => ({
      productName: i.productName ?? i.product_name ?? '-',
      spec: i.spec ?? '-',
      material: i.material ?? '-',
      origin: i.origin ?? '-',
      quantity: i.quantity ?? 0,
      weight: i.weight ?? 0,
      unitPrice: i.unitPrice ?? i.unit_price ?? 0,
      amount: i.amount ?? 0,
      pickedWeight: i.pickedWeight ?? i.picked_weight ?? 0,
    }))
  } catch {
    // keep empty
  } finally {
    loading.value = false
  }
}

async function loadProgress() {
  if (!contractId.value) return
  try {
    const res = (await getContractProgress(contractId.value)) as any
    const data = res?.data ?? res ?? {}
    const events = data.events ?? data ?? []
    progressEvents.value = Array.isArray(events)
      ? events.map((e: any) => ({
          time: e.time ?? e.createdAt ?? '',
          eventType: e.eventType ?? e.event_type ?? '进度',
          title: e.title ?? e.content ?? '',
        }))
      : []
    pickupOrders.value = (data.pickupOrders ?? data.pickup_orders ?? []).map((p: any) => ({
      id: p.id ?? p.pickupOrderId,
      pickupOrderNo: p.pickupOrderNo ?? p.pickup_order_no ?? '-',
      pickupDate: p.pickupDate ?? p.pickup_date ?? p.createdAt ?? '-',
      totalWeight: p.totalWeight ?? p.total_weight ?? 0,
      totalAmount: p.totalAmount ?? p.total_amount ?? 0,
      status: p.status ?? '-',
    }))
  } catch {
    progressEvents.value = []
  }
}

async function loadSettlements() {
  if (!contractId.value) return
  try {
    const res = (await listSettlementsByContract(contractId.value)) as any
    const data = res?.data ?? res ?? {}
    const list = data.list ?? data.records ?? data ?? []
    settlements.value = Array.isArray(list)
      ? list.map((s: any) => ({
          settlementNo: s.settlementNo ?? s.settlement_no ?? '-',
          settlementDate: s.settlementDate ?? s.settlement_date ?? s.createdAt ?? '-',
          weight: s.weight ?? 0,
          amount: s.amount ?? 0,
          status: s.status ?? '-',
        }))
      : []
  } catch {
    settlements.value = []
  }
}

function viewPickup(row: any) {
  const id = row.id ?? row.pickupOrderId
  if (id) router.push({ name: 'pickupDetail', params: { id: String(id) } })
}

async function handleInitiateSign() {
  try {
    await apiInitiateSign(contract.id)
    loadDetail()
  } catch {
    // handled
  }
}

function handleViewPdf() {
  // Placeholder: 查看签章PDF - API not in evidence.ts
  ElMessage.info('签章PDF功能待对接')
}

watch(contractId, () => {
  loadDetail()
  loadProgress()
  loadSettlements()
}, { immediate: true })

onMounted(() => {
  loadDetail()
  loadProgress()
  loadSettlements()
})
</script>

<style lang="scss" scoped>
.contract-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 12px;

    .header-left {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
    }

    .contract-no {
      font-size: 20px;
      font-weight: 600;
    }

    .ml-2 { margin-left: 8px; }
    .mr-2 { margin-right: 8px; }
  }

  .info-section {
    margin-bottom: 16px;
  }

  .progress-card {
    margin-bottom: 16px;

    .progress-item {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .progress-label {
      display: block;
      font-size: 14px;
      margin-bottom: 8px;
      color: #606266;
    }
  }

  .detail-tabs {
    margin-top: 16px;
  }
}
</style>
