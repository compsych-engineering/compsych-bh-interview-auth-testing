package com.compsych.interview.e2e;

/** Everything environment-specific, read from env vars so the suite can point at any deployment. */
final class Config {

    /** The running app under test. */
    static final String BASE_URL = stripTrailingSlash(env("BASE_URL", "http://localhost:8099"));

    /** Credentials for an administrator. The suite only ever signs in as this user. */
    static final String ADMIN_USER = env("TEST_ADMIN_USER", "admin");
    static final String ADMIN_PASSWORD = env("TEST_ADMIN_PASSWORD", "password");

    /** Set HEADED=true to watch the browser. */
    static final boolean HEADED = Boolean.parseBoolean(env("HEADED", "false"));

    /** Optional installed-browser channel (e.g. "chrome") instead of Playwright's bundled Chromium. */
    static final String BROWSER_CHANNEL = env("PW_CHANNEL", "");

    private Config() {
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
