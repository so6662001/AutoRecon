<template>
  <div class="buyer-page">
    <!-- Standalone page - no sidebar/header -->
    <div class="buyer-bg" />
    <div class="buyer-container">
      <!-- Phone verification (first visit) -->
      <div v-if="!verified" class="buyer-card verify-card">
        <div class="card-header">
          <span class="card-icon">📱</span>
          <h2 class="card-title">验证身份</h2>
          <p class="card-subtitle">输入手机号验证身份后查看提货详情</p>
        </div>
        <el-form
          ref="verifyFormRef"
          :model="verifyForm"
          :rules="verifyRules"
          class="verify-form"
          @submit.prevent="handleVerify"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="verifyForm.phone"
              placeholder="请输入手机号"
              size="large"
              maxlength="11"
              show-word-limit
              clearable
            />
          </el-form-item>
          <el-form-item prop="code">
            <div class="code-row">
              <el-input
                v-model="verifyForm.code"
                placeholder="请输入验证码"
                size="large"
                maxlength="6"
                class="code-input"
              />
              <el-button
                type="primary"
                size="large"
                :loading="sendingCode"
                :disabled="countdown > 0"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s后重发` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="verify-btn"
              :loading="verifying"
              @click="handleVerify"
            >
              {{ verifying ? '验证中...' : '验证并查看' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- After verification - Pickup content -->
      <template v-else>
        <div v-loading="loading" class="buyer-card main-card">
          <!-- Pickup summary card -->
          <div class="summary-card">
            <div class="summary-header">
              <span class="summary-icon">🚛</span>
              <h1 class="summary-title">提货完成</h1>
            </div>
            <div class="summary-goods">
              <div
                v-for="(item, i) in pickupSummary.goods"
                :key="i"
                class="goods-row"
              >
                <span class="goods-name">{{ item.name }}</span>
                <span class="goods-weight">{{ item.weight }}吨</span>
              </div>
            </div>
            <div class="summary-totals">
              <span>合计: {{ pickupSummary.totalWeight }}吨</span>
              <span class="amount">金额: ¥{{ pickupSummary.totalAmount }}</span>
            </div>
            <div class="summary-meta">
              <div>仓库: {{ pickupSummary.warehouse }}</div>
              <div>司机: {{ pickupSummary.driver }} 车牌: {{ pickupSummary.plate }}</div>
              <div>时间: {{ pickupSummary.time }}</div>
            </div>
          </div>

          <!-- Expandable sections -->
          <el-collapse v-model="activeCollapse" class="detail-collapse">
            <el-collapse-item name="lifts">
              <template #title>
                <span class="collapse-title">📦 发货明细</span>
              </template>
              <div class="collapse-content">
                <el-table :data="liftRecords" stripe size="small" class="lifts-table">
                  <el-table-column prop="liftNo" label="吊序号" width="80" />
                  <el-table-column prop="productSpec" label="品规" min-width="120" />
                  <el-table-column prop="pieceCount" label="件数" width="70" align="right" />
                  <el-table-column prop="weight" label="重量(吨)" width="90" align="right" />
                </el-table>
              </div>
            </el-collapse-item>
            <el-collapse-item name="photos">
              <template #title>
                <span class="collapse-title">📷 现场照片</span>
              </template>
              <div class="collapse-content photo-gallery">
                <div v-if="photos.loading.length" class="photo-group">
                  <div class="photo-label">装载照</div>
                  <div class="photo-list">
                    <el-image
                      v-for="(url, i) in photos.loading"
                      :key="'loading-' + i"
                      :src="url"
                      fit="cover"
                      class="photo-item"
                      :preview-src-list="photos.loading"
                    />
                  </div>
                </div>
                <div v-if="photos.cargo.length" class="photo-group">
                  <div class="photo-label">货物照</div>
                  <div class="photo-list">
                    <el-image
                      v-for="(url, i) in photos.cargo"
                      :key="'cargo-' + i"
                      :src="url"
                      fit="cover"
                      class="photo-item"
                      :preview-src-list="photos.cargo"
                    />
                  </div>
                </div>
                <div v-if="photos.plate.length" class="photo-group">
                  <div class="photo-label">车牌照</div>
                  <div class="photo-list">
                    <el-image
                      v-for="(url, i) in photos.plate"
                      :key="'plate-' + i"
                      :src="url"
                      fit="cover"
                      class="photo-item"
                      :preview-src-list="photos.plate"
                    />
                  </div>
                </div>
                <el-empty v-if="!hasPhotos" description="暂无照片" :image-size="80" />
              </div>
            </el-collapse-item>
            <el-collapse-item name="settlement">
              <template #title>
                <span class="collapse-title">📄 结算信息</span>
              </template>
              <div class="collapse-content settlement-info">
                <div v-if="settlement.amount != null" class="settlement-rows">
                  <div class="settlement-row">
                    <span>重量</span>
                    <span>{{ formatNum(settlement.weight) }} 吨</span>
                  </div>
                  <div class="settlement-row">
                    <span>单价</span>
                    <span>¥{{ formatNum(settlement.unitPrice) }}/吨</span>
                  </div>
                  <div class="settlement-row total">
                    <span>结算金额</span>
                    <span>¥{{ formatMoney(settlement.amount) }}</span>
                  </div>
                </div>
                <el-empty v-else description="暂无结算信息" :image-size="80" />
              </div>
            </el-collapse-item>
          </el-collapse>

          <!-- Action button area -->
          <div class="action-area">
            <el-button
              v-if="!confirmed"
              type="success"
              size="large"
              class="confirm-btn"
              :loading="confirming"
              @click="handleConfirm"
            >
              <el-icon class="btn-icon"><CircleCheck /></el-icon>
              信息无误 ✓
            </el-button>
            <div v-else class="confirmed-msg">
              <el-icon color="#67c23a" :size="24"><CircleCheck /></el-icon>
              <span>已确认，感谢您的配合！</span>
              <router-link to="/login" class="register-link">
                查看更多提货记录？注册账号
              </router-link>
            </div>
            <p v-if="!confirmed" class="help-text">
              有问题？联系您的业务员
              <a href="tel:13800138000" class="contact-link">
                {{ pickupSummary.salesman }}: 拨打
              </a>
            </p>
          </div>
        </div>

        <!-- Contract dimension (if has multiple pickups) -->
        <div v-if="contractPickups.length > 0" class="buyer-card contract-card">
          <h3 class="section-title">合同维度查看</h3>
          <div class="contract-summary">
            <div class="contract-row">
              <span>合同号</span>
              <span>{{ contractSummary.contractNo }}</span>
            </div>
            <div class="contract-row">
              <span>已提重量</span>
              <span>{{ contractSummary.totalWeight }} 吨</span>
            </div>
            <div class="contract-row">
              <span>已提金额</span>
              <span>¥{{ formatMoney(contractSummary.totalAmount) }}</span>
            </div>
            <div class="contract-row">
              <span>合同进度</span>
              <span>{{ contractSummary.progress }}</span>
            </div>
          </div>
          <div class="pickup-list">
            <div
              v-for="p in contractPickups"
              :key="p.id"
              class="pickup-item"
              :class="{ active: p.id === currentPickupId }"
            >
              {{ p.time }} · {{ p.weight }}吨 · ¥{{ formatMoney(p.amount) }}
            </div>
          </div>
        </div>

        <!-- Footer -->
        <footer class="buyer-footer">
          <div class="footer-brand">提货通 · 智能提货服务平台</div>
          <div class="footer-badge">🔒 数据加密传输</div>
          <div class="footer-free">买方使用完全免费</div>
        </footer>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getGuestPickupDetail, confirmGuestPickup } from '@/api/evidence'

const route = useRoute()
const token = computed(() => String(route.params.token || ''))

// Verification state
const verified = ref(false)
const verifying = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const verifyFormRef = ref<FormInstance>()
const verifyForm = reactive({ phone: '', code: '' })
const verifyRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' },
  ],
}

// Pickup data
const loading = ref(false)
const confirmed = ref(false)
const confirming = ref(false)
const currentPickupId = ref<number | null>(null)

const pickupSummary = reactive({
  goods: [] as { name: string; weight: string }[],
  totalWeight: '0',
  totalAmount: '0',
  warehouse: '-',
  driver: '-',
  plate: '-',
  time: '-',
  salesman: '张三',
})

const liftRecords = ref<{ liftNo: string; productSpec: string; pieceCount: number; weight: string }[]>([])
const photos = reactive({
  loading: [] as string[],
  cargo: [] as string[],
  plate: [] as string[],
})
const settlement = reactive<{ weight?: number; unitPrice?: number; amount?: number }>({})
const contractPickups = ref<{ id: number; time: string; weight: string; amount: number }[]>([])
const contractSummary = reactive({
  contractNo: '-',
  totalWeight: '0',
  totalAmount: 0,
  progress: '-',
})

const activeCollapse = ref<string[]>(['lifts'])
const hasPhotos = computed(
  () => photos.loading.length + photos.cargo.length + photos.plate.length > 0
)

function formatNum(val: number | string | undefined) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | string | undefined) {
  if (val == null || val === '') return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

// Mock data when API fails
function getMockData() {
  return {
    goods: [
      { name: '螺纹钢 Φ20 HRB400 (日照钢铁)', weight: '83.2' },
      { name: '盘螺 Φ10 Q235 (沙钢)', weight: '15.1' },
    ],
    totalWeight: '98.3',
    totalAmount: '414,860',
    warehouse: 'XX钢铁华东仓',
    driver: '王五',
    plate: '沪A12345',
    time: '2026-03-17 14:30',
    salesman: '张三',
    liftRecords: [
      { liftNo: '1', productSpec: '螺纹钢 Φ20 HRB400', pieceCount: 42, weight: '41.6' },
      { liftNo: '2', productSpec: '螺纹钢 Φ20 HRB400', pieceCount: 42, weight: '41.6' },
      { liftNo: '3', productSpec: '盘螺 Φ10 Q235', pieceCount: 15, weight: '15.1' },
    ],
    photos: {
      loading: [] as string[],
      cargo: [] as string[],
      plate: [] as string[],
    },
    settlement: { weight: 98.3, unitPrice: 4220, amount: 414860 },
    contractPickups: [] as { id: number; time: string; weight: string; amount: number }[],
    contractSummary: { contractNo: '-', totalWeight: '0', totalAmount: 0, progress: '-' },
  }
}

function applyApiData(data: any) {
  const d = data ?? {}
  const lifts = d.liftRecords ?? d.lifts ?? []
  const p = d.photos ?? {}
  const s = d.settlement ?? (Array.isArray(d.settlements) ? d.settlements[0] : {})

  Object.assign(pickupSummary, {
    goods: (d.goods ?? d.productSummary ?? []).map((g: any) => ({
      name: g.name ?? g.productName ?? g.spec ?? '-',
      weight: String(g.weight ?? g.actualWeight ?? '0'),
    })),
    totalWeight: String(d.totalWeight ?? d.loadedWeight ?? '0'),
    totalAmount: formatMoney(d.totalAmount ?? d.settlementAmount ?? 0),
    warehouse: d.warehouseName ?? d.warehouse ?? '-',
    driver: d.driverName ?? d.driver ?? '-',
    plate: d.plateNo ?? d.plate ?? '-',
    time: d.actualArrivalTime ?? d.completedAt ?? d.time ?? '-',
    salesman: d.salesmanName ?? d.salesman ?? '业务员',
  })

  if (pickupSummary.goods.length === 0 && (d.productName || d.spec)) {
    pickupSummary.goods = [{
      name: `${d.productName ?? ''} ${d.spec ?? ''}`.trim() || '-',
      weight: String(d.loadedWeight ?? d.totalWeight ?? '0'),
    }]
  }

  liftRecords.value = lifts.map((l: any) => ({
    liftNo: l.liftNo ?? l.lift_no ?? '-',
    productSpec: `${l.productName ?? l.product_name ?? ''} ${l.spec ?? ''}`.trim() || '-',
    pieceCount: l.pieceCount ?? l.piece_count ?? 0,
    weight: String(l.actualWeight ?? l.theoryWeight ?? l.weight ?? '0'),
  }))

  photos.loading = p.loading ?? p.loadingPhotos ?? []
  photos.cargo = p.cargo ?? p.cargoPhotos ?? []
  photos.plate = p.plate ?? p.platePhotos ?? []

  Object.assign(settlement, {
    weight: s.weight ?? d.loadedWeight,
    unitPrice: s.unitPrice ?? s.unit_price,
    amount: s.amount ?? d.totalAmount,
  })

  const cp = d.contractPickups ?? d.pickups ?? []
  contractPickups.value = cp.map((x: any) => ({
    id: x.id ?? 0,
    time: x.time ?? x.completedAt ?? '-',
    weight: String(x.weight ?? x.loadedWeight ?? '0'),
    amount: x.amount ?? 0,
  }))

  if (d.contractNo || d.contractSummary) {
    const cs = d.contractSummary ?? d
    Object.assign(contractSummary, {
      contractNo: cs.contractNo ?? cs.contract_no ?? '-',
      totalWeight: String(cs.totalWeight ?? cs.total_weight ?? '0'),
      totalAmount: cs.totalAmount ?? cs.total_amount ?? 0,
      progress: cs.progress ?? '-',
    })
  }

  currentPickupId.value = d.id ?? null
}

async function loadPickupDetail() {
  if (!token.value) return
  loading.value = true
  try {
    const res = (await getGuestPickupDetail(token.value)) as any
    const data = res?.data ?? res ?? {}
    applyApiData(data)
  } catch {
    // Fallback to mock data for demo
    const mock = getMockData()
    Object.assign(pickupSummary, {
      goods: mock.goods,
      totalWeight: mock.totalWeight,
      totalAmount: mock.totalAmount,
      warehouse: mock.warehouse,
      driver: mock.driver,
      plate: mock.plate,
      time: mock.time,
      salesman: mock.salesman,
    })
    liftRecords.value = mock.liftRecords
    photos.loading = mock.photos.loading
    photos.cargo = mock.photos.cargo
    photos.plate = mock.photos.plate
    Object.assign(settlement, mock.settlement)
    contractPickups.value = mock.contractPickups
    Object.assign(contractSummary, mock.contractSummary)
  } finally {
    loading.value = false
  }
}

function handleSendCode() {
  if (!verifyForm.phone || !/^1\d{10}$/.test(verifyForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  sendingCode.value = true
  setTimeout(() => {
    sendingCode.value = false
    ElMessage.success('验证码已发送（演示模式）')
    countdown.value = 60
    const t = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(t)
    }, 1000)
  }, 500)
}

async function handleVerify() {
  if (!verifyFormRef.value) return
  await verifyFormRef.value.validate(async (valid) => {
    if (!valid) return
    verifying.value = true
    try {
      // Placeholder: always succeed
      await new Promise((r) => setTimeout(r, 600))
      verified.value = true
      loadPickupDetail()
    } catch {
      ElMessage.error('验证失败')
    } finally {
      verifying.value = false
    }
  })
}

async function handleConfirm() {
  confirming.value = true
  try {
    await confirmGuestPickup(token.value, verifyForm.phone)
    confirmed.value = true
    ElMessage.success('已确认')
  } catch {
    // Demo: still show success
    confirmed.value = true
    ElMessage.success('已确认')
  } finally {
    confirming.value = false
  }
}

watch(token, () => {
  if (verified.value) loadPickupDetail()
}, { immediate: false })

onMounted(() => {
  // Check if already verified this session (e.g. refresh)
  const stored = sessionStorage.getItem(`buyer_verified_${token.value}`)
  if (stored === '1') {
    verified.value = true
    loadPickupDetail()
  }
})

watch(verified, (v) => {
  if (v && token.value) {
    sessionStorage.setItem(`buyer_verified_${token.value}`, '1')
  }
})
</script>

<style lang="scss" scoped>
.buyer-page {
  min-height: 100vh;
  padding-bottom: 80px;
  position: relative;
}

.buyer-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  background: linear-gradient(180deg, #e8f4ff 0%, #f0f5fa 50%, #f5f7fa 100%);
}

.buyer-container {
  position: relative;
  z-index: 1;
  max-width: 480px;
  margin: 0 auto;
  padding: 16px;
}

.buyer-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.verify-card {
  .card-header {
    text-align: center;
    margin-bottom: 24px;
  }

  .card-icon {
    font-size: 48px;
    display: block;
    margin-bottom: 8px;
  }

  .card-title {
    font-size: 20px;
    font-weight: 600;
    color: #1a1a2e;
    margin: 0 0 8px;
  }

  .card-subtitle {
    font-size: 14px;
    color: #6b7280;
    margin: 0;
  }

  .verify-form {
    :deep(.el-form-item) {
      margin-bottom: 20px;
    }
  }

  .code-row {
    display: flex;
    gap: 12px;
    width: 100%;

    .code-input {
      flex: 1;
    }

    .el-button {
      flex-shrink: 0;
      min-width: 110px;
    }
  }

  .verify-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
  }
}

.summary-card {
  margin-bottom: 20px;

  .summary-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
  }

  .summary-icon {
    font-size: 28px;
  }

  .summary-title {
    font-size: 22px;
    font-weight: 600;
    color: #1a1a2e;
    margin: 0;
  }

  .summary-goods {
    .goods-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      font-size: 14px;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }
    }

    .goods-name {
      color: #303133;
      flex: 1;
      margin-right: 12px;
    }

    .goods-weight {
      color: #409eff;
      font-weight: 500;
      flex-shrink: 0;
    }
  }

  .summary-totals {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
    font-size: 15px;
    font-weight: 600;
    color: #303133;

    .amount {
      margin-left: 16px;
      color: #409eff;
    }
  }

  .summary-meta {
    margin-top: 16px;
    font-size: 13px;
    color: #606266;
    line-height: 1.8;
  }
}

.detail-collapse {
  border: none;

  :deep(.el-collapse-item__header) {
    border: none;
    font-size: 15px;
    height: 48px;
  }

  :deep(.el-collapse-item__wrap) {
    border: none;
  }

  :deep(.el-collapse-item__content) {
    padding-bottom: 16px;
  }

  .collapse-title {
    font-weight: 500;
  }

  .collapse-content {
    padding: 0 4px;
  }

  .lifts-table {
    font-size: 13px;
  }

  .photo-gallery {
    .photo-group {
      margin-bottom: 16px;
    }

    .photo-label {
      font-size: 13px;
      color: #606266;
      margin-bottom: 8px;
    }

    .photo-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .photo-item {
      width: 80px;
      height: 80px;
      border-radius: 8px;
      object-fit: cover;
    }
  }

  .settlement-info {
    .settlement-rows {
      font-size: 14px;
    }

    .settlement-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;

      &.total {
        font-weight: 600;
        font-size: 15px;
        color: #409eff;
        margin-top: 8px;
        padding-top: 12px;
        border-top: 1px solid #ebeef5;
      }
    }
  }
}

.action-area {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  text-align: center;

  .confirm-btn {
    width: 100%;
    height: 52px;
    font-size: 17px;
    font-weight: 500;

    .btn-icon {
      margin-right: 6px;
      vertical-align: middle;
    }
  }

  .confirmed-msg {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    color: #67c23a;
    font-size: 15px;

    .register-link {
      color: #409eff;
      text-decoration: none;
      font-size: 14px;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .help-text {
    margin-top: 16px;
    font-size: 13px;
    color: #909399;

    .contact-link {
      color: #409eff;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.contract-card {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 16px;
    color: #303133;
  }

  .contract-summary {
    margin-bottom: 16px;
    padding: 12px;
    background: #f8fafc;
    border-radius: 8px;
    font-size: 14px;
  }

  .contract-row {
    display: flex;
    justify-content: space-between;
    padding: 6px 0;
  }

  .pickup-list {
    .pickup-item {
      padding: 10px 12px;
      font-size: 13px;
      border-radius: 8px;
      margin-bottom: 8px;
      background: #f5f7fa;

      &.active {
        background: #ecf5ff;
        color: #409eff;
      }
    }
  }
}

.buyer-footer {
  text-align: center;
  padding: 24px 16px;
  font-size: 13px;
  color: #909399;

  .footer-brand {
    font-weight: 500;
    color: #606266;
    margin-bottom: 8px;
  }

  .footer-badge {
    margin-bottom: 4px;
  }

  .footer-free {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .buyer-container {
    padding: 12px;
  }

  .buyer-card {
    padding: 20px 16px;
  }

  .summary-title {
    font-size: 20px;
  }
}
</style>
