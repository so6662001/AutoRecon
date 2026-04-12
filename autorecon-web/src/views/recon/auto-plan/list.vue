<template>
  <div class="auto-plan-page">
    <h2 class="page-title">自动对账计划</h2>

    <div class="action-bar">
      <el-button type="primary" @click="handleCreate">新建计划</el-button>
    </div>

    <el-row v-loading="loading" :gutter="20" class="plan-grid">
      <el-col v-for="plan in plans" :key="plan.id" :xs="24" :sm="12" :lg="8">
        <el-card class="plan-card" shadow="hover">
          <div class="card-header">
            <span class="plan-name">{{ plan.name }}</span>
            <el-tag :type="plan.enabled ? 'success' : 'info'" size="small">
              {{ plan.enabled ? '已启用' : '已停用' }}
            </el-tag>
          </div>
          <div class="plan-info">
            <p>频率：{{ getFreqText(plan.frequency) }} · 执行日 {{ plan.executionDay }} 日 {{ plan.executionTime }}</p>
            <p>上次执行：{{ plan.lastExecutedAt || '-' }}</p>
            <p>下次执行：{{ plan.nextExecutedAt || '-' }}</p>
          </div>
          <div class="card-actions">
            <el-switch
              :model-value="plan.enabled"
              @change="() => handleToggle(plan)"
            />
            <el-button type="primary" link size="small" @click="handleEdit(plan)">编辑</el-button>
            <el-button type="primary" link size="small" @click="showHistory(plan)">执行历史</el-button>
            <el-button type="primary" link size="small" @click="handleTrigger(plan)">立即执行</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(plan)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑计划' : '新建计划'"
      width="560px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="计划名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="适用客户" prop="scope">
          <el-select v-model="form.scope" placeholder="请选择" style="width: 100%">
            <el-option label="全部客户" value="ALL" />
            <el-option label="指定客户" value="SPECIFIED" />
          </el-select>
        </el-form-item>
        <el-form-item label="频率" prop="frequency">
          <el-select v-model="form.frequency" placeholder="请选择" style="width: 100%">
            <el-option label="每月" value="MONTHLY" />
            <el-option label="每半月" value="BIWEEKLY" />
            <el-option label="每周" value="WEEKLY" />
            <el-option label="每季度" value="QUARTERLY" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行日" prop="executionDay">
          <el-input-number v-model="form.executionDay" :min="1" :max="31" style="width: 100%" />
        </el-form-item>
        <el-form-item label="执行时间" prop="executionTime">
          <el-time-picker
            v-model="form.executionTime"
            placeholder="选择时间"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="对账模板" prop="templateId">
          <el-select v-model="form.templateId" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="对账周期类型" prop="periodType">
          <el-select v-model="form.periodType" placeholder="请选择" style="width: 100%">
            <el-option label="上月" value="LAST_MONTH" />
            <el-option label="上半月" value="FIRST_HALF" />
            <el-option label="上周" value="LAST_WEEK" />
          </el-select>
        </el-form-item>
        <el-form-item label="生成后自动发送">
          <el-switch v-model="form.autoSend" />
        </el-form-item>
        <el-form-item label="包含付款信息">
          <el-switch v-model="form.includePayment" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="historyVisible" title="执行历史" size="500px">
      <el-timeline v-if="historyLogs.length > 0">
        <el-timeline-item
          v-for="log in historyLogs"
          :key="log.id"
          :timestamp="log.createdAt"
          placement="top"
        >
          <el-card shadow="hover">
            <p>{{ log.action || '自动对账执行' }}</p>
            <p v-if="log.detail" style="color: #999; font-size: 12px">{{ log.detail }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无执行记录" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listAutoPlans,
  createAutoPlan,
  updateAutoPlan,
  toggleAutoPlan,
  triggerAutoPlan,
  deleteAutoPlan,
  getAutoPlanLogs,
} from '@/api/system'
import { listTemplates } from '@/api/recon'

interface AutoPlan {
  id: number
  name: string
  frequency: string
  executionDay: number
  executionTime: string
  enabled: boolean
  lastExecutedAt?: string
  nextExecutedAt?: string
}

interface Template {
  id: number
  name: string
}

const loading = ref(false)
const plans = ref<AutoPlan[]>([])
const templates = ref<Template[]>([])
const formVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const historyVisible = ref(false)
const historyLogs = ref<{ id: number; action?: string; detail?: string; createdAt?: string }[]>([])

