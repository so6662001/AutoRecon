<template>
  <el-dialog
    v-model="visible"
    :title="currentAgreement?.title || '协议确认'"
    width="700px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    append-to-body
  >
    <div v-if="currentAgreement" class="agreement-content">
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px">
        <template #title>
          {{ currentAgreement.summary || '协议已更新，请阅读并确认' }}
        </template>
      </el-alert>

      <div class="agreement-text" ref="scrollContainer" @scroll="onScroll">
        <div v-html="renderedContent" style="line-height: 1.8; font-size: 14px"></div>
      </div>

      <div style="margin-top: 16px">
        <el-checkbox v-model="agreed" :disabled="!hasScrolledToBottom">
          我已阅读并同意
          <strong>{{ currentAgreement.title }}</strong>
          ({{ currentAgreement.versionNo }})
        </el-checkbox>
        <p v-if="!hasScrolledToBottom" style="color: #999; font-size: 12px; margin-top: 4px">
          请先滚动阅读完整协议内容
        </p>
      </div>
    </div>

    <template #footer>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span style="color: #999; font-size: 12px">
          {{ currentIndex + 1 }} / {{ agreements.length }} 份协议需要确认
        </span>
        <el-button type="primary" :disabled="!agreed" :loading="confirming" @click="handleConfirm">
          确认并同意
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmAgreement } from '@/api/system'

interface AgreementItem {
  id: number
  agreementType: number
  versionNo: string
  title: string
  content: string
  summary: string
}

const props = defineProps<{
  modelValue: boolean
  agreements: AgreementItem[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  allConfirmed: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const currentIndex = ref(0)
const agreed = ref(false)
const confirming = ref(false)
const hasScrolledToBottom = ref(false)
const scrollContainer = ref<HTMLElement | null>(null)

const currentAgreement = computed(() => props.agreements[currentIndex.value])

const renderedContent = computed(() => {
  const content = currentAgreement.value?.content || ''
  return content
    .replace(/^### (.*$)/gm, '<h4>$1</h4>')
    .replace(/^## (.*$)/gm, '<h3>$1</h3>')
    .replace(/^# (.*$)/gm, '<h2>$1</h2>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
})

function updateScrollState() {
  const el = scrollContainer.value
  if (!el) return
  if (el.scrollHeight <= el.clientHeight + 2) {
    hasScrolledToBottom.value = true
  }
}

function onScroll(e: Event) {
  const el = e.target as HTMLElement
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 50) {
    hasScrolledToBottom.value = true
  }
}

async function handleConfirm() {
  if (!currentAgreement.value) return
  confirming.value = true
  try {
    await confirmAgreement({
      agreementVersionId: currentAgreement.value.id,
      agreementType: currentAgreement.value.agreementType,
    })

    if (currentIndex.value < props.agreements.length - 1) {
      currentIndex.value++
      agreed.value = false
      hasScrolledToBottom.value = false
      await nextTick()
      if (scrollContainer.value) {
        scrollContainer.value.scrollTop = 0
      }
      updateScrollState()
    } else {
      ElMessage.success('协议确认完成')
      emit('allConfirmed')
      visible.value = false
    }
  } catch {
    ElMessage.error('确认失败，请重试')
  } finally {
    confirming.value = false
  }
}

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      currentIndex.value = 0
      agreed.value = false
      hasScrolledToBottom.value = false
      void nextTick(() => {
        if (scrollContainer.value) {
          scrollContainer.value.scrollTop = 0
        }
        updateScrollState()
      })
    }
  }
)

watch(currentAgreement, () => {
  void nextTick(() => updateScrollState())
})
</script>

<style scoped>
.agreement-text {
  max-height: 400px;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}
</style>
