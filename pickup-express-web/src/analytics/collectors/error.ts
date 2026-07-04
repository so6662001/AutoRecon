import { EventType } from '../types'
import type { Tracker } from '../core/tracker'

const DEDUP_INTERVAL = 5000
const MAX_STACK_LENGTH = 500

export class ErrorCollector {
  private tracker: Tracker
  private recentErrors = new Map<string, number>()

  constructor(tracker: Tracker) {
    this.tracker = tracker
  }

  install(): void {
    window.addEventListener('error', (e: ErrorEvent) => {
      this.handleJsError(e)
    })

    window.addEventListener('unhandledrejection', (e: PromiseRejectionEvent) => {
      this.handlePromiseRejection(e)
    })
  }

  private isDuplicate(message: string): boolean {
    const now = Date.now()
    const lastSeen = this.recentErrors.get(message)
    if (lastSeen && now - lastSeen < DEDUP_INTERVAL) {
      return true
    }
    this.recentErrors.set(message, now)
    this.cleanupOldEntries(now)
    return false
  }

  private cleanupOldEntries(now: number): void {
    for (const [key, time] of this.recentErrors) {
      if (now - time >= DEDUP_INTERVAL) {
        this.recentErrors.delete(key)
      }
    }
  }

  private handleJsError(e: ErrorEvent): void {
    const message = e.message || 'Unknown error'
    if (this.isDuplicate(message)) return

    const stack = e.error?.stack ? e.error.stack.slice(0, MAX_STACK_LENGTH) : ''

    this.tracker.track(EventType.ERROR, 'js_error', {
      action: {
        category: 'error',
        label: message,
        extra: {
          filename: e.filename || '',
          lineno: e.lineno || 0,
          colno: e.colno || 0,
          stack,
        },
      },
    })
  }

  private handlePromiseRejection(e: PromiseRejectionEvent): void {
    const reason = e.reason
    const message = reason instanceof Error
      ? reason.message
      : String(reason || 'Unhandled promise rejection')

    if (this.isDuplicate(message)) return

    const stack = reason instanceof Error
      ? (reason.stack || '').slice(0, MAX_STACK_LENGTH)
      : ''

    this.tracker.track(EventType.ERROR, 'promise_rejection', {
      action: {
        category: 'error',
        label: message,
        extra: {
          filename: '',
          lineno: 0,
          colno: 0,
          stack,
        },
      },
    })
  }
}
