package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Modern template with clean, minimalist design.
 * Features a sleek layout with subtle gradients and modern typography.
 */
@Component
public class ModernTemplate extends AbstractBioDataTemplate {

    public ModernTemplate() {
        super(
                "modern",
                "Modern Elegance",
                "A clean, minimalist design with modern typography and subtle gradients. Perfect for professionals.",
                "#2C5F7C",  // Steel Blue
                "#4A8DB7",  // Light Blue
                "#F5F8FA",  // Light gray-blue
                false       // Free template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-modern bg-gradient-to-br from-blue-50 to-indigo-50";
    }
}
