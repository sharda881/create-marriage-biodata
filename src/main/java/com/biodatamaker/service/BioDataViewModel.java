package com.biodatamaker.service;

import com.biodatamaker.dto.BioDataDTO;
import com.biodatamaker.dto.BioDataPreviewDTO;
import com.biodatamaker.dto.TemplateDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.template.BioDataTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the presentation model for a bio-data + template. Shared by {@code PdfService}
 * (Thymeleaf context for the PDF) and the REST layer ({@link BioDataPreviewDTO} for the
 * React preview) so both render paths derive the same fields the same way.
 */
@Component
@Slf4j
public class BioDataViewModel {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Thymeleaf context variables for the PDF templates (photo embedded as Base64).
     */
    public Map<String, Object> buildPdfContext(BioData bioData, BioDataTemplate template) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("bioData", bioData);
        vars.put("template", template);
        vars.put("formattedDob", formatDate(bioData.getDateOfBirth()));
        vars.put("age", bioData.getAge());
        vars.put("currentYear", LocalDate.now().getYear());

        String photoAsBase64 = null;
        if (bioData.getPhotoPath() != null && !bioData.getPhotoPath().isBlank()) {
            photoAsBase64 = encodeImageAsBase64("." + bioData.getPhotoPath());
        }
        vars.put("photoAsBase64", photoAsBase64);
        vars.put("hasPhoto", photoAsBase64 != null);

        vars.put("hasEducation", hasEducation(bioData));
        vars.put("hasProfession", hasProfession(bioData));
        vars.put("hasFamily", hasFamily(bioData));
        vars.put("hasContact", hasContact(bioData));
        vars.put("hasPreferences", hasPreferences(bioData));
        vars.put("customFieldsMap", parseCustomFields(bioData.getCustomFields()));
        return vars;
    }

    /**
     * JSON preview model for the SPA (photo as an absolute URL).
     */
    public BioDataPreviewDTO buildPreview(BioData bioData, BioDataTemplate template, boolean needsPayment) {
        String photoUrl = null;
        if (bioData.getPhotoPath() != null && !bioData.getPhotoPath().isBlank()) {
            photoUrl = bioData.getPhotoPath().startsWith("http")
                    ? bioData.getPhotoPath()
                    : baseUrl + bioData.getPhotoPath();
        }
        return new BioDataPreviewDTO(
                BioDataDTO.fromEntity(bioData),
                TemplateDTO.fromTemplate(template),
                formatDate(bioData.getDateOfBirth()),
                bioData.getAge(),
                LocalDate.now().getYear(),
                photoUrl,
                photoUrl != null,
                hasEducation(bioData),
                hasProfession(bioData),
                hasFamily(bioData),
                hasContact(bioData),
                hasPreferences(bioData),
                parseCustomFields(bioData.getCustomFields()),
                needsPayment
        );
    }

    public String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMATTER);
    }

    public Map<String, String> parseCustomFields(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse custom fields JSON: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String encodeImageAsBase64(String imagePath) {
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            String mimeType = "image/png";
            String lower = imagePath.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            }
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.warn("Could not read image file for Base64 encoding: {}", imagePath, e);
            return null;
        }
    }

    private boolean hasEducation(BioData b) {
        return b.getHighestQualification() != null || b.getCollegeName() != null;
    }

    private boolean hasProfession(BioData b) {
        return b.getOccupation() != null || b.getEmployerName() != null;
    }

    private boolean hasFamily(BioData b) {
        return b.getFatherName() != null || b.getMotherName() != null;
    }

    private boolean hasContact(BioData b) {
        return b.getContactNumber() != null || b.getEmailAddress() != null;
    }

    private boolean hasPreferences(BioData b) {
        return b.getPreferredAgeRange() != null || b.getPreferredEducation() != null;
    }
}
