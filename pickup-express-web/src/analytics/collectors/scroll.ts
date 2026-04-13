import { EventType } from '../types'
import type { Tracker } from '../core/tracker'

const MILESTONES = [25, 50, 75, 100]
const DEBOUNCE_MS = 200

export class ScrollCollector {
  private tracker: Tracker
  private reachedMilestones = new Set<number>()
  private debounceTimer: ReturnType<typeof setTimeout> | null = null

  constructor(tracker: Tracker) {
    this.tracker = tracker
  }

  install(): void {
    window.addEventListener('scroll', () => this.onScroll(), { passive: true })
  }

  reset(): void {
    this.reachedMilestones.clear()
  }

  private onScroll(): void {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer)
    }
    this.debounceTimer = setTimeout(() => this.checkMilestones(), DEBOUNCE_MS)
  }

  private checkMilestones(): void {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    const scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight

    if (scrollHeight <= 0) return

    const depth = Math.round((scrollTop / scrollHeight) * 100)

    for (const milestone of MILESTONES) {
      if (depth >= milestone && !this.reachedMilestones.has(milestone)) {
        this.reachedMilestones.add(milestone)
        this.tracker.track(EventType.SCROLL_DEPTH, 'scroll_depth', {
          action: {
            category: 'scroll',
            label: `${milestone}%`,
            value: milestone,
          },
        })
      }
    }
  }
}
