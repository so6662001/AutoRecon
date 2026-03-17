<template>
  <div class="page-container delivery-progress-page">
    <div v-loading="loading" class="delivery-content">
      <!-- Header -->
      <div class="detail-header">
        <div class="header-info">
          <span class="pickup-no">{{ order.pickupOrderNo }}</span>
          <span class="separator">|</span>
          <span class="contract-no">{{ order.contractNo }}</span>
          <span class="separator">|</span>
          <span class="buyer-name">{{ order.buyerName }}</span>
        </div>
        <el-button type="primary" :loading="refreshing" @click="handleRefresh">
          手动刷新
        </el-button>
      </div>

      <!-- Progress overview: 大进度环 -->
      <el-card shadow="hover" class="progress-overview">
        <div class="progress-ring-container">
          <v-chart
            v-if="ringOption"
            :option="ringOption"
            class="progress-ring"
            autoresize
          />
          <div class="progress-center">
            <div class="progress-text">已装 {{ order.loadedLifts ?? 0 }} / {{ order.totalLifts ?? 0 }} 吊</div>
            <div class="progress-weight">已装 {{ formatNum(order.loadedWeight) }} 吨</div>
          </div>
        </div>
      </el-card>

      <!-- Lift records table -->
      <el-card shadow="hover" class="lift-records-card">
        <template #header>
          <span>吊装记录</span>
        </template>
        <el-table :data="liftRecords" stripe>
          <el-table-column prop="liftNo" label="吊序号" width="100" />
          <el-table-column prop="productSpec" label="品规" min-width="140" />
          <el-table-column prop="pieceCount" label="件数" width="80" align="right" />
          <el-table-column prop="weight" label="重量" width="100" align="right" />
          <el-table-column prop="createdAt" label="时间" width="180" />
          <el-table-column prop="operator" label="操作员" width="100" />
          <el-table-column label="" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isNew" type="success" size="small">新</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- Photos section -->
      <el-card shadow="hover" class="photos-card">
        <template #header>
          <span>已上传照片</span>
        </template>
        <div class="photo-gallery">
          <el-image
            v-for="(url, i) in photos"
            :key="i"
            :src="url"
            fit="cover"
            class="photo-item"
            :preview-src-list="photos"
          />
          <el-empty v-if="!photos.length" description="暂无照片" :image-size="80" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getPickupOrderDetail, getDeliveryProgress } from '@/api/evidence'

use([PieChart, CanvasRenderer, TitleComponent])

const route = useRoute()
const loading = ref(false)
const refreshing = ref(false)

const order = reactive<Record<string, any>>({
  id: null,
  pickupOrderNo: '',
  contractNo: '',
  buyerName: '',
  loadedLifts: 0,
  totalLifts: 0,
  loadedWeight: 0,
})

const liftRecords = ref<Array<{ liftNo: string; productSpec: string; pieceCount: number; weight: number; createdAt: string; operator: string; isNew?: boolean }>>([])
const photos = ref<string[]>([])
const lastLoadedCount = ref(0)

const pickupId = computed(() => Number(route.params.id))

const ringOption = computed(() => {
  const total = Number(order.totalLifts) || 1
  const loaded = Number(order.loadedLifts) || 0
  const percent = Math.min(100, Math.round((loaded / total) * 100))
  return {
    series: [
      {
        type: 'pie',
        radius: ['60%', '80%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        label: { show: false },
        labelLine: { show: false },
        data: [
          { value: percent, itemStyle: { color: '#409eff' } },
          { value: 100 - percent, itemStyle: { color: '#ebeef5' } },
        ],
      },
    ],
  }
})

function formatNum(val: number | string) {
  if (val == null || val === '') return '-'
  return Number(val).toLocaleString('zh-CN')
}

async function loadOrder() {
  if (!pickupId.value) return
  try {
    const res = (await getPickupOrderDetail(pickupId.value)) as any
    const data = res?.data ?? res ?? {}
    order.id = data.id ?? pickupId.value
    order.pickupOrderNo = data.pickupOrderNo ?? data.pickup_order_no ?? '-'
    order.contractNo = data.contractNo ?? data.contract_no ?? '-'
    order.buyerName = data.buyerName ?? data.buyer_name ?? '-'
  } catch {
    // keep empty
  }
}

async function loadProgress() {
  if (!pickupId.value) return
  loading.value = true
  try {
    const res = (await getDeliveryProgress(pickupId.value)) as any
    const data = res?.data ?? res ?? {}
    order.loadedLifts = data.loadedLifts ?? data.loaded_lifts ?? 0
    order.totalLifts = data.totalLifts ?? data.total_lifts ?? 0
    order.loadedWeight = data.loadedWeight ?? data.loaded_weight ?? 0

    const lifts = data.liftRecords ?? data.lifts ?? []
    const prevCount = liftRecords.value.length
    liftRecords.value = lifts.map((l: any, idx: number) => ({
      liftNo: l.liftNo ?? l.lift_no ?? '-',
      productSpec: l.productSpec ?? l.product_spec ?? (`${l.productName ?? ''} ${l.spec ?? ''}`.trim() || '-'),
      pieceCount: l.pieceCount ?? l.piece_count ?? 0,
      weight: l.weight ?? l.actualWeight ?? l.actual_weight ?? 0,
      createdAt: l.createdAt ?? l.created_at ?? '-',
      operator: l.operator ?? '-',
      isNew: idx >= prevCount,
    }))
    lastLoadedCount.value = liftRecords.value.length

    const p = data.photos ?? []
    photos.value = Array.isArray(p) ? p : (p.loading ?? []).concat(p.cargo ?? [], p.plate ?? [], p.weigh ?? [])
  } catch {
    // keep empty
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await loadProgress()
    await loadOrder()
  } finally {
    refreshing.value = false
  }
}

watch(pickupId, () => {
  loadOrder()
  loadProgress()
}, { immediate: true })

onMounted(() => {
  loadOrder()
  loadProgress()
})
</script>

<style lang="scss" scoped>
.delivery-progress-page {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 12px;

    .header-info {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .pickup-no {
      font-size: 20px;
      font-weight: 600;
    }

    .contract-no,
    .buyer-name {
      font-size: 14px;
      color: #606266;
    }

    .separator {
      color: #dcdfe6;
    }
  }

  .progress-overview {
    margin-bottom: 16px;
  }

  .progress-ring-container {
    position: relative;
    height: 200px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .progress-ring {
    width: 200px;
    height: 200px;
    position: absolute;
  }

  .progress-center {
    position: relative;
    z-index: 1;
    text-align: center;
  }

  .progress-text {
    font-size: 18px;
    font-weight: 600;
  }

  .progress-weight {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
  }

  .lift-records-card {
    margin-bottom: 16px;
  }

  .photos-card {
    margin-bottom: 16px;
  }

  .photo-gallery {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;

    .photo-item {
      width: 120px;
      height: 120px;
      border-radius: 4px;
    }
  }
}
</style>
