<template>
  <div class="page-container">
    <div class="action-bar">
      <el-button type="primary" @click="showDispatchDialog = true">新建派车</el-button>
    </div>

    <!-- 派车申请 Dialog -->
    <el-dialog
      v-model="showDispatchDialog"
      title="派车申请"
      width="520px"
      :close-on-click-modal="false"
      @close="resetDispatchForm"
    >
      <el-form ref="dispatchFormRef" :model="dispatchForm" :rules="dispatchRules" label-width="120px">
        <el-form-item label="合同选择" prop="contractId">
          <el-select
            v-model="dispatchForm.contractId"
            filterable
            remote
            placeholder="搜索合同号"
            :remote-method="searchContracts"
            :loading="contractSearchLoading"
            style="width: 100%"
            @focus="loadContractsForSelect"
          >
            <el-option
              v-for="c in contractOptions"
              :key="c.id"
              :label="`${c.contractNo} - ${c.buyerName || ''}`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="派车模式" prop="dispatchMode">
          <el-radio-group v-model="dispatchForm.dispatchMode">
            <el-radio :value="1">客户派车</el-radio>
            <el-radio :value="2">销售派车</el-radio>
            <el-radio :value="3">承运公司</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="dispatchForm.dispatchMode === 1 || dispatchForm.dispatchMode === 2">
          <el-form-item label="车牌号" prop="vehiclePlate">
            <el-input v-model="dispatchForm.vehiclePlate" placeholder="请输入车牌号" />
          </el-form-item>
          <el-form-item label="驾驶员姓名" prop="driverName">
            <el-input v-model="dispatchForm.driverName" placeholder="请输入驾驶员姓名" />
          </el-form-item>
          <el-form-item label="驾驶员电话" prop="driverPhone">
            <el-input v-model="dispatchForm.driverPhone" placeholder="请输入驾驶员电话" />
          </el-form-item>
          <el-form-item label="预计到达时间" prop="expectedArrivalAt">
            <el-date-picker
              v-model="dispatchForm.expectedArrivalAt"
              type="datetime"
              placeholder="选择预计到达时间"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </template>
        <template v-if="dispatchForm.dispatchMode === 3">
          <el-form-item label="承运公司" prop="carrierId">
            <el-select
              v-model="dispatchForm.carrierId"
              placeholder="请选择承运公司"
              style="width: 100%"
              @focus="loadCarriers"
            >
              <el-option
                v-for="c in carrierOptions"
                :key="c.id"
                :label="c.carrierName"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="showDispatchDialog = false">取消</el-button>
        <el-button type="primary" :loading="dispatchSubmitting" @click="submitDispatch">确定</el-button>
      </template>
    </el-dialog>

    <!-- 待处理列表 -->
    <div class="section">
      <h3>待处理列表</h3>
      <el-table :data="pendingList" v-loading="loadingPending" stripe>
        <el-table-column prop="pickupNo" label="提货单号" width="120" />
        <el-table-column prop="contractNo" label="合同号" width="120" />
        <el-table-column prop="buyerName" label="客户" />
        <el-table-column label="派车模式" width="100">
          <template #default="{ row }">
            {{ dispatchModeLabel(row.dispatchMode) }}
          </template>
        </el-table-column>
        <el-table-column prop="vehiclePlate" label="车牌" width="100" />
        <el-table-column prop="driverName" label="驾驶员" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.dispatchMode === 3" type="info" size="small">待分配</el-tag>
            <el-tag v-else type="warning" size="small">待确认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.dispatchMode === 2">
              <el-button type="primary" link size="small" @click="handleConfirm(row, true)">确认</el-button>
              <el-button type="danger" link size="small" @click="handleConfirm(row, false)">拒绝</el-button>
            </template>
            <el-button v-else-if="row.dispatchMode === 3" type="primary" size="small" @click="openAssignDialog(row)">
              分配驾驶员
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分配驾驶员 Dialog -->
    <el-dialog
      v-model="showAssignDialog"
      title="分配驾驶员"
      width="400px"
      :close-on-click-modal="false"
      @close="resetAssignForm"
    >
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="100px">
        <el-form-item label="驾驶员姓名" prop="driverName">
          <el-input v-model="assignForm.driverName" placeholder="请输入驾驶员姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="driverPhone">
          <el-input v-model="assignForm.driverPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="车牌号" prop="vehiclePlate">
          <el-input v-model="assignForm.vehiclePlate" placeholder="请输入车牌号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAssignDialog = false">取消</el-button>
        <el-button type="primary" :loading="assignSubmitting" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  requestDispatch,
  confirmDispatch,
  assignDriver,
  queryContracts,
  queryPickupOrders,
  listCarriers,
} from '@/api/evidence'

