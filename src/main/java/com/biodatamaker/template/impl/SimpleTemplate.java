package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Simple template with straightforward, easy-to-read layout.
 * Features clean typography and organized sections.
 */
@Component
public class SimpleTemplate extends AbstractBioDataTemplate {

    public SimpleTemplate() {
        super(
                "simple",
                "Simple & Clean",
                "A straightforward design with clear organization and easy readability. Great for quick sharing.",
                "#3D5A4C",  // Dark Green
                "#5B8A72",  // Light Green
                "#F5F8F5",  // Very light green
                false       // Free template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-simple bg-gradient-to-br from-green-50 to-emerald-50";
    }
}
