<template>
  <div class="page-container template-manage">
    <div class="action-bar">
      <el-button type="primary" @click="openEditDialog()">新增模板</el-button>
    </div>

    <div class="template-grid">
      <el-card v-for="item in tableData" :key="item.id" shadow="hover" class="template-card">
        <div class="card-header">
          <span class="template-name">{{ item.name }}</span>
          <el-tag v-if="item.isDefault" type="success" size="small">默认</el-tag>
        </div>
        <div class="card-meta">
          <el-tag size="small">{{ item.contractType || '-' }}</el-tag>
          <span class="source-label">来源: {{ item.source === 'system' ? '系统' : '自定义' }}</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" link size="small" @click="openEditDialog(item)">编辑</el-button>
          <el-button
            v-if="!item.isDefault"
            type="danger"
            link
            size="small"
            @click="handleDelete(item)"
          >
            删除
          </el-button>
          <el-button
            v-if="!item.isDefault"
            type="primary"
            link
            size="small"
            @click="setDefault(item)"
          >
            设为默认
          </el-button>
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="showEditDialog"
      :title="editingId ? '编辑模板' : '新增模板'"
      width="500px"
      :close-on-click-modal="false"
      @close="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="120px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="合同类型" prop="contractType">
          <el-select v-model="editForm.contractType" placeholder="请选择" style="width: 100%">
            <el-option label="留货" value="留货" />
            <el-option label="订货" value="订货" />
            <el-option label="框架" value="框架" />
            <el-option label="简易" value="简易" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-radio-group v-model="editForm.source">
            <el-radio value="system">系统</el-radio>
            <el-radio value="custom">自定义</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="产地字段" prop="hasOriginField">
          <el-switch v-model="editForm.hasOriginField" />
        </el-form-item>
        <el-form-item label="条款优先级" prop="clausePriority">
          <el-radio-group v-model="editForm.clausePriority">
            <el-radio value="body">正文优先</el-radio>
            <el-radio value="attachment">附件优先</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="设为默认" prop="isDefault">
          <el-switch v-model="editForm.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { listTemplates, createTemplate, updateTemplate, deleteTemplate } from '@/api/evidence'

const loading = ref(false)
const submitting = ref(false)
const showEditDialog = ref(false)
const editingId = ref<number | null>(null)
const editFormRef = ref<FormInstance>()
const tableData = ref<any[]>([])

const editForm = reactive({
  name: '',
  contractType: '订货',
  source: 'custom' as 'system' | 'custom',
  hasOriginField: true,
  clausePriority: 'body' as 'body' | 'attachment',
  isDefault: false,
})

const editRules: FormRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = (await listTemplates()) as any
    const data = res?.data ?? res
    const list = Array.isArray(data) ? data : data?.list ?? data?.records ?? []
    tableData.value = list.map((r: any) => ({
      id: r.id,
      name: r.name ?? r.templateName ?? '-',
      contractType: r.contractType ?? r.contract_type ?? '-',
      source: r.source ?? 'custom',
      isDefault: r.isDefault ?? r.is_default ?? false,
    }))
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function openEditDialog(row?: any) {
  editingId.value = row?.id ?? null
  if (row) {
    editForm.name = row.name
    editForm.contractType = row.contractType ?? '订货'
    editForm.source = row.source ?? 'custom'
    editForm.hasOriginField = row.hasOriginField ?? true
    editForm.clausePriority = row.clausePriority ?? 'body'
    editForm.isDefault = row.isDefault ?? false
  }
  showEditDialog.value = true
}

function resetEditForm() {
  editingId.value = null
  editForm.name = ''
  editForm.contractType = '订货'
  editForm.source = 'custom'
  editForm.hasOriginField = true
  editForm.clausePriority = 'body'
  editForm.isDefault = false
  editFormRef.value?.resetFields()
}

async function submitEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        name: editForm.name,
        contractType: editForm.contractType,
        source: editForm.source,
        hasOriginField: editForm.hasOriginField,
        clausePriority: editForm.clausePriority,
        isDefault: editForm.isDefault,
      }
      if (editingId.value) {
        await updateTemplate(editingId.value, payload)
        ElMessage.success('更新成功')
      } else {
        await createTemplate(payload)
        ElMessage.success('新增成功')
      }
      showEditDialog.value = false
      loadData()
    } catch {
      // error handled
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该模板？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteTemplate(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') return
  }
}

async function setDefault(row: any) {
  try {
    await updateTemplate(row.id, { ...row, isDefault: true })
    ElMessage.success('已设为默认')
    loadData()
  } catch {
    // error handled
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.template-manage {
  .action-bar {
    margin-bottom: 20px;
  }

  .template-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;

    .template-card {
      .card-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;

        .template-name {
          font-weight: 600;
          font-size: 16px;
        }
      }

      .card-meta {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;

        .source-label {
          font-size: 12px;
          color: var(--el-text-color-secondary);
        }
      }

      .card-actions {
        display: flex;
        gap: 8px;
      }
    }
  }
}
</style>
