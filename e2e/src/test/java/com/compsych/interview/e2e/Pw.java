package com.compsych.interview.e2e;

import com.microsoft.playwright.Playwright;
import java.util.Map;

/** Creates Playwright instances that never download browsers on their own. */
final class Pw {

    private Pw() {
    }

    /**
     * By default {@code Playwright.create()} downloads Chromium, Firefox and WebKit if they are
     * missing, mid-run. We install Chromium explicitly (see README / run-e2e.sh) and skip the rest.
     */
    static Playwright create() {
        return Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
    }
}
