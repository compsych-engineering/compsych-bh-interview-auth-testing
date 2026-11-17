package com.compsych.interview;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory token -> username map. No expiry; a restart logs everyone out. */
public final class SessionStore {

    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    public String create(String username) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, username);
        return token;
    }

    public Optional<String> usernameFor(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(tokens.get(token));
    }
}
