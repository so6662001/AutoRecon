# Module Integration Guide — Host Platform + AutoRecon + Pickup Express

This document describes how to integrate AutoRecon and Pickup Express as embeddable modules within a host platform, including backend (Spring Boot) and frontend (Vue3) integration, shared authentication, and cross-module data sync.

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Host Platform (Vue3 + Spring Boot)               │
├─────────────────────────────────────────────────────────────────────────┤
│  Frontend                                                                │
│  ┌──────────────┐  ┌─────────────────┐  ┌─────────────────────┐         │
│  │ Host App     │  │ AutoRecon       │  │ Pickup Express      │         │
│  │ (Router,     │  │ (plugin.ts)     │  │ (plugin.ts)         │         │
│  │  Layout)     │  │ /recon/*        │  │ /pickup/*           │         │
│  └──────┬───────┘  └────────┬────────┘  └──────────┬──────────┘         │
│         │                   │                      │                    │
│         └───────────────────┼──────────────────────┘                    │
│                             │ eventBus (cross-module events)             │
├─────────────────────────────────────────────────────────────────────────┤
│  Backend                                                                 │
│  ┌──────────────┐  ┌─────────────────┐  ┌─────────────────────┐         │
│  │ Host Auth    │  │ AutoRecon       │  │ Pickup Express       │         │
│  │ (JWT, etc.)  │  │ (recon-web)     │  │ (pe-web)             │         │
│  └──────┬───────┘  └────────┬────────┘  └──────────┬──────────┘         │
│         │                   │                      │                    │
│         └───────────────────┼──────────────────────┘                    │
│                             │ AuthTokenResolver (shared auth)            │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Host App**: Owns router, layout, and authentication. Injects token and config into modules.
- **AutoRecon**: Reconciliation, bills, settlements, payments. Mounted at `/recon`.
- **Pickup Express**: Pickup orders, delivery, settlements. Mounted at `/pickup`.
- **eventBus**: Enables cross-module events (e.g. settlement created in Pickup → sync to AutoRecon).

---

## 2. Backend Integration

### 2.1 Maven Dependencies

Add both modules to your host Spring Boot application:

```xml
<!-- AutoRecon -->
<dependency>
    <groupId>com.autorecon</groupId>
    <artifactId>recon-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.autorecon</groupId>
    <artifactId>recon-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.autorecon</groupId>
    <artifactId>recon-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Pickup Express -->
<dependency>
    <groupId>com.pickupexpress</groupId>
    <artifactId>pe-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.pickupexpress</groupId>
    <artifactId>pe-domain</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.pickupexpress</groupId>
    <artifactId>pe-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.pickupexpress</groupId>
    <artifactId>pe-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2.2 AuthTokenResolver

Both modules use `AuthTokenResolver` to validate tokens and set user context. Implement once for the host platform:

```java
@Component
public class HostPlatformAuthTokenResolver implements AuthTokenResolver {

    @Override
    public AuthInfo resolve(String token) {
        YourUser user = yourAuthService.validateToken(token);
        if (user == null) return null;
        return AuthInfo.builder()
                .userId(user.getId())
                .enterpriseId(user.getEnterpriseId())
                .username(user.getUsername())
                .enterpriseName(user.getEnterpriseName())
                .roles(String.join(",", user.getRoles()))
                .build();
    }
}
```

**Note**: AutoRecon uses `com.autorecon.common.auth.AuthTokenResolver` and Pickup Express uses `com.pickupexpress.common.auth.AuthTokenResolver`. If both modules are in the same host, you may need to implement both interfaces (or create an adapter) unless they share a common auth module.

### 2.3 Configuration

```yaml
# application.yml
autorecon:
  enabled: true
  api-prefix: /api
  auth:
    enabled: true
    exclude-paths:
      - /api/v1/guest/**
      - /api/v1/users/login

pickup-express:
  enabled: true
  api-prefix: /api
  auth:
    enabled: true
    exclude-paths:
      - /api/v1/guest/**
      - /api/v1/users/login
```

---

## 3. Frontend Integration

### 3.1 Host Vue3 App Setup

```typescript
// main.ts or host app entry
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import {
  createAutoRecon,
  createReconRouter,
  configureEmbed as configureRecon,
} from 'autorecon-web/src/plugin'
import {
  createPickupExpress,
  createPickupExpressRouter,
  configureEmbed as configurePickup,
  eventBus,
  EVENTS,
} from 'pickup-express-web/src/plugin'

const token = 'your-jwt-from-host-auth'

configureRecon({
  embedded: true,
  basePath: '/recon',
  apiBaseUrl: '/api',
  authToken: token,
  showSidebar: false,
  showHeader: false,
  showLogin: false,
  onError: (err) => handleAuthError(err),
})

configurePickup({
  embedded: true,
  basePath: '/pickup',
  apiBaseUrl: '/api',
  authToken: token,
  showSidebar: false,
  showHeader: false,
  showLogin: false,
  onError: (err) => handleAuthError(err),
})

const app = createApp(HostApp)
const pinia = createPinia()
const router = createHostRouter()  // Your router with /recon and /pickup routes

app.use(pinia)
app.use(ElementPlus)
app.use(createAutoRecon())
app.use(createPickupExpress())
// Merge module routes or use router.addRoute for /recon and /pickup
app.use(router)
app.mount('#app')
```

### 3.2 Standalone Mount (Alternative)

If you prefer to mount each module in a dedicated container:

```typescript
import { mountAutoRecon } from 'autorecon-web/src/plugin'
import { mountPickupExpress } from 'pickup-express-web/src/plugin'

// When user navigates to /recon
mountAutoRecon('#recon-container', { embedded: true, basePath: '/recon', ... })

// When user navigates to /pickup
mountPickupExpress('#pickup-container', { embedded: true, basePath: '/pickup', ... })
```

---

## 4. Shared Authentication Flow

1. **Host** authenticates user and obtains JWT (or session token).
2. **Host** calls `configureEmbed({ authToken: token, showLogin: false })` for each module before mount.
3. **Host** may also set `localStorage.setItem('token', token)` so module stores read it.
4. **Modules** send `Authorization: Bearer <token>` on API requests.
5. **Backend** `AuthTokenResolver` validates token and sets `AuthContext` (userId, enterpriseId, etc.).
6. On **401**, modules call `config.onError(err)` instead of redirecting; host refreshes token or redirects to login.

---

## 5. Cross-Module Data Sync (Event Bus)

Use the shared `eventBus` to sync data between Pickup Express and AutoRecon.

### 5.1 Settlement: Pickup Express → AutoRecon

When a settlement is created in Pickup Express, AutoRecon can sync it:

```typescript
// In host app or AutoRecon bootstrap
import { eventBus, EVENTS } from 'pickup-express-web/src/plugin'

eventBus.on(EVENTS.PICKUP_SETTLEMENT_CREATED, (settlement) => {
  // Call AutoRecon API to sync settlement, or refresh AutoRecon bill list
  syncSettlementToRecon(settlement)
})
```

### 5.2 Pre-defined Events

| Event | Emitter | Use Case |
|-------|---------|----------|
| `PICKUP_SETTLEMENT_CREATED` | Pickup Express | Sync new settlement to AutoRecon |
| `PICKUP_DELIVERY_COMPLETED` | Pickup Express | Update host dashboard |
| `RECON_BILL_CONFIRMED` | AutoRecon | Notify host or Pickup |
| `RECON_SETTLEMENT_SYNCED` | AutoRecon | Refresh Pickup or host |
| `USER_LOGOUT` | Either | Clear host auth |
| `ENTERPRISE_CHANGED` | Either | Refresh enterprise context |

### 5.3 Emitting from Modules

Inside Pickup Express (e.g. settlement list view):

```typescript
import { eventBus, EVENTS } from '@/utils/event-bus'

eventBus.emit(EVENTS.PICKUP_SETTLEMENT_CREATED, { id: 123, amount: 1000, ... })
```

---

## 6. Configuration Reference

### AutoRecon Embed Config

| Option | Default | Description |
|--------|---------|-------------|
| `embedded` | `false` | Running in host |
| `basePath` | `'/'` | Route base (e.g. `/recon`) |
| `apiBaseUrl` | env | API base URL |
| `authToken` | - | JWT from host |
| `showSidebar` | `true` | Show sidebar |
| `showHeader` | `true` | Show header |
| `showLogin` | `true` | Show login page |
| `onError` | - | 401/error callback |

### Pickup Express Embed Config

Same structure as AutoRecon. See `pickup-express-web/INTEGRATION.md` for details.

---

## 7. Build and Deploy

- **Standalone**: Each frontend builds independently (`npm run build`).
- **Embedded**: Host app imports from module source (e.g. monorepo workspace) or pre-built assets. For Vite library mode, configure `build.lib` in each module's `vite.config.ts` if publishing as npm packages.

---

## 8. Related Documentation

- `autorecon-web`: See `src/plugin.ts` and `src/config/embed.ts`
- `pickup-express-web/INTEGRATION.md`: Frontend integration details
- `autorecon-server/INTEGRATION.md`: AutoRecon backend integration
- `pickup-express-server/INTEGRATION.md`: Pickup Express backend integration
