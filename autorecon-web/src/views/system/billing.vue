<template>
  <div class="billing-page">
    <h2 class="page-title">计费管理</h2>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="plan-card" shadow="hover">
          <template #header>
            <span>当前套餐</span>
            <el-tag :type="(planTagType || 'info') as 'success' | 'primary' | 'warning' | 'info' | 'danger'" size="small">{{ subscription?.planName ?? '免费版' }}</el-tag>
          </template>
          <div class="plan-info">
            <p>到期时间：{{ subscription?.expireAt ?? '-' }}</p>
            <p>自动续费：{{ subscription?.autoRenew ? '是' : '否' }}</p>
          </div>
          <el-button type="primary" @click="upgradeVisible = true">升级套餐</el-button>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card class="usage-card" shadow="hover">
          <template #header>使用情况</template>
          <div class="usage-bars">
            <div class="usage-item">
              <span class="label">对账单</span>
              <el-progress
                :percentage="billUsagePercent"
                :stroke-width="12"
                :color="progressColor"
              />
              <span class="value">{{ usage?.billsUsed ?? 0 }}/{{ usage?.billsLimit ?? 100 }}</span>
            </div>
            <div class="usage-item">
              <span class="label">客户数</span>
              <el-progress
                :percentage="customerUsagePercent"
                :stroke-width="12"
                :color="progressColor"
              />
              <span class="value">{{ usage?.customersUsed ?? 0 }}/{{ usage?.customersLimit ?? 30 }}</span>
            </div>
            <div class="usage-item">
              <span class="label">签章额度</span>
              <el-progress
                :percentage="sealRemainPercent"
                :stroke-width="12"
                :color="progressColor"
              />
              <span class="value">剩余 {{ usage?.sealRemaining ?? 0 }}</span>
            </div>
            <div class="usage-item">
              <span class="label">存储空间</span>
              <el-progress
                :percentage="storageUsagePercent"
                :stroke-width="12"
                :color="progressColor"
              />
              <span class="value">{{ usage?.storageUsed ?? 0 }} MB</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="seal-card">
      <template #header>签章包购买</template>
      <el-select v-model="selectedPackage" placeholder="选择套餐" style="width: 200px; margin-right: 12px">
        <el-option label="50次 ¥200" :value="1" />
        <el-option label="200次 ¥600" :value="2" />
        <el-option label="500次 ¥1000" :value="3" />
        <el-option label="2000次 ¥3000" :value="4" />
      </el-select>
      <el-button type="primary" :loading="purchasing" @click="handlePurchase">购买</el-button>
    </el-card>

    <el-card class="bills-card">
      <template #header>账单历史</template>
      <el-table v-loading="billsLoading" :data="bills" stripe>
        <el-table-column prop="month" label="月份" width="120" />
        <el-table-column prop="subscriptionFee" label="套餐费" width="120" align="right">
          <template #default="{ row }">¥{{ row.subscriptionFee ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="sealFee" label="签章费" width="120" align="right">
          <template #default="{ row }">¥{{ row.sealFee ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="total" label="合计" width="120" align="right">
          <template #default="{ row }">¥{{ (row.subscriptionFee ?? 0) + (row.sealFee ?? 0) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PAID' ? 'success' : 'warning'" size="small">
              {{ row.status === 'PAID' ? '已支付' : '待支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewBill(row)">查看</el-button>
            <el-button type="primary" link size="small" @click="handleInvoice(row)">开票</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="upgradeVisible" title="升级套餐" width="700px">
      <el-row :gutter="16">
        <el-col v-for="p in planOptions" :key="p.value" :span="6">
          <el-card
            class="plan-option"
            :class="{ selected: selectedPlan === p.value }"
            shadow="hover"
            @click="selectedPlan = p.value"
          >
            <div class="plan-name">{{ p.label }}</div>
            <div class="plan-price">¥{{ p.price }}/月</div>
            <div class="plan-desc">{{ p.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
      <template #footer>
        <el-button @click="upgradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="subscribing" @click="handleSubscribe">确认升级</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getCurrentSubscription,
  getUsage,
  getBillingBills,
  subscribe,
  purchaseSealPackage,
} from '@/api/system'

interface Subscription {
  planName?: string
  planCode?: string
  expireAt?: string
  autoRenew?: boolean
}

interface Usage {
  billsUsed?: number
  billsLimit?: number
  customersUsed?: number
  customersLimit?: number
  sealRemaining?: number
  storageUsed?: number
}

interface BillItem {
  month: string
  subscriptionFee?: number
  sealFee?: number
  status?: string
}

const subscription = ref<Subscription | null>(null)
const usage = ref<Usage | null>(null)
const bills = ref<BillItem[]>([])
const billsLoading = ref(false)
const selectedPackage = ref(1)
const purchasing = ref(false)
const upgradeVisible = ref(false)
const selectedPlan = ref('BASIC')
const subscribing = ref(false)

const planOptions = [
  { value: 'FREE', label: '免费版', price: 0, desc: '基础功能' },
  { value: 'BASIC', label: '基础版', price: 99, desc: '适合小微企业' },
  { value: 'STANDARD', label: '标准版', price: 299, desc: '适合成长企业' },
  { value: 'ENTERPRISE', label: '企业版', price: 999, desc: '全功能' },
]

const planTagType = computed(() => {
  const name = subscription.value?.planName ?? ''
  if (name.includes('企业')) return 'danger'
  if (name.includes('标准')) return 'primary'
  if (name.includes('基础')) return 'success'
  return 'info'
})

const progressColor = '#409eff'

const billUsagePercent = computed(() => {
  const u = usage.value
  if (!u?.billsLimit) return 0
  return Math.min(100, ((u.billsUsed ?? 0) / u.billsLimit) * 100)
})

const customerUsagePercent = computed(() => {
  const u = usage.value
  if (!u?.customersLimit) return 0
  return Math.min(100, ((u.customersUsed ?? 0) / u.customersLimit) * 100)
})

const sealRemainPercent = computed(() => {
  const u = usage.value
  const total = (u?.sealRemaining ?? 0) + 100
  return Math.min(100, ((u?.sealRemaining ?? 0) / total) * 100)
})

const storageUsagePercent = computed(() => {
  const u = usage.value
  const limit = (u as Record<string, number>)?.storageLimit ?? 1024
  return Math.min(100, ((u?.storageUsed ?? 0) / limit) * 100)
})

async function fetchData() {
  try {
    subscription.value = (await getCurrentSubscription()) as Subscription
  } catch {
    subscription.value = null
  }
  try {
    usage.value = (await getUsage()) as Usage
  } catch {
    usage.value = null
  }
  billsLoading.value = true
  try {
    const res = await getBillingBills({ page: 1, pageSize: 10 }) as { list?: BillItem[] }
    bills.value = res?.list ?? []
  } catch {
    bills.value = []
  } finally {
    billsLoading.value = false
  }
}

async function handlePurchase() {
  purchasing.value = true
  try {
    await purchaseSealPackage(selectedPackage.value)
    ElMessage.success('购买成功')
    fetchData()
  } catch {
    // error handled by interceptor
  } finally {
    purchasing.value = false
  }
}

async function handleSubscribe() {
  subscribing.value = true
  try {
    await subscribe({ planCode: selectedPlan.value })
    ElMessage.success('升级成功')
    upgradeVisible.value = false
    fetchData()
  } catch {
    // error handled by interceptor
  } finally {
    subscribing.value = false
  }
}

function handleViewBill(row: BillItem) {
  ElMessage.info(`查看账单 ${row.month}`)
}

function handleInvoice(row: BillItem) {
  ElMessage.info(`开票 ${row.month}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.billing-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .plan-card {
    margin-bottom: 20px;

    .plan-info {
      margin-bottom: 16px;
      color: #606266;

      p {
        margin: 8px 0;
      }
    }
  }

  .usage-card {
    margin-bottom: 20px;

    .usage-item {
      margin-bottom: 16px;

      .label {
        display: block;
        margin-bottom: 4px;
        font-size: 14px;
      }

      .value {
        font-size: 12px;
        color: #909399;
        margin-left: 8px;
      }

      :deep(.el-progress) {
        display: inline-block;
        width: calc(100% - 100px);
      }
    }
  }

  .seal-card {
    margin-bottom: 20px;
  }

  .bills-card {
    margin-bottom: 20px;
  }

  .plan-option {
    cursor: pointer;
    margin-bottom: 12px;

    &.selected {
      border-color: var(--el-color-primary);
    }

    .plan-name {
      font-weight: 600;
      margin-bottom: 4px;
    }

    .plan-price {
      font-size: 18px;
      color: var(--el-color-primary);
      margin-bottom: 8px;
    }

    .plan-desc {
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>
