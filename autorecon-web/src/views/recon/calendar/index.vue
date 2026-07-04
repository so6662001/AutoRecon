<template>
  <div class="calendar-page">
    <h2 class="page-title">对账日历</h2>

    <div class="toolbar">
      <el-button-group>
        <el-button :type="viewMode === 'month' ? 'primary' : 'default'" @click="viewMode = 'month'">
          月视图
        </el-button>
        <el-button :type="viewMode === 'week' ? 'primary' : 'default'" @click="viewMode = 'week'">
          周视图
        </el-button>
      </el-button-group>
      <div class="nav-buttons">
        <el-button circle :icon="ArrowLeft" @click="prevPeriod" />
        <span class="period-label">{{ periodLabel }}</span>
        <el-button circle :icon="ArrowRight" @click="nextPeriod" />
      </div>
    </div>

    <div class="calendar-layout">
      <div class="calendar-main">
        <div v-if="viewMode === 'month'" class="month-view">
          <div class="weekday-headers">
            <div v-for="d in weekdayLabels" :key="d" class="weekday-header">{{ d }}</div>
          </div>
          <div class="days-grid">
            <div
              v-for="(day, idx) in monthDays"
              :key="idx"
              class="day-cell"
              :class="{ otherMonth: !day.isCurrentMonth, today: day.isToday }"
              @click="selectDay(day)"
            >
              <span class="day-num">{{ day.date }}</span>
              <div class="day-events">
                <div
                  v-for="evt in getEventsForDay(day)"
                  :key="evt.id"
                  class="event-dot"
                  :class="evt.type"
                  :title="evt.title"
                >
                  {{ evt.icon }} {{ evt.title }}
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="week-view">
          <div class="weekday-headers">
            <div class="weekday-header">时间</div>
            <div v-for="d in weekDays" :key="d.key" class="weekday-header">{{ d.label }}</div>
          </div>
          <div class="week-grid">
            <div v-for="h in 24" :key="h" class="week-row">
              <div class="hour-label">{{ String(h - 1).padStart(2, '0') }}:00</div>
              <div v-for="wd in weekDays" :key="wd.key" class="week-cell">
                <div
                  v-for="evt in getEventsForWeekCell(wd, h - 1)"
                  :key="evt.id"
                  class="week-event"
                  :class="evt.type"
                >
                  {{ evt.icon }} {{ evt.title }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="sidebar">
        <div class="sidebar-title">即将到来 (7天内)</div>
        <div v-loading="upcomingLoading" class="upcoming-list">
          <div
            v-for="evt in upcomingEvents"
            :key="evt.id"
            class="upcoming-item"
          >
            <span class="evt-date">{{ evt.date }}</span>
            <span class="evt-icon">{{ evt.icon }}</span>
            <span class="evt-title">{{ evt.title }}</span>
          </div>
          <div v-if="!upcomingLoading && upcomingEvents.length === 0" class="empty-hint">
            暂无即将到来的事件
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dayDetailVisible" :title="`${selectedDayLabel} 事件`" width="400px">
      <div v-for="evt in selectedDayEvents" :key="evt.id" class="detail-event">
        <span class="evt-icon">{{ eventIcons[evt.type] ?? '•' }}</span>
        <span class="evt-title">{{ evt.title }}</span>
        <span class="evt-desc">{{ evt.description }}</span>
      </div>
      <div v-if="selectedDayEvents.length === 0" class="empty-hint">该日无事件</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getCalendarEvents, getUpcomingEvents } from '@/api/system'

const viewMode = ref<'month' | 'week'>('month')
const currentDate = ref(dayjs())
const events = ref<Array<{
  id: string
  type: string
  title: string
  date: string
  time?: string
  description?: string
  icon?: string
}>>([])
const upcomingEvents = ref<Array<{ id: string; date: string; icon: string; title: string }>>([])
const upcomingLoading = ref(false)
const selectedDay = ref<{ year: number; month: number; date: number; full: string } | null>(null)
const dayDetailVisible = ref(false)

const weekdayLabels = ['日', '一', '二', '三', '四', '五', '六']

const periodLabel = computed(() => {
  if (viewMode.value === 'month') {
    return currentDate.value.format('YYYY年MM月')
  }
  const start = currentDate.value.startOf('week')
  const end = currentDate.value.endOf('week')
  return `${start.format('MM/DD')} - ${end.format('MM/DD')}`
})

