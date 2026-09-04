package com.biodatamaker.dto;

import java.util.List;
import java.util.Map;

/**
 * Everything the SPA needs to render a bio-data preview with the selected template.
 * Mirrors the variables the Thymeleaf PDF templates receive so the on-screen React
 * preview and the generated PDF stay in sync.
 *
 * <p>When {@code needsPayment} is true, sensitive values in {@link #bioData} are
 * replaced server-side with a blur placeholder (so a screenshot of the preview is
 * useless) while their labels stay visible; {@code lockedFields} lists which fields
 * the SPA should render blurred / struck-through, and {@code unlockMessage} is the
 * CTA text ("download to see everything, payment required") to show alongside them.
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
        boolean needsPayment,
        List<String> lockedFields,
        String unlockMessage
) {
}
