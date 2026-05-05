package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Premium Green template with traditional Indian biodata design.
 * Features dark green background with golden accents and decorative corners.
 */
@Component
public class PremiumGreenTemplate extends AbstractBioDataTemplate {

    public PremiumGreenTemplate() {
        super(
                "premium-green",
                "Premium Green",
                "Traditional Indian design with dark green background and gold decorative elements. Classic and elegant.",
                "#2C4A3A",  // Dark green
                "#D4AF37",  // Gold
                "#2C4A3A",  // Dark green background
                true        // Premium template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-premium-green bg-gradient-to-br from-green-900 to-green-800";
    }
}
