import { createApp } from 'vue'
import type { App } from 'vue'
import type { EmbedConfig } from './config/embed'
import { configureEmbed } from './config/embed'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createPickupExpressRouter } from './router'
import AppComponent from './App.vue'
import './styles/global.scss'

/**
 * Pickup Express Vue plugin for embedding in a host platform.
 * Use createPickupExpress(config) for plugin mode, or mountPickupExpress(element, config) for standalone mount.
 */
export interface PickupExpressPlugin {
  install: (app: App) => void
}

/**
 * Creates the Pickup Express plugin for use with app.use().
 * Call configureEmbed(config) before or pass config here.
 */
export function createPickupExpress(config: Partial<EmbedConfig> = {}): PickupExpressPlugin {
  return {
    install(app: App) {
      configureEmbed(config)
      for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
        app.component(key, component)
      }
      app.component('PickupExpressApp', AppComponent)
    },
  }
}

/**
 * Mounts Pickup Express as a standalone app in an element (for embedding in host platform).
 * Uses its own router and Pinia instance. For full host integration, use createPickupExpress + createPickupExpressRouter.
 */
export function mountPickupExpress(element: Element | string, config: Partial<EmbedConfig> = {}) {
  configureEmbed(config)
  const app = createApp(AppComponent)
  const pinia = createPinia()
  const router = createPickupExpressRouter(config.basePath)

  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.use(pinia)
  app.use(router)
  app.use(ElementPlus)
  const el = typeof element === 'string' ? document.querySelector(element) : element
  if (el) app.mount(el)
  return app
}

export { configureEmbed, getEmbedConfig, isEmbedded } from './config/embed'
export { createPickupExpressRouter } from './router'
export { eventBus, EVENTS } from './utils/event-bus'
