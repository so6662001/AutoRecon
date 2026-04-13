import type { AnalyticsEvent } from '../types'

const FAILED_EVENTS_KEY = 'analytics_failed'
const MAX_FAILED_EVENTS = 200
const MAX_RETRIES = 2
const RETRY_DELAY = 1000

export class EventSender {
  private endpoint: string

  constructor(endpoint: string) {
    this.endpoint = endpoint
    this.resendFailed()
  }

  async send(events: AnalyticsEvent[]): Promise<boolean> {
    for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      try {
        const response = await fetch(this.endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ events }),
        })

        if (response.ok) return true

        if (attempt < MAX_RETRIES) {
          await this.delay(RETRY_DELAY)
        }
      } catch {
        if (attempt < MAX_RETRIES) {
          await this.delay(RETRY_DELAY)
        }
      }
    }

    this.saveFailed(events)
    return false
  }

  sendBeacon(events: AnalyticsEvent[]): boolean {
    if (typeof navigator === 'undefined' || !navigator.sendBeacon) {
      this.saveFailed(events)
      return false
    }

    const blob = new Blob([JSON.stringify({ events })], { type: 'application/json' })
    const sent = navigator.sendBeacon(this.endpoint, blob)

    if (!sent) {
      this.saveFailed(events)
    }

    return sent
  }

  private saveFailed(events: AnalyticsEvent[]): void {
    try {
      const stored = localStorage.getItem(FAILED_EVENTS_KEY)
      const existing: AnalyticsEvent[] = stored ? JSON.parse(stored) : []
      const combined = [...existing, ...events].slice(-MAX_FAILED_EVENTS)
      localStorage.setItem(FAILED_EVENTS_KEY, JSON.stringify(combined))
    } catch {
      // storage full or unavailable
    }
  }

  private resendFailed(): void {
    try {
      const stored = localStorage.getItem(FAILED_EVENTS_KEY)
      if (!stored) return

      const events: AnalyticsEvent[] = JSON.parse(stored)
      if (events.length === 0) return

      localStorage.removeItem(FAILED_EVENTS_KEY)
      this.send(events).catch(() => {})
    } catch {
      localStorage.removeItem(FAILED_EVENTS_KEY)
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms))
  }
}
