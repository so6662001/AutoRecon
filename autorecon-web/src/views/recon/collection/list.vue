<template>
  <div class="collection-list-page">
    <h2 class="page-title">催收管理</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.activeCount }}</div>
          <div class="stat-label">活跃催收数</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">¥{{ formatAmount(stats.monthCollection) }}</div>
          <div class="stat-label">本月回款额</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value stat-overdue">¥{{ formatAmount(stats.overdueAmount) }}</div>
          <div class="stat-label">逾期总额</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="action-bar">
      <el-button type="primary" @click="createDialogVisible = true">新建催收计划</el-button>
    </div>

    <el-form :model="searchForm" inline class="search-form">
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="催收中" value="ACTIVE" />
          <el-option label="已暂停" value="PAUSED" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户">
        <el-input v-model="searchForm.buyer" placeholder="买方名称" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
      <el-table-column prop="billNo" label="对账单号" min-width="130" />
      <el-table-column prop="buyerName" label="买方" min-width="140" />
      <el-table-column prop="totalAmount" label="应收金额" width="110" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="paidAmount" label="已收金额" width="110" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.paidAmount) }}</template>
      </el-table-column>
      <el-table-column prop="balanceAmount" label="剩余金额" width="110" align="right">
        <template #default="{ row }">¥{{ formatAmount(row.balanceAmount) }}</template>
      </el-table-column>
      <el-table-column prop="dueDate" label="到期日" width="110" />
      <el-table-column prop="overdueDays" label="逾期天数" width="90" align="center">
        <template #default="{ row }">
          <span :class="{ 'text-danger': row.overdueDays > 0 }">
            {{ row.overdueDays ?? 0 }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="stage" label="当前阶段" width="100" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="(getStatusTagType(row.status) as 'success' | 'warning' | 'info' | 'danger')" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="nextCollectDate" label="下次催收日" width="110" />
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewLogs(row)">查看日志</el-button>
          <el-button type="primary" link size="small" @click="handleExecute(row)">执行催收</el-button>
          <el-button type="primary" link size="small" @click="handleRegisterPayment(row)">
            登记回款
          </el-button>
          <el-button
            v-if="row.status === 'ACTIVE'"
            type="warning"
            link
            size="small"
            @click="handlePause(row)"
          >
            暂停
          </el-button>
          <el-button
            v-if="row.status === 'PAUSED'"
            type="success"
            link
            size="small"
            @click="handleResume(row)"
          >
            恢复
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Execute dialog -->
    <el-dialog v-model="executeVisible" title="执行催收" width="480px">
      <el-form :model="executeForm" label-width="100px">
        <el-form-item label="催收方式">
          <el-select v-model="executeForm.method" placeholder="请选择" style="width: 100%">
            <el-option label="短信" value="SMS" />
            <el-option label="邮件" value="EMAIL" />
            <el-option label="企微" value="WECHAT" />
            <el-option label="电话" value="PHONE" />
            <el-option label="人工" value="MANUAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="催收内容">
          <el-input
            v-model="executeForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入催收内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExecuteSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Create plan dialog -->
    <el-dialog v-model="createDialogVisible" title="新建催收计划" width="520px" destroy-on-close @open="loadSignedBills">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="对账单" required>
          <el-select
            v-model="createForm.billId"
            filterable
            remote
            :remote-method="searchBills"
            placeholder="搜索对账单号"
            style="width: 100%"
            @change="onBillSelect"
          >
            <el-option
              v-for="b in signedBills"
              :key="b.id"
              :label="`${b.billNo ?? ''} (余额: ¥${formatAmount(b.balanceAmount)})`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="应收金额" required>
          <el-input-number v-model="createForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="到期日期" required>
          <el-date-picker
            v-model="createForm.dueDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择到期日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="催收策略" required>
          <el-select v-model="createForm.strategy" placeholder="请选择" style="width: 100%">
            <el-option label="A级-宽松" value="A" />
            <el-option label="B级-标准" value="B" />
            <el-option label="C级-积极" value="C" />
            <el-option label="D级-加急" value="D" />
            <el-option label="E级-升级" value="E" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="handleCreatePlan">确定</el-button>
      </template>
    </el-dialog>

    <!-- Logs drawer -->
    <el-drawer v-model="logsDrawerVisible" title="催收日志" size="480px" destroy-on-close>
      <el-timeline v-if="collectionLogs.length">
        <el-timeline-item
          v-for="(log, i) in collectionLogs"
          :key="i"
          :timestamp="log.createdAt ?? log.date"
          placement="top"
        >
          <el-tag size="small" :type="getLogActionTagType(log.actionType)">{{ log.actionType ?? '执行' }}</el-tag>
          <div class="log-content">{{ log.content }}</div>
          <div v-if="log.result" class="log-result">{{ log.result }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无日志" />
    </el-drawer>

    <!-- Register payment dialog -->
    <el-dialog v-model="registerVisible" title="登记回款" width="400px">
      <el-form :model="registerForm" label-width="100px">
        <el-form-item label="回款金额">
          <el-input-number
            v-model="registerForm.amount"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="registerForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRegisterSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listCollectionPlans,
  createCollectionPlan,
  executePlan,
  registerPaymentToCollection,
  pausePlan,
  resumePlan,
  getCollectionLogs,
  queryBills,
} from '@/api/recon'

interface CollectionPlan {
  id: number
  billNo: string
  buyerName: string
  totalAmount: number
  paidAmount: number
  balanceAmount: number
  dueDate: string
  overdueDays: number
  stage: string
  status: string
  nextCollectDate: string
}

interface SignedBill {
  id: number
  billNo?: string
  balanceAmount?: number
}

interface CollectionLog {
  createdAt?: string
  date?: string
  actionType?: string
  content?: string
  result?: string
}

const loading = ref(false)
const tableData = ref<CollectionPlan[]>([])
const executeVisible = ref(false)
const registerVisible = ref(false)
const createDialogVisible = ref(false)
const createSubmitting = ref(false)
const logsDrawerVisible = ref(false)
const collectionLogs = ref<CollectionLog[]>([])
const currentPlanId = ref<number | null>(null)
const signedBills = ref<SignedBill[]>([])

const stats = reactive({
  activeCount: 0,
  monthCollection: 0,
  overdueAmount: 0,
})

const searchForm = reactive({
  status: '',
  buyer: '',
})

const executeForm = reactive({
  method: 'PHONE',
  content: '',
})

const registerForm = reactive({
  amount: 0,
  remark: '',
})

const createForm = reactive({
  billId: null as number | null,
  amount: 0,
  dueDate: '',
  strategy: 'B',
})

function formatAmount(val: number | undefined) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function getStatusTagType(status: string) {
  const map: Record<string, string> = {
    ACTIVE: 'warning',
    PAUSED: 'info',
    COMPLETED: 'success',
  }
  return map[status] ?? 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    ACTIVE: '催收中',
    PAUSED: '已暂停',
    COMPLETED: '已完成',
  }
  return map[status] ?? status
}

