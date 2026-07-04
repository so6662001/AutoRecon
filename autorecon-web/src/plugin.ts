import { createApp } from 'vue'
import type { App } from 'vue'
import type { EmbedConfig } from './config/embed'
import { configureEmbed } from './config/embed'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createReconRouter } from './router'
import AppComponent from './App.vue'
import './styles/global.scss'

/**
 * AutoRecon Vue plugin for embedding in a host platform.
 * Use createAutoRecon(config) for plugin mode, or mountAutoRecon(element, config) for standalone mount.
 */
export interface AutoReconPlugin {
  install: (app: App) => void
}

/**
 * Creates the AutoRecon plugin for use with app.use().
 * Call configureEmbed(config) before or pass config here.
 */
export function createAutoRecon(config: Partial<EmbedConfig> = {}): AutoReconPlugin {
  return {
    install(app: App) {
      configureEmbed(config)
      for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
        app.component(key, component)
      }
      app.component('AutoReconApp', AppComponent)
    },
  }
}

/**
 * Mounts AutoRecon as a standalone app in an element (for embedding in host platform).
 * Uses its own router and Pinia instance. For full host integration, use createAutoRecon + createReconRouter.
 */
export function mountAutoRecon(element: Element | string, config: Partial<EmbedConfig> = {}) {
  configureEmbed(config)
  const app = createApp(AppComponent)
  const pinia = createPinia()
  const router = createReconRouter(config.basePath)

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
export { createReconRouter } from './router'
export { eventBus, EVENTS } from './utils/event-bus'
