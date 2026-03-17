<template>
  <div class="page-container carrier-manage">
    <div class="action-bar">
      <el-button type="primary" @click="openEditDialog()">新增承运公司</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="name" label="公司名称" min-width="160" />
      <el-table-column prop="contact" label="联系人" width="120" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === '正常' ? 'success' : 'info'" size="small">{{ row.status || '正常' }}</el-tag>
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
      :title="editingId ? '编辑承运公司' : '新增承运公司'"
      width="450px"
      :close-on-click-modal="false"
      @close="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="公司名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="editForm.contact" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入电话" />
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
import { listCarriers, createCarrier, updateCarrier } from '@/api/evidence'

const loading = ref(false)
const submitting = ref(false)
const showEditDialog = ref(false)
const editingId = ref<number | null>(null)
const editFormRef = ref<FormInstance>()
const tableData = ref<any[]>([])

const editForm = reactive({
  name: '',
  contact: '',
  phone: '',
})

const editRules: FormRules = {
  name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = (await listCarriers()) as any
    const data = res?.data ?? res
    const list = Array.isArray(data) ? data : data?.list ?? data?.records ?? []
    tableData.value = list.map((r: any) => ({
      id: r.id,
      name: r.name ?? r.carrierName ?? '-',
      contact: r.contact ?? r.contactName ?? '-',
      phone: r.phone ?? r.contactPhone ?? '-',
      status: r.status ?? '正常',
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
    editForm.contact = row.contact
    editForm.phone = row.phone
  }
  showEditDialog.value = true
}

function resetEditForm() {
  editingId.value = null
  editForm.name = ''
  editForm.contact = ''
  editForm.phone = ''
  editFormRef.value?.resetFields()
}

async function submitEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        carrierName: editForm.name,
        contact: editForm.contact,
        phone: editForm.phone,
      }
      if (editingId.value) {
        await updateCarrier(editingId.value, payload)
        ElMessage.success('更新成功')
      } else {
        await createCarrier(payload)
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
.carrier-manage {
  .action-bar {
    margin-bottom: 20px;
  }
}
</style>
