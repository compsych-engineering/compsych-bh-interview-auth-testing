package com.compsych.interview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accounts seeded from data/accounts.json. Updates live in memory only, so a restart
 * restores the original data.
 */
public final class AccountStore {

    public record Account(String id, String owner, String displayName, String email) {
    }

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public AccountStore(List<Account> seed) {
        seed.forEach(a -> accounts.put(a.id(), a));
    }

    public static AccountStore fromClasspath() {
        try (InputStream in = AccountStore.class.getResourceAsStream("/data/accounts.json")) {
            return new AccountStore(new ObjectMapper().readValue(in, new TypeReference<List<Account>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load data/accounts.json", e);
        }
    }

    /** All accounts, ordered by id. */
    public List<Account> list() {
        return accounts.values().stream().sorted(Comparator.comparing(Account::id)).toList();
    }

    public Optional<Account> find(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    /** Updates the editable fields; returns empty if the account does not exist. */
    public Optional<Account> update(String id, String displayName, String email) {
        return Optional.ofNullable(accounts.computeIfPresent(id,
                (k, a) -> new Account(a.id(), a.owner(), displayName, email)));
    }
}
