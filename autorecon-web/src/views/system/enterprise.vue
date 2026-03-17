<template>
  <div class="enterprise-page">
    <h2 class="page-title">企业信息</h2>

    <el-card class="info-card">
      <template #header>基本信息</template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="统一社会信用代码" prop="creditCode">
          <el-input v-model="form.creditCode" placeholder="请输入信用代码" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="企业LOGO">
          <el-upload
            class="logo-uploader"
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :before-upload="beforeUpload"
            @change="handleLogoChange"
          >
            <img v-if="form.logoUrl" :src="sanitizeUrl(form.logoUrl)" class="logo-preview" />
            <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveInfo">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="auth-card">
      <template #header>认证状态</template>
      <div v-if="authStatus === 'AUTHENTICATED'" class="auth-status authenticated">
        <el-tag type="success" size="large">已认证</el-tag>
        <p class="auth-detail">认证通过，可正常使用签章等高级功能</p>
        <p v-if="authDetails.legalPersonIdNo" class="auth-detail">法人身份证：{{ maskIdNo(authDetails.legalPersonIdNo) }}</p>
        <p v-if="authDetails.legalPersonPhone" class="auth-detail">法人手机：{{ maskPhone(authDetails.legalPersonPhone) }}</p>
      </div>
      <div v-else-if="authStatus === 'PENDING'" class="auth-status pending">
        <el-tag type="warning" size="large">认证中</el-tag>
        <p class="auth-detail">您的认证申请正在审核中，请耐心等待</p>
      </div>
      <div v-else class="auth-status not-auth">
        <el-tag type="info" size="large">未认证</el-tag>
        <p class="auth-detail">完成企业认证后可使用签章等高级功能</p>
        <el-button type="primary" @click="authFormVisible = true">立即认证</el-button>
      </div>
    </el-card>

    <el-dialog v-model="authFormVisible" title="企业认证" width="560px" @close="resetAuthForm">
      <el-form ref="authFormRef" :model="authForm" :rules="authRules" label-width="120px">
        <el-form-item label="企业全称" prop="enterpriseName">
          <el-input v-model="authForm.enterpriseName" placeholder="请输入企业全称" />
        </el-form-item>
        <el-form-item label="统一社会信用代码" prop="creditCode">
          <el-input v-model="authForm.creditCode" placeholder="请输入统一社会信用代码" />
        </el-form-item>
        <el-form-item label="法人姓名" prop="legalPerson">
          <el-input v-model="authForm.legalPerson" placeholder="请输入法人姓名" />
        </el-form-item>
        <el-form-item label="法人身份证号" prop="legalIdNo">
          <el-input v-model="authForm.legalIdNo" placeholder="请输入法人身份证号" />
        </el-form-item>
        <el-form-item label="法人手机号" prop="legalPhone">
          <el-input v-model="authForm.legalPhone" placeholder="请输入法人手机号" />
        </el-form-item>
        <el-form-item label="营业执照" prop="businessLicense">
          <el-upload action="#" :auto-upload="false" :limit="1" :before-upload="beforeUpload" @change="onAuthFileChange">
            <el-button type="primary" size="small">上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="法人身份证正面">
          <el-upload action="#" :auto-upload="false" :limit="1" :before-upload="beforeUpload">
            <el-button type="primary" size="small">上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="法人身份证反面">
          <el-upload action="#" :auto-upload="false" :limit="1" :before-upload="beforeUpload">
            <el-button type="primary" size="small">上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="授权委托书(可选)">
          <el-upload action="#" :auto-upload="false" :limit="1" :before-upload="beforeUpload">
            <el-button type="primary" size="small">上传</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="authFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="authSubmitting" @click="handleSubmitAuth">提交认证</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getEnterprise,
  updateEnterprise,
  submitAuth,
  getAuthStatus,
} from '@/api/system'
import { maskIdNo, maskPhone } from '@/utils/mask'
import { sanitizeUrl } from '@/utils/sanitize-url'

const enterpriseId = ref(1)
const formRef = ref<FormInstance>()
const authFormRef = ref<FormInstance>()
const authStatus = ref<'NONE' | 'PENDING' | 'AUTHENTICATED'>('NONE')
const authDetails = reactive<{ legalPersonIdNo?: string; legalPersonPhone?: string }>({})
const authFormVisible = ref(false)
const authSubmitting = ref(false)

