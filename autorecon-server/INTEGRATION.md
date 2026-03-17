# AutoRecon Module Integration Guide

This guide explains how to embed AutoRecon as a module within an existing platform.

## 1. Maven Dependency

Add AutoRecon as a dependency:

```xml
<dependency>
    <groupId>com.autorecon</groupId>
    <artifactId>recon-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Or for finer control, use individual modules:

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
```

## 2. Component Scan

Ensure your Spring Boot application scans the AutoRecon packages:

```java
@SpringBootApplication(scanBasePackages = {"com.yourplatform", "com.autorecon"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## 3. AuthContext Implementation

To integrate with your platform's authentication, implement the `AuthContext` interface and register it as a bean. AutoRecon will use your implementation instead of the default.

```java
@Component
public class YourPlatformAuthContext implements AuthContext {

    @Override
    public Long getCurrentUserId() {
        // Get from your SecurityContext, JWT, Session, etc.
        return YourAuthService.getCurrentUserId();
    }

    @Override
    public Long getCurrentEnterpriseId() {
        return YourAuthService.getCurrentEnterpriseId();
    }

    @Override
    public String getCurrentUsername() {
        return YourAuthService.getCurrentUsername();
    }

    @Override
    public String getCurrentEnterpriseName() {
        return YourAuthService.getCurrentEnterpriseName();
    }

    @Override
    public boolean hasRole(String role) {
        return YourAuthService.hasRole(role);
    }

    @Override
    public boolean isAuthenticated() {
        return YourAuthService.isAuthenticated();
    }
}
```

The `DefaultAuthContext` uses ThreadLocal and is replaced when you provide your own bean. Your `AuthInterceptor` (or equivalent) should call `DefaultAuthContext.setAuthInfo()` before AutoRecon controllers execute, or implement `AuthContext` to read from your auth storage.

## 4. Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
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

## 5. API Prefix Customization

AutoRecon controllers use `/api/v1/...` paths. To mount under a different prefix (e.g. `/platform/autorecon/api`), configure a context path or use a reverse proxy. The `autorecon.api-prefix` property is reserved for future use.

## 6. Module Enable/Disable

Use `autorecon.module.*` properties to enable or disable specific features. Disabled modules can be wired to return 404 or empty data via custom configuration.

## 7. Demo Mode Startup

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
