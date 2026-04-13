import type { Directive, DirectiveBinding } from 'vue'
import { EventType } from '../types'
import type { Tracker } from '../core/tracker'
import { ExposureCollector } from '../collectors/exposure'

interface TrackBindingObject {
  event: string
  category?: string
  label?: string
  value?: number
}

type TrackBindingValue = string | TrackBindingObject

export function vTrack(tracker: Tracker): Directive<HTMLElement, TrackBindingValue> {
  return {
    mounted(el: HTMLElement, binding: DirectiveBinding<TrackBindingValue>) {
      const handler = () => {
        const eventName = resolveEventName(binding)
        const data = resolveEventData(binding)

        tracker.track(EventType.CLICK, eventName, { action: data })
      }

      el.addEventListener('click', handler)
      ;(el as any).__trackClickHandler = handler
    },

    unmounted(el: HTMLElement) {
      const handler = (el as any).__trackClickHandler
      if (handler) {
        el.removeEventListener('click', handler)
        delete (el as any).__trackClickHandler
      }
    },
  }
}

function resolveEventName(binding: DirectiveBinding<TrackBindingValue>): string {
  if (binding.arg) return binding.arg
  if (typeof binding.value === 'string') return binding.value
  return binding.value?.event || 'click'
}

function resolveEventData(binding: DirectiveBinding<TrackBindingValue>): Record<string, any> {
  if (typeof binding.value === 'string') {
    return { category: '', label: binding.value }
  }

  if (binding.value && typeof binding.value === 'object') {
    const { event: _, ...rest } = binding.value
    return {
      category: rest.category || '',
      label: rest.label || '',
      ...(rest.value !== undefined ? { value: rest.value } : {}),
    }
  }

  return { category: '', label: '' }
}

export function vTrackExposure(tracker: Tracker): Directive<HTMLElement> {
  const exposureCollector = new ExposureCollector(tracker)

  return {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
      const options = typeof binding.value === 'string'
        ? { event: binding.value }
        : binding.value || { event: 'exposure' }

      exposureCollector.observe(el, options)
    },

    unmounted(el: HTMLElement) {
      exposureCollector.unobserve(el)
    },
  }
}
