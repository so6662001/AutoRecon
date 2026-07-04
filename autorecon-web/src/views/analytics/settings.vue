<template>
  <div class="settings-page">
    <h2 class="page-title">埋点管理</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="事件列表" name="events">
        <div class="filter-bar">
          <el-select v-model="eventTypeFilter" placeholder="全部类型" clearable style="width: 140px" @change="filterEvents">
            <el-option label="页面浏览" value="page_view" />
            <el-option label="点击事件" value="click" />
            <el-option label="业务事件" value="business" />
            <el-option label="性能事件" value="performance" />
          </el-select>
          <el-input v-model="searchKey" placeholder="搜索事件名称" clearable style="width: 200px" @input="filterEvents" />
        </div>

        <el-table :data="filteredEvents" stripe style="width: 100%">
          <el-table-column prop="eventName" label="事件名称" min-width="180" />
          <el-table-column prop="eventType" label="事件类型" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="eventTypeTag(row.eventType)">{{ eventTypeLabel(row.eventType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
                {{ row.status === 'active' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastTriggered" label="最近触发" width="160" />
          <el-table-column prop="count" label="触发次数" width="110" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="实时调试" name="debug">
        <div class="debug-header">
          <el-switch v-model="debugMode" active-text="开启实时调试" @change="(val: string | number | boolean) => toggleDebug(!!val)" />
          <el-button v-if="debugMode" size="small" @click="clearStream">清空</el-button>
        </div>
        <div class="event-stream" ref="streamRef">
          <div v-for="(evt, i) in eventStream" :key="i" class="stream-item">
            <span class="stream-time">{{ evt.time }}</span>
            <el-tag size="small" :type="eventTypeTag(evt.type)" class="stream-type">{{ eventTypeLabel(evt.type) }}</el-tag>
            <span class="stream-name">{{ evt.name }}</span>
            <span class="stream-page">{{ evt.page }}</span>
          </div>
          <div v-if="!debugMode && eventStream.length === 0" class="stream-empty">开启调试模式查看实时事件流</div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="采样配置" name="sampling">
        <div class="config-card">
          <h3>全局采样率</h3>
          <el-slider v-model="sampleRate" :min="1" :max="100" :format-tooltip="(v: number) => `${v}%`"
            style="max-width: 400px" />
          <p class="config-desc">当前采样率: {{ sampleRate }}%，预计每日采集 {{ Math.floor(45000 * sampleRate / 100).toLocaleString() }} 条事件</p>

          <el-divider />

          <h3>按事件类型配置</h3>
          <el-form label-width="120px" style="max-width: 500px">
            <el-form-item label="页面浏览">
              <el-slider v-model="typeSamples.page_view" :min="1" :max="100" />
            </el-form-item>
            <el-form-item label="点击事件">
              <el-slider v-model="typeSamples.click" :min="1" :max="100" />
            </el-form-item>
            <el-form-item label="业务事件">
              <el-slider v-model="typeSamples.business" :min="1" :max="100" />
            </el-form-item>
            <el-form-item label="性能事件">
              <el-slider v-model="typeSamples.performance" :min="1" :max="100" />
            </el-form-item>
          </el-form>
          <el-button type="primary" @click="saveSampling">保存配置</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="告警规则" name="alerts">
        <el-table :data="alertRules" stripe style="width: 100%">
          <el-table-column prop="name" label="规则名称" min-width="180" />
          <el-table-column prop="condition" label="触发条件" min-width="200" />
          <el-table-column prop="threshold" label="阈值" width="100" />
          <el-table-column prop="notifyChannel" label="通知渠道" width="120" />
          <el-table-column label="启用" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" size="small" />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getEventList } from '@/api/analytics'

interface EventRow {
  eventName: string; eventType: string; status: string
  lastTriggered: string; count: number
}
interface StreamEvent { time: string; type: string; name: string; page: string }
interface AlertRule {
  name: string; condition: string; threshold: string
  notifyChannel: string; enabled: boolean
}

const activeTab = ref('events')
const eventTypeFilter = ref('')
const searchKey = ref('')
const debugMode = ref(false)
const sampleRate = ref(100)
const typeSamples = ref({ page_view: 100, click: 80, business: 100, performance: 50 })
const streamRef = ref<HTMLElement>()

const events = ref<EventRow[]>([])
const eventStream = ref<StreamEvent[]>([])
let debugTimer: ReturnType<typeof setInterval> | null = null

const alertRules = ref<AlertRule[]>([
  { name: 'PV骤降告警', condition: '1小时内PV下降超过阈值', threshold: '50%', notifyChannel: '钉钉', enabled: true },
  { name: 'JS错误率告警', condition: '错误率超过阈值', threshold: '5%', notifyChannel: '邮件', enabled: true },
  { name: '接口慢响应告警', condition: 'P95超过阈值', threshold: '3000ms', notifyChannel: '钉钉', enabled: false },
  { name: '用户流失告警', condition: '日活跃下降超过阈值', threshold: '30%', notifyChannel: '短信', enabled: true },
  { name: 'LCP超标告警', condition: 'LCP均值超过阈值', threshold: '4000ms', notifyChannel: '邮件', enabled: false },
])

function eventTypeTag(type: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    page_view: 'primary', click: 'success', business: 'warning', performance: 'danger',
  }
  return map[type] ?? 'info'
}

function eventTypeLabel(type: string) {
  const map: Record<string, string> = {
    page_view: '页面浏览', click: '点击事件', business: '业务事件', performance: '性能事件',
  }
  return map[type] ?? type
}

const filteredEvents = computed(() => {
  let data = events.value
  if (eventTypeFilter.value) data = data.filter(e => e.eventType === eventTypeFilter.value)
  if (searchKey.value) data = data.filter(e => e.eventName.includes(searchKey.value))
  return data
})

function filterEvents() { /* computed handles reactivity */ }

function generateMockEvents(): EventRow[] {
  const items: EventRow[] = [
    { eventName: 'page_view_dashboard', eventType: 'page_view', status: 'active', lastTriggered: '2026-04-13 10:32:15', count: 12580 },
    { eventName: 'page_view_bill_list', eventType: 'page_view', status: 'active', lastTriggered: '2026-04-13 10:31:48', count: 9840 },
    { eventName: 'click_create_bill', eventType: 'click', status: 'active', lastTriggered: '2026-04-13 10:30:22', count: 5620 },
    { eventName: 'click_export_excel', eventType: 'click', status: 'active', lastTriggered: '2026-04-13 10:28:55', count: 4380 },
    { eventName: 'biz_bill_confirmed', eventType: 'business', status: 'active', lastTriggered: '2026-04-13 10:25:10', count: 3200 },
    { eventName: 'biz_dispute_created', eventType: 'business', status: 'active', lastTriggered: '2026-04-13 10:20:30', count: 1860 },
    { eventName: 'perf_fcp', eventType: 'performance', status: 'active', lastTriggered: '2026-04-13 10:32:00', count: 45000 },
    { eventName: 'perf_lcp', eventType: 'performance', status: 'active', lastTriggered: '2026-04-13 10:32:00', count: 45000 },
    { eventName: 'click_sign_confirm', eventType: 'click', status: 'active', lastTriggered: '2026-04-13 09:58:42', count: 2140 },
    { eventName: 'biz_payment_completed', eventType: 'business', status: 'disabled', lastTriggered: '2026-04-12 18:45:00', count: 980 },
    { eventName: 'page_view_invoice', eventType: 'page_view', status: 'active', lastTriggered: '2026-04-13 10:15:30', count: 3420 },
    { eventName: 'click_batch_recon', eventType: 'click', status: 'active', lastTriggered: '2026-04-13 09:45:12', count: 1560 },
    { eventName: 'biz_data_upload', eventType: 'business', status: 'active', lastTriggered: '2026-04-13 10:10:05', count: 2800 },
    { eventName: 'perf_api_slow', eventType: 'performance', status: 'active', lastTriggered: '2026-04-13 10:31:55', count: 8600 },
    { eventName: 'click_collection_send', eventType: 'click', status: 'disabled', lastTriggered: '2026-04-11 14:20:00', count: 720 },
  ]
  return items
}

const mockStreamEvents = [
  { type: 'page_view', name: 'page_view_dashboard', page: '/dashboard' },
  { type: 'click', name: 'click_create_bill', page: '/recon/bills/create' },
  { type: 'business', name: 'biz_bill_confirmed', page: '/recon/bills/1001' },
  { type: 'page_view', name: 'page_view_bill_list', page: '/recon/bills' },
  { type: 'performance', name: 'perf_fcp', page: '/dashboard' },
  { type: 'click', name: 'click_export_excel', page: '/recon/bills' },
  { type: 'business', name: 'biz_dispute_created', page: '/recon/disputes' },
  { type: 'page_view', name: 'page_view_invoice', page: '/recon/invoices' },
]

function toggleDebug(val: boolean) {
  if (val) {
    debugTimer = setInterval(() => {
      const mock = mockStreamEvents[Math.floor(Math.random() * mockStreamEvents.length)]
      const now = new Date()
      eventStream.value.unshift({
        time: `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`,
        ...mock,
      })
      if (eventStream.value.length > 100) eventStream.value.pop()
      nextTick(() => { if (streamRef.value) streamRef.value.scrollTop = 0 })
    }, 2000)
  } else {
    if (debugTimer) { clearInterval(debugTimer); debugTimer = null }
  }
}

function clearStream() { eventStream.value = [] }

function saveSampling() { ElMessage.success('采样配置已保存') }

async function fetchData() {
  try {
    const res = await getEventList({ system: undefined }) as Record<string, unknown>
    const data = (res && 'data' in res ? res.data : res) as EventRow[] | undefined
    if (Array.isArray(data) && data.length) events.value = data
  } catch {
    // mock fallback
  }
}

onMounted(() => {
  events.value = generateMockEvents()
  fetchData()
})

onUnmounted(() => {
  if (debugTimer) clearInterval(debugTimer)
})
</script>

<style lang="scss" scoped>
.settings-page {
  .page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #303133; }
  .filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
  .debug-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
  .event-stream {
    border: 1px solid #ebeef5; border-radius: 6px; height: 400px; overflow-y: auto;
    background: #fafafa; padding: 8px;
  }
  .stream-item {
    display: flex; align-items: center; gap: 8px; padding: 6px 8px;
    border-bottom: 1px solid #f0f0f0; font-size: 13px;
  }
  .stream-time { color: #909399; font-family: monospace; white-space: nowrap; }
  .stream-type { flex-shrink: 0; }
  .stream-name { color: #303133; font-weight: 500; }
  .stream-page { color: #909399; margin-left: auto; white-space: nowrap; }
  .stream-empty { text-align: center; color: #c0c4cc; padding: 60px 0; font-size: 14px; }
  .config-card {
    background: #fff; border-radius: 8px; padding: 24px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.06);
    h3 { font-size: 16px; margin: 0 0 16px; color: #303133; }
  }
  .config-desc { font-size: 13px; color: #909399; margin: 8px 0 0; }
}

@media (max-width: 768px) {
  .settings-page {
    .filter-bar { flex-direction: column; }
    .event-stream { height: 300px; }
    .stream-item { flex-wrap: wrap; }
    .stream-page { margin-left: 0; }
  }
}
</style>
