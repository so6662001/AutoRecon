import type { AnalyticsConfig, AnalyticsEvent, PageInfo, UserInfo } from '../types'
import { EventType } from '../types'
import { getDeviceInfo, generateUUID } from './device'
import { SessionManager } from './session'
import { IdentityManager } from './identity'
import { EventSender } from '../transport/sender'

export class Tracker {
  private config: AnalyticsConfig
  private eventQueue: AnalyticsEvent[] = []
  private flushTimer: ReturnType<typeof setInterval> | null = null
  private sender: EventSender
  private sessionManager: SessionManager
  private identityManager: IdentityManager
  private currentPage: Partial<PageInfo> = {}
  private destroyed = false

  constructor(config: AnalyticsConfig) {
    this.config = config
    this.sender = new EventSender(config.endpoint)
    this.sessionManager = new SessionManager()
    this.identityManager = new IdentityManager()

    this.startFlushTimer()

    if (typeof window !== 'undefined') {
      window.addEventListener('beforeunload', this.onBeforeUnload)
      document.addEventListener('visibilitychange', this.onVisibilityChange)
    }
  }

  track(eventType: EventType, eventName: string, data?: Partial<AnalyticsEvent>): void {
    if (this.destroyed) return

    if (Math.random() >= this.config.sampleRate) return

    const event = this.buildEvent(eventType, eventName, data)

    if (this.config.debug) {
      console.log(`[Analytics] ${eventType}:${eventName}`, event)
    }

    this.eventQueue.push(event)

    if (this.eventQueue.length >= this.config.batchSize) {
      this.flush()
    }
  }

  identify(user: UserInfo): void {
    this.identityManager.identify(user)
  }

  setPage(page: Partial<PageInfo>): void {
    this.currentPage = { ...this.currentPage, ...page }
  }

  flush(): void {
    if (this.eventQueue.length === 0) return

    const events = this.eventQueue.splice(0)
    this.sender.send(events).catch(() => {
      if (this.config.debug) {
        console.warn('[Analytics] Failed to send events')
      }
    })
  }

  destroy(): void {
    this.destroyed = true
    this.flush()

    if (this.flushTimer) {
      clearInterval(this.flushTimer)
      this.flushTimer = null
    }

    if (typeof window !== 'undefined') {
      window.removeEventListener('beforeunload', this.onBeforeUnload)
      document.removeEventListener('visibilitychange', this.onVisibilityChange)
    }
  }

  private buildEvent(eventType: EventType, eventName: string, data?: Partial<AnalyticsEvent>): AnalyticsEvent {
    const session = this.sessionManager.getSession()
    const device = getDeviceInfo()
    const user = this.identityManager.getUser()

    return {
      eventId: generateUUID(),
      eventType,
      eventName,
      timestamp: Date.now(),
      system: this.config.system,
      session,
      user,
      device,
      page: this.currentPage,
      ...data,
    }
  }

  private startFlushTimer(): void {
    if (this.config.flushInterval > 0) {
      this.flushTimer = setInterval(() => this.flush(), this.config.flushInterval)
    }
  }

  private onBeforeUnload = (): void => {
    if (this.eventQueue.length > 0) {
      this.sender.sendBeacon(this.eventQueue.splice(0))
    }
  }

  private onVisibilityChange = (): void => {
    if (document.visibilityState === 'hidden') {
      this.flush()
    }
  }
}
