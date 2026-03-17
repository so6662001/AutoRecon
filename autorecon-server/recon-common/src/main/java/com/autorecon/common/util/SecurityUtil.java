package com.autorecon.common.util;

import com.autorecon.common.auth.DefaultAuthContext;

/**
 * Security utility for current user context.
 * Delegates to AuthContext (DefaultAuthContext uses ThreadLocal).
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * Get current user ID.
     *
     * @return current user ID, or null if not authenticated
     */
    public static Long getCurrentUserId() {
        var info = DefaultAuthContext.getCurrentAuthInfo();
        return info != null ? info.userId() : null;
    }

    /**
     * Get current enterprise ID.
     *
     * @return current enterprise ID, or null if not set
     */
    public static Long getCurrentEnterpriseId() {
        var info = DefaultAuthContext.getCurrentAuthInfo();
        return info != null ? info.enterpriseId() : null;
    }

    /**
     * Get current username.
     *
     * @return current username, or null if not authenticated
     */
    public static String getCurrentUsername() {
        var info = DefaultAuthContext.getCurrentAuthInfo();
        return info != null ? info.username() : null;
    }

    /**
     * Get current enterprise name.
     *
     * @return current enterprise name, or null if not set
     */
    public static String getCurrentEnterpriseName() {
        var info = DefaultAuthContext.getCurrentAuthInfo();
        return info != null ? info.enterpriseName() : null;
    }

    /**
     * Check if current user is authenticated.
     *
     * @return true if authenticated
     */
    public static boolean isAuthenticated() {
        var info = DefaultAuthContext.getCurrentAuthInfo();
        return info != null && info.userId() != null;
    }
}
