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
        if (user != null) {
            bioDataService.validateDownloadAccess(user, bioDataId);
        }
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

    private byte[] convertHtmlToPdf(String html) {
        try (BrowserContext context = playwrightService.getBrowser().newContext();
             Page page = context.newPage()) {
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.LOAD));
            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setScale(1));
        }
    }

    private String renderBioDataHtml(BioData bioData, BioDataTemplate template) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(viewModel.buildPdfContext(bioData, template));
        String templatePath = getTemplateSpecificPdfPath(template.getTemplateId());
        log.info("PDF Generation - BioData ID: {}, Template ID: {}, PDF Path: {}",
                bioData.getId(), template.getTemplateId(), templatePath);
        return templateEngine.process(templatePath, context);
    }

    /**
     * Each template has its own PDF design; fall back to traditional.
     */
    private String getTemplateSpecificPdfPath(String templateId) {
        return switch (templateId.toLowerCase(Locale.ROOT)) {
            case "royal" -> "biodata/pdf/royal-pdf";
            case "modern" -> "biodata/pdf/modern-pdf";
            case "elegant" -> "biodata/pdf/elegant-pdf";
            case "floral" -> "biodata/pdf/floral-pdf";
            case "simple" -> "biodata/pdf/simple-pdf";
            default -> "biodata/pdf/traditional-pdf";
        };
    }
}
