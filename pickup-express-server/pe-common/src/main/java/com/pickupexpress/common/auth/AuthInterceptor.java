package com.pickupexpress.common.auth;

import com.pickupexpress.common.config.PickupExpressProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Auth interceptor - extracts token and sets auth context.
 * For demo mode, accepts any token or no token and sets default user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final PickupExpressProperties pickupExpressProperties;
    private final Optional<AuthTokenResolver> authTokenResolver;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** Predefined demo tokens - only these are accepted in demo mode */
    private static final Set<String> DEMO_TOKENS = Set.of("demo_token_1", "demo_token_2", "demo_token_admin");

    /** Default demo user/enterprise when auth is disabled or in demo mode */
    private static final long DEMO_USER_ID = 1L;
    private static final long DEMO_ENTERPRISE_ID = 1L;
    private static final String DEMO_USERNAME = "admin";
    private static final String DEMO_ENTERPRISE_NAME = "华东钢铁贸易有限公司";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (isExcludedPath(path)) {
            return true;
        }

        if (pickupExpressProperties.isDemoMode()) {
            String token = extractToken(request);
            if (token != null && DEMO_TOKENS.contains(token.trim())) {
                setDemoAuth();
                return true;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        if (!pickupExpressProperties.getAuth().isEnabled()) {
            setDemoAuth();
            return true;
        }

        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        if (authTokenResolver.isEmpty()) {
            log.warn("Token present but no AuthTokenResolver bean configured - host must implement AuthTokenResolver");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        AuthTokenResolver.AuthInfo info = authTokenResolver.get().resolve(token);
        if (info != null && info.getUserId() != null) {
            Set<String> roles = parseRoles(info.getRoles());
            DefaultAuthContext.setAuthInfo(
                    info.getUserId(),
                    info.getEnterpriseId(),
                    info.getUsername(),
                    info.getEnterpriseName(),
                    roles
            );
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    private Set<String> parseRoles(String rolesStr) {
        if (rolesStr == null || rolesStr.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        DefaultAuthContext.clearAuthInfo();
    }

    private boolean isExcludedPath(String path) {
        for (String pattern : getExcludePatterns()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private java.util.List<String> getExcludePatterns() {
        java.util.List<String> patterns = new java.util.ArrayList<>(pickupExpressProperties.getAuth().getExcludePaths());
        patterns.add("/api/v1/guest/view/**");
        patterns.add("/api/v1/guest/verify-phone");
        patterns.add("/api/v1/guest/confirm/**");
        patterns.add("/api/v1/users/login");
        patterns.add("/doc.html");
        patterns.add("/swagger-resources/**");
        patterns.add("/v3/api-docs/**");
        patterns.add("/webjars/**");
        patterns.add("/favicon.ico");
        return patterns;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(pickupExpressProperties.getAuth().getTokenHeader());
        if (header == null || !header.startsWith(pickupExpressProperties.getAuth().getTokenPrefix())) {
            return null;
        }
        return header.substring(pickupExpressProperties.getAuth().getTokenPrefix().length()).trim();
    }

    private void setDemoAuth() {
        DefaultAuthContext.setAuthInfo(
                DEMO_USER_ID,
                DEMO_ENTERPRISE_ID,
                DEMO_USERNAME,
                DEMO_ENTERPRISE_NAME,
                Set.of("admin", "platform_admin")
        );
    }
}
