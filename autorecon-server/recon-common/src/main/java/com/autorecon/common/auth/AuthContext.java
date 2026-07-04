package com.autorecon.common.auth;

/**
 * Authentication context interface for embeddable module integration.
 * Host platforms can provide their own implementation to integrate with existing auth systems.
 */
public interface AuthContext {

    /**
     * Get current user ID.
     *
     * @return current user ID, or null if not authenticated
     */
    Long getCurrentUserId();

    /**
     * Get current enterprise ID.
     *
     * @return current enterprise ID, or null if not set
     */
    Long getCurrentEnterpriseId();

    /**
     * Get current username.
     *
     * @return current username, or null if not authenticated
     */
    String getCurrentUsername();

    /**
     * Get current enterprise name.
     *
     * @return current enterprise name, or null if not set
     */
    String getCurrentEnterpriseName();

    /**
     * Check if current user has the given role.
     *
     * @param role role name to check
     * @return true if user has the role
     */
    boolean hasRole(String role);

    /**
     * Check if current user is authenticated.
     *
     * @return true if authenticated
     */
    boolean isAuthenticated();
}
