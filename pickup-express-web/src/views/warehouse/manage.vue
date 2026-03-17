<template>
  <div class="page-container warehouse-manage">
    <div class="action-bar">
      <el-button type="primary" @click="openEditDialog()">新增仓库</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="name" label="仓库名称" min-width="120" />
      <el-table-column prop="code" label="编码" width="120" />
      <el-table-column prop="address" label="地址" min-width="180" />
      <el-table-column prop="contact" label="联系人" width="100" />
      <el-table-column prop="phone" label="电话" width="120" />
      <el-table-column label="默认发货模式" width="130">
        <template #default="{ row }">
          <el-tag size="small">{{ deliveryModeLabel(row.deliveryMode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否有WMS" width="110">
        <template #default="{ row }">
          <el-tag :type="row.hasWms ? 'success' : 'info'" size="small">{{ row.hasWms ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showEditDialog"
      :title="editingId ? '编辑仓库' : '新增仓库'"
      width="500px"
      :close-on-click-modal="false"
      @close="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="120px">
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入仓库名称" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="editForm.code" placeholder="请输入编码" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="editForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="editForm.contact" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="GPS坐标" prop="gps">
          <el-input v-model="editForm.gps" placeholder="经度,纬度" />
        </el-form-item>
        <el-form-item label="默认发货模式" prop="deliveryMode">
          <el-select v-model="editForm.deliveryMode" placeholder="请选择" style="width: 100%">
            <el-option label="WMS" value="WMS" />
            <el-option label="H5助手" value="H5助手" />
            <el-option label="第三方" value="第三方" />
            <el-option label="驾驶员确认" value="驾驶员确认" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否有WMS" prop="hasWms">
          <el-switch v-model="editForm.hasWms" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { listWarehouses, createWarehouse, updateWarehouse } from '@/api/evidence'

const loading = ref(false)
const submitting = ref(false)
const showEditDialog = ref(false)
const editingId = ref<number | null>(null)
const editFormRef = ref<FormInstance>()
const tableData = ref<any[]>([])

const editForm = reactive({
  name: '',
  code: '',
  address: '',
  contact: '',
  phone: '',
  gps: '',
  deliveryMode: 'WMS',
  hasWms: true,
})

const editRules: FormRules = {
  name: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
}

function deliveryModeLabel(mode: string) {
  const map: Record<string, string> = {
    WMS: 'WMS',
    H5助手: 'H5助手',
    第三方: '第三方',
    驾驶员确认: '驾驶员确认',
  }
  return map[mode] ?? mode ?? '-'
}

async function loadData() {
  loading.value = true
  try {
    const res = (await listWarehouses()) as any
    const data = res?.data ?? res
    const list = Array.isArray(data) ? data : data?.list ?? data?.records ?? []
    tableData.value = list.map((r: any) => ({
      id: r.id,
      name: r.name ?? r.warehouseName ?? '-',
      code: r.code ?? r.warehouseCode ?? '-',
      address: r.address ?? '-',
      contact: r.contact ?? r.contactName ?? '-',
      phone: r.phone ?? r.contactPhone ?? '-',
      deliveryMode: r.deliveryMode ?? r.delivery_mode ?? 'WMS',
      hasWms: r.hasWms ?? r.has_wms ?? false,
    }))
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function openEditDialog(row?: any) {
  editingId.value = row?.id ?? null
  if (row) {
    editForm.name = row.name
    editForm.code = row.code
    editForm.address = row.address
    editForm.contact = row.contact
    editForm.phone = row.phone
    editForm.gps = row.gps ?? ''
    editForm.deliveryMode = row.deliveryMode ?? 'WMS'
    editForm.hasWms = row.hasWms ?? false
  }
  showEditDialog.value = true
}

function resetEditForm() {
  editingId.value = null
  editForm.name = ''
  editForm.code = ''
  editForm.address = ''
  editForm.contact = ''
  editForm.phone = ''
  editForm.gps = ''
  editForm.deliveryMode = 'WMS'
  editForm.hasWms = true
  editFormRef.value?.resetFields()
}

async function submitEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        name: editForm.name,
        code: editForm.code,
        address: editForm.address,
        contact: editForm.contact,
        phone: editForm.phone,
        gps: editForm.gps || undefined,
        deliveryMode: editForm.deliveryMode,
        hasWms: editForm.hasWms,
      }
      if (editingId.value) {
        await updateWarehouse(editingId.value, payload)
        ElMessage.success('更新成功')
      } else {
        await createWarehouse(payload)
        ElMessage.success('新增成功')
      }
      showEditDialog.value = false
      loadData()
    } catch {
      // error handled
    } finally {
      submitting.value = false
    }
  })
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.warehouse-manage {
  .action-bar {
    margin-bottom: 20px;
  }
}
</style>
