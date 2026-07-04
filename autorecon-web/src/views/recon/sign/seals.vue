<template>
  <div class="seals-page">
    <div class="page-header">
      <h2 class="page-title">印章管理</h2>
      <el-button type="primary" @click="showAddSealDialog = true">
        <el-icon><Plus /></el-icon>
        添加印章
      </el-button>
    </div>

    <div class="seals-section">
      <h3 class="section-title">印章列表</h3>
      <el-row :gutter="20">
        <el-col v-for="seal in seals" :key="seal.id" :xs="24" :sm="12" :md="8" :lg="6">
          <div class="seal-card">
            <div class="seal-avatar">{{ seal.name?.slice(0, 1) || '印' }}</div>
            <div class="seal-name">{{ seal.name }}</div>
            <div class="seal-meta">
              <el-tag size="small" type="info">{{ getSealTypeText(seal.type) }}</el-tag>
              <el-tag
                :type="seal.status === 'ACTIVE' ? 'success' : 'info'"
                size="small"
                style="margin-left: 8px"
              >
                {{ seal.status === 'ACTIVE' ? '启用' : '停用' }}
              </el-tag>
            </div>
            <div class="seal-operator-count">经办人: {{ seal.operatorCount ?? 0 }}</div>
            <div class="seal-actions">
              <el-button
                v-if="seal.status === 'ACTIVE'"
                type="warning"
                link
                size="small"
                @click="handleDisable(seal)"
              >
                停用
              </el-button>
              <el-button
                v-else
                type="success"
                link
                size="small"
                @click="handleEnable(seal)"
              >
                启用
              </el-button>
              <el-button type="danger" link size="small" @click="handleRevoke(seal)">
                注销
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="operators-section">
      <div class="section-header">
        <h3 class="section-title">经办人</h3>
        <el-button type="primary" size="small" @click="handleAddOperator">
          添加经办人
        </el-button>
      </div>
      <el-table :data="operators" v-loading="operatorsLoading" stripe>
        <el-table-column prop="name" label="经办人姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="sealNames" label="可用印章" min-width="180">
          <template #default="{ row }">
            {{ Array.isArray(row.seals) ? row.seals.map((s: { name: string }) => s.name).join('、') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="amountLimit" label="金额上限" width="120" align="right">
          <template #default="{ row }">
            {{ row.amountLimit != null ? `¥${Number(row.amountLimit).toLocaleString()}` : '不限' }}
          </template>
        </el-table-column>
        <el-table-column prop="needApproval" label="需审批" width="90" align="center">
          <template #default="{ row }">
            {{ row.needApproval ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row: opRow }">
            <el-button type="primary" link size="small" @click="handleEditOperator(opRow)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDeleteOperator(opRow)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showAddSealDialog" title="添加印章" width="480px" destroy-on-close @close="resetSealForm">
      <el-form :model="sealForm" label-width="100px">
        <el-form-item label="印章名称" required>
          <el-input v-model="sealForm.name" placeholder="请输入印章名称" />
        </el-form-item>
        <el-form-item label="印章类型" required>
          <el-select v-model="sealForm.type" placeholder="请选择" style="width: 100%">
            <el-option label="公章" value="OFFICIAL" />
            <el-option label="合同章" value="CONTRACT" />
            <el-option label="财务章" value="FINANCE" />
            <el-option label="法人章" value="LEGAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源" required>
          <el-radio-group v-model="sealForm.source">
            <el-radio label="auto">自动生成</el-radio>
            <el-radio label="upload">上传印模</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddSealDialog = false">取消</el-button>
        <el-button type="primary" :loading="sealSubmitting" @click="handleAddSeal">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAddOperatorDialog" :title="editingOperatorId ? '编辑经办人' : '添加经办人'" width="480px" destroy-on-close @close="closeOperatorDialog">
      <el-form :model="operatorForm" label-width="100px">
        <el-form-item label="姓名" required>
          <el-input v-model="operatorForm.name" placeholder="请输入经办人姓名" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="operatorForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="可用印章">
          <el-select v-model="operatorForm.sealIds" multiple placeholder="请选择" style="width: 100%">
            <el-option
              v-for="s in seals"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额上限">
          <el-input-number v-model="operatorForm.amountLimit" :min="0" placeholder="不限" style="width: 100%" />
        </el-form-item>
        <el-form-item label="需审批">
          <el-switch v-model="operatorForm.needApproval" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddOperatorDialog = false">取消</el-button>
        <el-button type="primary" :loading="operatorSubmitting" @click="handleSaveOperator">{{ editingOperatorId ? '保存' : '确定' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listSeals,
  createSeal,
  disableSeal,
  enableSeal,
  revokeSeal,
  listOperators,
  createOperator,
  updateOperator,
  disableOperator,
} from '@/api/recon'

interface SealItem {
  id: number
  name: string
  type: string
  status: string
  operatorCount?: number
}

interface OperatorItem {
  id: number
  name: string
  phone: string
  seals?: { id: number; name: string }[]
  sealIds?: number[]
  amountLimit?: number
  needApproval?: boolean
  status: string
}

const seals = ref<SealItem[]>([])
const operators = ref<OperatorItem[]>([])
const operatorsLoading = ref(false)
const showAddSealDialog = ref(false)
const showAddOperatorDialog = ref(false)
const sealSubmitting = ref(false)
const operatorSubmitting = ref(false)
const editingOperatorId = ref<number | null>(null)

const sealForm = reactive({
  name: '',
  type: 'OFFICIAL',
  source: 'auto',
})

const operatorForm = reactive({
  name: '',
  phone: '',
  sealIds: [] as number[],
  amountLimit: undefined as number | undefined,
  needApproval: false,
})

function getSealTypeText(type: string): string {
  const map: Record<string, string> = {
    OFFICIAL: '公章',
    CONTRACT: '合同章',
    FINANCE: '财务章',
    LEGAL: '法人章',
  }
  return map[type] ?? type
}

function resetSealForm() {
  sealForm.name = ''
  sealForm.type = 'OFFICIAL'
  sealForm.source = 'auto'
}

async function handleDisable(seal: SealItem) {
  try {
    await ElMessageBox.confirm(`确定要停用印章「${seal.name}」吗？`, '提示', {
      type: 'warning',
    })
    await disableSeal(seal.id)
    ElMessage.success('已停用')
    fetchSeals()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handleEnable(seal: SealItem) {
  try {
    await enableSeal(seal.id)
    ElMessage.success('已启用')
    fetchSeals()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleRevoke(seal: SealItem) {
  try {
    await ElMessageBox.confirm('注销后不可恢复，确定注销？', '确认注销', {
      type: 'warning',
    })
    await revokeSeal(seal.id)
    ElMessage.success('已注销')
    fetchSeals()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handleAddSeal() {
  if (!sealForm.name.trim()) {
    ElMessage.warning('请输入印章名称')
    return
  }
  sealSubmitting.value = true
  try {
    await createSeal({
      name: sealForm.name,
      type: sealForm.type,
      source: sealForm.source,
    })
    ElMessage.success('添加成功')
    showAddSealDialog.value = false
    resetSealForm()
    fetchSeals()
  } catch {
    ElMessage.error('添加失败')
  } finally {
    sealSubmitting.value = false
  }
}

function handleEditOperator(row: OperatorItem) {
  editingOperatorId.value = row.id
  operatorForm.name = row.name
  operatorForm.phone = row.phone
  operatorForm.sealIds = Array.isArray(row.seals)
    ? row.seals.map((s) => s.id)
    : (row as OperatorItem & { sealIds?: number[] }).sealIds ?? []
  operatorForm.amountLimit = row.amountLimit
  operatorForm.needApproval = row.needApproval ?? false
  showAddOperatorDialog.value = true
}

function closeOperatorDialog() {
  editingOperatorId.value = null
  operatorForm.name = ''
  operatorForm.phone = ''
  operatorForm.sealIds = []
  operatorForm.amountLimit = undefined
  operatorForm.needApproval = false
}

async function handleDeleteOperator(row: OperatorItem) {
  try {
    await ElMessageBox.confirm(`确定要停用经办人「${row.name}」吗？`, '确认停用', {
      type: 'warning',
    })
    await disableOperator(row.id)
    ElMessage.success('已停用')
    fetchOperators()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handleSaveOperator() {
  if (!operatorForm.name.trim() || !operatorForm.phone.trim()) {
    ElMessage.warning('请填写姓名和手机号')
    return
  }
  operatorSubmitting.value = true
  try {
    const payload = {
      name: operatorForm.name,
      phone: operatorForm.phone,
      sealIds: operatorForm.sealIds,
      amountLimit: operatorForm.amountLimit,
      needApproval: operatorForm.needApproval,
    }
    if (editingOperatorId.value) {
      await updateOperator(editingOperatorId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await createOperator(payload)
      ElMessage.success('添加成功')
    }
    showAddOperatorDialog.value = false
    closeOperatorDialog()
    fetchOperators()
  } catch {
    ElMessage.error(editingOperatorId.value ? '保存失败' : '添加失败')
  } finally {
    operatorSubmitting.value = false
  }
}

async function handleAddOperator() {
  closeOperatorDialog()
  showAddOperatorDialog.value = true
}

async function fetchSeals() {
  try {
    const res = await listSeals() as SealItem[]
    seals.value = Array.isArray(res) ? res : []
  } catch {
    seals.value = []
  }
}

async function fetchOperators() {
  operatorsLoading.value = true
  try {
    const res = await listOperators() as OperatorItem[]
    operators.value = Array.isArray(res) ? res : []
  } catch {
    operators.value = []
  } finally {
    operatorsLoading.value = false
  }
}

onMounted(() => {
  fetchSeals()
  fetchOperators()
})
</script>

<style lang="scss" scoped>
.seals-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
  }

  .page-title {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .seals-section,
  .operators-section {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 24px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .section-title {
    margin: 0 0 16px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .section-header .section-title {
    margin: 0;
  }

  .seal-card {
    padding: 20px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    text-align: center;
    margin-bottom: 20px;
    transition: box-shadow 0.2s;

    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
    }

    .seal-avatar {
      width: 64px;
      height: 64px;
      margin: 0 auto 12px;
      border-radius: 50%;
      background: linear-gradient(135deg, #409eff, #79bbff);
      color: #fff;
      font-size: 24px;
      font-weight: bold;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .seal-name {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 8px;
    }

    .seal-meta {
      margin-bottom: 8px;
    }

    .seal-operator-count {
      font-size: 12px;
      color: #909399;
      margin-bottom: 12px;
    }

    .seal-actions {
      padding-top: 12px;
      border-top: 1px solid #ebeef5;
    }
  }
}
</style>
