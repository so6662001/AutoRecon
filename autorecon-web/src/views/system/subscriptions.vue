<template>
  <div class="subscriptions-page">
    <h2 class="page-title">提醒订阅</h2>

    <el-card class="config-card">
      <template #header>通知方式</template>
      <el-table :data="subscriptionRows" border size="small">
        <el-table-column prop="eventType" label="事件类型" width="180" />
        <el-table-column prop="inApp" label="站内信" width="80" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.inApp" />
          </template>
        </el-table-column>
        <el-table-column prop="sms" label="短信" width="80" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.sms" />
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮件" width="80" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.email" />
          </template>
        </el-table-column>
        <el-table-column prop="im" label="企微/钉钉" width="100" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.im" />
          </template>
        </el-table-column>
        <el-table-column prop="frequency" label="频率" width="140">
          <template #default="{ row }">
            <el-select v-model="row.frequency" size="small" style="width: 100%">
              <el-option label="每次" value="EACH" />
              <el-option label="每日汇总" value="DAILY" />
              <el-option label="每周汇总" value="WEEKLY" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="config-card">
      <template #header>免打扰时段</template>
      <el-form label-width="120px">
        <el-form-item label="免打扰时段">
          <el-time-picker
            v-model="quietStart"
            placeholder="开始时间"
            format="HH:mm"
            value-format="HH:mm"
          />
          <span class="time-sep">至</span>
          <el-time-picker
            v-model="quietEnd"
            placeholder="结束时间"
            format="HH:mm"
            value-format="HH:mm"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <div class="action-bar">
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSubscriptions, updateSubscriptions } from '@/api/system'

interface SubscriptionRow {
  eventType: string
  eventKey: string
  inApp: boolean
  sms: boolean
  email: boolean
  im: boolean
  frequency: string
}

const eventTypes = [
  { key: 'NEW_BILL', label: '收到新对账单' },
  { key: 'BILL_TIMEOUT', label: '对账单即将超时' },
  { key: 'DISPUTE_REPLY', label: '收到异议回复' },
  { key: 'BILL_SIGNED', label: '对账单已签章' },
  { key: 'PAYMENT_DUE', label: '付款到期提醒' },
  { key: 'COLLECTION', label: '催收通知' },
  { key: 'MONTHLY_REPORT', label: '月度对账报告' },
  { key: 'CREDIT_CHANGE', label: '信用评分变更' },
  { key: 'FACTORING_STATUS', label: '保理融资状态' },
]

const saving = ref(false)
const subscriptionRows = ref<SubscriptionRow[]>([])
const quietStart = ref('22:00')
const quietEnd = ref('08:00')

function initRows() {
  subscriptionRows.value = eventTypes.map((e) => ({
    eventType: e.label,
    eventKey: e.key,
    inApp: true,
    sms: false,
    email: true,
    im: false,
    frequency: 'EACH',
  }))
}

async function fetchSubscriptions() {
  try {
    const res = await getSubscriptions() as Record<string, unknown> | undefined
    if (res?.items) {
      const items = res.items as Array<Record<string, unknown>>
      subscriptionRows.value = eventTypes.map((e) => {
        const found = items.find((i) => i.eventKey === e.key)
        return {
          eventType: e.label,
          eventKey: e.key,
          inApp: found ? Boolean(found.inApp) : true,
          sms: found ? Boolean(found.sms) : false,
          email: found ? Boolean(found.email) : true,
          im: found ? Boolean(found.im) : false,
          frequency: (found?.frequency as string) ?? 'EACH',
        }
      })
    } else {
      initRows()
    }
    if (res?.quietHours) {
      const q = res.quietHours as { start?: string; end?: string }
      quietStart.value = q.start ?? '22:00'
      quietEnd.value = q.end ?? '08:00'
    }
  } catch {
    initRows()
  }
}

async function handleSave() {
  saving.value = true
  try {
    const data = {
      items: subscriptionRows.value.map((r) => ({
        eventKey: r.eventKey,
        inApp: r.inApp,
        sms: r.sms,
        email: r.email,
        im: r.im,
        frequency: r.frequency,
      })),
      quietHours: {
        start: quietStart.value,
        end: quietEnd.value,
      },
    }
    await updateSubscriptions(data)
    ElMessage.success('保存成功')
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchSubscriptions()
})
</script>

<style lang="scss" scoped>
.subscriptions-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .config-card {
    margin-bottom: 20px;
  }

  .time-sep {
    margin: 0 12px;
    color: #909399;
  }

  .action-bar {
    margin-top: 20px;
  }
}
</style>
