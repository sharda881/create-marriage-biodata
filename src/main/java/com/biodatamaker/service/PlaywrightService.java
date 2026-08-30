package com.biodatamaker.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Manages a single shared Playwright/Chromium instance (launched at startup,
 * closed on shutdown). If Chromium can't start, the app still boots — only the
 * PDF / invitation-card endpoints fail (503) instead of taking everything down.
 */
@Service
@Slf4j
public class PlaywrightService {

    private Playwright playwright;
    private Browser browser;

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Playwright and launching Chromium...");
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--no-sandbox", "--disable-setuid-sandbox")));
            log.info("Chromium launched.");
        } catch (Exception e) {
            log.error("Playwright/Chromium unavailable — PDF and invitation-card "
                    + "generation will return 503. {}", e.getMessage());
        }
    }

    /** The shared browser, or a 503 if Chromium failed to start. */
    public Browser getBrowser() {
        if (browser == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "PDF rendering is temporarily unavailable");
        }
        return browser;
    }

    @PreDestroy
    public void destroy() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
