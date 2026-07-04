import { EventType } from '../types'
import type { PerformanceInfo } from '../types'
import type { Tracker } from '../core/tracker'

export class PerformanceCollector {
  private tracker: Tracker
  private reported = false

  constructor(tracker: Tracker) {
    this.tracker = tracker
  }

  install(): void {
    if (this.reported) return

    if (document.readyState === 'complete') {
      this.scheduleReport()
    } else {
      window.addEventListener('load', () => this.scheduleReport(), { once: true })
    }
  }

  private scheduleReport(): void {
    setTimeout(() => this.collectAndReport(), 5000)
  }

  private collectAndReport(): void {
    if (this.reported) return
    this.reported = true

    const metrics: Partial<PerformanceInfo> = {}

    this.collectTimingMetrics(metrics)
    this.collectWebVitals(metrics)

    this.tracker.track(EventType.PERFORMANCE, 'page_performance', {
      performance: metrics,
    })
  }

  private collectTimingMetrics(metrics: Partial<PerformanceInfo>): void {
    const timing = performance.timing
    if (!timing) return

    const nav = timing.navigationStart

    if (timing.responseStart > 0) {
      metrics.ttfb = timing.responseStart - nav
    }
    if (timing.domContentLoadedEventEnd > 0) {
      metrics.domReady = timing.domContentLoadedEventEnd - nav
    }
    if (timing.loadEventEnd > 0) {
      metrics.loadComplete = timing.loadEventEnd - nav
    }

    const paintEntries = performance.getEntriesByType('paint')
    const fcp = paintEntries.find((e) => e.name === 'first-contentful-paint')
    if (fcp) {
      metrics.fcp = fcp.startTime
    }
  }

  private collectWebVitals(metrics: Partial<PerformanceInfo>): void {
    if (typeof PerformanceObserver === 'undefined') return

    try {
      const lcpEntries = performance.getEntriesByType('largest-contentful-paint')
      if (lcpEntries.length > 0) {
        metrics.lcp = lcpEntries[lcpEntries.length - 1].startTime
      }
    } catch {
      // LCP not available
    }

    try {
      const layoutShiftEntries = performance.getEntriesByType('layout-shift') as (PerformanceEntry & { value: number; hadRecentInput: boolean })[]
      if (layoutShiftEntries.length > 0) {
        metrics.cls = layoutShiftEntries
          .filter((e) => !e.hadRecentInput)
          .reduce((sum, e) => sum + e.value, 0)
      }
    } catch {
      // CLS not available
    }
  }
}
