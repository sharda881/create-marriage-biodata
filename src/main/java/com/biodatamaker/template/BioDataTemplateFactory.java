package com.biodatamaker.template;

import com.biodatamaker.exception.TemplateNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory class for resolving and managing BioData templates.
 * Uses the Factory Pattern to provide the correct template implementation.
 */
@Component
public class BioDataTemplateFactory {

    private final Map<String, BioDataTemplate> templateRegistry;

    /**
     * Constructor that auto-discovers all template implementations via Spring DI
     */
    public BioDataTemplateFactory(List<BioDataTemplate> templates) {
        this.templateRegistry = new HashMap<>();
        templates.forEach(template -> 
            templateRegistry.put(template.getTemplateId(), template)
        );
    }

    /**
     * Get a template by its ID
     * @param templateId The unique template identifier
     * @return The template implementation
     * @throws TemplateNotFoundException if template not found
     */
    public BioDataTemplate getTemplate(String templateId) {
        BioDataTemplate template = templateRegistry.get(templateId);
        if (template == null) {
            throw new TemplateNotFoundException("Template not found: " + templateId);
        }
        return template;
    }

    /**
     * Get a template by ID with a fallback to default template
     * @param templateId The template ID (may be null)
     * @return The template or default template
     */
    public BioDataTemplate getTemplateOrDefault(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return getDefaultTemplate();
        }
        return templateRegistry.getOrDefault(templateId, getDefaultTemplate());
    }

    /**
     * Get the default template (Traditional)
     */
    public BioDataTemplate getDefaultTemplate() {
        return templateRegistry.get("traditional");
    }

    /**
     * Get all available templates
     */
    public Collection<BioDataTemplate> getAllTemplates() {
        return templateRegistry.values();
    }

    /**
     * Get all free templates
     */
    public List<BioDataTemplate> getFreeTemplates() {
        return templateRegistry.values().stream()
                .filter(template -> !template.isPremium())
                .collect(Collectors.toList());
    }

    /**
     * Get all premium templates
     */
    public List<BioDataTemplate> getPremiumTemplates() {
        return templateRegistry.values().stream()
                .filter(BioDataTemplate::isPremium)
                .collect(Collectors.toList());
    }

    /**
     * Check if a template exists
     */
    public boolean templateExists(String templateId) {
        return templateRegistry.containsKey(templateId);
    }

    /**
     * Get template count
     */
    public int getTemplateCount() {
        return templateRegistry.size();
    }

    /**
     * Get list of all template IDs
     */
    public List<String> getAllTemplateIds() {
        return templateRegistry.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }
}
