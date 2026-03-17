package com.pickupexpress.common.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default AuthContext implementation using ThreadLocal.
 * Host platform can replace this bean with their own implementation.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(AuthContext.class)
public class DefaultAuthContext implements AuthContext {

    private static final ThreadLocal<AuthInfo> AUTH_INFO = new ThreadLocal<>();

    @Override
    public Long getCurrentUserId() {
        AuthInfo info = AUTH_INFO.get();
        return info != null ? info.userId : null;
    }

    @Override
    public Long getCurrentEnterpriseId() {
        AuthInfo info = AUTH_INFO.get();
        return info != null ? info.enterpriseId : null;
    }

    @Override
    public String getCurrentUsername() {
        AuthInfo info = AUTH_INFO.get();
        return info != null ? info.username : null;
    }

    @Override
    public String getCurrentEnterpriseName() {
        AuthInfo info = AUTH_INFO.get();
        return info != null ? info.enterpriseName : null;
    }

    @Override
    public boolean hasRole(String role) {
        AuthInfo info = AUTH_INFO.get();
        if (info == null || info.roles == null) {
            return false;
        }
        return info.roles.contains(role);
    }

    @Override
    public boolean isAuthenticated() {
        AuthInfo info = AUTH_INFO.get();
        return info != null && info.userId != null;
    }

    /**
     * Set auth info for current thread. Called by AuthInterceptor.
     */
    public static void setAuthInfo(Long userId, Long enterpriseId, String username, String enterpriseName, java.util.Set<String> roles) {
        if (userId != null || enterpriseId != null || username != null) {
            AUTH_INFO.set(new AuthInfo(userId, enterpriseId, username, enterpriseName, roles));
        } else {
            AUTH_INFO.remove();
        }
    }

    /**
     * Clear auth info for current thread. Called by AuthInterceptor after request.
     */
    public static void clearAuthInfo() {
        AUTH_INFO.remove();
    }

    /**
     * Get current auth info for setting. Used when host needs to inject auth from token.
     */
    public static void setAuthInfo(AuthInfo info) {
        if (info != null && (info.userId != null || info.enterpriseId != null || info.username != null)) {
            AUTH_INFO.set(info);
        } else {
            AUTH_INFO.remove();
        }
    }

    /**
     * Get current auth info. Used by SecurityUtil.
     */
    public static AuthInfo getCurrentAuthInfo() {
        return AUTH_INFO.get();
    }

    public record AuthInfo(Long userId, Long enterpriseId, String username, String enterpriseName, java.util.Set<String> roles) {
        public AuthInfo {
            roles = roles != null && !roles.isEmpty()
                    ? java.util.Collections.unmodifiableSet(new java.util.HashSet<>(roles))
                    : java.util.Collections.emptySet();
        }
    }
}
