<template>
  <div class="template-list-page">
    <h2 class="page-title">模板管理</h2>

    <div class="action-bar">
      <el-button type="primary" @click="handleCreate">新增模板</el-button>
    </div>

    <el-row v-loading="loading" :gutter="20" class="template-grid">
      <el-col v-for="t in templates" :key="t.id" :xs="24" :sm="12" :lg="8">
        <el-card class="template-card" shadow="hover">
          <div class="card-header">
            <span class="template-name">{{ t.templateName }}</span>
            <el-tag :type="(getTypeTag(t.templateType) as 'success' | 'warning' | 'info')" size="small">{{ getTypeText(t.templateType) }}</el-tag>
            <el-tag v-if="t.isDefault === 1" type="success" size="small">默认</el-tag>
          </div>
          <div class="card-actions">
            <el-button type="primary" link size="small" @click="handlePreview(t)">预览</el-button>
            <el-button type="primary" link size="small" @click="handleCopy(t)">复制</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(t)">编辑</el-button>
            <el-button
              v-if="t.isDefault !== 1"
              type="danger"
              link
              size="small"
              @click="handleDelete(t)"
            >
              删除
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Preview dialog -->
    <el-dialog v-model="previewVisible" title="模板预览" width="900px" @closed="previewData = null">
      <el-skeleton v-if="previewLoading" :rows="6" animated />
      <div v-else-if="previewData" class="preview-layout">
        <div class="preview-header">
          <p>{{ previewData.templateName }} - 对账单</p>
          <p v-if="previewData.groupBy" class="preview-meta">分组: {{ previewData.groupBy }} · 排序: {{ previewData.sortBy || '-' }}</p>
        </div>

        <template v-if="previewData.groupBy === 'CONTRACT' && previewData.groupedItems">
          <div v-for="(rows, contractNo) in previewData.groupedItems" :key="contractNo" class="preview-group">
            <h4 class="group-title">合同 {{ contractNo }}</h4>
            <el-table :data="rows" border size="small">
              <el-table-column
                v-for="col in dynamicPreviewColumns"
                :key="col.key"
                :prop="col.key"
                :label="col.label"
                min-width="88"
              />
            </el-table>
          </div>
        </template>
        <template v-else>
          <el-table :data="previewData.sampleItems || []" border size="small">
            <el-table-column
              v-for="col in dynamicPreviewColumns"
              :key="col.key"
              :prop="col.key"
              :label="col.label"
              min-width="88"
            />
          </el-table>
        </template>

        <h4 v-if="previewData.contractSummaries?.length" class="section-title">合同汇总</h4>
        <el-table
          v-if="previewData.contractSummaries?.length"
          :data="previewData.contractSummaries"
          border
          size="small"
        >
          <el-table-column prop="contractNo" label="合同号" min-width="120" />
          <el-table-column prop="itemCount" label="行数" width="80" />
          <el-table-column prop="totalWeight" label="合计重量" min-width="100" />
          <el-table-column prop="totalAmount" label="合计金额" min-width="120" />
        </el-table>

        <h4 class="section-title">合计</h4>
        <el-descriptions v-if="previewData.totalSummary" :column="2" border size="small">
          <el-descriptions-item
            v-for="(val, key) in previewData.totalSummary"
            :key="String(key)"
            :label="summaryLabel(key)"
          >
            {{ formatSummaryVal(val) }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">款项汇总</h4>
        <div v-if="previewData.paymentSummary" class="payment-block">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item
              v-for="([pKey, val]) in flatPaymentEntries(previewData.paymentSummary)"
              :key="pKey"
              :label="paymentLabel(pKey)"
            >
              {{ formatPaymentVal(val) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>

    <!-- Create/Edit dialog -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑模板' : '新增模板'"
      width="560px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板类型" prop="templateType">
          <el-select v-model="form.templateType" placeholder="请选择" style="width: 100%">
            <el-option label="标准" :value="1" />
            <el-option label="简易" :value="2" />
            <el-option label="自定义" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="设为默认" prop="isDefault">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
        <el-divider>表头配置</el-divider>
        <el-form-item label="公司信息">
          <el-switch v-model="form.showCompany" />
        </el-form-item>
        <el-form-item label="LOGO">
          <el-switch v-model="form.showLogo" />
        </el-form-item>
        <el-divider>列配置</el-divider>
        <el-form-item label="显示列">
          <el-checkbox-group v-model="form.columns">
            <el-checkbox
              v-for="c in columnOptions"
              :key="c.value"
              :label="c.value"
            >
              {{ c.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="分组方式" prop="groupBy">
          <el-select v-model="form.groupBy" placeholder="请选择" style="width: 100%">
            <el-option label="按合同" value="CONTRACT" />
            <el-option label="按日期" value="DATE" />
            <el-option label="不分组" value="NONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序方式" prop="sortBy">
          <el-select v-model="form.sortBy" placeholder="请选择" style="width: 100%">
            <el-option label="按日期" value="DATE" />
            <el-option label="按合同号" value="CONTRACT_NO" />
            <el-option label="按品名" value="PRODUCT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  copyTemplate,
  previewTemplate as fetchPreviewTemplate,
} from '@/api/recon'

interface Template {
  id: number
  templateName: string
  templateType: number
  isDefault?: number
  headerConfig?: string
  columnConfig?: string
  footerConfig?: string
  styleConfig?: string
  groupBy?: string
  sortBy?: string
}

interface PreviewVO {
  templateName?: string
  templateType?: number
  visibleColumns?: string[]
  groupBy?: string
  sortBy?: string
  sampleItems?: Record<string, unknown>[]
  groupedItems?: Record<string, Record<string, unknown>[]>
  contractSummaries?: Record<string, unknown>[]
  totalSummary?: Record<string, unknown>
  paymentSummary?: Record<string, unknown>
}

const columnOptions = [
  { value: 'contractNo', label: '合同号' },
  { value: 'orderNo', label: '订单号' },
  { value: 'deliveryNo', label: '发货单号' },
  { value: 'productName', label: '品名' },
  { value: 'spec', label: '规格' },
  { value: 'material', label: '材质' },
  { value: 'origin', label: '产地' },
  { value: 'quantity', label: '数量' },
  { value: 'weight', label: '重量' },
  { value: 'unitPrice', label: '单价' },
  { value: 'amount', label: '金额' },
  { value: 'taxAmount', label: '税额' },
  { value: 'totalAmount', label: '合计' },
]

const columnLabelMap: Record<string, string> = Object.fromEntries(
  columnOptions.map((c) => [c.value, c.label])
)

const loading = ref(false)
const templates = ref<Template[]>([])
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref<PreviewVO | null>(null)
const formVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive({
  templateName: '',
  templateType: 1,
  isDefault: false,
  showCompany: true,
  showLogo: true,
  columns: ['contractNo', 'productName', 'quantity', 'weight', 'amount', 'totalAmount'] as string[],
  groupBy: 'CONTRACT',
  sortBy: 'DATE',
})

const rules: FormRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateType: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
}

const dynamicPreviewColumns = computed(() => {
  const keys = previewData.value?.visibleColumns?.length
    ? previewData.value.visibleColumns
    : ['contractNo', 'productName', 'quantity', 'amount']
  return keys.map((key) => ({
    key,
    label: columnLabelMap[key] || key,
  }))
})

function getTypeTag(type: number | undefined) {
  const map: Record<number, string> = {
    1: '',
    2: 'info',
    3: 'warning',
  }
  return type != null ? map[type] ?? 'info' : 'info'
}

function getTypeText(type: number | undefined) {
  const map: Record<number, string> = {
    1: '标准',
    2: '简易',
    3: '自定义',
  }
  return type != null ? map[type] ?? String(type) : ''
}

function parseHeader(t: Template) {
  if (!t.headerConfig) return { showCompany: true, showLogo: true }
  try {
    const o = JSON.parse(t.headerConfig) as { showCompany?: boolean; showLogo?: boolean }
    return {
      showCompany: o.showCompany ?? true,
      showLogo: o.showLogo ?? true,
    }
  } catch {
    return { showCompany: true, showLogo: true }
  }
}

function parseColumns(t: Template): string[] {
  if (!t.columnConfig) return form.columns
  try {
    const arr = JSON.parse(t.columnConfig) as string[]
    return Array.isArray(arr) && arr.length ? arr : form.columns
  } catch {
    return form.columns
  }
}

function handleCreate() {
  editingId.value = null
  formVisible.value = true
}

function handleEdit(t: Template) {
  editingId.value = t.id
  form.templateName = t.templateName
  form.templateType = t.templateType ?? 1
  form.isDefault = (t.isDefault ?? 0) === 1
  const h = parseHeader(t)
  form.showCompany = h.showCompany
  form.showLogo = h.showLogo
  form.columns = parseColumns(t)
  form.groupBy = t.groupBy || 'CONTRACT'
  form.sortBy = t.sortBy || 'DATE'
  formVisible.value = true
}

async function handlePreview(t: Template) {
  previewVisible.value = true
  previewLoading.value = true
  previewData.value = null
  try {
    const res = (await fetchPreviewTemplate(t.id)) as unknown
    if (res && typeof res === 'object' && 'data' in res) {
      previewData.value = (res as { data: PreviewVO }).data
    } else {
      previewData.value = res as PreviewVO
    }
  } catch {
    previewData.value = null
  } finally {
    previewLoading.value = false
  }
}

async function handleCopy(t: Template) {
  try {
    await copyTemplate(t.id)
    ElMessage.success('已复制模板')
    fetchTemplates()
  } catch {
    // interceptor
  }
}

async function handleDelete(t: Template) {
  await ElMessageBox.confirm('确定要删除该模板吗？', '确认删除', { type: 'warning' })
  try {
    await deleteTemplate(t.id)
    ElMessage.success('已删除')
    fetchTemplates()
  } catch {
    // error handled by interceptor
  }
}

function resetForm() {
  form.templateName = ''
  form.templateType = 1
  form.isDefault = false
  form.showCompany = true
  form.showLogo = true
  form.columns = ['contractNo', 'productName', 'quantity', 'weight', 'amount', 'totalAmount']
  form.groupBy = 'CONTRACT'
  form.sortBy = 'DATE'
  editingId.value = null
}

function buildPayload() {
  return {
    templateName: form.templateName,
    templateType: form.templateType,
    isDefault: form.isDefault ? 1 : 0,
    headerConfig: JSON.stringify({ showCompany: form.showCompany, showLogo: form.showLogo }),
    columnConfig: JSON.stringify(form.columns),
    footerConfig: '{}',
    styleConfig: '{}',
    groupBy: form.groupBy,
    sortBy: form.sortBy,
  }
}

async function handleSubmit() {
  await formRef.value?.validate()
  try {
    const data = buildPayload()
    if (editingId.value) {
      await updateTemplate(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createTemplate(data)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    fetchTemplates()
  } catch (e) {
    if (e !== false) throw e
  }
}

async function fetchTemplates() {
  loading.value = true
  try {
    const res = (await listTemplates()) as unknown
    let list: Template[] = []
    if (Array.isArray(res)) {
      list = res as Template[]
    } else if (res && typeof res === 'object' && 'data' in res) {
      const d = (res as { data?: unknown }).data
      list = Array.isArray(d) ? (d as Template[]) : []
    }
    templates.value = list
  } catch {
    templates.value = []
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

function summaryLabel(key: string | number) {
  const map: Record<string, string> = {
    totalQuantity: '总数量',
    totalWeight: '总重量',
    totalAmount: '总金额',
    totalTaxAmount: '总税额',
    totalWithTax: '价税合计',
  }
  return map[String(key)] || String(key)
}

function formatSummaryVal(val: unknown) {
  if (val != null && typeof val === 'object') return JSON.stringify(val)
  return val as string | number
}

function paymentLabel(key: string) {
  const map: Record<string, string> = {
    prevBalance: '上期余额',
    currentTradeAmount: '本期交易额',
    currentPaymentAmount: '本期付款',
    totalPayable: '应付合计',
    totalPaid: '已付合计',
    currentBalance: '当前余额',
    payments: '付款明细',
  }
  return map[key] || key
}

function formatPaymentVal(val: unknown) {
  if (Array.isArray(val)) {
    return val.map((row) => JSON.stringify(row)).join('； ')
  }
  if (val != null && typeof val === 'object') return JSON.stringify(val)
  return val as string | number
}

function flatPaymentEntries(summary: Record<string, unknown>): [string, unknown][] {
  const entries: [string, unknown][] = []
  for (const [k, v] of Object.entries(summary)) {
    if (k === 'payments' && Array.isArray(v)) {
      v.forEach((p, i) => {
        entries.push([`payments_${i}`, p])
      })
    } else {
      entries.push([k, v])
    }
  }
  return entries
}

onMounted(() => {
  fetchTemplates()
})
</script>

<style lang="scss" scoped>
.template-list-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .action-bar {
    margin-bottom: 20px;
  }

  .template-grid {
    margin-bottom: 20px;
  }

  .template-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;

      .template-name {
        font-weight: 600;
        flex: 1;
      }
    }

    .card-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  }

  .preview-layout {
    .preview-header {
      margin-bottom: 16px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 4px;

      p {
        margin: 0;
        font-weight: 600;
      }

      .preview-meta {
        margin-top: 8px !important;
        font-weight: 400;
        font-size: 13px;
        color: #606266;
      }
    }

    .preview-group {
      margin-bottom: 20px;

      .group-title {
        margin: 0 0 8px;
        font-size: 14px;
        font-weight: 600;
      }
    }

    .section-title {
      margin: 16px 0 8px;
      font-size: 14px;
      font-weight: 600;
    }

    .payment-block {
      margin-top: 8px;
    }
  }

  :deep(.el-checkbox-group) {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 24px;
  }
}
</style>
