package com.biodatamaker.service;

import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import com.biodatamaker.template.BioDataTemplate;
import com.biodatamaker.template.BioDataTemplateFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service for generating PDFs by calling an external Node.js service.
 * This service renders HTML using Thymeleaf and then sends it to a Playwright-based
 * service to convert the HTML to a PDF document.
 */
@Service
@Slf4j
public class PdfService {

    private final TemplateEngine templateEngine;
    private final BioDataTemplateFactory templateFactory;
    private final BioDataService bioDataService;
    private final WebClient webClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    public PdfService(TemplateEngine templateEngine,
                      BioDataTemplateFactory templateFactory,
                      BioDataService bioDataService,
                      WebClient.Builder webClientBuilder,
                      @Value("${app.pdf.service.url}") String pdfServiceUrl) {
        this.templateEngine = templateEngine;
        this.templateFactory = templateFactory;
        this.bioDataService = bioDataService;
        this.webClient = webClientBuilder.baseUrl(pdfServiceUrl).build();
    }

    /**
     * Generates a PDF for a given bio-data ID.
     * This method supports both authenticated and anonymous users. For authenticated users,
     * it validates their access rights before proceeding.
     *
     * @param bioDataId The ID of the bio-data to generate the PDF for.
     * @param user      The user requesting the PDF. Can be null for anonymous access.
     * @return A byte array containing the generated PDF.
     * @throws IOException if there is an error during HTML rendering or PDF generation.
     */
    public byte[] generatePdf(Long bioDataId, User user) throws IOException {
        if (user != null) {
            bioDataService.validateDownloadAccess(user, bioDataId);
        }
        BioData bioData = bioDataService.getBioDataById(bioDataId);
        return generatePdfFromBioData(bioData);
    }

    /**
     * Generates a PDF from a BioData entity.
     * This method orchestrates the process of rendering HTML and calling the external
     * PDF generation service. It also increments the download count for the bio-data.
     *
     * @param bioData The BioData entity to generate the PDF from.
     * @return A byte array containing the generated PDF.
     * @throws IOException if the PDF generation service fails.
     */
    public byte[] generatePdfFromBioData(BioData bioData) throws IOException {
        // Get template
        BioDataTemplate template = templateFactory.getTemplateOrDefault(bioData.getSelectedTemplateId());

        // Render HTML using Thymeleaf
        String html = renderBioDataHtml(bioData, template);

        try {
            byte[] pdf = generatePdfFromHtml(html);
            bioDataService.incrementDownloadCount(bioData.getId());
            log.info("Successfully generated PDF for bio-data {} using template {}", bioData.getId(), template.getTemplateId());
            return pdf;
        } catch (Exception e) {
            log.error("Failed to generate PDF from Node.js service for bio-data ID: {}", bioData.getId(), e);
            throw new IOException("PDF generation service failed.", e);
        }
    }

    /**
     * Calls the external Node.js service to convert an HTML string to a PDF.
     *
     * @param html The HTML content as a string.
     * @return A byte array of the generated PDF.
     */
    private byte[] generatePdfFromHtml(String html) {
        log.debug("Sending HTML to PDF generation service...");
        return webClient.post()
                .uri("/generate-pdf")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(html)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(30)) // Add a timeout for safety
                .block();
    }

    /**
     * Generate preview HTML (for browser display)
     */
    public String generatePreviewHtml(Long bioDataId, User user) {
        BioData bioData = bioDataService.getBioDataForUser(bioDataId, user);
        BioDataTemplate template = templateFactory.getTemplateOrDefault(bioData.getSelectedTemplateId());
        return renderBioDataHtml(bioData, template);
    }

    /**
     * Render bio-data to HTML using Thymeleaf template
     */
    private String renderBioDataHtml(BioData bioData, BioDataTemplate template) {
        Context context = new Context(Locale.ENGLISH);

        // Add bio-data to context
        context.setVariable("bioData", bioData);
        context.setVariable("template", template);

        // Add formatted values
        context.setVariable("formattedDob", formatDate(bioData.getDateOfBirth()));
        context.setVariable("age", bioData.getAge());
        context.setVariable("currentYear", LocalDate.now().getYear());

        // Add helper booleans
        context.setVariable("hasPhoto", bioData.getPhotoPath() != null && !bioData.getPhotoPath().isBlank());
        context.setVariable("hasEducation", hasEducationDetails(bioData));
        context.setVariable("hasProfession", hasProfessionDetails(bioData));
        context.setVariable("hasFamily", hasFamilyDetails(bioData));
        context.setVariable("hasContact", hasContactDetails(bioData));
        context.setVariable("hasPreferences", hasPartnerPreferences(bioData));

        // Parse custom fields JSON to Map
        Map<String, String> customFieldsMap = parseCustomFields(bioData.getCustomFields());
        context.setVariable("customFieldsMap", customFieldsMap);

        // Use template-specific PDF file based on selected template
        String templateId = template.getTemplateId();
        String templatePath = getTemplateSpecificPdfPath(templateId);

        log.info("PDF Generation - BioData ID: {}, Selected Template ID: {}, Template Object ID: {}, PDF Path: {}",
                bioData.getId(), bioData.getSelectedTemplateId(), templateId, templatePath);

        return templateEngine.process(templatePath, context);
    }

    /**
     * Parse custom fields JSON string to Map
     */
    private Map<String, String> parseCustomFields(String customFieldsJson) {
        if (customFieldsJson == null || customFieldsJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(customFieldsJson, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse custom fields JSON: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * Format date for display
     */
    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }

    // Helper methods to check if sections have data
    private boolean hasEducationDetails(BioData b) {
        return b.getHighestQualification() != null || b.getCollegeName() != null;
    }

    private boolean hasProfessionDetails(BioData b) {
        return b.getOccupation() != null || b.getEmployerName() != null;
    }

    private boolean hasFamilyDetails(BioData b) {
        return b.getFatherName() != null || b.getMotherName() != null;
    }

    private boolean hasContactDetails(BioData b) {
        return b.getContactNumber() != null || b.getEmailAddress() != null;
    }

    private boolean hasPartnerPreferences(BioData b) {
        return b.getPreferredAgeRange() != null || b.getPreferredEducation() != null;
    }

    /**
     * Get template-specific PDF file path based on template ID.
     * Each template has its own unique PDF design.
     */
    private String getTemplateSpecificPdfPath(String templateId) {
        return switch (templateId.toLowerCase()) {
            case "traditional" -> "biodata/pdf/traditional-pdf";
            case "royal" -> "biodata/pdf/royal-pdf";
            case "modern" -> "biodata/pdf/modern-pdf";
            case "elegant" -> "biodata/pdf/elegant-pdf";
            case "floral" -> "biodata/pdf/floral-pdf";
            case "simple" -> "biodata/pdf/simple-pdf";
            default -> "biodata/pdf/traditional-pdf"; // fallback to traditional
        };
    }
}
