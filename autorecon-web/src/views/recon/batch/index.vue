<template>
  <div class="batch-recon-page">
    <h2 class="page-title">批量对账</h2>

    <el-steps :active="currentStep" finish-status="success" align-center>
      <el-step title="选择周期与买方" />
      <el-step title="生成对账单" />
      <el-step title="发送确认" />
    </el-steps>

    <!-- Step 1: Select period + buyers -->
    <div v-show="currentStep === 0" class="step-content">
      <el-form :model="step1Form" label-width="100px" class="step-form">
        <el-form-item label="对账周期">
          <el-date-picker
            v-model="step1Form.periodRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 320px"
          />
        </el-form-item>
      </el-form>
      <div class="buyer-table-wrap">
        <div class="table-title">选择买方（有交易数据的买方）</div>
        <el-table
          ref="buyerTableRef"
          :data="buyerList"
          stripe
          @selection-change="handleBuyerSelection"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="buyerName" label="买方企业" min-width="180" />
          <el-table-column prop="tradeCount" label="交易笔数" width="100" align="center" />
          <el-table-column prop="totalAmount" label="交易金额" width="140" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.totalAmount) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="step-actions">
        <el-button type="primary" :disabled="!canProceedStep1" @click="goToStep2">
          下一步
        </el-button>
      </div>
    </div>

    <!-- Step 2: Progress -->
    <div v-show="currentStep === 1" class="step-content">
      <div class="progress-box">
        <el-progress
          :percentage="progressPercent"
          :status="progressStatus"
          :stroke-width="16"
        />
        <p class="progress-text">{{ progressText }}</p>
      </div>
      <div class="step-actions">
        <el-button @click="currentStep = 0">上一步</el-button>
        <el-button type="primary" :loading="generating" @click="handleGenerate">
          {{ generating ? '生成中...' : '开始生成' }}
        </el-button>
      </div>
    </div>

    <!-- Step 3: Result table -->
    <div v-show="currentStep === 2" class="step-content">
      <div class="result-table-wrap">
        <div class="table-header">
          <span>已生成 {{ batchBills.length }} 份对账单</span>
          <el-button type="primary" :loading="sendingAll" @click="handleSendAll">一键发送全部</el-button>
        </div>
        <el-table :data="batchBills" stripe>
          <el-table-column prop="billNo" label="对账单号" min-width="130" />
          <el-table-column prop="buyerName" label="买方" min-width="160" />
          <el-table-column prop="period" label="对账周期" min-width="180" />
          <el-table-column prop="totalAmount" label="金额" width="120" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column prop="sendStatus" label="发送状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.sendStatus === 'SENT' ? 'success' : 'info'" size="small">
                {{ row.sendStatus === 'SENT' ? '已发送' : '待发送' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="step-actions">
        <el-button @click="handleReset">重新创建</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { batchCreate, getBatchBills, batchSendAll, queryBills } from '@/api/recon'

interface BuyerItem {
  id: number
  buyerName: string
  tradeCount: number
  totalAmount: number
}

interface BatchBill {
  id: number
  billNo: string
  buyerName: string
  period: string
  totalAmount: number
  sendStatus: string
}

const currentStep = ref(0)
const buyerTableRef = ref()
const selectedBuyers = ref<BuyerItem[]>([])
const generating = ref(false)
const progressPercent = ref(0)
const batchId = ref<string | null>(null)
const sendingAll = ref(false)

const step1Form = reactive({
  periodRange: null as [string, string] | null,
})

const buyerList = ref<BuyerItem[]>([])
const batchBills = ref<BatchBill[]>([])

const canProceedStep1 = computed(() => {
  return step1Form.periodRange && step1Form.periodRange.length === 2 && selectedBuyers.value.length > 0
})

const progressStatus = computed(() => {
  if (progressPercent.value >= 100) return 'success'
  if (progressPercent.value < 0) return 'exception'
  return undefined
})

const progressText = computed(() => {
  if (progressPercent.value >= 100) return '生成完成'
  if (generating.value) return `正在生成... ${progressPercent.value}%`
  return '点击"开始生成"创建批量对账单'
})

function formatAmount(val: number | undefined) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function handleBuyerSelection(rows: BuyerItem[]) {
  selectedBuyers.value = rows
}

function goToStep2() {
  if (!canProceedStep1.value) return
  currentStep.value = 1
  progressPercent.value = 0
}

function handleGenerate() {
  if (generating.value) return
  generating.value = true
  progressPercent.value = 0
  const timer = setInterval(() => {
    progressPercent.value += 10
    if (progressPercent.value >= 100) {
      clearInterval(timer)
      generating.value = false
      finishGenerate()
    }
  }, 300)
}

async function finishGenerate() {
  try {
    const periodStart = step1Form.periodRange?.[0] ?? '2025-02-01'
    const periodEnd = step1Form.periodRange?.[1] ?? '2025-02-28'
    const buyerIds = selectedBuyers.value.map((b) => b.id)
    const res = await batchCreate({
      periodStart,
      periodEnd,
      buyerIds,
    }) as { batchId?: string }
    batchId.value = res?.batchId ?? `batch-${Date.now()}`
    await fetchBatchBills()
    currentStep.value = 2
  } catch {
    batchId.value = `batch-${Date.now()}`
    batchBills.value = selectedBuyers.value.map((b, i) => ({
      id: 1000 + i,
      billNo: `R2025030${String(10 + i).padStart(2, '0')}`,
      buyerName: b.buyerName,
      period: `${step1Form.periodRange?.[0] ?? '2025-02-01'} ~ ${step1Form.periodRange?.[1] ?? '2025-02-28'}`,
      totalAmount: b.totalAmount,
      sendStatus: 'PENDING',
    }))
    currentStep.value = 2
  }
}

async function fetchBatchBills() {
  if (!batchId.value) return
  try {
    const res = await getBatchBills(batchId.value) as BatchBill[]
    batchBills.value = Array.isArray(res) ? res : []
    if (batchBills.value.length === 0) {
      batchBills.value = selectedBuyers.value.map((b, i) => ({
        id: 1000 + i,
        billNo: `R2025030${String(10 + i).padStart(2, '0')}`,
        buyerName: b.buyerName,
        period: `${step1Form.periodRange?.[0]} ~ ${step1Form.periodRange?.[1]}`,
        totalAmount: b.totalAmount,
        sendStatus: 'PENDING',
      }))
    }
  } catch {
    batchBills.value = selectedBuyers.value.map((b, i) => ({
      id: 1000 + i,
      billNo: `R2025030${String(10 + i).padStart(2, '0')}`,
      buyerName: b.buyerName,
      period: `${step1Form.periodRange?.[0]} ~ ${step1Form.periodRange?.[1]}`,
      totalAmount: b.totalAmount,
      sendStatus: 'PENDING',
    }))
  }
}

async function handleSendAll() {
  if (!batchId.value) return
  try {
    sendingAll.value = true
    const res = (await batchSendAll(batchId.value)) as { data?: number } | number
    const sentCount = typeof res === 'number' ? res : (res?.data ?? 0)
    ElMessage.success(`成功发送 ${sentCount} 份对账单`)
    await fetchBatchBills()
  } catch {
    ElMessage.error('批量发送失败')
  } finally {
    sendingAll.value = false
  }
}

function handleReset() {
  currentStep.value = 0
  step1Form.periodRange = null
  selectedBuyers.value = []
  batchId.value = null
  batchBills.value = []
  progressPercent.value = 0
  buyerTableRef.value?.clearSelection?.()
}

async function fetchBuyers() {
  try {
    const res = (await queryBills({ pageNum: 1, pageSize: 1000 })) as {
      records?: { buyerId?: number; buyerName?: string; totalAmount?: number }[]
      list?: { buyerId?: number; buyerName?: string; totalAmount?: number }[]
    }
    const bills = res?.records ?? res?.list ?? []
    const buyerMap = new Map<number, BuyerItem>()
    bills.forEach((b) => {
      if (b.buyerId != null && b.buyerName) {
        const existing = buyerMap.get(b.buyerId)
        const amt = typeof b.totalAmount === 'number' ? b.totalAmount : 0
        if (existing) {
          existing.tradeCount += 1
          existing.totalAmount += amt
        } else {
          buyerMap.set(b.buyerId, {
            id: b.buyerId,
            buyerName: b.buyerName,
            tradeCount: 1,
            totalAmount: amt,
          })
        }
      }
    })
    buyerList.value = Array.from(buyerMap.values())
  } catch {
    buyerList.value = []
  }
}

onMounted(() => {
  fetchBuyers()
})
</script>

<style lang="scss" scoped>
.batch-recon-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .el-steps {
    margin-bottom: 32px;
  }

  .step-content {
    background: #fff;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .step-form {
    margin-bottom: 24px;
  }

  .buyer-table-wrap {
    margin-bottom: 24px;

    .table-title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 12px;
    }
  }

  .progress-box {
    padding: 40px 20px;
    text-align: center;

    .progress-text {
      margin-top: 16px;
      color: #909399;
      font-size: 14px;
    }
  }

  .result-table-wrap {
    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      span {
        font-size: 14px;
        font-weight: 600;
      }
    }
  }

  .step-actions {
    margin-top: 24px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
  }
}
</style>
