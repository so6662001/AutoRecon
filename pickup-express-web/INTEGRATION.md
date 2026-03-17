# Pickup Express Frontend — Module Integration Guide

This guide explains how to embed the Pickup Express Vue3 frontend as a module in a host platform, and how to use it standalone.

## 1. Embedding in a Host Vue3 App

### Plugin Mode (Recommended)

When integrating with an existing Vue3 app that has its own router and layout:

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import {
  createPickupExpress,
  createPickupExpressRouter,
  configureEmbed,
  getEmbedConfig,
  eventBus,
  EVENTS,
} from 'pickup-express-web/src/plugin'

// Configure embed before creating the app
configureEmbed({
  embedded: true,
  basePath: '/pickup',
  apiBaseUrl: '/api',  // or your host's API base
  authToken: 'your-jwt-token',  // inject from host auth
  showSidebar: false,   // host provides nav
  showHeader: false,    // host provides header
  showLogin: false,     // host handles auth
  onError: (err) => {
    // Handle 401/errors (e.g. refresh token, redirect to login)
    console.error('Pickup Express error:', err)
  },
})

const app = createApp(App)
const pinia = createPinia()
const pickupRouter = createPickupExpressRouter('/pickup')

app.use(pinia)
app.use(ElementPlus)
app.use(createPickupExpress())
app.use(pickupRouter)

// Mount your root component that includes <router-view /> and routes to Pickup Express
app.mount('#app')
```

### Standalone Mount

When you need to mount Pickup Express in a specific DOM element (e.g. a tab or iframe-like container):

```typescript
import { mountPickupExpress } from 'pickup-express-web/src/plugin'

mountPickupExpress('#pickup-container', {
  embedded: true,
  basePath: '/pickup',
  apiBaseUrl: '/api',
  authToken: 'your-jwt-token',
  showSidebar: false,
  showHeader: false,
  showLogin: false,
})
```

## 2. Standalone Usage

Run the app independently:

```bash
npm run dev
# or
npm run build && npm run preview
```

The app uses `VITE_API_BASE_URL` (default `/api`) and shows full sidebar and header.

## 3. Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `embedded` | boolean | `false` | Whether running inside a host platform |
| `basePath` | string | `'/'` | Base path for routes (e.g. `/pickup`) |
| `apiBaseUrl` | string | env `VITE_API_BASE_URL` or `'/api'` | API base URL |
| `authToken` | string | - | JWT or session token (host injects) |
| `showSidebar` | boolean | `true` | Show/hide sidebar |
| `showHeader` | boolean | `true` | Show/hide header |
| `showLogin` | boolean | `true` | Show login page (false = host handles auth) |
| `onNavigate` | (path) => void | - | Callback when module navigates |
| `onLogout` | () => void | - | Callback when user logs out |
| `onError` | (error) => void | - | Callback on 401/errors (instead of redirect) |

## 4. Cross-Module Communication (Event Bus)

Use the shared `eventBus` for communication between Pickup Express, AutoRecon, and the host app.

### Subscribe to Events

```typescript
import { eventBus, EVENTS } from 'pickup-express-web/src/plugin'

// Listen for settlement created (Pickup Express emits)
eventBus.on(EVENTS.PICKUP_SETTLEMENT_CREATED, (settlement) => {
  // Sync to AutoRecon or refresh host UI
})

// Listen for delivery completed
eventBus.on(EVENTS.PICKUP_DELIVERY_COMPLETED, (data) => {
  // Update host dashboard
})

// Listen for shared events
eventBus.on(EVENTS.USER_LOGOUT, () => {
  // Clear host auth state
})
```

### Emit Events from Pickup Express

Inside Pickup Express views, import and emit:

```typescript
import { eventBus, EVENTS } from '@/utils/event-bus'

// When a settlement is created
eventBus.emit(EVENTS.PICKUP_SETTLEMENT_CREATED, { id: 123, ... })

// When delivery is completed
eventBus.emit(EVENTS.PICKUP_DELIVERY_COMPLETED, { pickupId: 456, ... })
```

### Pre-defined Events

| Event | Emitter | Payload |
|-------|---------|---------|
| `PICKUP_SETTLEMENT_CREATED` | Pickup Express | Settlement object |
| `PICKUP_DELIVERY_COMPLETED` | Pickup Express | Delivery data |
| `RECON_BILL_CONFIRMED` | AutoRecon | Bill object |
| `RECON_SETTLEMENT_SYNCED` | AutoRecon | Sync result |
| `USER_LOGOUT` | Either | - |
| `ENTERPRISE_CHANGED` | Either | Enterprise info |

## 5. Host App Integration Checklist

1. **Configure embed** before mounting: `configureEmbed({ ... })`
2. **Inject auth token** via `authToken` or ensure `localStorage.setItem('token', ...)` before mount
3. **Set `showLogin: false`** when host handles login
4. **Provide `onError`** to handle 401 (refresh token, redirect)
5. **Wire event bus** to sync data between modules (e.g. settlement → AutoRecon)
