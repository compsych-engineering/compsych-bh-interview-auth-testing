package com.compsych.interview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/** Read-only users loaded from data/users.json. Mock data: plaintext passwords on purpose. */
public final class UserStore {

    /** {@code grants} are optional per-user permissions on top of the role; absent means none. */
    public record User(String username, String password, String role, String accountId, List<String> grants) {
        public User {
            grants = grants == null ? List.of() : List.copyOf(grants);
        }
    }

    private final List<User> users;

    public UserStore(List<User> users) {
        this.users = List.copyOf(users);
    }

    public static UserStore fromClasspath() {
        try (InputStream in = UserStore.class.getResourceAsStream("/data/users.json")) {
            return new UserStore(new ObjectMapper().readValue(in, new TypeReference<List<User>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load data/users.json", e);
        }
    }

    public Optional<User> authenticate(String username, String password) {
        return users.stream()
                .filter(u -> u.username().equals(username) && u.password().equals(password))
                .findFirst();
    }

    public Optional<User> find(String username) {
        return users.stream().filter(u -> u.username().equals(username)).findFirst();
    }
}
