package config;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局测试上下文，用于在不同的测试用例或测试类之间安全地传递状态/ID
 */
public class TestContext {
    private static final ConcurrentHashMap<String, String> dataStore = new ConcurrentHashMap<>();

    public static void set(String key, String value) {
        dataStore.put(key, value);
    }

    public static String get(String key) {
        return dataStore.get(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        return dataStore.getOrDefault(key, defaultValue);
    }

    public static boolean containsKey(String key) {
        return dataStore.containsKey(key);
    }

    public static void clear() {
        dataStore.clear();
    }
}