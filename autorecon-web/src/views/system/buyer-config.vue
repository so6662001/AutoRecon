<template>
  <div class="buyer-config-page">
    <h2 class="page-title">买方配置</h2>

    <el-card class="config-card">
      <template #header>数据提交模式</template>
      <el-radio-group v-model="form.submissionMode" class="mode-radio-group">
        <el-row :gutter="16">
          <el-col v-for="m in submissionModes" :key="m.value" :span="8">
            <el-radio :label="m.value" class="mode-radio">
              <div class="mode-card">
                <div class="mode-title">{{ m.label }}</div>
                <div class="mode-desc">{{ m.desc }}</div>
              </div>
            </el-radio>
          </el-col>
        </el-row>
      </el-radio-group>
    </el-card>

    <el-card class="config-card">
      <template #header>确认模式</template>
      <el-radio-group v-model="form.defaultConfirmMode">
        <el-radio label="LINE">逐行确认</el-radio>
        <el-radio label="BATCH">整单确认</el-radio>
      </el-radio-group>
    </el-card>

    <el-card class="config-card">
      <template #header>功能开关</template>
      <el-form label-width="120px">
        <el-form-item label="OCR启用">
          <el-switch v-model="form.ocrEnabled" />
        </el-form-item>
        <el-form-item label="移动端启用">
          <el-switch v-model="form.mobileEnabled" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="form.submissionMode === 'EXCEL'" class="config-card">
      <template #header>Excel字段映射</template>
      <el-table :data="excelMappings" border size="small">
        <el-table-column prop="platformField" label="平台字段" width="180" />
        <el-table-column prop="excelColumn" label="您的Excel列名">
          <template #default="{ row }">
            <el-input v-model="row.excelColumn" size="small" placeholder="输入Excel列名" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="action-bar">
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBuyerConfig, saveBuyerConfig } from '@/api/system'

const submissionModes = [
  { value: 'ERP', label: 'ERP自动比对', desc: '适合有完整ERP的企业' },
  { value: 'EXCEL', label: 'Excel上传比对', desc: '适合有电子台账的企业' },
  { value: 'ONLINE', label: '平台在线填写', desc: '适合小微企业' },
  { value: 'OCR', label: '拍照OCR识别', desc: '适合只有纸质单据的企业' },
  { value: 'MOBILE', label: '移动端快捷确认', desc: '适合不常用电脑的场景' },
]

const defaultExcelFields = [
  { platformField: '合同号', excelColumn: '合同号' },
  { platformField: '订单号', excelColumn: '订单号' },
  { platformField: '品名', excelColumn: '品名' },
  { platformField: '规格', excelColumn: '规格' },
  { platformField: '数量', excelColumn: '数量' },
  { platformField: '金额', excelColumn: '金额' },
]

const saving = ref(false)
const form = reactive({
  submissionMode: 'ERP' as string,
  defaultConfirmMode: 'LINE' as string,
  ocrEnabled: false,
  mobileEnabled: false,
})

const excelMappings = ref<{ platformField: string; excelColumn: string }[]>([])

async function fetchConfig() {
  try {
    const res = await getBuyerConfig() as Record<string, unknown>
    if (res) {
      form.submissionMode = (res.submissionMode as string) ?? 'ERP'
      form.defaultConfirmMode = (res.defaultConfirmMode as string) ?? 'LINE'
      form.ocrEnabled = Boolean(res.ocrEnabled)
      form.mobileEnabled = Boolean(res.mobileEnabled)
      const mapping = res.excelFieldMapping as Array<{ platformField: string; excelColumn: string }> | undefined
      excelMappings.value = mapping?.length ? [...mapping] : [...defaultExcelFields]
    } else {
      excelMappings.value = [...defaultExcelFields]
    }
  } catch {
    excelMappings.value = [...defaultExcelFields]
  }
}

async function handleSave() {
  saving.value = true
  try {
    const data: Record<string, unknown> = {
      submissionMode: form.submissionMode,
      defaultConfirmMode: form.defaultConfirmMode,
      ocrEnabled: form.ocrEnabled,
      mobileEnabled: form.mobileEnabled,
    }
    if (form.submissionMode === 'EXCEL') {
      data.excelFieldMapping = excelMappings.value
    }
    await saveBuyerConfig(data)
    ElMessage.success('保存成功')
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<style lang="scss" scoped>
.buyer-config-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .config-card {
    margin-bottom: 20px;
  }

  .mode-radio-group {
    width: 100%;
  }

  .mode-radio {
    width: 100%;
    height: auto;
    margin-right: 0;
    margin-bottom: 12px;

    :deep(.el-radio__label) {
      width: 100%;
    }
  }

  .mode-card {
    padding: 12px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    min-height: 70px;

    .mode-title {
      font-weight: 600;
      margin-bottom: 4px;
    }

    .mode-desc {
      font-size: 12px;
      color: #909399;
    }
  }

  .mode-radio:deep(.el-radio__input.is-checked + .el-radio__label) .mode-card {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  .action-bar {
    margin-top: 20px;
  }
}
</style>
