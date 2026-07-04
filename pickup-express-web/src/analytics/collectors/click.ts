import { EventType } from '../types'
import type { Tracker } from '../core/tracker'

const INTERACTIVE_SELECTOR = 'button, a, [role="button"], .el-button, .el-menu-item, .el-tab'

export class ClickCollector {
  private tracker: Tracker

  constructor(tracker: Tracker) {
    this.tracker = tracker
  }

  install(): void {
    document.addEventListener('click', (e) => this.handleClick(e), true)
  }

  private handleClick(e: MouseEvent): void {
    const target = e.target as HTMLElement
    if (!target) return

    const tracked = target.closest('[data-track-event]')
    if (tracked) {
      this.trackDataAttributes(tracked as HTMLElement)
      return
    }

    const interactive = target.closest(INTERACTIVE_SELECTOR)
    if (interactive) {
      this.trackInteractiveElement(interactive as HTMLElement)
    }
  }

  private trackDataAttributes(el: HTMLElement): void {
    const event = el.getAttribute('data-track-event') || 'click'
    const category = el.getAttribute('data-track-category') || ''
    const label = el.getAttribute('data-track-label') || ''

    this.tracker.track(EventType.CLICK, event, {
      action: { category, label, element: el.tagName.toLowerCase() },
    })
  }

  private trackInteractiveElement(el: HTMLElement): void {
    const tag = el.tagName.toLowerCase()
    const id = el.id || ''
    const text = (el.innerText || '').trim().slice(0, 50)

    this.tracker.track(EventType.CLICK, 'element_click', {
      action: {
        category: tag,
        label: text,
        element: tag,
        elementId: id,
      },
    })
  }
}
