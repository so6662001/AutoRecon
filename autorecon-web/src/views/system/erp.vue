<template>
  <div class="erp-page">
    <h2 class="page-title">ERP配置</h2>

    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">添加连接</el-button>
    </div>

    <el-row v-loading="loading" :gutter="20" class="connection-grid">
      <el-col v-for="conn in connections" :key="conn.id" :xs="24" :sm="12" :lg="8">
        <el-card class="connection-card" shadow="hover">
          <div class="card-header">
            <span class="conn-name">{{ conn.name }}</span>
            <el-tag :type="getTypeTag(conn.type) as 'success' | 'primary' | 'warning' | 'info' | 'danger'" size="small">{{ getTypeText(conn.type) }}</el-tag>
            <span class="status-dot" :class="{ online: conn.status === 'online' }" />
          </div>
          <div class="conn-url">{{ conn.baseUrl || conn.endpoint || '-' }}</div>
          <div class="card-actions">
            <el-button type="primary" link size="small" @click="handleTest(conn)">测试连接</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(conn)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(conn)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑连接' : '添加连接'"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="连接名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入连接名称" />
        </el-form-item>
        <el-form-item label="连接类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%" @change="onTypeChange">
            <el-option label="REST API" value="REST" />
            <el-option label="WebService" value="WEBSERVICE" />
            <el-option label="数据库直连" value="DB" />
            <el-option label="文件上传" value="FILE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="addressLabel" prop="address">
          <el-input v-model="form.address" :placeholder="addressPlaceholder" />
        </el-form-item>
        <el-form-item label="认证方式" prop="authType">
          <el-select v-model="form.authType" placeholder="请选择" style="width: 100%">
            <el-option label="API Key" value="API_KEY" />
            <el-option label="OAuth2" value="OAUTH2" />
            <el-option label="用户名密码" value="BASIC" />
            <el-option label="证书" value="CERT" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证配置">
          <el-input v-model="form.authConfig" type="textarea" :rows="4" placeholder='JSON格式，如 {"apiKey":"xxx"}' />
        </el-form-item>
        <el-form-item label="字段映射">
          <el-input v-model="form.fieldMapping" type="textarea" :rows="4" placeholder='JSON格式，如 {"contractNo":"合同号"}' />
        </el-form-item>
        <el-form-item label="拉取策略" prop="pullStrategy">
          <el-radio-group v-model="form.pullStrategy">
            <el-radio label="REALTIME">实时</el-radio>
            <el-radio label="SCHEDULED">定时</el-radio>
            <el-radio label="MANUAL">手动</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.pullStrategy === 'SCHEDULED'" label="Cron表达式" prop="cron">
          <el-input v-model="form.cron" placeholder="如 0 0 2 * * ? 表示每天凌晨2点" />
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
  listErpConnections,
  createErpConnection,
  updateErpConnection,
  deleteErpConnection,
  testErpConnection,
} from '@/api/system'

interface ErpConnection {
  id: number
  name: string
  type: string
  status?: string
  baseUrl?: string
  endpoint?: string
}

const loading = ref(false)
const connections = ref<ErpConnection[]>([])
const formVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  type: 'REST',
  address: '',
  authType: 'API_KEY',
  authConfig: '{}',
  fieldMapping: '{}',
  pullStrategy: 'MANUAL' as 'REALTIME' | 'SCHEDULED' | 'MANUAL',
  cron: '',
})

const addressLabel = computed(() => {
  const map: Record<string, string> = {
    REST: '连接地址',
    WEBSERVICE: 'WebService URL',
    DB: '数据库连接串',
    FILE: '上传目录/接口',
  }
  return map[form.type] ?? '连接地址'
})

