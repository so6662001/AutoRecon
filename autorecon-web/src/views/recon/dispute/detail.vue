<template>
  <div class="dispute-detail-page">
    <div class="page-header">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <div class="info-card">
      <h3 class="card-title">异议信息</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="对账单号">{{ dispute.billNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="(getStatusTagType(dispute.status) as 'success' | 'warning' | 'info' | 'danger')" size="small">{{ getStatusText(dispute.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="品名">{{ dispute.productName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ dispute.spec }}</el-descriptions-item>
        <el-descriptions-item label="异议类型">{{ getDisputeTypeText(dispute.disputeType) }}</el-descriptions-item>
        <el-descriptions-item label="提出方">{{ dispute.raisedBy }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ dispute.description }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="chat-section">
      <div class="chat-header">沟通记录</div>
      <div ref="messagesRef" class="messages-area">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ 'is-seller': msg.senderType === 'seller', 'is-buyer': msg.senderType === 'buyer' }"
        >
          <div class="message-bubble">
            <div class="message-sender">{{ msg.senderName }}</div>
            <div class="message-time">{{ msg.createdAt }}</div>
            <div v-if="msg.contentType === 'text'" class="message-content">{{ msg.content }}</div>
            <img v-else-if="msg.contentType === 'image'" :src="msg.content" class="message-image" alt="图片" />
            <div v-if="msg.read" class="message-read">已读</div>
          </div>
        </div>
        <div v-if="loading" class="loading-wrap">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="inputContent"
          type="textarea"
          :rows="3"
          placeholder="输入消息..."
          resize="none"
          @keydown.ctrl.enter="handleSend"
        />
        <div class="input-actions">
          <el-button text @click="handleAttach">
            <el-icon><Paperclip /></el-icon>
            附件
          </el-button>
          <el-button type="primary" :loading="sending" :disabled="!inputContent.trim()" @click="handleSend">
            发送
          </el-button>
        </div>
      </div>
    </div>

    <div class="action-buttons">
      <el-button
        v-if="!['RESOLVED', 'ESCALATED'].includes(dispute.status)"
        type="success"
        @click="showResolveDialog = true"
      >
        标记已解决
      </el-button>
      <el-button
        v-if="!['RESOLVED', 'ESCALATED'].includes(dispute.status)"
        type="warning"
        @click="handleEscalate"
      >
        升级处理
      </el-button>
    </div>

    <el-dialog v-model="showResolveDialog" title="标记已解决" width="500px" destroy-on-close @close="resolveInput = ''">
      <el-input
        v-model="resolveInput"
        type="textarea"
        :rows="4"
        placeholder="请输入解决方案描述"
      />
      <template #footer>
        <el-button @click="showResolveDialog = false">取消</el-button>
        <el-button type="primary" :loading="resolving" :disabled="!resolveInput.trim()" @click="handleResolve">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Paperclip, Loading } from '@element-plus/icons-vue'
import { getDisputeDetail, getDisputeMessages, sendDisputeMessage, resolveDispute } from '@/api/recon'

interface DisputeInfo {
  id: number
  billNo: string
  productName?: string
  spec?: string
  disputeType: string
  description?: string
  raisedBy?: string
  status: string
}

interface MessageItem {
  id: number
  senderType: string
  senderName: string
  contentType: string
  content: string
  createdAt: string
  read?: boolean
}

const route = useRoute()
const router = useRouter()
const disputeId = () => Number(route.params.id)

const dispute = reactive<DisputeInfo>({
  id: 0,
  billNo: '',
  status: '',
  disputeType: '',
})
const messages = ref<MessageItem[]>([])
const loading = ref(false)
const sending = ref(false)
const resolving = ref(false)
const inputContent = ref('')
const resolveInput = ref('')
const showResolveDialog = ref(false)
const messagesRef = ref<HTMLElement | null>(null)

function getStatusTagType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    PROCESSING: '',
    RESOLVED: 'success',
    ESCALATED: 'danger',
  }
  return map[status] ?? 'info'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    ESCALATED: '已升级',
  }
  return map[status] ?? status
}

function getDisputeTypeText(type: string): string {
  const map: Record<string, string> = {
    WEIGHT: '重量差异',
    AMOUNT: '金额差异',
    QUANTITY: '数量差异',
    OTHER: '其他',
  }
  return map[type] ?? type
}

function goBack() {
  router.push({ name: 'disputeList' })
}

async function fetchDispute() {
  try {
    const res = await getDisputeDetail(disputeId()) as DisputeInfo
    Object.assign(dispute, res)
  } catch {
    ElMessage.error('加载异议信息失败')
  }
}

async function fetchMessages() {
  loading.value = true
  try {
    const res = await getDisputeMessages(disputeId()) as MessageItem[]
    messages.value = Array.isArray(res) ? res : []
    await nextTick()
    scrollToBottom()
  } catch {
    messages.value = []
  } finally {
    loading.value = false
  }
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

function handleAttach() {
  ElMessage.info('附件功能开发中')
}

async function handleSend() {
  const content = inputContent.value.trim()
  if (!content) return
  sending.value = true
  try {
    await sendDisputeMessage(disputeId(), { contentType: 'text', content })
    inputContent.value = ''
    await fetchMessages()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

async function handleResolve() {
  const resolution = resolveInput.value.trim()
  if (!resolution) return
  resolving.value = true
  try {
    await resolveDispute(disputeId(), { resolution })
    ElMessage.success('已标记为已解决')
    showResolveDialog.value = false
    Object.assign(dispute, { status: 'RESOLVED' })
  } catch {
    ElMessage.error('操作失败')
  } finally {
    resolving.value = false
  }
}

function handleEscalate() {
  ElMessage.info('升级处理功能开发中')
}

onMounted(() => {
  fetchDispute()
  fetchMessages()
})
</script>

<style lang="scss" scoped>
.dispute-detail-page {
  .page-header {
    margin-bottom: 20px;
  }

  .info-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);

    .card-title {
      margin: 0 0 16px;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .chat-section {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);

    .chat-header {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 16px;
    }

    .messages-area {
      height: 400px;
      overflow-y: auto;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 8px;
      margin-bottom: 16px;
    }

    .message-item {
      display: flex;
      margin-bottom: 16px;

      &.is-seller {
        justify-content: flex-end;

        .message-bubble {
          background: #409eff;
          color: #fff;
          border-radius: 12px 12px 4px 12px;
        }

        .message-time,
        .message-read {
          color: rgba(255, 255, 255, 0.8);
        }
      }

      &.is-buyer {
        justify-content: flex-start;

        .message-bubble {
          background: #fff;
          color: #303133;
          border: 1px solid #e4e7ed;
          border-radius: 12px 12px 12px 4px;
        }

        .message-time,
        .message-read {
          color: #909399;
        }
      }
    }

    .message-bubble {
      max-width: 70%;
      padding: 10px 14px;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    }

    .message-sender {
      font-size: 12px;
      font-weight: 600;
      margin-bottom: 4px;
    }

    .message-time {
      font-size: 11px;
      margin-bottom: 6px;
    }

    .message-content {
      font-size: 14px;
      line-height: 1.5;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .message-image {
      max-width: 200px;
      max-height: 200px;
      border-radius: 4px;
    }

    .message-read {
      font-size: 11px;
      margin-top: 4px;
    }

    .loading-wrap {
      text-align: center;
      padding: 20px;
      color: #909399;
    }

    .input-area {
      .input-actions {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        gap: 12px;
        margin-top: 12px;
      }
    }
  }

  .action-buttons {
    display: flex;
    gap: 12px;
  }
}
</style>