const showDispatchDialog = ref(false)
const showAssignDialog = ref(false)
const dispatchFormRef = ref<FormInstance>()
const assignFormRef = ref<FormInstance>()
const contractSearchLoading = ref(false)
const dispatchSubmitting = ref(false)
const assignSubmitting = ref(false)
const loadingPending = ref(false)

const contractOptions = ref<Array<{ id: number; contractNo: string; buyerName?: string }>>([])
const carrierOptions = ref<Array<{ id: number; carrierName: string }>>([])
const pendingList = ref<any[]>([])
const currentAssignRow = ref<any>(null)

function dispatchModeLabel(mode: number) {
  const map: Record<number, string> = { 1: '客户派车', 2: '销售派车', 3: '承运公司' }
  return map[mode] ?? '-'
}

const dispatchForm = reactive({
  contractId: undefined as number | undefined,
  dispatchMode: 1,
  vehiclePlate: '',
  driverName: '',
  driverPhone: '',
  carrierId: undefined as number | undefined,
  carrierName: '',
  expectedArrivalAt: '',
})

const dispatchRules: FormRules = {
  contractId: [{ required: true, message: '请选择合同', trigger: 'change' }],
  dispatchMode: [{ required: true, message: '请选择派车方式', trigger: 'change' }],
  vehiclePlate: [
    {
      required: true,
      message: '请输入车牌号',
      trigger: 'blur',
      validator: (_: any, __: string, cb: (e?: Error) => void) => {
        if (dispatchForm.dispatchMode === 3) return cb()
        if (!dispatchForm.vehiclePlate?.trim()) return cb(new Error('请输入车牌号'))
        cb()
      },
    },
  ],
  driverName: [
    {
      required: true,
      message: '请输入驾驶员姓名',
      trigger: 'blur',
      validator: (_: any, __: string, cb: (e?: Error) => void) => {
        if (dispatchForm.dispatchMode === 3) return cb()
        if (!dispatchForm.driverName?.trim()) return cb(new Error('请输入驾驶员姓名'))
        cb()
      },
    },
  ],
  driverPhone: [
    {
      required: true,
      message: '请输入驾驶员电话',
      trigger: 'blur',
      validator: (_: any, __: string, cb: (e?: Error) => void) => {
        if (dispatchForm.dispatchMode === 3) return cb()
        if (!dispatchForm.driverPhone?.trim()) return cb(new Error('请输入驾驶员电话'))
        cb()
      },
    },
  ],
  carrierId: [
    {
      required: true,
      message: '请选择承运公司',
      trigger: 'change',
      validator: (_: any, __: string, cb: (e?: Error) => void) => {
        if (dispatchForm.dispatchMode !== 3) return cb()
        if (!dispatchForm.carrierId) return cb(new Error('请选择承运公司'))
        cb()
      },
    },
  ],
}

const assignForm = reactive({
  pickupOrderId: 0,
  driverName: '',
  driverPhone: '',
  vehiclePlate: '',
})

const assignRules: FormRules = {
  driverName: [{ required: true, message: '请输入驾驶员姓名', trigger: 'blur' }],
  driverPhone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
}

async function loadContractsForSelect() {
  if (contractOptions.value.length) return
  await searchContracts('')
}

async function searchContracts(contractNo: string) {
  contractSearchLoading.value = true
  try {
    const res: any = await queryContracts({
      contractNo: contractNo || undefined,
      status: [0, 2, 3],
      pageNum: 1,
      pageSize: 50,
    })
    const data = res?.data ?? res
    const records = data?.records ?? []
    contractOptions.value = records.filter((c: any) => [0, 2, 3].includes(c.status))
  } catch {
    contractOptions.value = []
  } finally {
    contractSearchLoading.value = false
  }
}

