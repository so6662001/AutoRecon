<template>
  <div class="page-container authorized-person">
    <div class="action-bar">
      <el-button type="primary" @click="openAddDialog">新增授权提货人</el-button>
      <el-input
        v-model="searchForm.buyerName"
        placeholder="搜索客户"
        clearable
        style="width: 200px; margin-left: 16px"
        @keyup.enter="loadData"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="name" label="提货人姓名" width="120" />
      <el-table-column prop="phone" label="手机号" width="130">
        <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
      </el-table-column>
      <el-table-column prop="idCard" label="身份证号" width="180">
        <template #default="{ row }">{{ maskIdCard(row.idCard) }}</template>
      </el-table-column>
      <el-table-column prop="plateNo" label="车牌号" width="100" />
      <el-table-column label="适用合同" width="100">
        <template #default="{ row }">{{ row.contractScope === 'all' ? '全部' : '指定' }}</template>
      </el-table-column>
      <el-table-column label="登记方式" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ registerMethodLabel(row.registerMethod) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="客户确认" width="100">
        <template #default="{ row }">
          <el-tag :type="row.customerConfirmed ? 'success' : 'info'" size="small">
            {{ row.customerConfirmed ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expireAt" label="有效期" width="120">
        <template #default="{ row }">{{ formatDate(row.expireAt) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '正常' ? 'success' : 'info'" size="small">{{ row.status || '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === '正常'"
            type="warning"
            link
            size="small"
            @click="handleDisable(row)"
          >
            停用
          </el-button>
          <el-button
            v-if="!row.customerConfirmed"
            type="primary"
            link
            size="small"
            @click="handleConfirm(row)"
          >
            确认
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showAddDialog"
      title="新增授权提货人"
      width="500px"
      :close-on-click-modal="false"
      @close="resetAddForm"
    >
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="客户" prop="buyerId">
          <el-select
            v-model="addForm.buyerId"
            filterable
            remote
            placeholder="请选择客户"
            :remote-method="searchBuyers"
            :loading="buyerSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="b in buyerOptions"
              :key="b.id"
              :label="b.name"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="合同" prop="contractId">
          <el-select v-model="addForm.contractId" placeholder="不选则适用全部" clearable style="width: 100%">
            <el-option
              v-for="c in contractOptions"
              :key="c.id"
              :label="c.contractNo"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="addForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="addForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="addForm.plateNo" placeholder="请输入车牌号" />
        </el-form-item>
        <el-form-item label="最大提货量" prop="maxWeight">
          <el-input v-model.number="addForm.maxWeight" placeholder="吨" type="number" />
        </el-form-item>
        <el-form-item label="有效期" prop="expireAt">
          <el-date-picker
            v-model="addForm.expireAt"
            type="date"
            placeholder="选择有效期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import dayjs from 'dayjs'
import {
  listPickupPersons,
  registerPickupPerson,
  confirmPickupPerson,
  disablePickupPerson,
  queryBuyers,
  queryContracts,
} from '@/api/evidence'

const loading = ref(false)
const submitting = ref(false)
const buyerSearchLoading = ref(false)
const showAddDialog = ref(false)
const addFormRef = ref<FormInstance>()
const tableData = ref<any[]>([])
const buyerOptions = ref<Array<{ id: number; name: string }>>([])
const contractOptions = ref<Array<{ id: number; contractNo: string }>>([])

const searchForm = reactive({ buyerName: '' })

const addForm = reactive({
  buyerId: undefined as number | undefined,
  contractId: undefined as number | undefined,
  name: '',
  phone: '',
  idCard: '',
  plateNo: '',
  maxWeight: undefined as number | undefined,
  expireAt: '',
})

const addRules: FormRules = {
  buyerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
}

function maskPhone(phone: string) {
  if (!phone) return '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

function maskIdCard(id: string) {
  if (!id) return '-'
  return id.replace(/(\d{6})\d{8}(\d{4})/, '$1********$2')
}

function formatDate(val: string | undefined) {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD')
}

function registerMethodLabel(m: string) {
  const map: Record<string, string> = { customer: '客户', sales: '销售', warehouse: '仓库' }
  return map[m] ?? m ?? '-'
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {}
    if (searchForm.buyerName) params.buyerName = searchForm.buyerName
    const res = (await listPickupPersons(params)) as any
    const data = res?.data ?? res
    const list = Array.isArray(data) ? data : data?.list ?? data?.records ?? []
    tableData.value = list.map((r: any) => ({
      id: r.id,
      name: r.name ?? r.pickerName ?? '-',
      phone: r.phone ?? r.mobile ?? '-',
      idCard: r.idCard ?? r.id_card ?? '-',
      plateNo: r.plateNo ?? r.plate_no ?? '-',
      contractScope: r.contractId ? 'specified' : 'all',
      registerMethod: r.registerMethod ?? r.register_method ?? 'customer',
      customerConfirmed: r.customerConfirmed ?? r.customer_confirmed ?? false,
      expireAt: r.expireAt ?? r.expire_at,
      status: r.status ?? '正常',
    }))
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function searchBuyers(query: string) {
  buyerSearchLoading.value = true
  try {
    const res = (await queryBuyers({ name: query || undefined })) as any
    const data = res?.data ?? res
    const list = data?.list ?? data?.records ?? data ?? []
    buyerOptions.value = Array.isArray(list)
      ? list.map((b: any) => ({ id: b.id, name: b.name ?? b.buyerName ?? '-' }))
      : []
  } catch {
    buyerOptions.value = []
  } finally {
    buyerSearchLoading.value = false
  }
}

async function loadContracts() {
  if (!addForm.buyerId) return
  try {
    const res = (await queryContracts({ buyerId: addForm.buyerId, pageNum: 1, pageSize: 100 })) as any
    const data = res?.data ?? res
    const list = data?.records ?? data?.list ?? []
    contractOptions.value = list.map((c: any) => ({ id: c.id, contractNo: c.contractNo ?? c.contract_no }))
  } catch {
    contractOptions.value = []
  }
}

function openAddDialog() {
  showAddDialog.value = true
  searchBuyers('')
}

function resetAddForm() {
  addForm.buyerId = undefined
  addForm.contractId = undefined
  addForm.name = ''
  addForm.phone = ''
  addForm.idCard = ''
  addForm.plateNo = ''
  addForm.maxWeight = undefined
  addForm.expireAt = ''
  addFormRef.value?.resetFields()
}

async function submitAdd() {
  if (!addFormRef.value) return
  await addFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await registerPickupPerson({
        buyerId: addForm.buyerId,
        contractId: addForm.contractId,
        name: addForm.name,
        phone: addForm.phone,
        idCard: addForm.idCard,
        plateNo: addForm.plateNo,
        maxWeight: addForm.maxWeight,
        expireAt: addForm.expireAt,
      })
      ElMessage.success('新增成功')
      showAddDialog.value = false
      loadData()
    } catch {
      // error handled
    } finally {
      submitting.value = false
    }
  })
}

async function handleConfirm(row: any) {
  try {
    await confirmPickupPerson(row.id)
    ElMessage.success('已确认')
    loadData()
  } catch {
    // error handled
  }
}

async function handleDisable(row: any) {
  try {
    await ElMessageBox.confirm('确定停用该提货人？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await disablePickupPerson(row.id)
    ElMessage.success('已停用')
    loadData()
  } catch (e) {
    if (e !== 'cancel') return
  }
}

watch(
  () => addForm.buyerId,
  (val) => {
    if (val) loadContracts()
    else contractOptions.value = []
  }
)

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.authorized-person {
  .action-bar {
    margin-bottom: 20px;
  }
}
</style>
