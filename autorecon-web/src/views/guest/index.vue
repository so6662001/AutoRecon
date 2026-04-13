<template>
  <div class="guest-page guest-container">
    <div class="security-banner">
      <el-icon><Lock /></el-icon>
      本页面由 AutoRecon 提供技术支持，数据加密传输
    </div>

    <div class="guest-content">
      <div v-if="!verified" class="verify-card guest-card">
        <h2 class="card-title">手机验证</h2>
        <p class="card-desc">请输入手机号并完成验证以查看对账单</p>
        <el-form :model="verifyForm" class="verify-form">
          <el-form-item>
            <el-input
              v-model="verifyForm.phone"
              placeholder="请输入手机号"
              size="large"
              maxlength="11"
              clearable
            >
              <template #prefix>
                <el-icon><Iphone /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="verifyForm.code"
              placeholder="请输入验证码"
              size="large"
              maxlength="6"
              clearable
              class="code-input"
            >
              <template #prefix>
                <el-icon><Message /></el-icon>
              </template>
              <template #append>
                <el-button
                  :disabled="countdown > 0 || !verifyForm.phone"
                  @click="sendCode"
                >
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="verifying"
              :disabled="!verifyForm.phone || !verifyForm.code"
              class="verify-btn"
              data-track-event="guest_verify" data-track-category="guest_operation"
              @click="handleVerify"
            >
              验证
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <template v-else>
        <div v-if="confirmed" class="success-card guest-card">
          <el-icon class="success-icon"><CircleCheck /></el-icon>
          <h2>确认成功</h2>
          <p>感谢您的确认，对账单已处理完成。</p>
          <p class="register-hint">注册后可查看所有历史对账单</p>
          <el-button type="primary" @click="goRegister">立即注册</el-button>
        </div>

        <div v-else class="bill-card guest-card">
          <h2 class="card-title">对账单</h2>
          <div v-loading="loading" class="bill-summary">
            <div class="summary-row">
              <span class="label">卖方：</span>
              <span>{{ bill.sellerName }}</span>
            </div>
            <div class="summary-row">
              <span class="label">买方：</span>
              <span>{{ bill.buyerName }}</span>
            </div>
            <div class="summary-row">
              <span class="label">对账周期：</span>
              <span>{{ bill.period }}</span>
            </div>
            <div class="summary-row">
              <span class="label">总金额：</span>
              <span class="amount">¥{{ Number(bill.totalAmount || 0).toLocaleString() }}</span>
            </div>
            <div class="summary-row">
              <span class="label">已付金额：</span>
              <span>¥{{ Number(bill.paidAmount || 0).toLocaleString() }}</span>
            </div>
            <div class="summary-row">
              <span class="label">余额：</span>
              <span class="amount">¥{{ Number(bill.balance || 0).toLocaleString() }}</span>
            </div>
            <div class="bill-actions">
              <el-button @click="handleDownload">
                <el-icon><Download /></el-icon>
                下载 PDF
              </el-button>
              <el-button text @click="showDetail = !showDetail">
                {{ showDetail ? '收起明细' : '查看明细' }}
              </el-button>
            </div>
            <el-collapse-transition>
              <div v-show="showDetail" class="bill-table">
                <el-table :data="bill.items || []" stripe class="detail-table">
                  <el-table-column prop="productName" label="品名" min-width="120" />
                  <el-table-column prop="spec" label="规格" min-width="100" />
                  <el-table-column prop="quantity" label="数量" width="80" align="right" />
                  <el-table-column prop="amount" label="金额" width="120" align="right">
                    <template #default="{ row }">
                      ¥{{ Number(row.amount || 0).toLocaleString() }}
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-collapse-transition>
          </div>

          <div v-if="!disputeMode" class="action-buttons">
            <el-button type="success" size="large" class="confirm-btn" :loading="confirming" data-track-event="guest_confirm" data-track-category="guest_operation" @click="handleConfirm">
              <el-icon><CircleCheck /></el-icon>
              确认无异议
            </el-button>
            <el-button type="danger" size="large" plain data-track-event="guest_dispute" data-track-category="guest_operation" @click="disputeMode = true">
              <el-icon><CircleClose /></el-icon>
              我有异议
            </el-button>
          </div>

          <div v-else class="dispute-section">
            <el-input
              v-model="disputeMessage"
              type="textarea"
              :rows="4"
              placeholder="请描述您的异议..."
            />
            <div class="dispute-actions">
              <el-button @click="disputeMode = false">取消</el-button>
              <el-button type="danger" :loading="submitting" :disabled="!disputeMessage.trim()" @click="handleDispute">
                提交异议
              </el-button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <footer class="guest-footer">
      <div class="trust-badges footer-badges">
        <span><el-icon><Lock /></el-icon> 安全加密</span>
        <span><el-icon><Document /></el-icon> 合规存证</span>
        <span><el-icon><Service /></el-icon> 7×24 支持</span>
      </div>
      <p class="footer-text">© AutoRecon 对账平台 · 数据加密传输</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Iphone, Message, CircleCheck, CircleClose, Download, Document, Service } from '@element-plus/icons-vue'
import { guestViewBill, guestConfirm, guestDownloadPdf, verifyPhone, sendVerifyCode } from '@/api/recon'

interface BillItem {
  productName?: string
  spec?: string
  quantity?: number
  amount?: number
}

interface BillData {
  sellerName?: string
  buyerName?: string
  period?: string
  totalAmount?: number
  paidAmount?: number
  balance?: number
  pdfUrl?: string
  items?: BillItem[]
}

const route = useRoute()
const router = useRouter()
const token = computed(() => String(route.params.token))

