package com.biodatamaker.template;

import com.biodatamaker.entity.BioData;

/**
 * Strategy interface for BioData templates.
 * Each implementation represents a different visual style/layout for the bio-data.
 */
public interface BioDataTemplate {

    /**
     * Get the unique identifier for this template
     * @return Template ID (e.g., "modern", "traditional", "royal")
     */
    String getTemplateId();

    /**
     * Get the display name of the template
     * @return Human-readable template name
     */
    String getDisplayName();

    /**
     * Get the description of the template
     * @return Template description
     */
    String getDescription();

    /**
     * Get the Thymeleaf fragment path for this template
     * @return Path to the Thymeleaf template fragment (e.g., "biodata/modern")
     */
    String getThymeleafTemplatePath();

    /**
     * Get the preview image path
     * @return Path to template preview image
     */
    String getPreviewImagePath();

    /**
     * Check if this template is free or premium
     * @return true if template is premium (paid)
     */
    boolean isPremium();

    /**
     * Get the primary color for this template
     * @return CSS color value
     */
    String getPrimaryColor();

    /**
     * Get the secondary color for this template
     * @return CSS color value
     */
    String getSecondaryColor();

    /**
     * Get the background color for this template
     * @return CSS color value
     */
    default String getBackgroundColor() {
        return "#F5EBD8"; // Default cream color
    }

    /**
     * Validate if the bio-data has all required fields for this template
     * @param bioData The bio-data to validate
     * @return true if bio-data is valid for this template
     */
    default boolean isValidForTemplate(BioData bioData) {
        return bioData != null 
                && bioData.getFullName() != null 
                && !bioData.getFullName().isBlank();
    }

    /**
     * Get custom CSS class names for this template
     * @return CSS class names
     */
    default String getCssClasses() {
        return "template-" + getTemplateId();
    }
}
