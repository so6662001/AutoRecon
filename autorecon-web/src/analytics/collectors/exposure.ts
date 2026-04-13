import { EventType } from '../types'
import type { Tracker } from '../core/tracker'

interface ExposureOptions {
  event: string
  once?: boolean
}

export class ExposureCollector {
  private tracker: Tracker
  private observer: IntersectionObserver
  private elementOptions = new WeakMap<HTMLElement, ExposureOptions>()

  constructor(tracker: Tracker) {
    this.tracker = tracker
    this.observer = new IntersectionObserver(
      (entries) => this.handleIntersection(entries),
      { threshold: 0.5 },
    )
  }

  observe(el: HTMLElement, options: ExposureOptions): void {
    const opts = { once: true, ...options }
    this.elementOptions.set(el, opts)
    this.observer.observe(el)
  }

  unobserve(el: HTMLElement): void {
    this.observer.unobserve(el)
    this.elementOptions.delete(el)
  }

  private handleIntersection(entries: IntersectionObserverEntry[]): void {
    for (const entry of entries) {
      if (!entry.isIntersecting) continue

      const el = entry.target as HTMLElement
      const options = this.elementOptions.get(el)
      if (!options) continue

      this.tracker.track(EventType.EXPOSURE, options.event, {
        action: {
          category: 'exposure',
          label: options.event,
          element: el.tagName.toLowerCase(),
        },
      })

      if (options.once !== false) {
        this.unobserve(el)
      }
    }
  }
}
