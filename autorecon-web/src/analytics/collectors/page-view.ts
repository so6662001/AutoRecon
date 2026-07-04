import type { Router, RouteLocationNormalized } from 'vue-router'
import { EventType } from '../types'
import type { Tracker } from '../core/tracker'

export class PageViewCollector {
  private tracker: Tracker
  private enterTime = 0
  private currentPath = ''
  private pausedAt = 0
  private accumulatedDuration = 0

  constructor(tracker: Tracker) {
    this.tracker = tracker
  }

  install(router: Router): void {
    this.setupVisibilityListener()

    router.afterEach((to, from) => {
      if (from.path !== '/' || from.name !== undefined) {
        this.trackPageLeave()
      }
      this.trackPageView(to)
    })
  }

  private setupVisibilityListener(): void {
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.pausedAt = Date.now()
      } else if (this.pausedAt > 0) {
        this.accumulatedDuration += Date.now() - this.pausedAt
        this.pausedAt = 0
      }
    })
  }

  private trackPageLeave(): void {
    if (!this.currentPath) return

    const now = Date.now()
    let hiddenDuration = this.accumulatedDuration
    if (this.pausedAt > 0) {
      hiddenDuration += now - this.pausedAt
    }
    const duration = now - this.enterTime - hiddenDuration

    this.tracker.track(EventType.PAGE_LEAVE, 'page_leave', {
      page: {
        path: this.currentPath,
        title: document.title,
        module: this.inferModule(this.currentPath),
        duration,
      },
    })
  }

  private trackPageView(route: RouteLocationNormalized): void {
    this.enterTime = Date.now()
    this.pausedAt = 0
    this.accumulatedDuration = 0
    this.currentPath = route.path

    this.tracker.track(EventType.PAGE_VIEW, 'page_view', {
      page: {
        path: route.path,
        title: document.title,
        module: this.inferModule(route.path),
        referrer: document.referrer,
      },
    })
  }

  inferModule(path: string): string {
    const segments = path.split('/').filter(Boolean)
    return segments[0] || 'home'
  }
}
