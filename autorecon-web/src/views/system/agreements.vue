<template>
  <div class="agreements-page">
    <h2 class="page-title">协议管理</h2>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="用户服务协议" name="1" />
      <el-tab-pane label="隐私保护政策" name="2" />
    </el-tabs>

    <div class="toolbar">
      <el-button type="primary" @click="openPublish">发布新版本</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
      <el-table-column prop="versionNo" label="版本号" width="120" />
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="effectiveDate" label="生效日期" width="120">
        <template #default="{ row }">{{ formatDate(row.effectiveDate) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.publishedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openView(row)">查看</el-button>
          <el-button
            v-if="row.status === 1"
            type="warning"
            link
            size="small"
            @click="handleDeprecate(row)"
          >
            废弃
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="publishVisible" title="发布新版本" width="640px" destroy-on-close @close="resetPublish">
      <el-form ref="publishFormRef" :model="publishForm" :rules="publishRules" label-width="120px">
        <el-form-item label="版本号" prop="versionNo">
          <el-input v-model="publishForm.versionNo" placeholder="例如 v2.0" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="publishForm.title" placeholder="协议标题" />
        </el-form-item>
        <el-form-item label="更新摘要" prop="summary">
          <el-input v-model="publishForm.summary" type="textarea" :rows="3" placeholder="简要说明本次更新" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="publishForm.effectiveDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="协议内容" prop="content">
          <el-input
            v-model="publishForm.content"
            type="textarea"
            :rows="12"
            placeholder="支持 Markdown 格式"
          />
        </el-form-item>
        <el-form-item label="要求重新确认">
          <el-switch v-model="publishForm.requireReconfirm" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishSubmitting" @click="submitPublish">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" :title="viewRow?.title || '协议详情'" width="720px" destroy-on-close>
      <div v-if="viewRow" class="view-meta">
        <span>版本：{{ viewRow.versionNo }}</span>
        <span>生效：{{ formatDate(viewRow.effectiveDate) }}</span>
      </div>
      <div v-loading="viewLoading" class="view-body">
        <div v-html="viewRendered" class="markdown-body"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  listAgreementVersions,
  publishAgreement,
  getAgreementVersion,
  deprecateAgreementVersion,
} from '@/api/system'

interface AgreementVersionRow {
  id: number
  agreementType: number
  versionNo: string
  title: string
  content?: string
  summary?: string
  effectiveDate?: string
  publishedAt?: string
  status?: number
}

const activeTab = ref('1')
const loading = ref(false)
const rawList = ref<AgreementVersionRow[]>([])

const agreementType = computed(() => Number(activeTab.value))

const tableData = computed(() => rawList.value)

function statusText(status: number | undefined) {
  if (status === 0) return '草稿'
  if (status === 1) return '已发布'
  if (status === 2) return '已废弃'
  return '-'
}

function statusTagType(status: number | undefined): 'success' | 'info' | 'warning' | 'danger' {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'warning'
}

function formatDate(v: string | undefined) {
  if (!v) return '-'
  return dayjs(v).format('YYYY-MM-DD')
}

function formatDateTime(v: string | undefined) {
  if (!v) return '-'
  return dayjs(v).format('YYYY-MM-DD HH:mm:ss')
}

function renderMd(content: string) {
  return content
    .replace(/^### (.*$)/gm, '<h4>$1</h4>')
    .replace(/^## (.*$)/gm, '<h3>$1</h3>')
    .replace(/^# (.*$)/gm, '<h2>$1</h2>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

async function fetchList() {
  loading.value = true
  try {
    const res = (await listAgreementVersions(agreementType.value)) as {
      data?: AgreementVersionRow[]
    }
    const list = res?.data
    rawList.value = Array.isArray(list) ? list : []
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  fetchList()
}

onMounted(() => {
  fetchList()
})

const publishVisible = ref(false)
const publishSubmitting = ref(false)
const publishFormRef = ref<FormInstance>()
const publishForm = ref({
  versionNo: '',
  title: '',
  summary: '',
  effectiveDate: '' as string,
  content: '',
  requireReconfirm: 1 as number,
})

const publishRules: FormRules = {
  versionNo: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
}

function openPublish() {
  publishForm.value = {
    versionNo: '',
    title: '',
    summary: '',
    effectiveDate: '',
    content: '',
    requireReconfirm: 1,
  }
  publishVisible.value = true
}

function resetPublish() {
  publishFormRef.value?.resetFields()
}

async function submitPublish() {
  await publishFormRef.value?.validate().catch(() => Promise.reject())
  publishSubmitting.value = true
  try {
    await publishAgreement({
      agreementType: agreementType.value,
      versionNo: publishForm.value.versionNo.trim(),
      title: publishForm.value.title.trim(),
      summary: publishForm.value.summary || undefined,
      content: publishForm.value.content || undefined,
      effectiveDate: publishForm.value.effectiveDate,
      requireReconfirm: publishForm.value.requireReconfirm,
    })
    ElMessage.success('发布成功')
    publishVisible.value = false
    await fetchList()
  } catch {
    // request layer shows error
  } finally {
    publishSubmitting.value = false
  }
}

const viewVisible = ref(false)
const viewLoading = ref(false)
const viewRow = ref<AgreementVersionRow | null>(null)
const viewRendered = ref('')

async function openView(row: AgreementVersionRow) {
  viewRow.value = row
  viewVisible.value = true
  viewLoading.value = true
  viewRendered.value = ''
  try {
    const res = (await getAgreementVersion(row.id)) as { data?: AgreementVersionRow }
    const data = res?.data
    const content = data?.content || ''
    viewRendered.value = renderMd(content)
    if (data) {
      viewRow.value = { ...row, ...data }
    }
  } finally {
    viewLoading.value = false
  }
}

async function handleDeprecate(row: AgreementVersionRow) {
  try {
    await ElMessageBox.confirm(`确定将版本「${row.versionNo}」标记为已废弃吗？`, '废弃确认', {
      type: 'warning',
    })
    await deprecateAgreementVersion(row.id)
    ElMessage.success('已废弃')
    await fetchList()
  } catch (e) {
    if (e !== 'cancel') {
      // error toast from request
    }
  }
}
</script>

<style lang="scss" scoped>
.agreements-page {
  .page-title {
    margin: 0 0 16px;
    font-size: 20px;
    font-weight: 600;
  }
}

.toolbar {
  margin: 16px 0;
}

.view-meta {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.view-body {
  min-height: 120px;
  max-height: 480px;
  overflow-y: auto;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.markdown-body {
  line-height: 1.8;
  font-size: 14px;
}
</style>
