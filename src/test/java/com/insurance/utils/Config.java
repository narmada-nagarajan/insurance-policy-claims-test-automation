package com.insurance.utils;

/** Central place for run-time settings; override from the command line, e.g. -Dheadless=true -Dbrowser=firefox. */
public final class Config {

    private Config() {
    }

    public static int port() {
        return Integer.getInteger("app.port", 8089);
    }

    public static String baseUrl() {
        return System.getProperty("baseUrl", "http://localhost:" + port());
    }

    public static boolean headless() {
        return Boolean.parseBoolean(System.getProperty("headless", "false"));
    }

    public static String browser() {
        return System.getProperty("browser", "chrome").toLowerCase();
    }

    /** Explicit-wait timeout in seconds. */
    public static int timeoutSeconds() {
        return Integer.getInteger("timeout", 10);
    }
}
