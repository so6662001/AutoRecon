<template>
  <div class="page-container pickup-detail">
    <div v-loading="loading" class="detail-content">
      <!-- Header -->
      <div class="detail-header">
        <div class="header-left">
          <span class="pickup-no">{{ order.pickupOrderNo }}</span>
          <el-tag :type="statusTagType(order.status)" size="small" class="ml-2">
            {{ order.status }}
          </el-tag>
        </div>
        <div class="header-actions">
          <el-button v-if="canCancel" type="danger" @click="handleCancel">取消</el-button>
          <el-button type="primary" @click="handleViewDelivery">发货进度</el-button>
          <el-button @click="handleViewCode">查看提货码</el-button>
          <el-button @click="handleViewEvidence">查看证据包</el-button>
        </div>
      </div>

      <!-- 提货码卡片 -->
      <el-card shadow="never" class="qrcode-card">
        <div class="qrcode-content">
          <div class="code-section">
            <div class="code-label">提货码</div>
            <div class="code-value">{{ order.pickupCode || '-' }}</div>
            <div class="code-meta">
              <span>状态: {{ order.pickupCodeStatus || '有效' }}</span>
              <span class="ml-4">有效期: {{ order.pickupCodeExpiry || '-' }}</span>
            </div>
          </div>
          <div class="qrcode-placeholder">
            <div class="qrcode-box">二维码占位</div>
          </div>
        </div>
      </el-card>

      <!-- 车辆信息 -->
      <el-descriptions :column="descriptionsColumn" border class="vehicle-section">
        <el-descriptions-item label="派车模式">{{ order.dispatchMode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="车牌号">{{ order.plateNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="驾驶员">{{ order.driverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ maskPhone(order.driverPhone || '') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="承运公司">{{ order.carrierName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计到达时间">{{ order.expectedArrivalTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际到达时间" :span="2">{{ order.actualArrivalTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="发货明细" name="lifts">
          <div class="table-wrapper">
            <el-table :data="liftRecords" stripe>
              <el-table-column prop="liftNo" label="吊序号" width="100" />
              <el-table-column prop="productName" label="品名" />
              <el-table-column prop="spec" label="规格" />
              <el-table-column prop="pieceCount" label="件数" align="right" />
              <el-table-column prop="theoryWeight" label="理论重量" align="right" />
              <el-table-column prop="actualWeight" label="过磅重量" align="right" />
              <el-table-column prop="operator" label="操作员" width="100" />
              <el-table-column prop="createdAt" label="时间" width="180" />
              <el-table-column prop="source" label="数据来源" width="100">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.source || '-' }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
        <el-tab-pane label="发货进度" name="progress">
          <div class="delivery-progress">
            <el-progress
              :percentage="deliveryProgressPercent"
              :stroke-width="20"
              :format="() => `已装 ${order.loadedLifts ?? 0} / 共 ${order.totalLifts ?? 0} 吊`"
            />
            <div class="progress-summary">
              已装重量: {{ formatNum(order.loadedWeight) }} 吨
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="现场照片" name="photos">
          <div class="photo-gallery">
            <div v-if="photos.loading.length" class="photo-group">
              <div class="photo-label">装载照</div>
              <el-image
                v-for="(url, i) in photos.loading"
                :key="'loading-' + i"
                :src="url"
                fit="cover"
                class="photo-item"
                :preview-src-list="photos.loading"
              />
            </div>
            <div v-if="photos.cargo.length" class="photo-group">
              <div class="photo-label">货物照</div>
              <el-image
                v-for="(url, i) in photos.cargo"
                :key="'cargo-' + i"
                :src="url"
                fit="cover"
                class="photo-item"
                :preview-src-list="photos.cargo"
              />
            </div>
            <div v-if="photos.plate.length" class="photo-group">
              <div class="photo-label">车牌照</div>
              <el-image
                v-for="(url, i) in photos.plate"
                :key="'plate-' + i"
                :src="url"
                fit="cover"
                class="photo-item"
                :preview-src-list="photos.plate"
              />
            </div>
            <div v-if="photos.weigh.length" class="photo-group">
              <div class="photo-label">磅单照</div>
              <el-image
                v-for="(url, i) in photos.weigh"
                :key="'weigh-' + i"
                :src="url"
                fit="cover"
                class="photo-item"
                :preview-src-list="photos.weigh"
              />
            </div>
            <el-empty v-if="!hasPhotos" description="暂无照片" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="确权信息" name="verification">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="是否预登记">{{ verification.preRegistered ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="短信">{{ verification.smsVerified ? '已验证' : '未验证' }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ verification.phoneVerified ? '已验证' : '未验证' }}</el-descriptions-item>
            <el-descriptions-item label="身份采集">{{ verification.identityCollected ? '已采集' : '未采集' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="结算单" name="settlement">
          <el-descriptions v-if="settlement.id" :column="descriptionsColumn" border>
            <el-descriptions-item label="结算单号">{{ settlement.settlementNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ settlement.status }}</el-descriptions-item>
            <el-descriptions-item label="重量">{{ formatNum(settlement.weight) }}</el-descriptions-item>
            <el-descriptions-item label="金额">{{ formatMoney(settlement.amount) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ settlement.createdAt }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无结算单" />
        </el-tab-pane>
        <el-tab-pane label="提货时间线" name="timeline">
          <el-timeline>
            <el-timeline-item
              v-for="(evt, idx) in timelineEvents"
              :key="idx"
              :timestamp="evt.time"
              placement="top"
            >
              <el-tag size="small" class="mr-2">{{ evt.eventType }}</el-tag>
              {{ evt.title }}
            </el-timeline-item>
            <el-empty v-if="!timelineEvents.length" description="暂无进度" />
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getPickupOrderDetail,
  cancelPickupOrder,
  getPickupQrcode,
  getDeliveryProgress,
  getPickupTimeline,
  listSettlements,
  listVerificationRecords,
} from '@/api/evidence'
import { maskPhone } from '@/utils/mask'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('lifts')

const isMobileLayout = ref(false)
function updateMobileLayout() {
  isMobileLayout.value = typeof window !== 'undefined' && window.innerWidth <= 768
}
const descriptionsColumn = computed(() => (isMobileLayout.value ? 1 : 2))

const order = reactive<Record<string, any>>({
  id: null,
  pickupOrderNo: '',
  status: '',
  pickupCode: '',
  pickupCodeStatus: '',
  pickupCodeExpiry: '',
  dispatchMode: '',
  plateNo: '',
  driverName: '',
  driverPhone: '',
  carrierName: '',
  expectedArrivalTime: '',
  actualArrivalTime: '',
  loadedLifts: 0,
  totalLifts: 0,
  loadedWeight: 0,
})

const liftRecords = ref<any[]>([])
const photos = reactive({
  loading: [] as string[],
  cargo: [] as string[],
  plate: [] as string[],
  weigh: [] as string[],
})
const verification = reactive({
  preRegistered: false,
  smsVerified: false,
  phoneVerified: false,
  identityCollected: false,
})
const settlement = reactive<Record<string, any>>({})
const timelineEvents = ref<{ time: string; eventType: string; title: string }[]>([])

const pickupId = computed(() => Number(route.params.id))

const deliveryProgressPercent = computed(() => {
  const total = Number(order.totalLifts) || 0
  const loaded = Number(order.loadedLifts) || 0
  if (total <= 0) return 0
  return Math.round((loaded / total) * 100)
})

const canCancel = computed(() => {
  const s = order.status
  return s !== '已完成' && s !== '已取消'
})

const hasPhotos = computed(() =>
  photos.loading.length + photos.cargo.length + photos.plate.length + photos.weigh.length > 0
)

function statusTagType(s: string): 'success' | 'info' | 'danger' {
  const map: Record<string, 'success' | 'info' | 'danger'> = {
    待派车: 'info',
    已派车: 'info',
    发货中: 'info',
    已完成: 'success',
    已取消: 'danger',
  }
  return map[s] ?? 'info'
}

function formatNum(val: number | string) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | string) {
  if (val == null || val === '') return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

async function loadDetail() {
  if (!pickupId.value) return
  loading.value = true
  try {
    const res = (await getPickupOrderDetail(pickupId.value)) as any
    const data = res?.data ?? res ?? {}
    Object.assign(order, {
      id: data.id ?? pickupId.value,
      pickupOrderNo: data.pickupOrderNo ?? data.pickup_order_no ?? '-',
      status: data.status ?? data.deliveryStatus ?? '-',
      pickupCode: data.pickupCode ?? data.pickup_code ?? '-',
      pickupCodeStatus: data.pickupCodeStatus ?? '有效',
      pickupCodeExpiry: data.pickupCodeExpiry ?? data.pickup_code_expiry ?? '-',
      dispatchMode: data.dispatchMode ?? data.delivery_mode ?? '-',
      plateNo: data.plateNo ?? data.plate_no ?? '-',
      driverName: data.driverName ?? data.driver_name ?? '-',
      driverPhone: data.driverPhone ?? data.driver_phone ?? '-',
      carrierName: data.carrierName ?? data.carrier_name ?? '-',
      expectedArrivalTime: data.expectedArrivalTime ?? data.expected_arrival_time ?? '-',
      actualArrivalTime: data.actualArrivalTime ?? data.actual_arrival_time ?? '-',
      loadedLifts: data.loadedLifts ?? data.loaded_lifts ?? 0,
      totalLifts: data.totalLifts ?? data.total_lifts ?? 0,
      loadedWeight: data.loadedWeight ?? data.loaded_weight ?? 0,
    })
    liftRecords.value = (data.liftRecords ?? data.lifts ?? []).map((l: any) => ({
      liftNo: l.liftNo ?? l.lift_no ?? '-',
      productName: l.productName ?? l.product_name ?? '-',
      spec: l.spec ?? '-',
      pieceCount: l.pieceCount ?? l.piece_count ?? 0,
      theoryWeight: l.theoryWeight ?? l.theory_weight ?? 0,
      actualWeight: l.actualWeight ?? l.actual_weight ?? 0,
      operator: l.operator ?? '-',
      createdAt: l.createdAt ?? l.created_at ?? '-',
      source: l.source ?? '-',
    }))
    const p = data.photos ?? {}
    photos.loading = p.loading ?? p.loadingPhotos ?? []
    photos.cargo = p.cargo ?? p.cargoPhotos ?? []
    photos.plate = p.plate ?? p.platePhotos ?? []
    photos.weigh = p.weigh ?? p.weighPhotos ?? []
  } catch {
    // keep empty
  } finally {
    loading.value = false
  }
}

async function loadProgress() {
  if (!pickupId.value) return
  try {
    const res = (await getDeliveryProgress(pickupId.value)) as any
    const data = res?.data ?? res ?? {}
    order.loadedLifts = data.loadedLifts ?? data.loaded_lifts ?? order.loadedLifts
    order.totalLifts = data.totalLifts ?? data.total_lifts ?? order.totalLifts
    order.loadedWeight = data.loadedWeight ?? data.loaded_weight ?? order.loadedWeight
    const lifts = data.liftRecords ?? data.lifts ?? []
    if (lifts.length) {
      liftRecords.value = lifts.map((l: any) => ({
        liftNo: l.liftNo ?? l.lift_no ?? '-',
        productName: l.productName ?? l.product_name ?? '-',
        spec: l.spec ?? '-',
        pieceCount: l.pieceCount ?? l.piece_count ?? 0,
        theoryWeight: l.theoryWeight ?? l.theory_weight ?? 0,
        actualWeight: l.actualWeight ?? l.actual_weight ?? 0,
        operator: l.operator ?? '-',
        createdAt: l.createdAt ?? l.created_at ?? '-',
        source: l.source ?? '-',
      }))
    }
  } catch {
    // ignore
  }
}

async function loadVerification() {
  if (!pickupId.value) return
  try {
    const res = (await listVerificationRecords({ pickupOrderId: pickupId.value })) as any
    const data = res?.data ?? res ?? {}
    const list = Array.isArray(data) ? data : (data.list ?? data.records ?? [])
    const rec = list[0]
    if (rec) {
      verification.preRegistered = rec.preRegistered ?? rec.pre_registered ?? false
      verification.smsVerified = rec.smsVerified ?? rec.sms_verified ?? false
      verification.phoneVerified = rec.phoneVerified ?? rec.phone_verified ?? false
      verification.identityCollected = rec.identityCollected ?? rec.identity_collected ?? false
    }
  } catch {
    // ignore
  }
}

async function loadSettlement() {
  if (!pickupId.value) return
  try {
    const res = (await listSettlements({ pickupOrderId: pickupId.value })) as any
    const data = res?.data ?? res ?? {}
    const list = data?.list ?? data?.records ?? data ?? []
    const rec = Array.isArray(list) ? list[0] : null
    if (rec) {
      Object.assign(settlement, {
        id: rec.id ?? rec.settlementId,
        settlementNo: rec.settlementNo ?? rec.settlement_no ?? '-',
        status: rec.status ?? '-',
        weight: rec.weight ?? 0,
        amount: rec.amount ?? 0,
        createdAt: rec.createdAt ?? rec.created_at ?? '-',
      })
    }
  } catch {
    // settlement may not exist
  }
}

async function loadTimeline() {
  if (!pickupId.value) return
  try {
    const res = (await getPickupTimeline(pickupId.value)) as any
    const data = res?.data ?? res ?? {}
    const events = data.events ?? data ?? []
    timelineEvents.value = Array.isArray(events)
      ? events.map((e: any) => ({
          time: e.time ?? e.createdAt ?? '',
          eventType: e.eventType ?? e.event_type ?? '进度',
          title: e.title ?? e.content ?? '',
        }))
      : []
  } catch {
    timelineEvents.value = []
  }
}

async function handleCancel() {
  try {
    await cancelPickupOrder(order.id)
    loadDetail()
  } catch {
    // handled
  }
}

async function handleViewCode() {
  try {
    const res = (await getPickupQrcode(order.id)) as any
    const data = res?.data ?? res ?? {}
    order.pickupCode = data.code ?? data.pickupCode ?? order.pickupCode
    order.pickupCodeExpiry = data.expiry ?? data.expiryTime ?? order.pickupCodeExpiry
    ElMessage.info('提货码已刷新')
  } catch {
    ElMessage.info('提货码: ' + (order.pickupCode || '-'))
  }
}

function handleViewEvidence() {
  router.push({ path: `/evidence/${order.id}` })
}

function handleViewDelivery() {
  router.push({ name: 'pickupDelivery', params: { id: String(order.id) } })
}

watch(pickupId, () => {
  loadDetail()
  loadProgress()
  loadVerification()
  loadSettlement()
  loadTimeline()
}, { immediate: true })

onMounted(() => {
  updateMobileLayout()
  window.addEventListener('resize', updateMobileLayout)
  loadDetail()
  loadProgress()
  loadVerification()
  loadSettlement()
  loadTimeline()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateMobileLayout)
})
</script>

<style lang="scss" scoped>
.pickup-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 12px;

    .header-left {
      display: flex;
      align-items: center;
    }

    .pickup-no {
      font-size: 20px;
      font-weight: 600;
    }

    .ml-2 { margin-left: 8px; }
    .mr-2 { margin-right: 8px; }
  }

  .qrcode-card {
    margin-bottom: 16px;
  }

  .qrcode-content {
    display: flex;
    gap: 24px;
    align-items: center;
    flex-wrap: wrap;
  }

  .code-section {
    .code-label {
      font-size: 14px;
      color: #909399;
      margin-bottom: 4px;
    }

    .code-value {
      font-size: 28px;
      font-weight: 600;
      letter-spacing: 4px;
    }

    .code-meta {
      font-size: 12px;
      color: #909399;
      margin-top: 8px;
    }

    .ml-4 { margin-left: 16px; }
  }

  .qrcode-placeholder {
    .qrcode-box {
      width: 120px;
      height: 120px;
      border: 1px dashed #dcdfe6;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #909399;
      font-size: 12px;
    }
  }

  .vehicle-section {
    margin-bottom: 16px;
  }

  .delivery-progress {
    padding: 24px 0;

    .progress-summary {
      margin-top: 12px;
      font-size: 16px;
    }
  }

  .photo-gallery {
    .photo-group {
      margin-bottom: 24px;
    }

    .photo-label {
      font-size: 14px;
      margin-bottom: 8px;
      color: #606266;
    }

    .photo-item {
      width: 120px;
      height: 120px;
      margin-right: 12px;
      margin-bottom: 12px;
      border-radius: 4px;
    }
  }

  .detail-tabs {
    margin-top: 16px;
  }
}

@media (max-width: 768px) {
  .pickup-detail.page-container,
  .page-container.pickup-detail {
    padding: 12px;
  }

  .detail-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }

  .detail-header .header-actions {
    display: flex;
    flex-direction: column;
    width: 100%;
    gap: 8px;
  }

  .detail-header .header-actions .el-button {
    width: 100%;
    margin-left: 0;
  }

  .vehicle-section {
    --el-descriptions-item-bordered-label-background: #fafafa;
  }

  .code-meta {
    font-size: 14px;
  }

  .table-wrapper {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  :deep(.el-table) {
    font-size: 13px;
  }

  .photo-gallery .photo-item {
    width: 80px;
    height: 80px;
  }
}
</style>
