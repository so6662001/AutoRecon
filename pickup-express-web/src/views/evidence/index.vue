<template>
  <div class="page-container evidence-page">
    <el-page-header @back="goBack" title="返回" />

    <div v-loading="loading" class="evidence-content">
      <div class="overview-section">
        <h3>证据包概览</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="提货单号">{{ overview.pickupNo }}</el-descriptions-item>
          <el-descriptions-item label="合同号">{{ overview.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ overview.buyerName }}</el-descriptions-item>
          <el-descriptions-item label="归档时间">{{ overview.archivedAt }}</el-descriptions-item>
          <el-descriptions-item label="哈希值" :span="2">
            <el-text class="hash-text" truncated>{{ overview.hash }}</el-text>
          </el-descriptions-item>
        </el-descriptions>
        <div class="action-bar">
          <el-button type="primary" :loading="verifying" @click="handleVerify">验证证据完整性</el-button>
          <el-button type="success" :loading="downloading" @click="handleDownload">下载证据包(ZIP)</el-button>
        </div>
      </div>

      <div class="files-section">
        <h3>证据文件列表</h3>
        <div class="file-grid">
          <el-card v-if="files.contractPdf" shadow="hover" class="file-card">
            <template #header>
              <span>📄 签章合同PDF</span>
            </template>
            <el-link type="primary" :href="files.contractPdf" target="_blank">预览/下载</el-link>
          </el-card>
          <el-card v-if="files.pickupPdf" shadow="hover" class="file-card">
            <template #header>
              <span>📄 提货单PDF</span>
            </template>
            <el-link type="primary" :href="files.pickupPdf" target="_blank">预览/下载</el-link>
          </el-card>
          <el-card v-if="files.deliveryDetail" shadow="hover" class="file-card">
            <template #header>
              <span>📦 发货明细</span>
            </template>
            <el-button type="primary" link @click="showJsonViewer = true">查看</el-button>
          </el-card>
          <el-card v-if="files.photos?.length" shadow="hover" class="file-card">
            <template #header>
              <span>📷 现场照片</span>
            </template>
            <el-image
              v-for="(url, i) in files.photos"
              :key="i"
              :src="url"
              :preview-src-list="files.photos"
              :initial-index="i"
              fit="cover"
              class="thumb-img"
            />
          </el-card>
          <el-card v-if="files.signPhoto" shadow="hover" class="file-card">
            <template #header>
              <span>✍️ 签字照片</span>
            </template>
            <el-image :src="files.signPhoto" :preview-src-list="[files.signPhoto]" fit="cover" class="thumb-img" />
          </el-card>
          <el-card v-if="files.settlementPdf" shadow="hover" class="file-card">
            <template #header>
              <span>📄 结算单PDF</span>
            </template>
            <el-link type="primary" :href="files.settlementPdf" target="_blank">预览/下载</el-link>
          </el-card>
        </div>
      </div>

      <el-dialog v-model="showJsonViewer" title="发货明细" width="600px">
        <pre class="json-viewer">{{ JSON.stringify(files.deliveryDetail, null, 2) }}</pre>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getEvidencePackage, verifyEvidence, downloadEvidenceZip } from '@/api/evidence'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const verifying = ref(false)
const downloading = ref(false)
const showJsonViewer = ref(false)

const overview = reactive({
  pickupNo: '-',
  contractNo: '-',
  buyerName: '-',
  archivedAt: '-',
  hash: '-',
})

const files = reactive<{
  contractPdf?: string
  pickupPdf?: string
  deliveryDetail?: any
  photos?: string[]
  signPhoto?: string
  settlementPdf?: string
}>({})

function goBack() {
  router.back()
}

async function loadEvidence() {
  const pickupOrderId = route.params.pickupOrderId
  if (!pickupOrderId) return
  loading.value = true
  try {
    const res = (await getEvidencePackage(Number(pickupOrderId))) as any
    const data = res?.data ?? res
    overview.pickupNo = data?.pickupNo ?? data?.pickup_no ?? '-'
    overview.contractNo = data?.contractNo ?? data?.contract_no ?? '-'
    overview.buyerName = data?.buyerName ?? data?.buyer_name ?? '-'
    overview.archivedAt = data?.archivedAt ? dayjs(data.archivedAt).format('YYYY-MM-DD HH:mm') : '-'
    overview.hash = data?.hash ?? data?.evidenceHash ?? '-'

    files.contractPdf = data?.contractPdf ?? data?.contract_pdf
    files.pickupPdf = data?.pickupPdf ?? data?.pickup_pdf
    files.deliveryDetail = data?.deliveryDetail ?? data?.delivery_detail ?? {}
    files.photos = data?.photos ?? data?.deliveryPhotos ?? []
    files.signPhoto = data?.signPhoto ?? data?.sign_photo
    files.settlementPdf = data?.settlementPdf ?? data?.settlement_pdf
  } catch {
    // error handled
  } finally {
    loading.value = false
  }
}

async function handleVerify() {
  const pickupOrderId = route.params.pickupOrderId
  if (!pickupOrderId) return
  verifying.value = true
  try {
    const res = (await verifyEvidence(Number(pickupOrderId))) as any
    const data = res?.data ?? res
    const valid = data?.valid ?? data?.verified ?? true
    ElMessage.success(valid ? '证据完整性验证通过' : '证据验证失败')
  } catch {
    ElMessage.error('验证失败')
  } finally {
    verifying.value = false
  }
}

async function handleDownload() {
  const pickupOrderId = route.params.pickupOrderId
  if (!pickupOrderId) return
  downloading.value = true
  try {
    const blob = (await downloadEvidenceZip(Number(pickupOrderId))) as unknown as Blob
    if (blob instanceof Blob) {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `evidence-${pickupOrderId}.zip`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('下载成功')
    }
  } catch {
    ElMessage.error('下载失败')
  } finally {
    downloading.value = false
  }
}

onMounted(loadEvidence)
</script>

<style lang="scss" scoped>
.evidence-page {
  .evidence-content {
    margin-top: 20px;
  }

  .overview-section,
  .files-section {
    margin-bottom: 24px;

    h3 {
      margin: 0 0 12px;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .action-bar {
    margin-top: 16px;
    display: flex;
    gap: 12px;
  }

  .hash-text {
    font-family: monospace;
    font-size: 12px;
  }

  .file-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;

    .file-card {
      .thumb-img {
        width: 100%;
        height: 80px;
        border-radius: 4px;
        cursor: pointer;
      }
    }
  }

  .json-viewer {
    background: #f5f5f5;
    padding: 12px;
    border-radius: 4px;
    max-height: 400px;
    overflow: auto;
    font-size: 12px;
  }
}
</style>
