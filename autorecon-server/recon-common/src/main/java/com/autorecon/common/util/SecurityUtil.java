package com.autorecon.common.util;

/**
 * Security utility for current user context.
 * Placeholder implementation - to be replaced with real auth (e.g. JWT, Spring Security).
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
        // TODO: Replace with real auth - get from SecurityContext/ThreadLocal
        return 1L;
    }

    /**
     * Get current enterprise ID.
     *
     * @return current enterprise ID, or null if not set
     */
    public static Long getCurrentEnterpriseId() {
        // TODO: Replace with real auth - get from SecurityContext/ThreadLocal
        return 1L;
    }
}
