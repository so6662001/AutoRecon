<template>
  <div class="page-container timeout-config">
    <div class="config-header">
      <el-select v-model="selectedBuyerId" placeholder="全部客户(平台范围)" clearable style="width: 220px">
        <el-option label="平台默认" value="" />
        <el-option
          v-for="b in buyerOptions"
          :key="b.id"
          :label="b.name"
          :value="b.id"
        />
      </el-select>
      <span class="hint">选择客户可查看/配置该客户的专属超时</span>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="scene" label="场景" width="140" />
      <el-table-column prop="scope" label="平台范围" width="120">
        <template #default="{ row }">{{ row.buyerId ? '客户专属' : '平台' }}</template>
      </el-table-column>
      <el-table-column prop="value" label="当前配置值" width="120">
        <template #default="{ row }">
          {{ row.value }} {{ row.unit === 'hour' ? '小时' : '天' }}
        </template>
      </el-table-column>
      <el-table-column prop="remindBefore" label="到期前提醒" width="120">
        <template #default="{ row }">{{ row.remindBefore ?? '-' }} 小时</template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showEditDialog"
      title="编辑超时配置"
      width="450px"
      :close-on-click-modal="false"
      @close="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="120px">
        <el-form-item label="场景" prop="scene">
          <el-input v-model="editForm.scene" disabled />
        </el-form-item>
        <el-form-item label="超时值" prop="value">
          <el-input-number v-model="editForm.value" :min="1" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-radio-group v-model="editForm.unit">
            <el-radio value="hour">小时</el-radio>
            <el-radio value="day">天</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="到期前提醒(小时)" prop="remindBefore">
          <el-input-number v-model="editForm.remindBefore" :min="0" placeholder="小时" />
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
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getTimeoutConfig, saveTimeoutConfig, queryBuyers } from '@/api/evidence'

const loading = ref(false)
const submitting = ref(false)
const showEditDialog = ref(false)
const selectedBuyerId = ref<number | string | undefined>(undefined)
const editFormRef = ref<FormInstance>()
const tableData = ref<any[]>([])
const buyerOptions = ref<Array<{ id: number; name: string }>>([])

const SCENES = [
  { key: 'no_diff', label: '无差异' },
  { key: 'spec_change', label: '品规变更' },
  { key: 'qty_over', label: '数量超差' },
  { key: 'settlement_confirm', label: '结算确认' },
]

const editForm = reactive({
  id: undefined as number | undefined,
  scene: '',
  sceneKey: '',
  value: 24,
  unit: 'hour' as 'hour' | 'day',
  remindBefore: 2,
  buyerId: undefined as number | undefined,
})

const editRules: FormRules = {
  value: [{ required: true, message: '请输入超时值', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {}
    if (selectedBuyerId.value !== undefined && selectedBuyerId.value !== '') params.buyerId = selectedBuyerId.value as number
    const res = (await getTimeoutConfig(params)) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data ?? []
    if (Array.isArray(list) && list.length > 0) {
      tableData.value = list.map((r: any) => ({
        id: r.id,
        scene: r.sceneLabel ?? r.scene ?? '-',
        sceneKey: r.sceneKey ?? r.scene_key,
        value: r.value ?? 24,
        unit: r.unit ?? 'hour',
        remindBefore: r.remindBefore ?? r.remind_before,
        buyerId: r.buyerId ?? r.buyer_id,
      }))
    } else {
      tableData.value = SCENES.map((s) => ({
        id: undefined,
        scene: s.label,
        sceneKey: s.key,
        value: 24,
        unit: 'hour',
        remindBefore: 2,
        buyerId: selectedBuyerId.value ?? undefined,
      }))
    }
  } catch {
    tableData.value = SCENES.map((s) => ({
      id: undefined,
      scene: s.label,
      sceneKey: s.key,
      value: 24,
      unit: 'hour',
      remindBefore: 2,
      buyerId: selectedBuyerId.value ?? undefined,
    }))
  } finally {
    loading.value = false
  }
}

async function loadBuyers() {
  try {
    const res = (await queryBuyers({})) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data ?? []
    buyerOptions.value = Array.isArray(list)
      ? list.map((b: any) => ({ id: b.id, name: b.name ?? b.buyerName ?? '-' }))
      : []
  } catch {
    buyerOptions.value = []
  }
}

function openEditDialog(row: any) {
  editForm.id = row.id
  editForm.scene = row.scene
  editForm.sceneKey = row.sceneKey
  editForm.value = row.value
  editForm.unit = row.unit ?? 'hour'
  editForm.remindBefore = row.remindBefore ?? 2
  editForm.buyerId = row.buyerId
  showEditDialog.value = true
}

function resetEditForm() {
  editForm.id = undefined
  editForm.scene = ''
  editForm.sceneKey = ''
  editForm.value = 24
  editForm.unit = 'hour'
  editForm.remindBefore = 2
  editForm.buyerId = undefined
  editFormRef.value?.resetFields()
}

async function submitEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await saveTimeoutConfig({
        id: editForm.id,
        sceneKey: editForm.sceneKey,
        value: editForm.value,
        unit: editForm.unit,
        remindBefore: editForm.remindBefore,
        buyerId: editForm.buyerId,
      })
      ElMessage.success('保存成功')
      showEditDialog.value = false
      loadData()
    } catch {
      // error handled
    } finally {
      submitting.value = false
    }
  })
}

watch(selectedBuyerId, () => loadData())

onMounted(() => {
  loadBuyers()
  loadData()
})
</script>

<style lang="scss" scoped>
.timeout-config {
  .config-header {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 12px;

    .hint {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }
}
</style>
