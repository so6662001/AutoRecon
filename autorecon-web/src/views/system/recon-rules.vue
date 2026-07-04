<template>
  <div class="page-container">
    <h2>对账规则</h2>
    <p class="page-desc">配置比对容差与自动确认策略</p>

    <el-form :model="form" label-width="180px" v-loading="loading" class="rule-form">
      <el-card shadow="hover" class="section-card">
        <template #header>重量容差</template>
        <el-form-item label="百分比容差 (%)">
          <el-input-number
            v-model="form.weightTolerancePercent"
            :min="0"
            :max="100"
            :precision="2"
            :step="0.01"
          />
          <span class="hint">按卖方–买方维度在明细比对时生效</span>
        </el-form-item>
      </el-card>

      <el-card shadow="hover" class="section-card">
        <template #header>金额容差</template>
        <el-form-item label="绝对值容差 (元)">
          <el-input-number v-model="form.amountToleranceAbs" :min="0" :precision="2" :step="10" />
        </el-form-item>
      </el-card>

      <el-card shadow="hover" class="section-card">
        <template #header>日期容差</template>
        <el-form-item label="允许天数差">
          <el-input-number v-model="form.dateToleranceDays" :min="0" :max="30" />
          <span class="hint">交货/结算日期在 ±N 天内视为可匹配</span>
        </el-form-item>
      </el-card>

      <el-card shadow="hover" class="section-card">
        <template #header>自动确认</template>
        <el-form-item label="容差内自动确认">
          <el-switch v-model="form.autoConfirmWithinTolerance" />
        </el-form-item>
      </el-card>

      <el-card shadow="hover" class="section-card">
        <template #header>差额处理</template>
        <el-form-item label="默认扣减策略">
          <el-select v-model="form.defaultDeductionStrategy" placeholder="选择策略" style="width: 280px">
            <el-option label="按较小金额扣减" value="按较小金额扣减" />
            <el-option label="按卖方数据为准" value="按卖方数据为准" />
            <el-option label="按买方数据为准" value="按买方数据为准" />
            <el-option label="人工复核" value="人工复核" />
          </el-select>
        </el-form-item>
      </el-card>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getReconRulesConfig, saveReconRulesConfig } from '@/api/system'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  weightTolerancePercent: 0.5 as number,
  amountToleranceAbs: 100 as number,
  dateToleranceDays: 1 as number,
  autoConfirmWithinTolerance: false,
  defaultDeductionStrategy: '按较小金额扣减',
})

function unwrap<T>(res: unknown): T | undefined {
  if (res && typeof res === 'object' && 'data' in res) {
    return (res as { data: T }).data
  }
  return res as T
}

async function load() {
  loading.value = true
  try {
    const res = await getReconRulesConfig()
    const data = unwrap<Record<string, unknown>>(res)
    if (data) {
      if (data.weightTolerancePercent != null) {
        const w = Number(data.weightTolerancePercent)
        form.weightTolerancePercent = Number.isNaN(w) ? 0.5 : w
      }
      if (data.amountToleranceAbs != null) {
        const a = Number(data.amountToleranceAbs)
        form.amountToleranceAbs = Number.isNaN(a) ? 100 : a
      }
      if (data.dateToleranceDays != null) {
        const d = Number(data.dateToleranceDays)
        form.dateToleranceDays = Number.isNaN(d) ? 1 : d
      }
      if (typeof data.autoConfirmWithinTolerance === 'boolean') {
        form.autoConfirmWithinTolerance = data.autoConfirmWithinTolerance
      }
      if (typeof data.defaultDeductionStrategy === 'string') {
        form.defaultDeductionStrategy = data.defaultDeductionStrategy
      }
    }
  } catch {
    /* defaults */
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await saveReconRulesConfig({ ...form })
    ElMessage.success('已保存')
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
  margin-bottom: 20px;
}
.section-card {
  margin-bottom: 16px;
}
.hint {
  color: #909399;
  margin-left: 12px;
  font-size: 13px;
}
.rule-form {
  max-width: 800px;
}
</style>
