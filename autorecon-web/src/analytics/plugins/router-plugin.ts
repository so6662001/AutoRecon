import type { Router } from 'vue-router'
import type { Tracker } from '../core/tracker'
import { PageViewCollector } from '../collectors/page-view'
import { ScrollCollector } from '../collectors/scroll'

export function createRouterPlugin(tracker: Tracker): (router: Router) => void {
  return (router: Router) => {
    const pageViewCollector = new PageViewCollector(tracker)
    pageViewCollector.install(router)

    const scrollCollector = new ScrollCollector(tracker)
    scrollCollector.install()

    router.afterEach(() => {
      scrollCollector.reset()
    })
  }
}
