package com.biodatamaker.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to manage the lifecycle of a Playwright instance and its browser.
 * This ensures the browser is launched once at application startup and closed gracefully
 * on shutdown, improving performance and resource management.
 */
@Service
@Slf4j
public class PlaywrightService {

    private Playwright playwright;
    @Getter
    private Browser browser;

    @PostConstruct
    public void init() {
        log.info("Initializing Playwright and launching Chromium browser...");
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true) // Run in headless mode for server environments
                .setArgs(List.of("--no-sandbox", "--disable-setuid-sandbox")));
        log.info("Chromium browser launched successfully.");
    }

    @PreDestroy
    public void destroy() {
        log.info("Closing Chromium browser and Playwright instance...");
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        log.info("Playwright instance closed.");
    }
}
