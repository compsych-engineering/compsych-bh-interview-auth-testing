package com.compsych.interview.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Core UI behavior, driven as an administrator in a real browser. Tests that change data put it
 * back through the API, so the suite can be re-run against the same server.
 */
class AccountUiTest extends BrowserTest {

    // ---- the login screen ----

    @Test
    void loginScreenShowsWelcomeAndMarksRequiredFields() {
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)))
                .containsText("Welcome To");
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1)))
                .containsText("Account Portal");

        assertThat(page.locator("label[for=username]")).hasText("Username*");
        assertThat(page.locator("label[for=password]")).hasText("Password*");
        assertThat(page.locator("#username")).hasAttribute("required", "");
        assertThat(page.locator("#password")).hasAttribute("required", "");
    }

    @Test
    void emptyLoginFormIsNotSubmitted() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();

        assertThat(page.locator("#login-form")).isVisible();
        assertThat(page.getByRole(AriaRole.STATUS)).isEmpty();
    }

    @Test
    void wrongPasswordShowsAnErrorAndStaysOnLogin() {
        signIn(Config.ADMIN_USER, "definitely-not-the-password");

        assertThat(page.getByRole(AriaRole.STATUS)).containsText("Invalid username or password");
        assertThat(page.getByRole(AriaRole.STATUS)).containsText("HTTP 401");
        assertThat(page.locator("#login-form")).isVisible();
        assertThat(page.locator("#account-list")).isHidden();
    }

    // ---- the account list ----

    @Test
    void adminSeesTheAccountTableAfterLogin() {
        signInAsAdmin();

        assertThat(page.locator("#account-list")).isVisible();
        assertThat(page.locator("#login-form")).isHidden();
        assertThat(page.locator("#session-bar")).containsText("Signed in as admin");
        assertThat(page.locator("#accounts-body tr")).hasCount(4);
        assertThat(page.locator("tr[data-id='acct-2']")).containsText("Bob Brown");
        assertThat(page.locator("tr[data-id='acct-2']")).containsText("bob@example.com");
    }

    // ---- editing an account ----

    @Test
    void selectingARowOpensTheFormPrefilledForThatAccount() {
        signInAsAdmin();

        page.locator("tr[data-id='acct-2']").click();

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Edit account acct-2")))
                .isVisible();
        assertThat(page.locator("#displayName")).hasValue("Bob Brown");
        assertThat(page.locator("#email")).hasValue("bob@example.com");
        assertThat(page.locator("#account-list")).isHidden();
    }

    @Test
    void switchingBetweenAccountsShowsTheSelectedAccount() {
        signInAsAdmin();

        page.locator("tr[data-id='acct-2']").click();
        page.locator("tr[data-id='acct-3']").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Edit account acct-3")))
                .isVisible();
        assertThat(page.locator("#displayName")).hasValue("Carol Chen");
        assertThat(page.locator("#email")).hasValue("carol@example.com");
    }

    @Test
    void editFormMarksFieldsRequiredAndOffersSaveAndBack() {
        signInAsAdmin();
        page.locator("tr[data-id='acct-2']").click();

        assertThat(page.locator("label[for=displayName]")).hasText("Display name*");
        assertThat(page.locator("label[for=email]")).hasText("Email*");
        assertThat(page.locator("#displayName")).hasAttribute("required", "");
        assertThat(page.locator("#email")).hasAttribute("required", "");
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save"))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Back to accounts")))
                .isVisible();
    }

    @Test
    void savedChangesAppearInTheAccountList() {
        String original = "Bob Brown";
        String edited = "Robert Brown (e2e)";
        signInAsAdmin();
        try {
            page.locator("tr[data-id='acct-2']").click();
            page.locator("#displayName").fill(edited);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();

            assertThat(page.getByRole(AriaRole.STATUS)).hasText("Account saved.");

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Back to accounts")).click();

            assertThat(page.locator("#account-list")).isVisible();
            assertThat(page.locator("tr[data-id='acct-2']")).containsText(edited);
        } finally {
            restoreAccount("acct-2", original, "bob@example.com");
        }
    }

    @Test
    void backReturnsToTheListAndLogoutReturnsToLogin() {
        signInAsAdmin();
        page.locator("tr[data-id='acct-3']").click();
        assertThat(page.locator("#account-form")).isVisible();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Back to accounts")).click();
        assertThat(page.locator("#account-list")).isVisible();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log out")).click();
        assertThat(page.locator("#login-form")).isVisible();
        assertThat(page.locator("#account-list")).isHidden();
        assertThat(page.locator("#session-bar")).isHidden();
        assertThat(page.getByRole(AriaRole.STATUS)).hasText(Pattern.compile("Logged out\\."));
    }

    // ---- helpers ----

    private void signInAsAdmin() {
        signIn(Config.ADMIN_USER, Config.ADMIN_PASSWORD);
        assertThat(page.locator("#account-list")).isVisible();
    }

    private void signIn(String username, String password) {
        page.getByLabel("Username").fill(username);
        page.getByLabel("Password").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
    }

    /** Cleanup goes through the API so a failed UI test cannot leave bad data behind. */
    private void restoreAccount(String id, String displayName, String email) {
        try (Playwright playwright = Pw.create(); ApiClient api = new ApiClient(playwright)) {
            api.updateAccount(id, api.adminToken(), displayName, email);
        }
    }
}