const form = reactive({
  name: '',
  scope: 'ALL',
  frequency: 'MONTHLY',
  executionDay: 1,
  executionTime: '02:00',
  templateId: null as number | null,
  periodType: 'LAST_MONTH',
  autoSend: true,
  includePayment: false,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  frequency: [{ required: true, message: '请选择频率', trigger: 'change' }],
}

function getFreqText(freq: string) {
  const map: Record<string, string> = {
    MONTHLY: '每月',
    BIWEEKLY: '每半月',
    WEEKLY: '每周',
    QUARTERLY: '每季度',
  }
  return map[freq] ?? freq
}

function handleCreate() {
  editingId.value = null
  formVisible.value = true
}

function handleEdit(plan: AutoPlan) {
  const p = plan as unknown as Record<string, string | number | boolean | null | undefined>
  editingId.value = plan.id
  form.name = plan.name
  form.scope = (p.scope as string) ?? 'ALL'
  form.frequency = plan.frequency
  form.executionDay = plan.executionDay ?? 1
  form.executionTime = plan.executionTime ?? '02:00'
  form.templateId = (p.templateId as number) ?? null
  form.periodType = (p.periodType as string) ?? 'LAST_MONTH'
  form.autoSend = (p.autoSend as boolean) ?? true
  form.includePayment = (p.includePayment as boolean) ?? false
  formVisible.value = true
}

async function handleToggle(plan: AutoPlan) {
  try {
    await toggleAutoPlan(plan.id)
    ElMessage.success(plan.enabled ? '已停用' : '已启用')
    fetchPlans()
  } catch {
    // error handled by interceptor
  }
}

async function handleTrigger(plan: AutoPlan) {
  try {
    await triggerAutoPlan(plan.id)
    ElMessage.success('已触发执行')
    fetchPlans()
  } catch {
    ElMessage.error('触发失败')
  }
}

async function showHistory(plan: AutoPlan) {
  try {
    const res = (await getAutoPlanLogs(plan.id)) as
      | { data?: { id: number; action?: string; detail?: string; createdAt?: string }[] }
      | { id: number; action?: string; detail?: string; createdAt?: string }[]
    const list = Array.isArray(res) ? res : (res?.data ?? [])
    historyLogs.value = list
  } catch {
    historyLogs.value = []
  }
  historyVisible.value = true
}

async function handleDelete(plan: AutoPlan) {
  await ElMessageBox.confirm('确定要删除该计划吗？', '确认删除', { type: 'warning' })
  try {
    await deleteAutoPlan(plan.id)
    ElMessage.success('已删除')
    fetchPlans()
  } catch {
    // error handled by interceptor
  }
}

function resetForm() {
  form.name = ''
  form.scope = 'ALL'
  form.frequency = 'MONTHLY'
  form.executionDay = 1
  form.executionTime = '02:00'
  form.templateId = null
  form.periodType = 'LAST_MONTH'
  form.autoSend = true
  form.includePayment = false
  editingId.value = null
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  try {
    const data = {
      name: form.name,
      scope: form.scope,
      frequency: form.frequency,
      executionDay: form.executionDay,
      executionTime: form.executionTime,
      templateId: form.templateId,
      periodType: form.periodType,
      autoSend: form.autoSend,
      includePayment: form.includePayment,
    }
    if (editingId.value) {
      await updateAutoPlan(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createAutoPlan(data)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    fetchPlans()
  } catch (e) {
    if (e !== false) throw e
  }
}

async function fetchPlans() {
  loading.value = true
  try {
    const res = await listAutoPlans() as AutoPlan[]
    plans.value = Array.isArray(res) ? res : []
  } catch {
    plans.value = []
  } finally {
    loading.value = false
  }
}

async function fetchTemplates() {
  try {
    const res = await listTemplates() as Template[]
    templates.value = Array.isArray(res) ? res : []
    if (templates.value.length === 0) {
      templates.value = [{ id: 1, name: '默认模板' }]
    }
  } catch {
    templates.value = [{ id: 1, name: '默认模板' }]
  }
}

onMounted(() => {
  fetchPlans()
  fetchTemplates()
})
</script>

<style lang="scss" scoped>
.auto-plan-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .action-bar {
    margin-bottom: 20px;
  }

  .plan-grid {
    margin-bottom: 20px;
  }

  .plan-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;

      .plan-name {
        font-weight: 600;
        flex: 1;
      }
    }

    .plan-info {
      font-size: 13px;
      color: #606266;
      margin-bottom: 12px;

      p {
        margin: 4px 0;
      }
    }

    .card-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }
}
</style>