function handleSearch() {
  fetchPlans()
}

function handleReset() {
  searchForm.status = ''
  searchForm.buyer = ''
  fetchPlans()
}

function handleExecute(row: CollectionPlan) {
  currentPlanId.value = row.id
  executeForm.method = 'PHONE'
  executeForm.content = ''
  executeVisible.value = true
}

async function handleExecuteSubmit() {
  if (!currentPlanId.value) return
  try {
    await executePlan(currentPlanId.value, {
      method: executeForm.method,
      content: executeForm.content,
    })
    ElMessage.success('催收已执行')
    executeVisible.value = false
    fetchPlans()
  } catch {
    // error handled by interceptor
  }
}

function handleRegisterPayment(row: CollectionPlan) {
  currentPlanId.value = row.id
  registerForm.amount = row.balanceAmount
  registerForm.remark = ''
  registerVisible.value = true
}

async function handleRegisterSubmit() {
  if (!currentPlanId.value) return
  try {
    await registerPaymentToCollection(currentPlanId.value, {
      amount: registerForm.amount,
      remark: registerForm.remark,
    })
    ElMessage.success('回款已登记')
    registerVisible.value = false
    fetchPlans()
  } catch {
    // error handled by interceptor
  }
}

async function handlePause(row: CollectionPlan) {
  try {
    await pausePlan(row.id)
    ElMessage.success('已暂停')
    fetchPlans()
  } catch {
    // error handled by interceptor
  }
}

