# AutoRecon Module Integration Guide

This guide explains how to embed AutoRecon as a Spring Boot Starter module within an existing platform.

## 1. Maven Dependency

Add AutoRecon as a dependency to your host application:

```xml
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

The module is controlled by `@ConditionalOnProperty(prefix = "autorecon", name = "enabled")`. When `autorecon.enabled=false`, the entire module (controllers, services, etc.) is disabled.

| Property | Default | Description |
|----------|---------|-------------|
| `autorecon.enabled` | `true` | Enable/disable the AutoRecon module. Set to `false` to exclude from host app. |
| `autorecon.api-prefix` | `/api` | API path prefix |
| `autorecon.demo-mode` | `false` | Enable demo mode (relaxed auth) |
| `autorecon.auth.enabled` | `true` | Enable auth interceptor |
| `autorecon.auth.token-header` | `Authorization` | Header for token |
| `autorecon.auth.token-prefix` | `Bearer ` | Token prefix |
| `autorecon.auth.exclude-paths` | `["/api/v1/guest/**", "/api/v1/users/login"]` | Paths excluded from auth |
| `autorecon.module.recon-enabled` | `true` | Enable recon module |
| `autorecon.module.sign-enabled` | `true` | Enable sign module |
| `autorecon.module.collection-enabled` | `true` | Enable collection module |
| `autorecon.module.finance-enabled` | `true` | Enable finance module |
| `autorecon.module.billing-enabled` | `true` | Enable billing module |
| `autorecon.module.engagement-enabled` | `true` | Enable engagement module |

Example `application.yml`:

```yaml
autorecon:
  api-prefix: /api
  demo-mode: false
  auth:
    enabled: true
    token-header: Authorization
    token-prefix: "Bearer "
    exclude-paths:
      - /api/v1/guest/**
      - /api/v1/users/login
  module:
    recon-enabled: true
    sign-enabled: true
```

## 4. Module Enable/Disable

To disable the AutoRecon module entirely in your host application:

```yaml
autorecon:
  enabled: false
```

Use `autorecon.module.*` properties to enable or disable specific features within the module.

## 5. API Prefix Customization

AutoRecon controllers use `/api/v1/...` paths. To mount under a different prefix (e.g. `/platform/autorecon/api`), configure a context path or use a reverse proxy. The `autorecon.api-prefix` property is reserved for future use.

## 6. Demo Mode Startup

To run AutoRecon in demo mode with H2 and pre-loaded steel industry data:

```bash
mvn -pl recon-web spring-boot:run -Dspring-boot.run.profiles=demo
```

Or with a packaged JAR:

```bash
java -jar recon-web/target/recon-web-1.0.0-SNAPSHOT.jar --spring.profiles.active=demo
```

**Requirements for demo:**
- No MySQL required (uses H2 in-memory)
- Redis on localhost:6379 (optional; bill number generation falls back if Redis is unavailable)
- RabbitMQ listeners are disabled

**Demo accounts:**
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | Platform admin |
| seller1 | 123456 | Seller admin |
| seller2 | 123456 | Seller admin |
| buyer1 | 123456 | Buyer admin |
| buyer2 | 123456 | Buyer admin |
| buyer3 | 123456 | Buyer admin |

**Demo URLs:**
- API docs: http://localhost:8080/doc.html
- H2 console: http://localhost:8080/h2-console (JDBC: `jdbc:h2:mem:autorecon_demo`, User: `sa`, Password: empty)

In demo mode, auth is disabled and a default user context is set. You can also pass `Authorization: Bearer demo:userId:enterpriseId` to simulate different users.

## 7. Domain model notes (design vs implementation)

Some design documents name entities that are intentionally modeled differently in this codebase:

| Design abstraction | Implementation |
|--------------------|----------------|
| **TemplateField** | JSON columns on `recon_template` (`header_config`, `column_config`, etc.) for flexibility instead of a separate table. |
| **MatchResult** | Match outcome stored on `recon_bill_item` (`match_status`, `buyer_*`, `diff_*`, etc.). |
| **AutoReconLog** | Use `audit_log` with `target_type = 'AUTO_RECON_PLAN'`. |
| **SealQuotaPackage** | Seal quota tracked via `service_usage`. |

No separate entities are required for these; the above is the intended design.
