import type { App } from 'vue'
import type { Tracker } from '../core/tracker'
import { vTrack, vTrackExposure } from '../directives/v-track'

export const TRACKER_KEY = Symbol('analytics-tracker')

export const analyticsVuePlugin = {
  install(app: App, options: { tracker: Tracker }): void {
    const { tracker } = options

    app.provide(TRACKER_KEY, tracker)

    app.directive('track', vTrack(tracker))
    app.directive('track-exposure', vTrackExposure(tracker))
  },
}
