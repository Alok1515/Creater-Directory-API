package com.creator.security;

/**
 * TenantContext holds the current request's tenant information using ThreadLocal.
 *
 * This is the foundation of our tenant isolation strategy. Every incoming request
 * is resolved to a specific user, agency, and role. All downstream service and
 * repository calls use this context to scope data access — making it structurally
 * difficult for a future engineer to accidentally write a query that leaks data
 * across tenants.
 *
 * The filter (TenantContextFilter) populates this context at the start of each
 * request, and it is cleared after the request completes to prevent thread leakage.
 */
public final class TenantContext {

    private TenantContext() {
        // Utility class — prevent instantiation
    }

    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_AGENCY_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_ROLE = new ThreadLocal<>();

    // --- User ID ---

    public static void setCurrentUserId(String userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static String getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    // --- Agency ID ---

    public static void setCurrentAgencyId(String agencyId) {
        CURRENT_AGENCY_ID.set(agencyId);
    }

    public static String getCurrentAgencyId() {
        return CURRENT_AGENCY_ID.get();
    }

    // --- User Role ---

    public static void setCurrentUserRole(String role) {
        CURRENT_USER_ROLE.set(role);
    }

    public static String getCurrentUserRole() {
        return CURRENT_USER_ROLE.get();
    }

    // --- Cleanup ---

    /**
     * Must be called after every request to prevent ThreadLocal leakage
     * in pooled thread environments.
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_AGENCY_ID.remove();
        CURRENT_USER_ROLE.remove();
    }
}