const form = reactive({
  companyName: '',
  creditCode: '',
  contact: '',
  phone: '',
  email: '',
  logoUrl: '',
})

const rules: FormRules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
}

const authForm = reactive({
  enterpriseName: '',
  creditCode: '',
  legalPerson: '',
  legalIdNo: '',
  legalPhone: '',
  businessLicense: null as File | null,
})

const authRules: FormRules = {
  enterpriseName: [{ required: true, message: '请输入企业全称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  legalPerson: [{ required: true, message: '请输入法人姓名', trigger: 'blur' }],
  legalIdNo: [{ required: true, message: '请输入法人身份证号', trigger: 'blur' }],
  legalPhone: [{ required: true, message: '请输入法人手机号', trigger: 'blur' }],
}

function beforeUpload(file: File) {
  const maxSize = 10 * 1024 * 1024 // 10MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }
  return true
}

function onAuthFileChange(uploadFile: { raw?: File }) {
  const file = uploadFile?.raw
  if (file && !beforeUpload(file)) return
  authForm.businessLicense = file ?? null
}

function handleLogoChange(file: { raw?: File }) {
  const raw = file?.raw
  if (raw && beforeUpload(raw)) {
    form.logoUrl = URL.createObjectURL(raw)
  }
}

async function fetchEnterprise() {
  try {
    const res = await getEnterprise(enterpriseId.value) as Record<string, unknown>
    if (res) {
      form.companyName = (res.companyName as string) ?? ''
      form.creditCode = (res.creditCode as string) ?? ''
      form.contact = (res.contact as string) ?? ''
      form.phone = (res.phone as string) ?? ''
      form.email = (res.email as string) ?? ''
      form.logoUrl = (res.logoUrl as string) ?? ''
    }
  } catch {
    // use defaults
  }
}

async function fetchAuthStatus() {
  try {
    const res = await getAuthStatus(enterpriseId.value) as { status?: string; legalPersonIdNo?: string; legalPersonPhone?: string; legalIdNo?: string; legalPhone?: string }
    authStatus.value = (res?.status as 'NONE' | 'PENDING' | 'AUTHENTICATED') ?? 'NONE'
    authDetails.legalPersonIdNo = res?.legalPersonIdNo ?? res?.legalIdNo
    authDetails.legalPersonPhone = res?.legalPersonPhone ?? res?.legalPhone
  } catch {
    authStatus.value = 'NONE'
  }
}

async function handleSaveInfo() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  try {
    await updateEnterprise(enterpriseId.value, {
      companyName: form.companyName,
      creditCode: form.creditCode,
      contact: form.contact,
      phone: form.phone,
      email: form.email,
      logoUrl: form.logoUrl || undefined,
    })
    ElMessage.success('保存成功')
  } catch {
    // error handled by interceptor
  }
}

function resetAuthForm() {
  authForm.enterpriseName = ''
  authForm.creditCode = ''
  authForm.legalPerson = ''
  authForm.legalIdNo = ''
  authForm.legalPhone = ''
  authForm.businessLicense = null
}

async function handleSubmitAuth() {
  try {
    await authFormRef.value?.validate()
  } catch {
    return
  }
  authSubmitting.value = true
  try {
    await submitAuth(enterpriseId.value, {
      enterpriseName: authForm.enterpriseName,
      creditCode: authForm.creditCode,
      legalPerson: authForm.legalPerson,
      legalIdNo: authForm.legalIdNo,
      legalPhone: authForm.legalPhone,
    })
    ElMessage.success('认证申请已提交')
    authFormVisible.value = false
    fetchAuthStatus()
  } catch {
    // error handled by interceptor
  } finally {
    authSubmitting.value = false
  }
}

onMounted(() => {
  fetchEnterprise()
  fetchAuthStatus()
})
</script>

<style lang="scss" scoped>
.enterprise-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .info-card,
  .auth-card {
    margin-bottom: 20px;
  }

  .logo-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      width: 120px;
      height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        border-color: var(--el-color-primary);
      }
    }
  }

  .logo-preview {
    width: 120px;
    height: 120px;
    object-fit: contain;
  }

  .logo-uploader-icon {
    font-size: 28px;
    color: #8c939d;
  }

  .auth-status {
    padding: 20px;

    .auth-detail {
      margin: 12px 0;
      color: #606266;
    }
  }
}
</style>
