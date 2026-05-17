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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
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
    private final String uploadPath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    public PdfService(TemplateEngine templateEngine,
                      BioDataTemplateFactory templateFactory,
                      BioDataService bioDataService,
                      WebClient.Builder webClientBuilder,
                      @Value("${app.pdf.service.url}") String pdfServiceUrl,
                      @Value("${app.upload.path}") String uploadPath) {
        this.templateEngine = templateEngine;
        this.templateFactory = templateFactory;
        this.bioDataService = bioDataService;
        this.webClient = webClientBuilder.baseUrl(pdfServiceUrl).build();
        this.uploadPath = uploadPath;
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
        // Validate access only for authenticated users
        if (user != null) {
            bioDataService.validateDownloadAccess(user, bioDataId);
        }

        // Get bio-data (for anonymous or authenticated)
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

        // Encode photo as base64 data URI for PDF rendering (Playwright can't access local files)
        if (bioData.getPhotoPath() != null && !bioData.getPhotoPath().isBlank()) {
            String photoBase64 = encodePhotoToBase64(bioData.getPhotoPath());
            context.setVariable("photoBase64DataUri", photoBase64);
        }

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

    /**
     * Reads the photo file and encodes it as a base64 data URI.
     * photoPath is stored as "/uploads/photos/filename.jpg"
     */
    private String encodePhotoToBase64(String photoPath) {
        try {
            // photoPath is stored as "/uploads/photos/filename.jpg", resolve relative to working directory
            Path filePath = Paths.get("." + photoPath);
            if (!Files.exists(filePath)) {
                // Fallback: try resolving against configured upload path
                String filename = Paths.get(photoPath).getFileName().toString();
                filePath = Paths.get(uploadPath, "photos", filename);
            }
            if (Files.exists(filePath)) {
                byte[] fileBytes = Files.readAllBytes(filePath);
                String base64 = Base64.getEncoder().encodeToString(fileBytes);
                String mimeType = Files.probeContentType(filePath);
                if (mimeType == null) {
                    mimeType = "image/jpeg";
                }
                return "data:" + mimeType + ";base64," + base64;
            }
            log.warn("Photo file not found: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to encode photo to base64: {}", e.getMessage());
        }
        return null;
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
