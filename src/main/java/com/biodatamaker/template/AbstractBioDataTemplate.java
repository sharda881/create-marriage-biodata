package com.biodatamaker.template;

import com.biodatamaker.entity.BioData;
import lombok.Getter;

/**
 * Abstract base class for BioData templates providing common functionality.
 */
@Getter
public abstract class AbstractBioDataTemplate implements BioDataTemplate {

    protected final String templateId;
    protected final String displayName;
    protected final String description;
    protected final String primaryColor;
    protected final String secondaryColor;
    protected final String backgroundColor;
    protected final boolean premium;

    protected AbstractBioDataTemplate(String templateId, String displayName, String description,
                                       String primaryColor, String secondaryColor, boolean premium) {
        this(templateId, displayName, description, primaryColor, secondaryColor, "#F5EBD8", premium);
    }

    protected AbstractBioDataTemplate(String templateId, String displayName, String description,
                                       String primaryColor, String secondaryColor, String backgroundColor, boolean premium) {
        this.templateId = templateId;
        this.displayName = displayName;
        this.description = description;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.backgroundColor = backgroundColor;
        this.premium = premium;
    }

    @Override
    public String getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public String getThymeleafTemplatePath() {
        return "biodata/templates/" + templateId;
    }

    @Override
    public String getPreviewImagePath() {
        return "/images/templates/" + templateId + "-preview.png";
    }

    @Override
    public boolean isPremium() {
        return premium;
    }

    @Override
    public boolean isValidForTemplate(BioData bioData) {
        return bioData != null
                && bioData.getFullName() != null
                && !bioData.getFullName().isBlank()
                && bioData.getDateOfBirth() != null;
    }
}
