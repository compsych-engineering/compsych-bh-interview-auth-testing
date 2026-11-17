package com.compsych.interview;

import java.util.Random;

/** Per-request delay for account lookups, configured from the environment. */
final class Latency {

    private static final int DEFAULT_MAX_MILLIS = 3000;

    private final int maxMillis;
    private final Random random;

    Latency(int maxMillis, Random random) {
        this.maxMillis = Math.max(0, maxMillis);
        this.random = random;
    }

    static Latency fromEnv() {
        int max = intEnv("SIMULATED_LATENCY_MS", DEFAULT_MAX_MILLIS);
        String seed = System.getenv("LATENCY_SEED");
        Random random = seed == null || seed.isBlank() ? new Random() : new Random(Long.parseLong(seed.trim()));
        return new Latency(max, random);
    }

    /** Blocks the calling request thread for a random delay. */
    void pause() {
        if (maxMillis == 0) {
            return;
        }
        int min = Math.max(1, maxMillis / 10);
        long millis = min + random.nextInt(maxMillis - min + 1);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int intEnv(String name, int fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }
}
