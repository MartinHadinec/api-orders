package com.example.orders.tenant;

import java.util.Optional;

public final class TenantContext {

    public static final String NONE = "__none__";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<String> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static String require() {
        return current().orElseThrow(
                () -> new IllegalStateException("The tenant is not configured—is the TenantFilter missing?"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
