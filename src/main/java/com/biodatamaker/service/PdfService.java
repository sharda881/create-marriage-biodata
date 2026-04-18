package com.biodatamaker.service;

import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.User;
import com.biodatamaker.template.BioDataTemplate;
import com.biodatamaker.template.BioDataTemplateFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service for generating PDF from BioData using OpenHTMLtoPDF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private final TemplateEngine templateEngine;
    private final BioDataTemplateFactory templateFactory;
    private final BioDataService bioDataService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    /**
     * Generate PDF for a bio-data - supports both authenticated and anonymous users
     * @param bioDataId The bio-data ID
     * @param user The user requesting the PDF (can be null for anonymous)
     * @return PDF as byte array
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
     * Generate PDF from BioData entity (internal use)
     */
    public byte[] generatePdfFromBioData(BioData bioData) throws IOException {
        // Get template
        BioDataTemplate template = templateFactory.getTemplateOrDefault(bioData.getSelectedTemplateId());

        // Render HTML using Thymeleaf
        String html = renderBioDataHtml(bioData, template);

        // Convert HTML to PDF
        byte[] pdf = convertHtmlToPdf(html);

        // Increment download count
        bioDataService.incrementDownloadCount(bioData.getId());

        log.info("Generated PDF for bio-data {} using template {}", bioData.getId(), template.getTemplateId());
        return pdf;
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

        // Use the PDF template (different from preview)
        String templatePath = "biodata/pdf-template";

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
     * Convert HTML string to PDF bytes using OpenHTMLtoPDF
     */
    private byte[] convertHtmlToPdf(String html) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Parse HTML with JSoup to ensure valid XHTML
            Document document = Jsoup.parse(html);
            document.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);

            // Convert to W3C Document
            W3CDom w3cDom = new W3CDom();
            org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(document);

            // Build PDF
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(w3cDoc, "/");
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new IOException("Failed to generate PDF: " + e.getMessage(), e);
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
}
