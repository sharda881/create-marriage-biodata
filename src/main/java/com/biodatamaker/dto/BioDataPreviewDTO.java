package com.biodatamaker.dto;

import java.util.Map;

/**
 * Everything the SPA needs to render a bio-data preview with the selected template.
 * Mirrors the variables the Thymeleaf PDF templates receive so the on-screen React
 * preview and the generated PDF stay in sync.
 */
public record BioDataPreviewDTO(
        BioDataDTO bioData,
        TemplateDTO template,
        String formattedDob,
        Integer age,
        int currentYear,
        String photoUrl,
        boolean hasPhoto,
        boolean hasEducation,
        boolean hasProfession,
        boolean hasFamily,
        boolean hasContact,
        boolean hasPreferences,
        Map<String, String> customFields,
        boolean needsPayment
) {
}
