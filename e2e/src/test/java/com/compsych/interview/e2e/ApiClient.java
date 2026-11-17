package com.compsych.interview.e2e;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import java.util.Map;

/** Thin HTTP helper over Playwright's request context. Knows the URLs; asserts nothing. */
final class ApiClient implements AutoCloseable {

    private final APIRequestContext http;

    ApiClient(Playwright playwright) {
        this.http = playwright.request()
                .newContext(new APIRequest.NewContextOptions().setBaseURL(Config.BASE_URL));
    }

    APIResponse health() {
        return http.get("/health");
    }

    APIResponse login(String username, String password) {
        return http.post("/api/login",
                RequestOptions.create().setData(Map.of("username", username, "password", password)));
    }

    /** Sends a raw string as the login body, for malformed-payload cases. */
    APIResponse loginRaw(String body) {
        return http.post("/api/login", jsonHeaders(RequestOptions.create()).setData(body));
    }

    APIResponse listAccounts(String token) {
        return http.get("/api/accounts", withToken(RequestOptions.create(), token));
    }

    APIResponse getAccount(String id, String token) {
        return http.get("/api/accounts/" + id, withToken(RequestOptions.create(), token));
    }

    APIResponse updateAccount(String id, String token, String displayName, String email) {
        return http.put("/api/accounts/" + id, withToken(RequestOptions.create(), token)
                .setData(Map.of("displayName", displayName, "email", email)));
    }

    /** Sends a raw string as the update body, for malformed-payload cases. */
    APIResponse updateAccountRaw(String id, String token, String body) {
        return http.put("/api/accounts/" + id,
                jsonHeaders(withToken(RequestOptions.create(), token)).setData(body));
    }

    /** Logs in as the configured administrator and returns the bearer token. */
    String adminToken() {
        APIResponse response = login(Config.ADMIN_USER, Config.ADMIN_PASSWORD);
        if (response.status() != 200) {
            throw new IllegalStateException(
                    "Admin login failed: HTTP " + response.status() + " " + response.text());
        }
        return json(response).get("token").getAsString();
    }

    static JsonObject json(APIResponse response) {
        return JsonParser.parseString(response.text()).getAsJsonObject();
    }

    private static RequestOptions withToken(RequestOptions options, String token) {
        return token == null ? options : options.setHeader("Authorization", "Bearer " + token);
    }

    private static RequestOptions jsonHeaders(RequestOptions options) {
        return options.setHeader("Content-Type", "application/json");
    }

    @Override
    public void close() {
        http.dispose();
    }
}
