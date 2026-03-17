# AutoRecon Web Integration Guide

This guide explains how to use the AutoRecon frontend as a standalone application or embed it as a module in an existing Vue 3 host platform.

## Standalone Mode

By default, the application runs in standalone mode with its own layout (sidebar, header) and authentication.

```bash
# Development
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
```

## Embedding in an Existing Vue 3 Host

### Option 1: Mount as a Sub-Application

Use `mountAutoRecon` to mount the full AutoRecon app in a DOM element. This creates an isolated Vue app with its own router and state.

```typescript
import { createApp } from 'vue'
import { mountAutoRecon } from 'autorecon-web'

// In your host app, mount AutoRecon in a container
const app = createApp(HostApp)
app.mount('#host-app')

// Mount AutoRecon in a dedicated container (e.g. when user navigates to /recon)
mountAutoRecon('#recon-container', {
  embedded: true,
  basePath: '/recon',
  apiBaseUrl: 'https://your-api.example.com/api',
  authToken: 'token-from-host',
  showSidebar: false,  // Host has its own navigation
  showHeader: false,  // Host has its own header
  showLogin: false,    // Host handles authentication
  onError: (error) => {
    // Handle 401 or other errors (e.g. redirect to host login)
    console.error('AutoRecon error:', error)
  },
  onLogout: () => {
    // Host handles logout
  },
})
```

### Option 2: Plugin Integration

Use the `createAutoRecon` plugin to integrate with your host's Vue app. This registers components and configures the embed settings.

```typescript
import { createApp } from 'vue'
import { createAutoRecon, createReconRouter } from 'autorecon-web'
import App from './App.vue'

const app = createApp(App)

app.use(createAutoRecon({
  embedded: true,
  basePath: '/recon',
  apiBaseUrl: '/api',
  showSidebar: false,
  showHeader: false,
  showLogin: false,
}))

// Add AutoRecon routes to your router
const router = createReconRouter('/recon')
app.use(router)

app.mount('#app')
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `embedded` | boolean | `false` | Whether running inside a host platform |
| `basePath` | string | `'/'` | Base path for routes (e.g. `/recon`) |
| `apiBaseUrl` | string | `VITE_API_BASE_URL` or `'/api'` | API base URL |
| `authToken` | string | - | Token injected by host (saved to localStorage) |
| `showSidebar` | boolean | `true` | Show the sidebar navigation |
| `showHeader` | boolean | `true` | Show the header bar |
| `showLogin` | boolean | `true` | Show login page (false when host handles auth) |
| `onNavigate` | (path: string) => void | - | Callback when internal navigation occurs |
| `onLogout` | () => void | - | Callback when user logs out |
| `onError` | (error: any) => void | - | Callback on 401 or request errors (embedded mode) |

## Demo Mode

Demo mode provides mock data when the backend is unavailable, useful for demos and development without a running server.

### Enabling Demo Mode

```bash
# Development with demo mode
npm run dev:demo
```

Or set `VITE_DEMO_MODE=true` in your environment (e.g. `.env.demo`).

### Demo Accounts

When demo mode is enabled, the login page shows demo account cards:

- **admin** / admin123 (系统管理员)
- **seller1** / 123456 (卖方管理员)
- **buyer1** / 123456 (买方管理员)

Click a card to auto-fill credentials and log in.

### Demo Mode Behavior

- Only activates when `VITE_DEMO_MODE=true` **and** a backend request fails
- Returns mock data for: dashboard, bill list, credit ranking, login
- Gracefully falls back when backend is available (real API is used)

## Building for Different Base Paths

### Standalone (default)

```bash
npm run build
# Output: dist/ (served at /)
```

### Embedded (e.g. under /recon/)

```bash
npm run build:embed
# Output: dist/ with assets under /recon/
```

When deploying, ensure your server serves the app at the correct base path. For example, with Nginx:

```nginx
location /recon/ {
  alias /path/to/dist/;
  try_files $uri $uri/ /recon/index.html;
}
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | API base URL | `/api` |
| `VITE_DEMO_MODE` | Enable demo mode | `false` |

## File Structure

```
src/
├── config/
│   ├── embed.ts    # Embed configuration
│   └── demo.ts     # Demo mode mock data
├── plugin.ts       # createAutoRecon, mountAutoRecon
├── utils/
│   ├── request.ts      # HTTP client (uses embed config)
│   └── demo-interceptor.ts  # Mock responses in demo mode
└── ...
```
