package com.zmm.mcp.auth;

/**
 * 持有当前请求的 API-Key，通过 ThreadLocal 在请求线程内传递
 */
public class ApiKeyContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    public static void set(String apiKey) {
        HOLDER.set(apiKey);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
