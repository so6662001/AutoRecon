# Pickup Express Module Integration Guide

This guide explains how to embed Pickup Express as a Spring Boot Starter module within an existing platform.

## 1. Maven Dependency

Add Pickup Express as a dependency to your host application:

```xml
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

Or add the full stack in one go (if you have an aggregator module). The module auto-configures via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` - no manual component scan required.

## 2. Implement AuthTokenResolver

To integrate with your platform's authentication, implement the `AuthTokenResolver` interface and register it as a bean. The `AuthInterceptor` will use it to resolve tokens and set auth context.

```java
@Component
public class YourPlatformAuthTokenResolver implements AuthTokenResolver {

    @Override
    public AuthInfo resolve(String token) {
        // Validate token (JWT, session, etc.) and return user/enterprise info
        YourUser user = yourAuthService.validateToken(token);
        if (user == null) {
            return null;
        }
        return AuthInfo.builder()
                .userId(user.getId())
                .enterpriseId(user.getEnterpriseId())
                .username(user.getUsername())
                .enterpriseName(user.getEnterpriseName())
                .roles(String.join(",", user.getRoles()))  // comma-separated
                .build();
    }
}
```

When a token is present but no `AuthTokenResolver` bean is configured, the interceptor returns 401 and logs a warning. Demo mode and auth-disabled behavior are unchanged.

## 3. Configuration Properties

The module is controlled by `@ConditionalOnProperty(prefix = "pickup-express", name = "enabled")`. When `pickup-express.enabled=false`, the entire module (controllers, services, etc.) is disabled.

| Property | Default | Description |
|----------|---------|-------------|
| `pickup-express.enabled` | `true` | Enable/disable the Pickup Express module. Set to `false` to exclude from host app. |
| `pickup-express.api-prefix` | `/api` | API path prefix |
| `pickup-express.demo-mode` | `false` | Enable demo mode (relaxed auth) |
| `pickup-express.auth.enabled` | `true` | Enable auth interceptor |
| `pickup-express.auth.token-header` | `Authorization` | Header for token |
| `pickup-express.auth.token-prefix` | `Bearer ` | Token prefix |
| `pickup-express.auth.exclude-paths` | `["/api/v1/guest/**", "/api/v1/users/login"]` | Paths excluded from auth |

Example `application.yml`:

```yaml
pickup-express:
  enabled: true
  api-prefix: /api
  demo-mode: false
  auth:
    enabled: true
    token-header: Authorization
    token-prefix: "Bearer "
    exclude-paths:
      - /api/v1/guest/**
      - /api/v1/users/login
```

## 4. Module Enable/Disable

To disable the Pickup Express module entirely in your host application:

```yaml
pickup-express:
  enabled: false
```

## 5. API Prefix Customization

Pickup Express controllers use `/api/v1/...` paths. To mount under a different prefix (e.g. `/platform/pickup-express/api`), configure a context path or use a reverse proxy. The `pickup-express.api-prefix` property is reserved for future use.