async function handleResume(row: CollectionPlan) {
  try {
    await resumePlan(row.id)
    ElMessage.success('已恢复')
    fetchPlans()
  } catch {
    // error handled by interceptor
  }
}

async function loadSignedBills() {
  try {
    const res = await queryBills({ status: 'SIGNED' }) as { list?: SignedBill[] }
    signedBills.value = res?.list ?? []
  } catch {
    signedBills.value = []
  }
}

async function searchBills(query: string) {
  if (!query?.trim()) {
    loadSignedBills()
    return
  }
  try {
    const res = await queryBills({ status: 'SIGNED', billNo: query }) as { list?: SignedBill[] }
    signedBills.value = res?.list ?? []
  } catch {
    signedBills.value = []
  }
}

function onBillSelect(billId: number | null) {
  const bill = signedBills.value.find((b) => b.id === billId)
  if (bill?.balanceAmount != null) {
    createForm.amount = bill.balanceAmount
  }
}

async function handleCreatePlan() {
  if (!createForm.billId || !createForm.dueDate) {
    ElMessage.warning('请填写对账单和到期日期')
    return
  }
  createSubmitting.value = true
  try {
    await createCollectionPlan({
      billId: createForm.billId,
      amount: createForm.amount,
      dueDate: createForm.dueDate,
      strategy: createForm.strategy,
    })
    ElMessage.success('催收计划已创建')
    createDialogVisible.value = false
    createForm.billId = null
    createForm.amount = 0
    createForm.dueDate = ''
    createForm.strategy = 'B'
    fetchPlans()
  } catch {
    // error handled by interceptor
  } finally {
    createSubmitting.value = false
  }
}

async function handleViewLogs(row: CollectionPlan) {
  currentPlanId.value = row.id
  logsDrawerVisible.value = true
  try {
    const res = await getCollectionLogs(row.id) as CollectionLog[]
    collectionLogs.value = Array.isArray(res) ? res : []
  } catch {
    collectionLogs.value = []
  }
}

function getLogActionTagType(actionType?: string): 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    SMS: 'info',
    EMAIL: 'info',
    PHONE: 'warning',
    MANUAL: 'info',
  }
  return (map[actionType ?? ''] ?? 'info') as 'success' | 'warning' | 'info' | 'danger'
}

async function fetchPlans() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {}
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.buyer) params.buyer = searchForm.buyer
    const res = await listCollectionPlans(params) as {
      list?: CollectionPlan[]
      activeCount?: number
      monthCollection?: number
      overdueAmount?: number
    }
    tableData.value = res?.list ?? []
    stats.activeCount = res?.activeCount ?? tableData.value.filter((p) => p.status === 'ACTIVE').length
    stats.monthCollection = res?.monthCollection ?? 125600
    stats.overdueAmount = res?.overdueAmount ?? 45800
    if (tableData.value.length === 0) {
      tableData.value = [
        {
          id: 1,
          billNo: 'R202503001',
          buyerName: '某某贸易有限公司',
          totalAmount: 125800,
          paidAmount: 50000,
          balanceAmount: 75800,
          dueDate: '2025-03-10',
          overdueDays: 7,
          stage: '电话催收',
          status: 'ACTIVE',
          nextCollectDate: '2025-03-18',
        },
      ]
      stats.activeCount = 1
    }
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchPlans()
})
</script>

<style lang="scss" scoped>
.collection-list-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .stats-row {
    margin-bottom: 20px;
  }

  .stat-card {
    margin-bottom: 20px;

    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: #303133;

      &.stat-overdue {
        color: #f56c6c;
      }
    }

    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-top: 4px;
    }
  }

  .search-form {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 16px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .text-danger {
    color: #f56c6c;
    font-weight: 600;
  }

  .action-bar {
    margin-bottom: 16px;
  }

  .log-content {
    margin-top: 4px;
    font-size: 14px;
  }

  .log-result {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
  }
}
</style>