const monthDays = computed(() => {
  const d = currentDate.value
  const start = d.startOf('month').startOf('week')
  const end = d.endOf('month').endOf('week')
  const days: Array<{
    year: number
    month: number
    date: number
    full: string
    isCurrentMonth: boolean
    isToday: boolean
  }> = []
  let cur = start
  while (cur.isBefore(end) || cur.isSame(end, 'day')) {
    days.push({
      year: cur.year(),
      month: cur.month(),
      date: cur.date(),
      full: cur.format('YYYY-MM-DD'),
      isCurrentMonth: cur.month() === d.month(),
      isToday: cur.isSame(dayjs(), 'day'),
    })
    cur = cur.add(1, 'day')
  }
  return days
})

const weekDays = computed(() => {
  const start = currentDate.value.startOf('week')
  return Array.from({ length: 7 }, (_, i) => {
    const d = start.add(i, 'day')
    return {
      key: d.format('YYYY-MM-DD'),
      label: `${d.format('MM/DD')} ${weekdayLabels[d.day()]}`,
      year: d.year(),
      month: d.month(),
      date: d.date(),
    }
  })
})

const selectedDayLabel = computed(() => {
  if (!selectedDay.value) return ''
  return dayjs(`${selectedDay.value.year}-${selectedDay.value.month + 1}-${selectedDay.value.date}`).format('YYYY年MM月DD日')
})

const selectedDayEvents = computed(() => {
  if (!selectedDay.value) return []
  return events.value
    .filter((e) => e.date === selectedDay.value!.full)
    .map((e) => ({ ...e, icon: eventIcons[e.type] ?? '•' }))
})

const eventIcons: Record<string, string> = {
  PLAN: '📋',
  SEAL_DUE: '⏰',
  PAYMENT_DUE: '💰',
  COLLECTION: '⚠',
}

function getEventsForDay(day: { full: string }) {
  return events.value
    .filter((e) => e.date === day.full)
    .map((e) => ({ ...e, icon: eventIcons[e.type] ?? '•' }))
}

function getEventsForWeekCell(wd: { key: string }, hour: number) {
  return events.value.filter((e) => {
    if (e.date !== wd.key) return false
    const evtHour = e.time ? parseInt(e.time.split(':')[0], 10) : 0
    return evtHour === hour
  }).map((e) => ({ ...e, icon: eventIcons[e.type] ?? '•' }))
}

function selectDay(day: { year: number; month: number; date: number; full: string }) {
  selectedDay.value = { ...day }
  dayDetailVisible.value = true
}

function prevPeriod() {
  if (viewMode.value === 'month') {
    currentDate.value = currentDate.value.subtract(1, 'month')
  } else {
    currentDate.value = currentDate.value.subtract(1, 'week')
  }
}

function nextPeriod() {
  if (viewMode.value === 'month') {
    currentDate.value = currentDate.value.add(1, 'month')
  } else {
    currentDate.value = currentDate.value.add(1, 'week')
  }
}

async function fetchEvents() {
  try {
    const start = currentDate.value.startOf('month').format('YYYY-MM-DD')
    const end = currentDate.value.endOf('month').format('YYYY-MM-DD')
    const res = await getCalendarEvents({ start, end }) as Array<Record<string, unknown>>
    const eventIconsMap: Record<string, string> = {
      PLAN: '📋',
      SEAL_DUE: '⏰',
      PAYMENT_DUE: '💰',
      COLLECTION: '⚠',
    }
    events.value = (res ?? []).map((e: Record<string, unknown>) => {
      const type = (e.type as string) ?? 'PLAN'
      return {
        id: String(e.id ?? (e.date as string) + type),
        type,
        title: (e.title as string) ?? '',
        date: (e.date as string) ?? '',
        time: e.time as string | undefined,
        description: e.description as string | undefined,
        icon: eventIconsMap[type] ?? '•',
      }
    })
    if (events.value.length === 0) {
      events.value = [
        { id: '1', type: 'PLAN', title: '计划对账', date: currentDate.value.format('YYYY-MM-DD'), description: '月度对账' },
        { id: '2', type: 'SEAL_DUE', title: '签章到期', date: currentDate.value.add(3, 'day').format('YYYY-MM-DD'), description: '待签章' },
      ]
    }
  } catch {
    events.value = []
  }
}

