package com.biodatamaker.service;

import com.biodatamaker.dto.InvitationCardDTO;
import com.biodatamaker.template.InvitationTheme;
import com.biodatamaker.template.InvitationThemeRegistry;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Renders a wedding invitation card to a PNG (or PDF) with Playwright.
 * Stateless — the card data comes straight from the request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);

    private final TemplateEngine templateEngine;
    private final PlaywrightService playwrightService;
    private final InvitationThemeRegistry themes;

    public byte[] renderPng(InvitationCardDTO card) {
        String html = renderHtml(card);
        try (BrowserContext context = playwrightService.getBrowser()
                .newContext(new com.microsoft.playwright.Browser.NewContextOptions().setDeviceScaleFactor(2));
             Page page = context.newPage()) {
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            ElementHandle cardEl = page.querySelector(".card");
            return cardEl != null ? cardEl.screenshot() : page.screenshot();
        }
    }

    public byte[] renderPdf(InvitationCardDTO card) {
        String html = renderHtml(card);
        try (BrowserContext context = playwrightService.getBrowser().newContext();
             Page page = context.newPage()) {
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            return page.pdf(new Page.PdfOptions()
                    .setWidth("148mm").setHeight("210mm").setPrintBackground(true));
        }
    }

    private String renderHtml(InvitationCardDTO card) {
        InvitationTheme theme = themes.themeFor(card.templateId());
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("card", card);
        vars.put("theme", theme);

        String month = "", day = "", year = "";
        if (card.weddingDate() != null && !card.weddingDate().isBlank()) {
            try {
                LocalDate d = LocalDate.parse(card.weddingDate());
                month = d.format(MONTH);
                day = String.valueOf(d.getDayOfMonth());
                year = String.valueOf(d.getYear());
            } catch (Exception e) {
                log.debug("Bad wedding date '{}'", card.weddingDate());
            }
        }
        vars.put("dateMonth", month);
        vars.put("dateDay", day);
        vars.put("dateYear", year);

        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariables(vars);
        return templateEngine.process("invitation/card", ctx);
    }
}
