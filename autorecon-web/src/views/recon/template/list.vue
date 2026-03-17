<template>
  <div class="template-list-page">
    <h2 class="page-title">模板管理</h2>

    <div class="action-bar">
      <el-button type="primary" @click="handleCreate">新增模板</el-button>
    </div>

    <el-row :gutter="20" class="template-grid">
      <el-col v-for="t in templates" :key="t.id" :xs="24" :sm="12" :lg="8">
        <el-card class="template-card" shadow="hover">
          <div class="card-header">
            <span class="template-name">{{ t.name }}</span>
            <el-tag :type="(getTypeTag(t.type) as 'success' | 'warning' | 'info')" size="small">{{ getTypeText(t.type) }}</el-tag>
            <el-tag v-if="t.isDefault" type="success" size="small">默认</el-tag>
          </div>
          <div class="card-actions">
            <el-button type="primary" link size="small" @click="handlePreview(t)">预览</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(t)">编辑</el-button>
            <el-button
              v-if="!t.isDefault"
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
    <el-dialog v-model="previewVisible" title="模板预览" width="700px">
      <div v-if="previewTemplate" class="preview-layout">
        <div class="preview-header">
          <span v-if="previewTemplate.showCompany">[公司信息]</span>
          <span v-if="previewTemplate.showLogo">[LOGO]</span>
          <p>{{ previewTemplate.name }} - 对账单</p>
        </div>
        <el-table :data="previewRows" border size="small">
          <el-table-column
            v-for="col in previewColumns"
            :key="col.key"
            :prop="col.key"
            :label="col.label"
            min-width="80"
          />
        </el-table>
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
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option label="标准" value="STANDARD" />
            <el-option label="简易" value="SIMPLE" />
            <el-option label="自定义" value="CUSTOM" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
} from '@/api/recon'

interface Template {
  id: number
  name: string
  type: string
  isDefault: boolean
  showCompany?: boolean
  showLogo?: boolean
  columns?: string[]
  groupBy?: string
  sortBy?: string
}

const columnOptions = [
  { value: 'contractNo', label: '合同号' },
  { value: 'orderNo', label: '订单号' },
  { value: 'deliveryNo', label: '发货单号' },
  { value: 'productName', label: '品名' },
  { value: 'spec', label: '规格' },
  { value: 'material', label: '材质' },
  { value: 'quantity', label: '数量' },
  { value: 'weight', label: '重量' },
  { value: 'unitPrice', label: '单价' },
  { value: 'amount', label: '金额' },
  { value: 'tax', label: '税额' },
  { value: 'total', label: '合计' },
]

const loading = ref(false)
const templates = ref<Template[]>([])
const previewVisible = ref(false)
const previewTemplate = ref<Template | null>(null)
const formVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  type: 'STANDARD',
  isDefault: false,
  showCompany: true,
  showLogo: true,
  columns: ['contractNo', 'productName', 'quantity', 'weight', 'amount', 'total'],
  groupBy: 'CONTRACT',
  sortBy: 'DATE',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
}

const previewColumns = [
  { key: 'contractNo', label: '合同号' },
  { key: 'productName', label: '品名' },
  { key: 'quantity', label: '数量' },
  { key: 'amount', label: '金额' },
]

const previewRows = [
  { contractNo: 'HT2025001', productName: '螺纹钢', quantity: 100, amount: 12580 },
  { contractNo: 'HT2025001', productName: '线材', quantity: 50, amount: 6200 },
]

function getTypeTag(type: string) {
  const map: Record<string, string> = {
    STANDARD: '',
    SIMPLE: 'info',
    CUSTOM: 'warning',
  }
  return map[type] ?? 'info'
}

function getTypeText(type: string) {
  const map: Record<string, string> = {
    STANDARD: '标准',
    SIMPLE: '简易',
    CUSTOM: '自定义',
  }
  return map[type] ?? type
}

function handleCreate() {
  editingId.value = null
  formVisible.value = true
}

function handleEdit(t: Template) {
  editingId.value = t.id
  form.name = t.name
  form.type = t.type
  form.isDefault = t.isDefault ?? false
  form.showCompany = t.showCompany ?? true
  form.showLogo = t.showLogo ?? true
  form.columns = t.columns ?? form.columns
  form.groupBy = t.groupBy ?? 'CONTRACT'
  form.sortBy = t.sortBy ?? 'DATE'
  formVisible.value = true
}

function handlePreview(t: Template) {
  previewTemplate.value = t
  previewVisible.value = true
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
  form.name = ''
  form.type = 'STANDARD'
  form.isDefault = false
  form.showCompany = true
  form.showLogo = true
  form.columns = ['contractNo', 'productName', 'quantity', 'weight', 'amount', 'total']
  form.groupBy = 'CONTRACT'
  form.sortBy = 'DATE'
  editingId.value = null
}

async function handleSubmit() {
  await formRef.value?.validate()
  try {
    const data = {
      name: form.name,
      type: form.type,
      isDefault: form.isDefault,
      showCompany: form.showCompany,
      showLogo: form.showLogo,
      columns: form.columns,
      groupBy: form.groupBy,
      sortBy: form.sortBy,
    }
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
    const res = await listTemplates() as Template[]
    templates.value = Array.isArray(res) ? res : []
    if (templates.value.length === 0) {
      templates.value = [
        { id: 1, name: '标准对账单', type: 'STANDARD', isDefault: true },
        { id: 2, name: '简易模板', type: 'SIMPLE', isDefault: false },
        { id: 3, name: '自定义模板A', type: 'CUSTOM', isDefault: false },
      ]
    }
  } catch {
    templates.value = [
      { id: 1, name: '标准对账单', type: 'STANDARD', isDefault: true },
      { id: 2, name: '简易模板', type: 'SIMPLE', isDefault: false },
      { id: 3, name: '自定义模板A', type: 'CUSTOM', isDefault: false },
    ]
  } finally {
    loading.value = false
  }
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
        margin: 8px 0 0;
        font-weight: 600;
      }
    }
  }

  :deep(.el-checkbox-group) {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 24px;
  }
}
</style>