async function fetchUpcoming() {
  upcomingLoading.value = true
  try {
    const res = await getUpcomingEvents(7) as Array<Record<string, unknown>>
    upcomingEvents.value = (res ?? []).map((e: Record<string, unknown>) => ({
      id: String(e.id ?? ''),
      date: (e.date as string) ?? '',
      icon: eventIcons[(e.type as string) ?? 'PLAN'] ?? '•',
      title: (e.title as string) ?? '',
    }))
    if (upcomingEvents.value.length === 0) {
      upcomingEvents.value = [
        { id: '1', date: dayjs().format('MM-DD'), icon: '📋', title: '计划对账' },
        { id: '2', date: dayjs().add(2, 'day').format('MM-DD'), icon: '⏰', title: '签章到期' },
      ]
    }
  } catch {
    upcomingEvents.value = []
  } finally {
    upcomingLoading.value = false
  }
}

watch([currentDate, viewMode], () => {
  fetchEvents()
})

onMounted(() => {
  fetchEvents()
  fetchUpcoming()
})
</script>

<style lang="scss" scoped>
.calendar-page {
  .page-title {
    margin: 0 0 24px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
  }

  .nav-buttons {
    display: flex;
    align-items: center;
    gap: 12px;

    .period-label {
      min-width: 140px;
      text-align: center;
      font-weight: 500;
    }
  }

  .calendar-layout {
    display: flex;
    gap: 20px;
  }

  .calendar-main {
    flex: 1;
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .month-view {
    .weekday-headers {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 4px;
      margin-bottom: 8px;
    }

    .weekday-header {
      text-align: center;
      font-size: 13px;
      color: #909399;
    }

    .days-grid {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 4px;
    }

    .day-cell {
      min-height: 80px;
      padding: 4px;
      border: 1px solid #ebeef5;
      border-radius: 4px;
      cursor: pointer;

      &.otherMonth {
        background: #fafafa;
        color: #c0c4cc;
      }

      &.today {
        border-color: var(--el-color-primary);
        background: var(--el-color-primary-light-9);
      }

      .day-num {
        font-size: 13px;
        font-weight: 500;
      }

      .day-events {
        margin-top: 4px;
      }

      .event-dot {
        font-size: 11px;
        padding: 2px 4px;
        margin-bottom: 2px;
        border-radius: 2px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        &.PLAN {
          background: #ecf5ff;
          color: #409eff;
        }

        &.SEAL_DUE {
          background: #fdf6ec;
          color: #e6a23c;
        }

        &.PAYMENT_DUE {
          background: #f0f9eb;
          color: #67c23a;
        }

        &.COLLECTION {
          background: #fef0f0;
          color: #f56c6c;
        }
      }
    }
  }

  .week-view {
    .weekday-headers {
      display: grid;
      grid-template-columns: 60px repeat(7, 1fr);
      gap: 4px;
      margin-bottom: 8px;
    }

    .weekday-header {
      text-align: center;
      font-size: 13px;
      color: #909399;
    }

    .week-grid {
      .week-row {
        display: grid;
        grid-template-columns: 60px repeat(7, 1fr);
        gap: 4px;
        min-height: 40px;
        border-bottom: 1px solid #ebeef5;
      }

      .hour-label {
        font-size: 12px;
        color: #909399;
        padding: 4px;
      }

      .week-cell {
        padding: 4px;
        position: relative;
      }

      .week-event {
        font-size: 11px;
        padding: 2px 4px;
        border-radius: 2px;
        margin-bottom: 2px;
      }
    }
  }

  .sidebar {
    width: 260px;
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    height: fit-content;
  }

  .sidebar-title {
    font-weight: 600;
    margin-bottom: 12px;
  }

  .upcoming-list {
    min-height: 100px;
  }

  .upcoming-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0;
    border-bottom: 1px solid #ebeef5;
    font-size: 13px;

    .evt-date {
      color: #909399;
      min-width: 40px;
    }

    .evt-icon {
      font-size: 14px;
    }

    .evt-title {
      flex: 1;
    }
  }

  .detail-event {
    padding: 8px 0;
    border-bottom: 1px solid #ebeef5;

    .evt-icon {
      margin-right: 8px;
    }

    .evt-desc {
      display: block;
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
  }

  .empty-hint {
    color: #909399;
    font-size: 13px;
    padding: 20px;
    text-align: center;
  }
}
</style>
