package com.biodatamaker.service;

import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import com.biodatamaker.template.BioDataTemplate;
import com.biodatamaker.template.BioDataTemplateFactory;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Locale;

/**
 * Service for generating PDFs by using a shared, embedded Playwright instance.
 * Renders HTML with Thymeleaf (templates under {@code templates/biodata/pdf/}) and then
 * converts it to a PDF with an in-process Chromium managed by {@link PlaywrightService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private final TemplateEngine templateEngine;
    private final BioDataTemplateFactory templateFactory;
    private final BioDataService bioDataService;
    private final PlaywrightService playwrightService;
    private final BioDataViewModel viewModel;

    /**
     * Generates a PDF for a given bio-data ID. For authenticated users, download access
     * (paywall) is validated first. {@code user} may be null for anonymous downloads.
     */
    public byte[] generatePdf(Long bioDataId, User user) throws IOException {
        bioDataService.validateDownloadAccess(user, bioDataId);
        return generatePdfFromBioData(bioDataService.getBioDataById(bioDataId));
    }

    /**
     * Generates a PDF from a BioData entity and increments its download count.
     */
    public byte[] generatePdfFromBioData(BioData bioData) throws IOException {
        BioDataTemplate template = templateFactory.getTemplateOrDefault(bioData.getSelectedTemplateId());
        String html = renderBioDataHtml(bioData, template);
        try {
            byte[] pdf = convertHtmlToPdf(html);
            bioDataService.incrementDownloadCount(bioData.getId());
            log.info("Generated PDF for bio-data {} using template {}", bioData.getId(), template.getTemplateId());
            return pdf;
        } catch (Exception e) {
            log.error("Failed to generate PDF via Playwright for bio-data {}", bioData.getId(), e);
            throw new IOException("PDF generation failed.", e);
        }
    }

    /** Usable A4 height in mm (297 minus a safety margin so we never spill to a 2nd page). */
    private static final double PAGE_MM = 286.0;

    /** Measure the actual flow height of the document in mm. */
    private static final String MEASURE_SCRIPT =
            "() => document.querySelector('.page').getBoundingClientRect().height / (96 / 25.4)";

    private byte[] convertHtmlToPdf(String html) {
        try (BrowserContext context = playwrightService.getBrowser().newContext();
             Page page = context.newPage()) {
            // NETWORKIDLE so the Google Fonts @import resolves before we measure / print
            // (falls back to system serif/sans when offline via font-display: swap).
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            double heightMm = ((Number) page.evaluate(MEASURE_SCRIPT)).doubleValue();
            // Shrink the whole render (frame included) so it fits one page; never below 0.5.
            double scale = Math.max(0.5, Math.min(1.0, PAGE_MM / heightMm));

            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setScale(scale));
        }
    }

    /**
     * All templates render through one shared, themed layout so the downloaded PDF
     * matches the on-screen React preview exactly.
     */
    private String renderBioDataHtml(BioData bioData, BioDataTemplate template) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(viewModel.buildPdfContext(bioData, template));
        log.info("PDF Generation - BioData ID: {}, Template ID: {}",
                bioData.getId(), template.getTemplateId());
        return templateEngine.process("biodata/pdf/document", context);
    }
}
