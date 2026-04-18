package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Traditional template with classic Indian wedding aesthetics.
 * Features ornate borders, traditional colors, and cultural elements.
 */
@Component
public class TraditionalTemplate extends AbstractBioDataTemplate {

    public TraditionalTemplate() {
        super(
                "traditional",
                "Traditional Classic",
                "A classic design with ornate borders and traditional Indian wedding colors. Ideal for traditional families.",
                "#8B0000",  // Dark Red/Maroon
                "#CD853F",  // Peru/Gold
                "#F5EBD8",  // Cream
                false       // Free template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-traditional bg-gradient-to-br from-red-50 to-orange-50";
    }
}
