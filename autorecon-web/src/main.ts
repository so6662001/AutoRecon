import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/global.scss'
import { createAnalytics } from '@/analytics'
import { request } from '@/utils/request'
import { createApiMonitorInterceptor } from '@/analytics/collectors/api-monitor'

const app = createApp(App)
const pinia = createPinia()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

const analytics = createAnalytics({
  system: 'autorecon',
  endpoint: '/api/analytics/collect',
  batchSize: 20,
  flushInterval: 10000,
  sampleRate: 1.0,
  enablePerformance: true,
  enableApiMonitor: true,
  enableErrorCapture: true,
  enableScrollDepth: true,
  enableExposure: true,
  debug: import.meta.env.DEV,
  privacyMode: {
    maskPhone: true,
    noInputCapture: true,
    amountAsRange: true,
  },
})

app.use(analytics.vuePlugin)
analytics.routerPlugin(router)

const apiInterceptors = createApiMonitorInterceptor(analytics.tracker)
request.interceptors.request.use(apiInterceptors.request)
request.interceptors.response.use(apiInterceptors.response, apiInterceptors.responseError)

app.mount('#app')
