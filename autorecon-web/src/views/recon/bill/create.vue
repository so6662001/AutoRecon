<template>
  <div class="bill-create-page">
    <h2 class="page-title">发起对账</h2>

    <el-steps :active="currentStep" finish-status="success" align-center class="steps">
      <el-step title="选择客户与周期" />
      <el-step title="对账明细" />
      <el-step title="预览确认" />
      <el-step title="完成" />
    </el-steps>

    <!-- Step 1 -->
    <div v-show="currentStep === 0" class="step-content">
      <el-form :model="step1Form" label-width="140px" class="form-step1">
        <el-form-item label="买方企业" required>
          <el-select
            v-model="step1Form.buyerId"
            filterable
            placeholder="请选择或搜索买方企业"
            style="width: 100%"
          >
            <el-option
              v-for="b in buyerOptions"
              :key="b.id"
              :label="b.name"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="对账周期" required>
          <el-date-picker
            v-model="step1Form.periodRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="对账模板" required>
          <el-select v-model="step1Form.templateId" placeholder="请选择模板" style="width: 100%">
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据来源">
          <el-radio-group v-model="step1Form.dataSource">
            <el-radio value="ERP">ERP自动拉取</el-radio>
            <el-radio value="EXCEL">手动导入Excel</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="付款抵扣策略">
          <el-radio-group v-model="step1Form.deductionStrategy">
            <el-radio value="FIFO">FIFO</el-radio>
            <el-radio value="SPECIFIED">指定抵扣</el-radio>
            <el-radio value="PROPORTION">按比例</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </div>

    <!-- Step 2 -->
    <div v-show="currentStep === 1" class="step-content">
      <div class="step2-actions">
        <el-button type="primary" @click="addRow">添加行</el-button>
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".xlsx,.xls"
          @change="handleExcelImport"
        >
          <el-button>导入Excel</el-button>
        </el-upload>
      </div>
      <el-table :data="items" border style="width: 100%">
        <el-table-column prop="contractNo" label="合同号" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.contractNo" size="small" placeholder="合同号" />
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.orderNo" size="small" placeholder="订单号" />
          </template>
        </el-table-column>
        <el-table-column prop="deliveryNo" label="发货单号" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.deliveryNo" size="small" placeholder="发货单号" />
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="品名" min-width="100">
          <template #default="{ row }">
            <el-input v-model="row.productName" size="small" placeholder="品名" />
          </template>
        </el-table-column>
        <el-table-column prop="spec" label="规格" width="80">
          <template #default="{ row }">
            <el-input v-model="row.spec" size="small" placeholder="规格" />
          </template>
        </el-table-column>
        <el-table-column prop="material" label="材质" width="80">
          <template #default="{ row }">
            <el-input v-model="row.material" size="small" placeholder="材质" />
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" align="right">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" size="small" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="重量(吨)" width="100" align="right">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" size="small" :min="0" :precision="4" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column prop="unitPrice" label="单价" width="100" align="right">
          <template #default="{ row }">
            <el-input-number v-model="row.unitPrice" size="small" :min="0" :precision="4" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="110" align="right">
          <template #default="{ row }">
            {{ formatAmount(calcAmount(row)) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link size="small" @click="removeRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="summary">
        <span>合计数量: {{ totalQuantity }}</span>
        <span>合计重量: {{ totalWeight }} 吨</span>
        <span>合计金额: ¥{{ formatAmount(totalAmount) }}</span>
      </div>
    </div>

    <!-- Step 3 -->
    <div v-show="currentStep === 2" class="step-content">
      <div class="preview-summary">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="买方企业">{{ getBuyerName() }}</el-descriptions-item>
          <el-descriptions-item label="对账周期">{{ getPeriodText() }}</el-descriptions-item>
          <el-descriptions-item label="对账模板">{{ getTemplateName() }}</el-descriptions-item>
          <el-descriptions-item label="数据来源">{{ step1Form.dataSource === 'ERP' ? 'ERP自动拉取' : '手动导入Excel' }}</el-descriptions-item>
          <el-descriptions-item label="付款抵扣策略">{{ getDeductionText() }}</el-descriptions-item>
          <el-descriptions-item label="合计金额">¥{{ formatAmount(totalAmount) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-table :data="items" border style="width: 100%; margin-top: 20px" max-height="300">
        <el-table-column prop="contractNo" label="合同号" width="120" />
        <el-table-column prop="orderNo" label="订单号" width="120" />
        <el-table-column prop="deliveryNo" label="发货单号" width="120" />
        <el-table-column prop="productName" label="品名" width="100" />
        <el-table-column prop="spec" label="规格" width="80" />
        <el-table-column prop="material" label="材质" width="80" />
        <el-table-column prop="quantity" label="数量" width="90" align="right" />
        <el-table-column prop="weight" label="重量(吨)" width="100" align="right" />
        <el-table-column prop="unitPrice" label="单价" width="100" align="right" />
        <el-table-column prop="amount" label="金额" width="110" align="right">
          <template #default="{ row }">{{ formatAmount(calcAmount(row)) }}</template>
        </el-table-column>
      </el-table>
      <el-form-item label="发送选项" class="send-option">
        <el-radio-group v-model="sendOption">
          <el-radio value="AUTO">生成后自动发送</el-radio>
          <el-radio value="MANUAL">人工确认后发送</el-radio>
        </el-radio-group>
      </el-form-item>
    </div>

    <!-- Step 4 -->
    <div v-show="currentStep === 3" class="step-content step-done">
      <el-result icon="success" title="创建成功" sub-title="对账单已成功创建">
        <template #extra>
          <p class="bill-no">对账单号: {{ createdBillNo }}</p>
          <div class="done-actions">
            <el-button type="primary" @click="goToDetail">查看对账单</el-button>
            <el-button @click="continueCreate">继续创建</el-button>
            <el-button @click="goToList">返回列表</el-button>
          </div>
        </template>
      </el-result>
    </div>

    <!-- Footer buttons -->
    <div v-if="currentStep < 3" class="step-footer">
      <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
      <el-button v-if="currentStep < 2" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-if="currentStep === 2" type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createBill, listTemplates } from '@/api/recon'

interface BillItem {
  contractNo: string
  orderNo: string
  deliveryNo: string
  productName: string
  spec: string
  material: string
  quantity: number
  weight: number
  unitPrice: number
}

interface Template {
  id: number
  name: string
}

interface Buyer {
  id: number
  name: string
}

const router = useRouter()
const currentStep = ref(0)
const submitting = ref(false)
const createdBillNo = ref('')
const createdBillId = ref<number | null>(null)

const templates = ref<Template[]>([])
const buyerOptions = ref<Buyer[]>([
  { id: 1, name: '某某贸易有限公司' },
  { id: 2, name: '某某制造有限公司' },
  { id: 3, name: '某某科技股份有限公司' },
])

const step1Form = reactive({
  buyerId: null as number | null,
  periodRange: null as [string, string] | null,
  templateId: null as number | null,
  dataSource: 'ERP' as string,
  deductionStrategy: 'FIFO' as string,
})

const items = ref<BillItem[]>([
  {
    contractNo: 'HT2025001',
    orderNo: 'DD202503001',
    deliveryNo: 'FH202503001',
    productName: '钢材',
    spec: 'Φ20',
    material: 'Q235',
    quantity: 100,
    weight: 2.5,
    unitPrice: 4500,
  },
])

const sendOption = ref('MANUAL')

function formatAmount(val: number) {
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function calcAmount(row: BillItem) {
  const q = row.quantity ?? 0
  const p = row.unitPrice ?? 0
  return q * p
}

const totalQuantity = computed(() =>
  items.value.reduce((s, r) => s + (r.quantity ?? 0), 0)
)
const totalWeight = computed(() =>
  items.value.reduce((s, r) => s + (r.weight ?? 0), 0)
)
const totalAmount = computed(() =>
  items.value.reduce((s, r) => s + calcAmount(r), 0)
)

function addRow() {
  items.value.push({
    contractNo: '',
    orderNo: '',
    deliveryNo: '',
    productName: '',
    spec: '',
    material: '',
    quantity: 0,
    weight: 0,
    unitPrice: 0,
  })
}

function removeRow(index: number) {
  items.value.splice(index, 1)
}

function handleExcelImport(uploadFile: { raw?: File }) {
  if (uploadFile?.raw) {
    ElMessage.info('Excel导入功能需对接后端解析接口')
  }
}

function getBuyerName() {
  const id = step1Form.buyerId
  return buyerOptions.value.find((b) => b.id === id)?.name ?? '-'
}

function getPeriodText() {
  const r = step1Form.periodRange
  return r ? `${r[0]} ~ ${r[1]}` : '-'
}

function getTemplateName() {
  const id = step1Form.templateId
  return templates.value.find((t) => t.id === id)?.name ?? '-'
}

function getDeductionText() {
  const m: Record<string, string> = {
    FIFO: 'FIFO',
    SPECIFIED: '指定抵扣',
    PROPORTION: '按比例',
  }
  return m[step1Form.deductionStrategy] ?? step1Form.deductionStrategy
}

function validateStep1() {
  if (!step1Form.buyerId) {
    ElMessage.warning('请选择买方企业')
    return false
  }
  if (!step1Form.periodRange?.length) {
    ElMessage.warning('请选择对账周期')
    return false
  }
  if (!step1Form.templateId) {
    ElMessage.warning('请选择对账模板')
    return false
  }
  return true
}

function validateStep2() {
  if (items.value.length === 0) {
    ElMessage.warning('请添加至少一条对账明细')
    return false
  }
  return true
}

function prevStep() {
  if (currentStep.value > 0) currentStep.value--
}

function nextStep() {
  if (currentStep.value === 0 && !validateStep1()) return
  if (currentStep.value === 1 && !validateStep2()) return
  if (currentStep.value < 2) currentStep.value++
}

async function submitCreate() {
  if (!validateStep1() || !validateStep2()) return
  submitting.value = true
  try {
    const payload = {
      buyerId: step1Form.buyerId,
      periodStart: step1Form.periodRange![0],
      periodEnd: step1Form.periodRange![1],
      templateId: step1Form.templateId,
      dataSource: step1Form.dataSource,
      deductionStrategy: step1Form.deductionStrategy,
      items: items.value.map((r) => ({
        ...r,
        amount: calcAmount(r),
      })),
      autoSend: sendOption.value === 'AUTO',
    }
    const res = await createBill(payload) as { billNo?: string; id?: number }
    createdBillNo.value = res?.billNo ?? 'R' + Date.now()
    createdBillId.value = res?.id ?? null
    currentStep.value = 3
    ElMessage.success('创建成功')
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

function goToDetail() {
  if (createdBillId.value) {
    router.push({ name: 'billDetail', params: { id: String(createdBillId.value) } })
  } else {
    router.push({ name: 'billList' })
  }
}

function continueCreate() {
  currentStep.value = 0
  createdBillNo.value = ''
  createdBillId.value = null
  items.value = [{
    contractNo: '',
    orderNo: '',
    deliveryNo: '',
    productName: '',
    spec: '',
    material: '',
    quantity: 0,
    weight: 0,
    unitPrice: 0,
  }]
}

function goToList() {
  router.push({ name: 'billList' })
}

async function loadTemplates() {
  try {
    const res = await listTemplates() as Template[]
    templates.value = Array.isArray(res) ? res : []
    if (templates.value.length === 0) {
      templates.value = [{ id: 1, name: '标准对账模板' }, { id: 2, name: '简化对账模板' }]
    }
  } catch {
    templates.value = [{ id: 1, name: '标准对账模板' }]
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style lang="scss" scoped>
.bill-create-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .steps {
    margin-bottom: 32px;
  }

  .step-content {
    background: #fff;
    padding: 24px;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
    min-height: 300px;
  }

  .form-step1 {
    max-width: 560px;
  }

  .step2-actions {
    margin-bottom: 16px;
    display: flex;
    gap: 12px;
  }

  .summary {
    margin-top: 16px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    display: flex;
    gap: 24px;
    font-weight: 500;
  }

  .preview-summary {
    margin-bottom: 20px;
  }

  .send-option {
    margin-top: 24px;
  }

  .step-done {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 400px;

    .bill-no {
      margin: 16px 0;
      font-size: 16px;
      font-weight: 500;
    }

    .done-actions {
      display: flex;
      gap: 12px;
      justify-content: center;
    }
  }

  .step-footer {
    margin-top: 24px;
    display: flex;
    justify-content: center;
    gap: 12px;
  }
}
</style>
