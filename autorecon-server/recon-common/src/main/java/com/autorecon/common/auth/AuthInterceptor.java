package com.autorecon.common.auth;

import com.autorecon.common.config.AutoReconProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Auth interceptor - extracts token and sets auth context.
 * For demo mode, accepts any token or no token and sets default user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AutoReconProperties autoReconProperties;

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

        if (autoReconProperties.isDemoMode()) {
            String token = extractToken(request);
            if (token != null && DEMO_TOKENS.contains(token.trim())) {
                setDemoAuth();
                return true;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        if (!autoReconProperties.getAuth().isEnabled()) {
            setDemoAuth();
            return true;
        }

        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // In production, host platform should provide AuthTokenResolver bean to resolve token
        // For default, we don't have token resolution - host must implement
        DefaultAuthContext.setAuthInfo(null, null, null, null, null);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
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
        java.util.List<String> patterns = new java.util.ArrayList<>(autoReconProperties.getAuth().getExcludePaths());
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
        String header = request.getHeader(autoReconProperties.getAuth().getTokenHeader());
        if (header == null || !header.startsWith(autoReconProperties.getAuth().getTokenPrefix())) {
            return null;
        }
        return header.substring(autoReconProperties.getAuth().getTokenPrefix().length()).trim();
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
