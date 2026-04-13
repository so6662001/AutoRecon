import type { SessionInfo } from '../types'
import { generateUUID } from './device'

const SESSION_KEY = 'analytics_session'

export class SessionManager {
  private timeout: number

  constructor(timeout: number = 30 * 60 * 1000) {
    this.timeout = timeout
  }

  getSession(): SessionInfo {
    const stored = sessionStorage.getItem(SESSION_KEY)

    if (stored) {
      try {
        const session: SessionInfo & { lastActive: number } = JSON.parse(stored)
        if (Date.now() - session.lastActive < this.timeout) {
          this.touch(session)
          return { sessionId: session.sessionId, startTime: session.startTime, pageCount: session.pageCount }
        }
      } catch {
        // corrupted data, create new session
      }
    }

    return this.createSession()
  }

  refreshSession(): void {
    this.createSession()
  }

  incrementPageCount(): void {
    const stored = sessionStorage.getItem(SESSION_KEY)
    if (!stored) return

    try {
      const session = JSON.parse(stored)
      session.pageCount++
      session.lastActive = Date.now()
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
    } catch {
      // ignore
    }
  }

  private createSession(): SessionInfo {
    const session = {
      sessionId: generateUUID(),
      startTime: Date.now(),
      pageCount: 0,
      lastActive: Date.now(),
    }
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
    return { sessionId: session.sessionId, startTime: session.startTime, pageCount: session.pageCount }
  }

  private touch(session: SessionInfo & { lastActive: number }): void {
    session.lastActive = Date.now()
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
  }
}
