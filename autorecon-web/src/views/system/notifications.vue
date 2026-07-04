<template>
  <div class="page-container">
    <h2>通知模板</h2>
    <p class="page-desc">按事件类型配置短信、邮件、推送模板，支持变量如 {billNo}</p>

    <div class="toolbar">
      <el-button type="primary" @click="openEdit()">新增模板</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" stripe v-loading="loading" empty-text="暂无模板">
      <el-table-column prop="eventType" label="事件类型" min-width="140" />
      <el-table-column label="渠道" width="160">
        <template #default="{ row }">{{ channelSummary(row) }}</template>
      </el-table-column>
      <el-table-column label="内容摘要" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ contentPreview(row) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'DISABLED' ? 'info' : 'success'" size="small">
            {{ row.status === 'DISABLED' ? '停用' : '启用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="preview(row)">预览</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑模板' : '新增模板'" width="640px" @closed="resetForm">
      <el-form :model="form" label-width="120px">
        <el-form-item label="事件类型" required>
          <el-input v-model="form.eventType" placeholder="例如：对账单已发送" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="短信模板">
          <el-input v-model="form.smsTemplate" type="textarea" :rows="3" placeholder="支持变量 {billNo}" />
        </el-form-item>
        <el-form-item label="邮件模板">
          <el-input v-model="form.emailTemplate" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="推送模板">
          <el-input v-model="form.pushTemplate" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="预览" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="事件">{{ previewRow?.eventType }}</el-descriptions-item>
        <el-descriptions-item label="短信">{{ fillVars(previewRow?.smsTemplate) }}</el-descriptions-item>
        <el-descriptions-item label="邮件">{{ fillVars(previewRow?.emailTemplate) }}</el-descriptions-item>
        <el-descriptions-item label="推送">{{ fillVars(previewRow?.pushTemplate) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNotificationTemplates, saveNotificationTemplates } from '@/api/system'

interface TemplateRow {
  id?: string
  eventType: string
  channels?: string
  smsTemplate?: string
  emailTemplate?: string
  pushTemplate?: string
  status?: string
}

const loading = ref(false)
const saving = ref(false)
const rows = ref<TemplateRow[]>([])

const editVisible = ref(false)
const form = reactive<TemplateRow>({
  eventType: '',
  smsTemplate: '',
  emailTemplate: '',
  pushTemplate: '',
  status: 'ENABLED',
})

const previewVisible = ref(false)
const previewRow = ref<TemplateRow | null>(null)

function unwrapData<T>(res: unknown): T | undefined {
  if (res && typeof res === 'object' && 'data' in res) {
    return (res as { data: T }).data
  }
  return res as T
}

function channelSummary(row: TemplateRow) {
  const parts: string[] = []
  if (row.smsTemplate?.trim()) parts.push('短信')
  if (row.emailTemplate?.trim()) parts.push('邮件')
  if (row.pushTemplate?.trim()) parts.push('推送')
  return parts.length ? parts.join('、') : '—'
}

function contentPreview(row: TemplateRow) {
  const t = row.smsTemplate || row.emailTemplate || row.pushTemplate || ''
  return t.length > 80 ? `${t.slice(0, 80)}…` : t || '—'
}

function fillVars(text?: string) {
  if (!text) return '—'
  return text
    .replace(/\{billNo\}/g, 'R202504001')
    .replace(/\{buyerName\}/g, '示例买方有限公司')
}

async function load() {
  loading.value = true
  try {
    const res = await getNotificationTemplates()
    const data = unwrapData<{ templates?: TemplateRow[] }>(res)
    rows.value = data?.templates?.length ? [...data.templates] : []
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = undefined
  form.eventType = ''
  form.smsTemplate = ''
  form.emailTemplate = ''
  form.pushTemplate = ''
  form.status = 'ENABLED'
}

function openEdit(row?: TemplateRow) {
  resetForm()
  if (row) {
    Object.assign(form, row)
  }
  editVisible.value = true
}

function preview(row: TemplateRow) {
  previewRow.value = row
  previewVisible.value = true
}

async function remove(row: TemplateRow) {
  try {
    await ElMessageBox.confirm(`删除事件「${row.eventType}」的模板？`, '确认', { type: 'warning' })
    rows.value = rows.value.filter((r) => r !== row)
    await persist()
    ElMessage.success('已删除')
  } catch {
    /* */
  }
}

async function persist() {
  await saveNotificationTemplates({ templates: rows.value })
}

async function save() {
  if (!form.eventType.trim()) {
    ElMessage.warning('请填写事件类型')
    return
  }
  saving.value = true
  try {
    const entry: TemplateRow = {
      id: form.id,
      eventType: form.eventType.trim(),
      smsTemplate: form.smsTemplate,
      emailTemplate: form.emailTemplate,
      pushTemplate: form.pushTemplate,
      status: form.status || 'ENABLED',
    }
    const ch: string[] = []
    if (entry.smsTemplate?.trim()) ch.push('SMS')
    if (entry.emailTemplate?.trim()) ch.push('EMAIL')
    if (entry.pushTemplate?.trim()) ch.push('PUSH')
    entry.channels = ch.join(',')

    if (form.id) {
      const i = rows.value.findIndex((r) => r.id === form.id)
      if (i >= 0) rows.value[i] = { ...entry }
    } else {
      entry.id = `t_${Date.now()}`
      rows.value.push({ ...entry })
    }
    await persist()
    ElMessage.success('已保存')
    editVisible.value = false
  } catch {
    /* */
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-desc {
  color: #999;
  margin-bottom: 16px;
}
.toolbar {
  margin-bottom: 16px;
}
</style>
