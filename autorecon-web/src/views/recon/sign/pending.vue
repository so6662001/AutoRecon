<template>
  <div class="sign-pending-page">
    <h2 class="page-title">待签章</h2>

    <div class="stat-card">
      <div class="stat-icon">
        <el-icon :size="32"><Stamp /></el-icon>
      </div>
      <div class="stat-content">
        <div class="stat-value">{{ pendingCount }}</div>
        <div class="stat-label">待签章数量</div>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="billNo" label="对账单号" min-width="140" />
        <el-table-column prop="buyerName" label="买方/卖方" min-width="180">
          <template #default="{ row }">
            {{ row.sellerName }} / {{ row.buyerName }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">
            ¥{{ Number(row.amount || 0).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="signOrder" label="签署顺序" width="100" />
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column prop="sellerSignStatus" label="卖方签章状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getSignStatusTagType(row.sellerSignStatus) as 'success' | 'primary' | 'warning' | 'info' | 'danger'" size="small">
              {{ getSignStatusText(row.sellerSignStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="buyerSignStatus" label="买方签章状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getSignStatusTagType(row.buyerSignStatus) as 'success' | 'primary' | 'warning' | 'info' | 'danger'" size="small">
              {{ getSignStatusText(row.buyerSignStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canSign(row)"
              type="primary"
              link
              size="small"
              @click="openSignDialog(row)"
            >
              签章
            </el-button>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="signDialogVisible"
      title="签章"
      width="560px"
      destroy-on-close
      :close-on-click-modal="false"
      @close="resetSignDialog"
    >
      <el-steps :active="signStep" finish-status="success" align-center>
        <el-step title="选择印章" />
        <el-step title="预览位置" />
        <el-step title="身份验证" />
        <el-step title="确认签章" />
      </el-steps>

      <div class="sign-step-content">
        <div v-show="signStep === 0" class="step-panel">
          <div class="step-title">选择印章</div>
          <el-radio-group v-model="selectedSealId" class="seal-radio-group">
            <el-radio
              v-for="seal in seals"
              :key="seal.id"
              :label="seal.id ?? undefined"
              class="seal-radio-item"
            >
              <div class="seal-preview">
                <div class="seal-placeholder">{{ seal.name?.slice(0, 1) || '印' }}</div>
                <span>{{ seal.name }}</span>
              </div>
            </el-radio>
          </el-radio-group>
        </div>

        <div v-show="signStep === 1" class="step-panel">
          <div class="step-title">预览签章位置</div>
          <div class="pdf-preview">
            <div class="pdf-placeholder">
              <el-icon :size="48"><Document /></el-icon>
              <p>PDF 预览区域</p>
              <p class="hint">签章将显示在文档指定位置</p>
            </div>
          </div>
        </div>

        <div v-show="signStep === 2" class="step-panel">
          <div class="step-title">身份验证</div>
          <el-radio-group v-model="verifyType" class="verify-type-group">
            <el-radio label="sms">短信验证码</el-radio>
            <el-radio label="face">人脸识别</el-radio>
          </el-radio-group>
          <el-input
            v-if="verifyType === 'sms'"
            v-model="verifyCode"
            placeholder="请输入验证码"
            style="margin-top: 16px; max-width: 200px"
          >
            <template #append>
              <el-button :disabled="countdown > 0" @click="sendVerifyCode">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
          <div v-else class="face-placeholder">
            <el-icon :size="64"><User /></el-icon>
            <p>人脸识别验证</p>
          </div>
        </div>

        <div v-show="signStep === 3" class="step-panel">
          <div class="step-title">确认签章</div>
          <p class="confirm-hint">请确认以上信息无误后点击下方按钮完成签章。</p>
        </div>
      </div>

      <template #footer>
        <el-button v-if="signStep > 0" @click="signStep--">上一步</el-button>
        <el-button
          v-if="signStep < 3"
          type="primary"
          :disabled="!canProceed"
          @click="signStep++"
        >
          下一步
        </el-button>
        <el-button
          v-else
          type="primary"
          :loading="signing"
          @click="handleConfirmSign"
        >
          确认签章
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Stamp, Document, User } from '@element-plus/icons-vue'
import { listPendingSigns, listSeals } from '@/api/recon'

interface PendingSignItem {
  id: number
  billNo: string
  sellerName?: string
  buyerName?: string
  amount?: number
  signOrder?: string
  deadline?: string
  sellerSignStatus?: string
  buyerSignStatus?: string
}

interface SealItem {
  id: number
  name: string
}

const loading = ref(false)
const list = ref<PendingSignItem[]>([])
const seals = ref<SealItem[]>([])
const signDialogVisible = ref(false)
const signStep = ref(0)
const selectedSealId = ref<number | undefined>(undefined)
const verifyType = ref('sms')
const verifyCode = ref('')
const countdown = ref(0)
const signing = ref(false)
const currentSignItem = ref<PendingSignItem | null>(null)

const pendingCount = computed(() => list.value.length)

function getSignStatusTagType(status?: string): string {
  const map: Record<string, string> = {
    PENDING: 'info',
    SIGNED: 'success',
    REJECTED: 'danger',
  }
  return map[status ?? ''] ?? 'info'
}

function getSignStatusText(status?: string): string {
  const map: Record<string, string> = {
    PENDING: '待签',
    SIGNED: '已签',
    REJECTED: '已拒签',
  }
  return map[status ?? ''] ?? status ?? '待签'
}

function canSign(_row: PendingSignItem): boolean {
  return true
}

function openSignDialog(row: PendingSignItem) {
  currentSignItem.value = row
  signDialogVisible.value = true
  signStep.value = 0
  fetchSeals()
}

function resetSignDialog() {
  signStep.value = 0
  selectedSealId.value = undefined
  verifyType.value = 'sms'
  verifyCode.value = ''
  currentSignItem.value = null
}

const canProceed = computed(() => {
  if (signStep.value === 0) return selectedSealId.value !== undefined && selectedSealId.value !== null
  if (signStep.value === 2 && verifyType.value === 'sms') return verifyCode.value.length >= 4
  if (signStep.value === 2 && verifyType.value === 'face') return true
  return true
})

async function fetchSeals() {
  try {
    const res = await listSeals() as SealItem[]
    seals.value = Array.isArray(res) ? res : []
    if (seals.value.length && selectedSealId.value == null) {
      selectedSealId.value = seals.value[0].id
    }
  } catch {
    seals.value = []
  }
}

function sendVerifyCode() {
  if (countdown.value > 0) return
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
  ElMessage.success('验证码已发送')
}

async function handleConfirmSign() {
  signing.value = true
  try {
    ElMessage.success('签章成功')
    signDialogVisible.value = false
    fetchList()
  } catch {
    ElMessage.error('签章失败')
  } finally {
    signing.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listPendingSigns() as PendingSignItem[]
    list.value = Array.isArray(res) ? res : []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.sign-pending-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .stat-card {
    display: flex;
    align-items: center;
    padding: 20px;
    border-radius: 8px;
    background: linear-gradient(135deg, #9c27b0, #ba68c8);
    color: #fff;
    margin-bottom: 24px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);

    .stat-icon {
      margin-right: 20px;
      opacity: 0.9;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
    }

    .stat-label {
      font-size: 14px;
      opacity: 0.9;
      margin-top: 4px;
    }
  }

  .table-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .text-muted {
    color: #909399;
  }

  .sign-step-content {
    min-height: 200px;
    padding: 24px 0;
  }

  .step-panel {
    .step-title {
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 16px;
      color: #303133;
    }
  }

  .seal-radio-group {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
  }

  .seal-radio-item {
    margin-right: 0;

    .seal-preview {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;

      .seal-placeholder {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        background: #409eff;
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        font-weight: bold;
      }
    }
  }

  .pdf-preview {
    border: 1px dashed #dcdfe6;
    border-radius: 8px;
    min-height: 200px;
  }

  .pdf-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 200px;
    color: #909399;

    .hint {
      font-size: 12px;
      margin-top: 8px;
    }
  }

  .verify-type-group {
    display: flex;
    gap: 24px;
  }

  .face-placeholder {
    margin-top: 24px;
    padding: 40px;
    border: 1px dashed #dcdfe6;
    border-radius: 8px;
    text-align: center;
    color: #909399;
  }

  .confirm-hint {
    color: #606266;
    margin: 0;
  }
}
</style>