const addressPlaceholder = computed(() => {
  const map: Record<string, string> = {
    REST: 'https://api.example.com/erp',
    WEBSERVICE: 'https://api.example.com/ws',
    DB: 'jdbc:mysql://host:3306/db',
    FILE: '/upload/erp',
  }
  return map[form.type] ?? '请输入'
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入连接名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择连接类型', trigger: 'change' }],
  address: [{ required: true, message: '请输入连接地址', trigger: 'blur' }],
}

function getTypeTag(type: string) {
  const map: Record<string, string> = {
    REST: '',
    WEBSERVICE: 'success',
    DB: 'warning',
    FILE: 'info',
  }
  return map[type] ?? 'info'
}

function getTypeText(type: string) {
  const map: Record<string, string> = {
    REST: 'REST API',
    WEBSERVICE: 'WebService',
    DB: '数据库直连',
    FILE: '文件上传',
  }
  return map[type] ?? type
}

function onTypeChange() {
  form.address = ''
}

function handleAdd() {
  editingId.value = null
  formVisible.value = true
}

function handleEdit(conn: ErpConnection) {
  const c = conn as unknown as Record<string, string | number | boolean | object | null | undefined>
  editingId.value = conn.id
  form.name = conn.name
  form.type = (c.type as string) ?? 'REST'
  form.address = (c.baseUrl as string) ?? (c.endpoint as string) ?? ''
  form.authType = (c.authType as string) ?? 'API_KEY'
  form.authConfig = typeof c.authConfig === 'string'
    ? (c.authConfig as string)
    : JSON.stringify(c.authConfig ?? {}, null, 2)
  form.fieldMapping = typeof c.fieldMapping === 'string'
    ? (c.fieldMapping as string)
    : JSON.stringify(c.fieldMapping ?? {}, null, 2)
  form.pullStrategy = (c.pullStrategy as 'REALTIME' | 'SCHEDULED' | 'MANUAL') ?? 'MANUAL'
  form.cron = (c.cron as string) ?? ''
  formVisible.value = true
}

async function handleTest(conn: ErpConnection) {
  try {
    await testErpConnection(conn.id)
    ElMessage.success('连接测试成功')
  } catch {
    ElMessage.error('连接测试失败')
  }
}

async function handleDelete(conn: ErpConnection) {
  await ElMessageBox.confirm('确定要删除该连接吗？', '确认删除', { type: 'warning' })
  try {
    await deleteErpConnection(conn.id)
    ElMessage.success('已删除')
    fetchConnections()
  } catch {
    // error handled by interceptor
  }
}

function resetForm() {
  form.name = ''
  form.type = 'REST'
  form.address = ''
  form.authType = 'API_KEY'
  form.authConfig = '{}'
  form.fieldMapping = '{}'
  form.pullStrategy = 'MANUAL'
  form.cron = ''
  editingId.value = null
}

async function handleSubmit() {
  await formRef.value?.validate()
  try {
    const data: Record<string, unknown> = {
      name: form.name,
      type: form.type,
      authType: form.authType,
      pullStrategy: form.pullStrategy,
    }
    if (form.type === 'REST' || form.type === 'WEBSERVICE') {
      data.baseUrl = form.address
    } else if (form.type === 'DB') {
      data.connectionString = form.address
    } else {
      data.endpoint = form.address
    }
    try {
      data.authConfig = JSON.parse(form.authConfig || '{}')
    } catch {
      data.authConfig = {}
    }
    try {
      data.fieldMapping = JSON.parse(form.fieldMapping || '{}')
    } catch {
      data.fieldMapping = {}
    }
    if (form.pullStrategy === 'SCHEDULED') {
      data.cron = form.cron
    }
    if (editingId.value) {
      await updateErpConnection(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createErpConnection(data)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    fetchConnections()
  } catch (e) {
    if (e !== false) throw e
  }
}

async function fetchConnections() {
  loading.value = true
  try {
    const res = await listErpConnections() as ErpConnection[]
    connections.value = Array.isArray(res) ? res : []
  } catch {
    connections.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchConnections()
})
</script>

<style lang="scss" scoped>
.erp-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .action-bar {
    margin-bottom: 20px;
  }

  .connection-grid {
    margin-bottom: 20px;
  }

  .connection-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .conn-name {
        font-weight: 600;
        flex: 1;
      }

      .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #c0c4cc;

        &.online {
          background: #67c23a;
        }
      }
    }

    .conn-url {
      font-size: 12px;
      color: #909399;
      margin-bottom: 12px;
      word-break: break-all;
    }

    .card-actions {
      display: flex;
      gap: 8px;
    }
  }
}
</style>
