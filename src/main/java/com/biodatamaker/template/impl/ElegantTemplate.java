package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Elegant template with maroon/gold theme.
 * Features classic Indian design with ornate borders.
 */
@Component
public class ElegantTemplate extends AbstractBioDataTemplate {

    public ElegantTemplate() {
        super(
                "elegant",
                "Elegant Maroon",
                "A sophisticated design with maroon and gold accents. Perfect for traditional families seeking an elegant look.",
                "#6B4C7B",  // Purple
                "#9B6BA8",  // Light purple
                "#FAF5FC",  // Light purple/pink
                false       // Free template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-elegant bg-gradient-to-br from-red-50 to-amber-50";
    }
}
