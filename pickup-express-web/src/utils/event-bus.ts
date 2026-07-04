type EventCallback = (...args: any[]) => void

class EventBus {
  private events: Map<string, EventCallback[]> = new Map()

  on(event: string, callback: EventCallback) {
    if (!this.events.has(event)) this.events.set(event, [])
    this.events.get(event)!.push(callback)
  }

  off(event: string, callback?: EventCallback) {
    if (!callback) {
      this.events.delete(event)
      return
    }
    const cbs = this.events.get(event)
    if (cbs) this.events.set(event, cbs.filter((cb) => cb !== callback))
  }

  emit(event: string, ...args: any[]) {
    this.events.get(event)?.forEach((cb) => cb(...args))
  }
}

export const eventBus = new EventBus()

/**
 * Pre-defined cross-module events for host platform integration.
 * Use eventBus.on(EVENTS.xxx, callback) to subscribe and eventBus.emit(EVENTS.xxx, ...args) to publish.
 */
export const EVENTS = {
  // AutoRecon emits
  RECON_BILL_CONFIRMED: 'recon:bill:confirmed',
  RECON_SETTLEMENT_SYNCED: 'recon:settlement:synced',

  // Pickup Express emits
  PICKUP_SETTLEMENT_CREATED: 'pickup:settlement:created',
  PICKUP_DELIVERY_COMPLETED: 'pickup:delivery:completed',

  // Shared
  USER_LOGOUT: 'shared:user:logout',
  ENTERPRISE_CHANGED: 'shared:enterprise:changed',
}
