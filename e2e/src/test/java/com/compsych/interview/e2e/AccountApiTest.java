package com.compsych.interview.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Core API behavior, exercised as an administrator. Black-box: only HTTP, never the app's code.
 * Tests that change data put it back, so the suite can be re-run against the same server.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountApiTest {

    private Playwright playwright;
    private ApiClient api;
    private String adminToken;

    @BeforeAll
    void setUp() {
        playwright = Pw.create();
        api = new ApiClient(playwright);
        adminToken = api.adminToken();
    }

    @AfterAll
    void tearDown() {
        api.close();
        playwright.close();
    }

    // ---- service ----

    @Test
    void healthEndpointReportsOk() {
        APIResponse response = api.health();

        assertEquals(200, response.status());
        assertEquals("ok", ApiClient.json(response).get("status").getAsString());
    }

    // ---- login ----

    @Test
    void loginWithValidCredentialsReturnsSession() {
        APIResponse response = api.login(Config.ADMIN_USER, Config.ADMIN_PASSWORD);

        assertEquals(200, response.status());
        JsonObject body = ApiClient.json(response);
        assertFalse(body.get("token").getAsString().isBlank(), "token should be issued");
        assertEquals(Config.ADMIN_USER, body.get("username").getAsString());
        assertEquals("admin", body.get("role").getAsString());
        assertFalse(body.get("accountId").getAsString().isBlank(), "account id should be returned");
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        APIResponse response = api.login(Config.ADMIN_USER, "definitely-not-the-password");

        assertEquals(401, response.status());
        assertTrue(ApiClient.json(response).has("error"), "error body expected");
    }

    @Test
    void loginWithUnknownUserIsRejected() {
        assertEquals(401, api.login("nobody-by-this-name", "password").status());
    }

    @Test
    void loginWithoutCredentialsIsRejected() {
        assertEquals(401, api.loginRaw("{}").status());
    }

    @Test
    void loginWithMalformedJsonIsBadRequest() {
        assertEquals(400, api.loginRaw("not json").status());
    }

    // ---- reading accounts ----

    @Test
    void getAccountReturnsItsDetails() {
        APIResponse response = api.getAccount("acct-1", adminToken);

        assertEquals(200, response.status());
        JsonObject account = ApiClient.json(response);
        assertEquals("acct-1", account.get("id").getAsString());
        assertEquals("alice", account.get("owner").getAsString());
        assertEquals("Alice Anderson", account.get("displayName").getAsString());
        assertEquals("alice@example.com", account.get("email").getAsString());
    }

    @Test
    void getUnknownAccountIsNotFound() {
        assertEquals(404, api.getAccount("acct-does-not-exist", adminToken).status());
    }

    @Test
    void listAccountsReturnsEverySeededAccount() {
        APIResponse response = api.listAccounts(adminToken);

        assertEquals(200, response.status());
        JsonArray accounts = com.google.gson.JsonParser.parseString(response.text()).getAsJsonArray();
        Set<String> ids = new HashSet<>();
        for (JsonElement account : accounts) {
            ids.add(account.getAsJsonObject().get("id").getAsString());
        }
        assertTrue(ids.containsAll(Set.of("acct-1", "acct-2", "acct-3", "acct-4")), "ids were " + ids);
    }

    // ---- updating accounts ----

    @Test
    void updatedAccountIsReturnedAndPersisted() {
        JsonObject original = ApiClient.json(api.getAccount("acct-3", adminToken));
        try {
            APIResponse updated = api.updateAccount("acct-3", adminToken, "Carol Updated", "carol.updated@example.com");

            assertEquals(200, updated.status());
            assertEquals("Carol Updated", ApiClient.json(updated).get("displayName").getAsString());

            JsonObject reread = ApiClient.json(api.getAccount("acct-3", adminToken));
            assertEquals("Carol Updated", reread.get("displayName").getAsString());
            assertEquals("carol.updated@example.com", reread.get("email").getAsString());
            assertEquals("carol", reread.get("owner").getAsString(), "owner is not editable");
        } finally {
            api.updateAccount("acct-3", adminToken,
                    original.get("displayName").getAsString(), original.get("email").getAsString());
        }
    }

    @Test
    void updateWithBlankFieldIsBadRequestAndChangesNothing() {
        JsonObject before = ApiClient.json(api.getAccount("acct-3", adminToken));

        assertEquals(400, api.updateAccount("acct-3", adminToken, "   ", "x@example.com").status());

        JsonObject after = ApiClient.json(api.getAccount("acct-3", adminToken));
        assertEquals(before, after);
    }

    @Test
    void updateWithMalformedJsonIsBadRequest() {
        assertEquals(400, api.updateAccountRaw("acct-3", adminToken, "not json").status());
    }

    @Test
    void updatingUnknownAccountIsNotFound() {
        assertEquals(404, api.updateAccount("acct-does-not-exist", adminToken, "Name", "n@example.com").status());
    }

    // ---- no credentials ----

    @Test
    void requestsWithoutAnAuthorizationHeaderAreRejected() {
        assertEquals(401, api.getAccount("acct-1", null).status());
        assertEquals(401, api.listAccounts(null).status());
        assertEquals(401, api.updateAccount("acct-1", null, "Name", "n@example.com").status());
    }
}
