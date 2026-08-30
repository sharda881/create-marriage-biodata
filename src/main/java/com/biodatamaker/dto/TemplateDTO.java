package com.biodatamaker.dto;

import com.biodatamaker.template.BioDataTemplate;

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
        String cssClasses,
        String previewImagePath
) {
    public static TemplateDTO fromTemplate(BioDataTemplate t) {
        return new TemplateDTO(
                t.getTemplateId(),
                t.getDisplayName(),
                t.getDescription(),
                t.getPrimaryColor(),
                t.getSecondaryColor(),
                t.getBackgroundColor(),
                t.isPremium(),
                t.getCssClasses(),
                t.getPreviewImagePath()
        );
    }
}
