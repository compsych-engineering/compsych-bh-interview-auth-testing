package com.compsych.interview;

/** Starts the mock auth service. Port comes from the PORT env var (default 8080). */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        String portEnv = System.getenv("PORT");
        int port = portEnv == null || portEnv.isBlank() ? 8080 : Integer.parseInt(portEnv.trim());
        AppServer.build().start(port);
        System.out.println("Auth app running at http://localhost:" + port);
    }
}
