import type { App } from 'vue'
import type { Router } from 'vue-router'
import type { AnalyticsConfig, AnalyticsEvent, PageInfo, UserInfo } from './types'
import { EventType } from './types'
import { Tracker } from './core/tracker'

let globalTracker: Tracker | null = null

export function createAnalytics(config: AnalyticsConfig) {
  const tracker = new Tracker(config)
  globalTracker = tracker

  const vuePlugin = {
    install(app: App) {
      app.config.globalProperties.$analytics = tracker
      app.provide('analytics', tracker)
    },
  }

  const routerPlugin = (router: Router) => {
    let enterTime = Date.now()

    router.afterEach((to, from) => {
      const now = Date.now()
      const duration = now - enterTime
      enterTime = now

      if (from.path && from.path !== to.path) {
        tracker.track(EventType.PAGE_LEAVE, 'page_leave', {
          page: {
            path: from.path,
            name: from.name as string,
            title: document.title,
            duration,
          },
        } as Partial<AnalyticsEvent>)
      }

      tracker.setPage({
        path: to.path,
        name: to.name as string,
        title: document.title,
        referrer: from.path,
      })

      tracker.track(EventType.PAGE_VIEW, 'page_view', {
        page: {
          path: to.path,
          name: to.name as string,
          title: document.title,
          referrer: from.path,
        },
      } as Partial<AnalyticsEvent>)
    })
  }

  return { tracker, vuePlugin, routerPlugin, useAnalytics }
}

export function useAnalytics() {
  const tracker = globalTracker
  if (!tracker) {
    console.warn('[Analytics] SDK not initialized. Call createAnalytics() first.')
  }

  return {
    track(name: string, data?: Partial<AnalyticsEvent>) {
      tracker?.track(EventType.ACTION, name, data)
    },

    trackSearch(page: string, data?: Partial<AnalyticsEvent>) {
      tracker?.track(EventType.SEARCH, 'search', {
        ...data,
        page: { path: page, ...data?.page },
      } as Partial<AnalyticsEvent>)
    },

    trackFormSubmit(form: string, data?: Partial<AnalyticsEvent>) {
      tracker?.track(EventType.FORM_SUBMIT, 'form_submit', {
        ...data,
        action: { element: 'form', elementId: form, ...data?.action },
      } as Partial<AnalyticsEvent>)
    },

    identify(user: UserInfo) {
      tracker?.identify(user)
    },

    setPage(page: Partial<PageInfo>) {
      tracker?.setPage(page)
    },
  }
}

export { EventType } from './types'
export type {
  AnalyticsConfig,
  AnalyticsEvent,
  SessionInfo,
  UserInfo,
  DeviceInfo,
  PageInfo,
  ActionInfo,
  PerformanceInfo,
  ApiInfo,
  ErrorInfo,
} from './types'
