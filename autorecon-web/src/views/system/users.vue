<template>
  <div class="users-page">
    <h2 class="page-title">用户管理</h2>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">添加用户</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100" />
      <el-table-column prop="phone" label="手机号" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="(getRoleTagType(row.role) || 'info') as 'success' | 'primary' | 'warning' | 'info' | 'danger'" size="small">{{ getRoleText(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="(row.status === 'ACTIVE' ? 'success' : 'info') as 'success' | 'primary' | 'warning' | 'info' | 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '正常' : '停用' }}
          </el-tag>
          <el-switch
            :model-value="row.status === 'ACTIVE'"
            @update:model-value="(v) => handleToggleStatus(row, !!v)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最后登录" width="160" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'ACTIVE'"
            type="warning"
            link
            size="small"
            @click="handleDisable(row)"
          >
            停用
          </el-button>
          <el-button
            v-else
            type="success"
            link
            size="small"
            @click="handleEnable(row)"
          >
            启用
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchUsers"
        @current-change="fetchUsers"
      />
    </div>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑用户' : '添加用户'"
      width="500px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item v-if="!editingId" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
          <div v-if="form.password" class="password-strength">
            <el-progress
              :percentage="passwordStrength"
              :stroke-width="6"
              :color="strengthColor"
            />
            <span class="strength-text">{{ strengthText }}</span>
          </div>
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="对账员" value="RECON" />
            <el-option label="财务" value="FINANCE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listUsers,
  createUser,
  updateUser,
  disableUser,
  enableUser,
} from '@/api/system'

interface UserItem {
  id: number
  username: string
  realName?: string
  phone?: string
  email?: string
  role: string
  status: string
  lastLoginAt?: string
}

const loading = ref(false)
const tableData = ref<UserItem[]>([])
const formVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  role: 'RECON',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const passwordStrength = computed(() => {
  const p = form.password
  if (!p) return 0
  let score = 0
  if (p.length >= 6) score += 25
  if (p.length >= 10) score += 15
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score += 25
  if (/\d/.test(p)) score += 20
  if (/[^a-zA-Z0-9]/.test(p)) score += 15
  return Math.min(100, score)
})

const strengthColor = computed(() => {
  const s = passwordStrength.value
  if (s < 40) return '#f56c6c'
  if (s < 70) return '#e6a23c'
  return '#67c23a'
})

const strengthText = computed(() => {
  const s = passwordStrength.value
  if (s < 40) return '弱'
  if (s < 70) return '中'
  return '强'
})

function getRoleTagType(role: string) {
  const map: Record<string, string> = {
    ADMIN: 'danger',
    RECON: '',
    FINANCE: 'success',
  }
  return map[role] ?? 'info'
}

function getRoleText(role: string) {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    RECON: '对账员',
    FINANCE: '财务',
  }
  return map[role] ?? role
}

function handleAdd() {
  editingId.value = null
  formVisible.value = true
}

function handleEdit(row: UserItem) {
  editingId.value = row.id
  form.username = row.username
  form.password = ''
  form.realName = row.realName ?? ''
  form.phone = row.phone ?? ''
  form.email = row.email ?? ''
  form.role = row.role ?? 'RECON'
  formVisible.value = true
}

async function handleToggleStatus(row: UserItem, active: boolean) {
  try {
    if (active) {
      await enableUser(row.id)
      ElMessage.success('已启用')
    } else {
      await disableUser(row.id)
      ElMessage.success('已停用')
    }
    fetchUsers()
  } catch {
    // error handled by interceptor
  }
}

async function handleDisable(row: UserItem) {
  await ElMessageBox.confirm('确定要停用该用户吗？', '确认停用', { type: 'warning' })
  try {
    await disableUser(row.id)
    ElMessage.success('已停用')
    fetchUsers()
  } catch {
    // error handled by interceptor
  }
}

async function handleEnable(row: UserItem) {
  try {
    await enableUser(row.id)
    ElMessage.success('已启用')
    fetchUsers()
  } catch {
    // error handled by interceptor
  }
}

function resetForm() {
  form.username = ''
  form.password = ''
  form.realName = ''
  form.phone = ''
  form.email = ''
  form.role = 'RECON'
  editingId.value = null
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  try {
    const data: Record<string, unknown> = {
      realName: form.realName,
      phone: form.phone || undefined,
      email: form.email || undefined,
      role: form.role,
    }
    if (!editingId.value) {
      data.username = form.username
      data.password = form.password
    }
    if (editingId.value) {
      await updateUser(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createUser(data)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    fetchUsers()
  } catch (e) {
    if (e !== false) throw e
  }
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await listUsers({
      page: pagination.page,
      pageSize: pagination.pageSize,
    }) as { list?: UserItem[]; total?: number }
    tableData.value = res?.list ?? []
    pagination.total = res?.total ?? 0
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style lang="scss" scoped>
.users-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .action-bar {
    margin-bottom: 16px;
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .password-strength {
    margin-top: 8px;

    .strength-text {
      font-size: 12px;
      margin-left: 8px;
    }
  }

  :deep(.el-table .el-switch) {
    margin-left: 8px;
  }
}
</style>
