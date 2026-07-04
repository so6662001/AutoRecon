<template>
  <div class="page-container">
    <h2>超时确认规则</h2>
    <p class="page-desc">设定对账单超时自动确认的时效，不同场景可分别配置</p>

    <el-card shadow="hover" class="section-card">
      <template #header>全局默认配置</template>
      <el-form :model="globalConfig" label-width="160px">
        <el-form-item label="无差异确认时效">
          <el-input-number v-model="globalConfig.noDiffDays" :min="1" :max="7" /> 天
          <span class="hint">平台范围: 1~7天</span>
        </el-form-item>
        <el-form-item label="品规变更确认时效">
          <el-input-number v-model="globalConfig.specChangeHours" :min="12" :max="72" /> 小时
        </el-form-item>
        <el-form-item label="数量超差确认时效">
          <el-input-number v-model="globalConfig.overDiffHours" :min="24" :max="72" /> 小时
        </el-form-item>
        <el-form-item label="结算单确认时效">
          <el-input-number v-model="globalConfig.settleDays" :min="1" :max="7" /> 天
        </el-form-item>
        <el-form-item label="到期前提醒">
          <el-input-number v-model="globalConfig.reminderHours" :min="1" :max="48" /> 小时前
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingGlobal" @click="saveGlobal">保存全局配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header-row">
          <span>按客户单独配置</span>
          <el-button type="primary" size="small" @click="openAdd">新增客户配置</el-button>
        </div>
      </template>
      <el-table :data="customerConfigs" stripe v-loading="loading">
        <el-table-column prop="buyerName" label="客户" min-width="140" />
        <el-table-column prop="noDiffDays" label="无差异(天)" width="110" />
        <el-table-column prop="specChangeHours" label="品规变更(小时)" width="130" />
        <el-table-column prop="overDiffHours" label="超差(小时)" width="110" />
        <el-table-column prop="settleDays" label="结算(天)" width="100" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editCustomer(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteCustomer(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑客户配置' : '新增客户配置'" width="520px" @closed="resetForm">
      <el-form :model="form" label-width="140px">
        <el-form-item label="客户名称" required>
          <el-input v-model="form.buyerName" placeholder="客户名称" />
        </el-form-item>
        <el-form-item label="买方企业ID">
          <el-input-number v-model="form.buyerId" :min="1" :controls="false" class="w-full" placeholder="可选" />
        </el-form-item>
        <el-form-item label="无差异(天)">
          <el-input-number v-model="form.noDiffDays" :min="1" :max="7" />
        </el-form-item>
        <el-form-item label="品规变更(小时)">
          <el-input-number v-model="form.specChangeHours" :min="12" :max="72" />
        </el-form-item>
        <el-form-item label="超差(小时)">
          <el-input-number v-model="form.overDiffHours" :min="24" :max="72" />
        </el-form-item>
        <el-form-item label="结算(天)">
          <el-input-number v-model="form.settleDays" :min="1" :max="7" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCustomer">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTimeoutConfig, saveTimeoutConfig } from '@/api/system'

interface CustomerRow {
  id?: string
  buyerId?: number
  buyerName: string
  noDiffDays: number
  specChangeHours: number
  overDiffHours: number
  settleDays: number
}

const loading = ref(false)
const savingGlobal = ref(false)
const globalConfig = reactive({
  noDiffDays: 3,
  specChangeHours: 24,
  overDiffHours: 48,
  settleDays: 3,
  reminderHours: 6,
})
const customerConfigs = ref<CustomerRow[]>([])

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<CustomerRow>({
  buyerName: '',
  buyerId: undefined,
  noDiffDays: 3,
  specChangeHours: 24,
  overDiffHours: 48,
  settleDays: 3,
})

function unwrapData<T>(res: unknown): T | undefined {
  if (res && typeof res === 'object' && 'data' in res) {
    return (res as { data: T }).data
  }
  return res as T
}

async function load() {
  loading.value = true
  try {
    const raw = await getTimeoutConfig()
    const data = unwrapData<{
      global?: typeof globalConfig
      customers?: CustomerRow[]
    }>(raw)
    if (data?.global) {
      Object.assign(globalConfig, data.global)
    }
    customerConfigs.value = Array.isArray(data?.customers) ? [...data.customers] : []
  } catch {
    customerConfigs.value = []
  } finally {
    loading.value = false
  }
}

async function saveGlobal() {
  savingGlobal.value = true
  try {
    await saveTimeoutConfig({
      global: { ...globalConfig },
      customers: customerConfigs.value,
    })
    ElMessage.success('全局配置已保存')
  } catch {
    // interceptor
  } finally {
    savingGlobal.value = false
  }
}

function openAdd() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function editCustomer(row: CustomerRow) {
  editingId.value = row.id ?? null
  form.buyerName = row.buyerName
  form.buyerId = row.buyerId
  form.noDiffDays = row.noDiffDays
  form.specChangeHours = row.specChangeHours
  form.overDiffHours = row.overDiffHours
  form.settleDays = row.settleDays
  dialogVisible.value = true
}

async function deleteCustomer(row: CustomerRow) {
  try {
    await ElMessageBox.confirm(`确定删除客户「${row.buyerName}」的配置？`, '确认', { type: 'warning' })
    customerConfigs.value = customerConfigs.value.filter((c) => c !== row)
    await saveTimeoutConfig({
      global: { ...globalConfig },
      customers: customerConfigs.value,
    })
    ElMessage.success('已删除')
  } catch {
    /* cancel */
  }
}

function resetForm() {
  form.buyerName = ''
  form.buyerId = undefined
  form.noDiffDays = globalConfig.noDiffDays
  form.specChangeHours = globalConfig.specChangeHours
  form.overDiffHours = globalConfig.overDiffHours
  form.settleDays = globalConfig.settleDays
}

async function submitCustomer() {
  if (!form.buyerName.trim()) {
    ElMessage.warning('请填写客户名称')
    return
  }
  const row: CustomerRow = {
    id: editingId.value ?? undefined,
    buyerName: form.buyerName.trim(),
    buyerId: form.buyerId,
    noDiffDays: form.noDiffDays,
    specChangeHours: form.specChangeHours,
    overDiffHours: form.overDiffHours,
    settleDays: form.settleDays,
  }
  if (editingId.value) {
    const i = customerConfigs.value.findIndex((c) => c.id === editingId.value)
    if (i >= 0) customerConfigs.value[i] = { ...customerConfigs.value[i], ...row, id: editingId.value }
  } else {
    customerConfigs.value.push(row)
  }
  try {
    await saveTimeoutConfig({
      global: { ...globalConfig },
      customers: customerConfigs.value,
    })
    ElMessage.success(editingId.value ? '已更新' : '已新增')
    dialogVisible.value = false
  } catch {
    /* handled */
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.page-desc {
  color: #999;
  margin-bottom: 20px;
}
.section-card {
  margin-bottom: 20px;
}
.hint {
  color: #999;
  margin-left: 12px;
}
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.w-full {
  width: 100%;
}
</style>