async function loadCarriers() {
  if (carrierOptions.value.length) return
  try {
    const res: any = await listCarriers()
    const data = res?.data ?? res
    carrierOptions.value = Array.isArray(data) ? data : data?.list ?? []
  } catch {
    carrierOptions.value = []
  }
}

async function submitDispatch() {
  if (!dispatchFormRef.value) return
  await dispatchFormRef.value.validate(async (valid) => {
    if (!valid) return
    dispatchSubmitting.value = true
    try {
      const payload: any = {
        contractId: dispatchForm.contractId,
        dispatchMode: dispatchForm.dispatchMode,
      }
      if (dispatchForm.dispatchMode === 3) {
        payload.carrierId = dispatchForm.carrierId
        const carrier = carrierOptions.value.find((c) => c.id === dispatchForm.carrierId)
        payload.carrierName = carrier?.carrierName ?? ''
      } else {
        payload.vehiclePlate = dispatchForm.vehiclePlate
        payload.driverName = dispatchForm.driverName
        payload.driverPhone = dispatchForm.driverPhone
        payload.expectedArrivalAt = dispatchForm.expectedArrivalAt || undefined
      }
      const res: any = await requestDispatch(payload)
      const id = res?.data ?? res
      ElMessage.success(`派车申请成功，提货单号: ${id}`)
      showDispatchDialog.value = false
      loadPendingLists()
    } catch {
      // error handled by interceptor
    } finally {
      dispatchSubmitting.value = false
    }
  })
}

function resetDispatchForm() {
  dispatchForm.contractId = undefined
  dispatchForm.dispatchMode = 1
  dispatchForm.vehiclePlate = ''
  dispatchForm.driverName = ''
  dispatchForm.driverPhone = ''
  dispatchForm.carrierId = undefined
  dispatchForm.expectedArrivalAt = ''
  dispatchFormRef.value?.resetFields()
}

async function loadPendingLists() {
  loadingPending.value = true
  try {
    const res: any = await queryPickupOrders({ status: 1, pageNum: 1, pageSize: 100 })
    const data = res?.data ?? res
    const records = data?.records ?? []
    pendingList.value = records.filter((r: any) => {
      if (r.dispatchMode === 3) return r.driverAssigned !== 1
      return r.dispatchMode === 2 && r.customerConfirmed !== 1
    })
  } catch {
    pendingList.value = []
  } finally {
    loadingPending.value = false
  }
}

async function handleConfirm(row: any, confirmed: boolean) {
  try {
    await confirmDispatch(row.id, confirmed)
    ElMessage.success(confirmed ? '已确认' : '已拒绝')
    loadPendingLists()
  } catch {
    // error handled by interceptor
  }
}

function openAssignDialog(row: any) {
  currentAssignRow.value = row
  assignForm.pickupOrderId = row.id
  assignForm.driverName = ''
  assignForm.driverPhone = ''
  assignForm.vehiclePlate = ''
  showAssignDialog.value = true
}

function resetAssignForm() {
  currentAssignRow.value = null
  assignForm.pickupOrderId = 0
  assignForm.driverName = ''
  assignForm.driverPhone = ''
  assignForm.vehiclePlate = ''
  assignFormRef.value?.resetFields()
}

async function submitAssign() {
  if (!assignFormRef.value) return
  await assignFormRef.value.validate(async (valid) => {
    if (!valid) return
    assignSubmitting.value = true
    try {
      await assignDriver({
        pickupOrderId: assignForm.pickupOrderId,
        driverName: assignForm.driverName,
        driverPhone: assignForm.driverPhone,
        vehiclePlate: assignForm.vehiclePlate,
      })
      ElMessage.success('分配成功')
      showAssignDialog.value = false
      loadPendingLists()
    } catch {
      // error handled
    } finally {
      assignSubmitting.value = false
    }
  })
}

onMounted(() => {
  loadPendingLists()
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 0;
}

.action-bar {
  margin-bottom: 20px;
}

.section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;

  h3 {
    margin: 0 0 16px;
    font-size: 16px;
    font-weight: 600;
  }
}
</style>
