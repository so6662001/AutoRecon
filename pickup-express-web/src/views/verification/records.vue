<template>
  <div class="page-container verification-records">
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="pickupNo" label="提货单号" width="120" />
      <el-table-column prop="contractNo" label="合同号" width="120" />
      <el-table-column prop="driverName" label="司机" width="100" />
      <el-table-column prop="plateNo" label="车牌" width="100" />
      <el-table-column label="是否预登记" width="100">
        <template #default="{ row }">
          <el-tag :type="row.preRegistered ? 'success' : 'info'" size="small">
            {{ row.preRegistered ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="确权等级" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.verifyLevel ?? '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="短信已发" width="90" align="center">
        <template #default="{ row }">{{ row.smsSent ? '✓' : '✗' }}</template>
      </el-table-column>
      <el-table-column label="电话确认" width="90" align="center">
        <template #default="{ row }">{{ row.phoneConfirmed ? '✓' : '✗' }}</template>
      </el-table-column>
      <el-table-column label="电话结果" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.phoneResult ?? '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="身份采集" width="90" align="center">
        <template #default="{ row }">{{ row.identityCollected ? '✓' : '✗' }}</template>
      </el-table-column>
      <el-table-column label="确权结果" width="100">
        <template #default="{ row }">
          <el-tag :type="(verifyResultTagType(row.verifyResult) || undefined) as any" size="small">
            {{ row.verifyResult ?? '待处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openDetailDialog(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDetailDialog" title="确权详情" width="600px">
      <div v-if="detailRecord" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="提货单号">{{ detailRecord.pickupNo }}</el-descriptions-item>
          <el-descriptions-item label="合同号">{{ detailRecord.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="司机">{{ detailRecord.driverName }}</el-descriptions-item>
          <el-descriptions-item label="车牌">{{ detailRecord.plateNo }}</el-descriptions-item>
          <el-descriptions-item label="预登记">{{ detailRecord.preRegistered ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="确权等级">{{ detailRecord.verifyLevel }}</el-descriptions-item>
          <el-descriptions-item label="短信已发">{{ detailRecord.smsSent ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="电话确认">{{ detailRecord.phoneConfirmed ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="电话结果">{{ detailRecord.phoneResult }}</el-descriptions-item>
          <el-descriptions-item label="身份采集">{{ detailRecord.identityCollected ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="确权结果">{{ detailRecord.verifyResult }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDate(detailRecord.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detailRecord.photos?.length" class="photos-section">
          <h4>现场照片</h4>
          <el-image
            v-for="(url, i) in detailRecord.photos"
            :key="i"
            :src="url"
            :preview-src-list="detailRecord.photos"
            :initial-index="i as number"
            fit="cover"
            class="detail-photo"
          />
        </div>
        <div v-if="detailRecord.recordingUrl" class="recording-section">
          <h4>录音链接</h4>
          <el-link :href="detailRecord.recordingUrl" target="_blank" type="primary">播放录音</el-link>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { listVerificationRecords } from '@/api/evidence'

const loading = ref(false)
const showDetailDialog = ref(false)
const tableData = ref<any[]>([])
const detailRecord = ref<any>(null)

function formatDate(val: string | undefined) {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm')
}

function verifyResultTagType(result: string): 'success' | 'danger' | 'warning' | 'info' {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = { 通过: 'success', 拒绝: 'danger', 待处理: 'warning' }
  return map[result] ?? 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = (await listVerificationRecords({})) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data ?? []
    tableData.value = Array.isArray(list)
      ? list.map((r: any) => ({
          id: r.id,
          pickupNo: r.pickupNo ?? r.pickup_no ?? '-',
          contractNo: r.contractNo ?? r.contract_no ?? '-',
          driverName: r.driverName ?? r.driver_name ?? '-',
          plateNo: r.plateNo ?? r.plate_no ?? '-',
          preRegistered: r.preRegistered ?? r.pre_registered ?? false,
          verifyLevel: r.verifyLevel ?? r.verify_level ?? '-',
          smsSent: r.smsSent ?? r.sms_sent ?? false,
          phoneConfirmed: r.phoneConfirmed ?? r.phone_confirmed ?? false,
          phoneResult: r.phoneResult ?? r.phone_result ?? '-',
          identityCollected: r.identityCollected ?? r.identity_collected ?? false,
          verifyResult: r.verifyResult ?? r.verify_result ?? '待处理',
          createdAt: r.createdAt ?? r.created_at,
          photos: r.photos ?? [],
          recordingUrl: r.recordingUrl ?? r.recording_url,
        }))
      : []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function openDetailDialog(row: any) {
  detailRecord.value = row
  showDetailDialog.value = true
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.verification-records {
  .detail-content {
    .photos-section,
    .recording-section {
      margin-top: 16px;

      h4 {
        margin: 0 0 8px;
        font-size: 14px;
      }
    }

    .detail-photo {
      width: 100px;
      height: 100px;
      margin-right: 8px;
      margin-bottom: 8px;
      border-radius: 4px;
      cursor: pointer;
    }
  }
}
</style>
