package com.compsych.interview.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/**
 * One browser per class, a fresh context and page per test. When a test fails, a Playwright
 * trace and a screenshot are written to target/artifacts/ so the failure can be diagnosed
 * after the run (CI uploads that directory).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BrowserTest {

    private static final Path ARTIFACTS = Path.of("target", "artifacts");

    protected Page page;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private boolean failed;

    /** Runs before @AfterEach, so teardown can tell whether the test failed. */
    @RegisterExtension
    final TestExecutionExceptionHandler failureTracker = (extensionContext, throwable) -> {
        failed = true;
        throw throwable;
    };

    @BeforeAll
    void launchBrowser() {
        playwright = Pw.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(!Config.HEADED);
        if (!Config.BROWSER_CHANNEL.isEmpty()) {
            options.setChannel(Config.BROWSER_CHANNEL);
        }
        browser = playwright.chromium().launch(options);
    }

    @AfterAll
    void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        failed = false;
        context = browser.newContext(new Browser.NewContextOptions().setBaseURL(Config.BASE_URL));
        context.tracing().start(new com.microsoft.playwright.Tracing.StartOptions()
                .setScreenshots(true).setSnapshots(true).setSources(false));
        page = context.newPage();
        page.navigate("/");
    }

    @AfterEach
    void closePage(TestInfo info) throws Exception {
        if (failed) {
            Files.createDirectories(ARTIFACTS);
            String name = getClass().getSimpleName() + "." + info.getTestMethod().map(m -> m.getName()).orElse("test");
            page.screenshot(new Page.ScreenshotOptions().setPath(ARTIFACTS.resolve(name + ".png")).setFullPage(true));
            context.tracing().stop(new com.microsoft.playwright.Tracing.StopOptions()
                    .setPath(ARTIFACTS.resolve(name + ".trace.zip")));
        } else {
            context.tracing().stop();
        }
        context.close();
    }
}