const verified = ref(false)
const loading = ref(false)
const confirming = ref(false)
const submitting = ref(false)
const verifying = ref(false)
const confirmed = ref(false)
const showDetail = ref(false)
const disputeMode = ref(false)
const countdown = ref(0)

const bill = ref<BillData>({})
const disputeMessage = ref('')

const verifyForm = reactive({
  phone: '',
  code: '',
})

const VERIFY_KEY = 'guest_verified_tokens'

function isVerifiedForToken(): boolean {
  try {
    const stored = sessionStorage.getItem(VERIFY_KEY)
    if (!stored) return false
    const tokens = JSON.parse(stored) as string[]
    return Array.isArray(tokens) && tokens.includes(token.value)
  } catch {
    return false
  }
}

function setVerifiedForToken() {
  try {
    const stored = sessionStorage.getItem(VERIFY_KEY)
    const tokens = stored ? JSON.parse(stored) as string[] : []
    if (!tokens.includes(token.value)) {
      tokens.push(token.value)
      sessionStorage.setItem(VERIFY_KEY, JSON.stringify(tokens))
    }
  } catch {
    // ignore
  }
}

async function sendCode() {
  if (!verifyForm.phone || verifyForm.phone.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    await sendVerifyCode({ phone: verifyForm.phone })
    ElMessage.success('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch {
    ElMessage.error('发送失败')
  }
}

async function handleVerify() {
  if (!verifyForm.phone || !verifyForm.code) {
    ElMessage.warning('请填写手机号和验证码')
    return
  }
  verifying.value = true
  try {
    await verifyPhone({ phone: verifyForm.phone, code: verifyForm.code })
    verified.value = true
    setVerifiedForToken()
    await fetchBill()
  } catch {
    ElMessage.error('验证失败')
  } finally {
    verifying.value = false
  }
}

async function fetchBill() {
  loading.value = true
  try {
    const res = await guestViewBill(token.value) as BillData
    bill.value = res ?? {}
  } catch {
    bill.value = {}
  } finally {
    loading.value = false
  }
}

async function handleConfirm() {
  confirming.value = true
  try {
    await guestConfirm(token.value, { action: 'confirm' })
    confirmed.value = true
    ElMessage.success('确认成功')
  } catch {
    ElMessage.error('确认失败')
  } finally {
    confirming.value = false
  }
}

async function handleDispute() {
  const msg = disputeMessage.value.trim()
  if (!msg) return
  submitting.value = true
  try {
    await guestConfirm(token.value, { action: 'dispute', message: msg })
    ElMessage.success('异议已提交')
    disputeMode.value = false
    disputeMessage.value = ''
  } catch {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

function sanitizeFilename(name: string): string {
  return name.replace(/[<>:"/\\|?*\x00-\x1F]/g, '_').substring(0, 100)
}

async function handleDownload() {
  try {
    const blob = await guestDownloadPdf(token.value)
    if (blob && blob instanceof Blob) {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `对账单-${sanitizeFilename(bill.value.period ?? 'download')}.pdf`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('下载成功')
    } else {
      throw new Error('Invalid response')
    }
  } catch {
    if (bill.value.pdfUrl) {
      window.open(bill.value.pdfUrl, '_blank')
    } else {
      ElMessage.error('下载失败，请稍后重试')
    }
  }
}

function goRegister() {
  router.push({ path: '/login', query: { redirect: '/dashboard' } })
}

onMounted(() => {
  if (isVerifiedForToken()) {
    verified.value = true
    fetchBill()
  }
})
</script>

<style lang="scss" scoped>
.guest-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #e8f4fc 0%, #f0f4f8 100%);
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.security-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
  font-size: 13px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.guest-content {
  flex: 1;
  max-width: 520px;
  width: 100%;
  margin: 0 auto;
}

.verify-card,
.bill-card,
.success-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.card-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.card-desc {
  margin: 0 0 28px;
  font-size: 14px;
  color: #909399;
}

.verify-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
}

.verify-btn {
  width: 100%;
}

.bill-summary {
  .summary-row {
    margin-bottom: 12px;
    font-size: 15px;

    .label {
      color: #909399;
      margin-right: 8px;
    }

    .amount {
      font-weight: 600;
      color: #303133;
    }
  }

  .bill-actions {
    margin: 20px 0;
    display: flex;
    gap: 12px;
  }

  .detail-table {
    margin-top: 16px;
  }
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 32px;
}

.confirm-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.dispute-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #ebeef5;

  .dispute-actions {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

.success-card {
  text-align: center;
  padding: 60px 40px;

  .success-icon {
    font-size: 64px;
    color: #67c23a;
    margin-bottom: 20px;
  }

  h2 {
    margin: 0 0 12px;
    font-size: 24px;
    color: #303133;
  }

  p {
    margin: 0 0 8px;
    color: #606266;
  }

  .register-hint {
    margin: 24px 0 !important;
    color: #409eff;
    font-size: 14px;
  }
}

.guest-footer {
  margin-top: 40px;
  padding-top: 24px;
  text-align: center;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.trust-badges {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;

  span {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.footer-text {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

@media (max-width: 640px) {
  .guest-container {
    padding: 12px;
  }

  .guest-card {
    padding: 16px;
    border-radius: 12px;
  }

  .security-banner {
    font-size: 14px;
  }

  .footer-text {
    font-size: 13px;
  }

  .footer-badges span {
    font-size: 12px;
  }

  .bill-table {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  :deep(.el-table) {
    font-size: 13px;
  }

  :deep(.el-button) {
    min-height: 44px;
  }

  .code-input :deep(.el-input) {
    font-size: 16px;
  }

  .trust-badges {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
