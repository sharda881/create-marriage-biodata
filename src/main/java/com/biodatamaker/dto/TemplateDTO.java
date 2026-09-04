package com.biodatamaker.dto;

import com.biodatamaker.template.BioDataTemplate;

import java.math.BigDecimal;

/**
 * Metadata about a bio-data design template, consumed by the SPA to render the
 * template gallery and to pick the matching React preview component.
 */
public record TemplateDTO(
        String id,
        String name,
        String description,
        String primaryColor,
        String secondaryColor,
        String backgroundColor,
        boolean premium,
        /** Effective price (0 = free) from {@code TemplatePricingService}. */
        BigDecimal price,
        String cssClasses,
        String previewImagePath
) {
    public static TemplateDTO fromTemplate(BioDataTemplate t, BigDecimal price) {
        return new TemplateDTO(
                t.getTemplateId(),
                t.getDisplayName(),
                t.getDescription(),
                t.getPrimaryColor(),
                t.getSecondaryColor(),
                t.getBackgroundColor(),
                t.isPremium(),
                price,
                t.getCssClasses(),
                t.getPreviewImagePath()
        );
    }
}
