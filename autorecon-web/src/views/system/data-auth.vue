<template>
  <div class="page-container">
    <h2>企业数据授权管理</h2>
    <p style="color:#999;margin-bottom:20px">管理企业业务数据的使用授权范围。企业管理员可代表企业进行授权变更。</p>

    <!-- Not initialized alert -->
    <el-alert v-if="needsInit" type="warning" :closable="false" style="margin-bottom:20px">
      <template #title>您的企业尚未完成数据授权初始化</template>
      <template #default>
        <p>首次使用需要设置数据授权范围，确定平台可以使用您的企业数据的具体方式。</p>
        <el-button type="primary" size="small" @click="handleInitialize" style="margin-top:8px">立即初始化</el-button>
      </template>
    </el-alert>

    <!-- Authorization cards -->
    <el-row :gutter="16" v-if="!needsInit">
      <el-col :span="8" v-for="auth in authorizations" :key="auth.type">
        <el-card shadow="hover" style="margin-bottom:16px" :class="{ 'auth-disabled': !auth.authorized }">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>
                <el-icon v-if="auth.authorized" style="color:#67c23a"><Select /></el-icon>
                <el-icon v-else style="color:#909399"><CloseBold /></el-icon>
                {{ auth.typeName }}
              </span>
              <el-switch 
                v-model="auth.authorized" 
                :disabled="!auth.revocable"
                @change="(val: string | number | boolean) => handleToggle(auth, val)"
                size="small"
              />
            </div>
          </template>
          <p style="font-size:13px;color:#666;min-height:48px">{{ getDescription(auth.type) }}</p>
          <div style="font-size:12px;color:#999;margin-top:8px">
            <span v-if="auth.authorizedAt">授权时间: {{ auth.authorizedAt }}</span>
            <el-tag v-if="!auth.revocable" size="small" type="info" style="margin-left:8px">必选</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Pending change notifications -->
    <el-card v-if="pendingChanges.length > 0" shadow="hover" style="margin-top:20px">
      <template #header>
        <span><el-icon><Bell /></el-icon> 待处理的授权变更通知 ({{ pendingChanges.length }})</span>
      </template>
      <el-table :data="pendingChanges" stripe>
        <el-table-column prop="changeSummary" label="变更说明" />
        <el-table-column prop="newAuthorizationTypes" label="涉及授权项" />
        <el-table-column prop="notifiedAt" label="通知时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleAcceptChange(row)">同意</el-button>
            <el-button type="danger" size="small" plain @click="handleRejectChange(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Info section -->
    <el-card shadow="never" style="margin-top:20px;background:#f5f7fa">
      <h4 style="margin-bottom:12px">数据使用说明</h4>
      <ul style="color:#666;line-height:2;font-size:13px">
        <li><strong>基础服务</strong>: 提供对账/提货/催收等核心功能所必需，不可关闭</li>
        <li><strong>AI模型训练</strong>: 使用脱敏后的交易行为数据训练平台智能模型(异议预测/容差学习/催收策略等)</li>
        <li><strong>行业指数</strong>: 脱敏聚合后生成行业基准报告(回款天数/对账效率/价格趋势等)</li>
        <li><strong>信用评估</strong>: 基于付款/逾期/异议行为的企业信用评分，可用于供应链金融风控</li>
        <li><strong>商业洞察</strong>: 脱敏聚合后的市场分析/智能推荐/库存分析等</li>
        <li><strong>异常监测</strong>: 识别异常交易模式/防欺诈，建议保持开启</li>
      </ul>
      <p style="color:#999;font-size:12px;margin-top:12px">
        所有可选授权项使用的数据均经过脱敏处理，不可反向识别具体企业。
        撤回授权后平台将在30天内停止对应用途。
        详见 <a href="/docs/privacy-policy" target="_blank">隐私保护政策</a>
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Select, CloseBold, Bell } from '@element-plus/icons-vue'
import { getDataAuthStatus, initializeDataAuth, updateDataAuth, revokeDataAuth, respondToAuthChange } from '@/api/system'

interface AuthDetail {
  type: string
  typeName: string
  authorized: boolean
  authorizedAt: string
  revocable: boolean
}

const needsInit = ref(false)
const authorizations = ref<AuthDetail[]>([])
const pendingChanges = ref<Record<string, unknown>[]>([])

const descriptions: Record<string, string> = {
  BASIC_SERVICE: '提供对账、提货、催收等核心平台功能所必需的数据处理',
  AI_TRAINING: '使用脱敏后的交易行为数据训练异议预测、容差学习、催收策略等AI模型',
  INDUSTRY_INDEX: '脱敏聚合后生成行业回款天数指数、对账效率基准、价格趋势等报告',
  CREDIT_ASSESSMENT: '基于付款行为和交易记录的企业信用评分，可用于供应链金融审核',
  BUSINESS_INSIGHT: '脱敏聚合后用于市场容量估算、交易对手推荐、库存周转分析',
  ANOMALY_DETECTION: '识别异常交易模式和可疑行为，保护平台交易安全',
}

function getDescription(type: string) { return descriptions[type] || '' }

async function loadStatus() {
  try {
    const res = await getDataAuthStatus() as { data?: { authorizations?: AuthDetail[]; pendingChanges?: Record<string, unknown>[] } } & { authorizations?: AuthDetail[]; pendingChanges?: Record<string, unknown>[] }
    const data = res?.data ?? res
    if (!data || !data.authorizations || data.authorizations.length === 0) {
      needsInit.value = true
      return
    }
    needsInit.value = false
    authorizations.value = data.authorizations
    pendingChanges.value = data.pendingChanges || []
  } catch {
    needsInit.value = true
  }
}

async function handleInitialize() {
  try {
    await initializeDataAuth()
    ElMessage.success('数据授权初始化完成')
    await loadStatus()
  } catch { ElMessage.error('初始化失败') }
}

async function handleToggle(auth: AuthDetail, val: string | number | boolean) {
  if (!val) {
    // Revoking
    try {
      await ElMessageBox.confirm(`确定撤回"${auth.typeName}"的数据授权？撤回后平台将在30天内停止对应的数据使用。`, '撤回授权', { type: 'warning' })
      await revokeDataAuth(auth.type)
      ElMessage.success('已撤回授权')
      await loadStatus()
    } catch {
      auth.authorized = true // Revert toggle
    }
  } else {
    // Re-authorizing
    try {
      await updateDataAuth({ authorizations: [{ authorizationType: auth.type, authorized: true }] })
      ElMessage.success('已授权')
      await loadStatus()
    } catch {
      auth.authorized = false
      ElMessage.error('授权失败')
    }
  }
}

async function handleAcceptChange(row: { id: number }) {
  try {
    await respondToAuthChange(row.id, { accept: true })
    ElMessage.success('已同意变更')
    await loadStatus()
  } catch { ElMessage.error('操作失败') }
}

async function handleRejectChange(row: { id: number }) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝变更', { inputPlaceholder: '原因...' })
    try {
      await respondToAuthChange(row.id, { accept: false, detail: value })
      ElMessage.success('已拒绝变更')
      await loadStatus()
    } catch { ElMessage.error('操作失败') }
  } catch {
    // user cancelled prompt
  }
}

onMounted(loadStatus)
</script>

<style scoped>
.auth-disabled { opacity: 0.6; }
</style>
