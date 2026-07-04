<template>
  <div class="page-container supplement-manage">
    <div class="action-bar">
      <el-button type="primary" @click="openCreateDialog">新建补录</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="pickupOrderNo" label="提货单号" width="160" />
      <el-table-column prop="submitter" label="提交人" width="120" />
      <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
      <el-table-column prop="submittedAt" label="提交时间" width="180" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openApproveDialog(row)">审核</el-button>
          <el-button type="danger" link size="small" @click="openRejectDialog(row)">拒绝</el-button>
          <el-button type="info" link size="small" @click="viewDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建补录对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建补录"
      width="600px"
      :close-on-click-modal="false"
      @close="resetCreateForm"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="提货单" prop="pickupOrderId">
          <el-select
            v-model="createForm.pickupOrderId"
            placeholder="请选择提货单"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="p in pickupOptions"
              :key="p.id"
              :label="`${p.pickupOrderNo || p.pickup_order_no || p.id} - ${p.buyerName || p.buyer_name || ''}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="补录原因" prop="reason">
          <el-input v-model="createForm.reason" type="textarea" :rows="3" placeholder="请输入补录原因" />
        </el-form-item>
        <el-form-item label="发货明细">
          <div class="delivery-detail-rows">
            <div
              v-for="(row, idx) in createForm.deliveryDetails"
              :key="idx"
              class="detail-row"
            >
              <el-input v-model="row.productSpec" placeholder="品规" style="width: 140px" />
              <el-input-number v-model="row.pieceCount" :min="1" placeholder="件数" style="width: 100px" />
              <el-input-number v-model="row.weight" :min="0" :precision="2" placeholder="重量" style="width: 100px" />
              <el-button type="danger" link size="small" @click="removeDetailRow(idx)">删除</el-button>
            </div>
            <el-button type="primary" link @click="addDetailRow">+ 添加一行</el-button>
          </div>
        </el-form-item>
        <el-form-item label="现场照片">
          <el-upload
            v-model:file-list="createForm.photoFiles"
            action="#"
            :auto-upload="false"
            list-type="picture-card"
            :limit="6"
            :before-upload="beforeUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog v-model="showApproveDialog" title="审核通过" width="400px" @close="resetApproveForm">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="备注">
          <el-input v-model="approveForm.comment" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApproveDialog = false">取消</el-button>
        <el-button type="primary" :loading="approveSubmitting" @click="submitApprove">确定</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="showRejectDialog" title="拒绝补录" width="400px" @close="resetRejectForm">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="拒绝原因" prop="comment">
          <el-input v-model="rejectForm.comment" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">确定拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="showViewDialog" title="补录详情" width="560px">
      <el-descriptions v-if="viewingItem" :column="1" border>
        <el-descriptions-item label="提货单号">{{ viewingItem.pickupOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="提交人">{{ viewingItem.submitter }}</el-descriptions-item>
        <el-descriptions-item label="原因">{{ viewingItem.reason }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ viewingItem.submittedAt }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(viewingItem.status)" size="small">{{ statusLabel(viewingItem.status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listPendingSupplements,
  createSupplement,
  approveSupplement,
  rejectSupplement,
  getSupplement,
  queryPickupOrders,
} from '@/api/evidence'

const loading = ref(false)
const tableData = ref<any[]>([])

// 新建
const showCreateDialog = ref(false)
const createSubmitting = ref(false)
const createFormRef = ref<FormInstance>()
const pickupOptions = ref<any[]>([])
const createForm = reactive({
  pickupOrderId: null as number | null,
  reason: '',
  deliveryDetails: [{ productSpec: '', pieceCount: 1, weight: 0 }] as { productSpec: string; pieceCount: number; weight: number }[],
  photoFiles: [] as any[],
})
const createRules: FormRules = {
  pickupOrderId: [{ required: true, message: '请选择提货单', trigger: 'change' }],
  reason: [{ required: true, message: '请输入补录原因', trigger: 'blur' }],
}

// 审核
const showApproveDialog = ref(false)
const approveSubmitting = ref(false)
const approvingItem = ref<any>(null)
const approveForm = reactive({ comment: '' })

// 拒绝
const showRejectDialog = ref(false)
const rejectSubmitting = ref(false)
const rejectFormRef = ref<FormInstance>()
const rejectingItem = ref<any>(null)
const rejectForm = reactive({ comment: '' })
const rejectRules: FormRules = {
  comment: [{ required: true, message: '请输入拒绝原因', trigger: 'blur' }],
}

// 查看
const showViewDialog = ref(false)
const viewingItem = ref<any>(null)

const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

function beforeUpload(file: File) {
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }
  return true
}

function statusTagType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    待审核: 'warning',
    已通过: 'success',
    已拒绝: 'danger',
  }
  return map[status ?? ''] ?? 'info'
}

function statusLabel(status: string) {
  return status || '待审核'
}

function addDetailRow() {
  createForm.deliveryDetails.push({ productSpec: '', pieceCount: 1, weight: 0 })
}

function removeDetailRow(idx: number) {
  createForm.deliveryDetails.splice(idx, 1)
}

async function loadData() {
  loading.value = true
  try {
    const res = (await listPendingSupplements()) as any
    const data = res?.data ?? res
    const list = Array.isArray(data) ? data : data?.list ?? data?.records ?? []
    tableData.value = list.map((r: any) => ({
      id: r.id,
      pickupOrderNo: r.pickupOrderNo ?? r.pickup_order_no ?? r.pickupOrderId ?? '-',
      submitter: r.submitter ?? r.submitterName ?? r.createdBy ?? '-',
      reason: r.reason ?? '-',
      submittedAt: r.submittedAt ?? r.createdAt ?? r.submitted_at ?? '-',
      status: r.status ?? '待审核',
    }))
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function loadPickupOptions() {
  try {
    const res = (await queryPickupOrders({ pageSize: 100 })) as any
    const data = res?.data ?? res
    const list = Array.isArray(data) ? data : data?.list ?? data?.records ?? []
    pickupOptions.value = list.map((r: any) => ({
      id: r.id,
      pickupOrderNo: r.pickupOrderNo ?? r.pickup_order_no,
      buyerName: r.buyerName ?? r.buyer_name,
    }))
  } catch {
    pickupOptions.value = []
  }
}

function openCreateDialog() {
  loadPickupOptions()
  showCreateDialog.value = true
}

function resetCreateForm() {
  createForm.pickupOrderId = null
  createForm.reason = ''
  createForm.deliveryDetails = [{ productSpec: '', pieceCount: 1, weight: 0 }]
  createForm.photoFiles = []
  createFormRef.value?.resetFields()
}

async function submitCreate() {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    createSubmitting.value = true
    try {
      const payload = {
        pickupOrderId: createForm.pickupOrderId,
        reason: createForm.reason,
        deliveryDetails: createForm.deliveryDetails.filter((d) => d.productSpec || d.weight > 0),
      }
      await createSupplement(payload)
      ElMessage.success('提交成功')
      showCreateDialog.value = false
      loadData()
    } catch {
      // error handled
    } finally {
      createSubmitting.value = false
    }
  })
}

function openApproveDialog(row: any) {
  approvingItem.value = row
  approveForm.comment = ''
  showApproveDialog.value = true
}

function resetApproveForm() {
  approvingItem.value = null
  approveForm.comment = ''
}

async function submitApprove() {
  if (!approvingItem.value) return
  approveSubmitting.value = true
  try {
    await approveSupplement(approvingItem.value.id, approveForm.comment || '')
    ElMessage.success('审核通过')
    showApproveDialog.value = false
    loadData()
  } catch {
    // error handled
  } finally {
    approveSubmitting.value = false
  }
}

function openRejectDialog(row: any) {
  rejectingItem.value = row
  rejectForm.comment = ''
  showRejectDialog.value = true
}

function resetRejectForm() {
  rejectingItem.value = null
  rejectForm.comment = ''
  rejectFormRef.value?.resetFields()
}

async function submitReject() {
  if (!rejectFormRef.value || !rejectingItem.value) return
  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return
    rejectSubmitting.value = true
    try {
      await rejectSupplement(rejectingItem.value.id, rejectForm.comment)
      ElMessage.success('已拒绝')
      showRejectDialog.value = false
      loadData()
    } catch {
      // error handled
    } finally {
      rejectSubmitting.value = false
    }
  })
}

async function viewDetail(row: any) {
  try {
    const res = (await getSupplement(row.id)) as any
    const data = res?.data ?? res ?? row
    viewingItem.value = {
      pickupOrderNo: data.pickupOrderNo ?? data.pickup_order_no ?? row.pickupOrderNo,
      submitter: data.submitter ?? data.submitterName ?? row.submitter,
      reason: data.reason ?? row.reason,
      submittedAt: data.submittedAt ?? data.createdAt ?? row.submittedAt,
      status: data.status ?? row.status,
    }
    showViewDialog.value = true
  } catch {
    viewingItem.value = row
    showViewDialog.value = true
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.supplement-manage {
  .action-bar {
    margin-bottom: 20px;
  }

  .delivery-detail-rows {
    .detail-row {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 12px;
    }
  }
}
</style>
