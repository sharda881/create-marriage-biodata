package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Premium Silver template with elegant silver and gray color scheme.
 * Features sophisticated design with metallic accents and modern aesthetics.
 */
@Component
public class PremiumSilverTemplate extends AbstractBioDataTemplate {

    public PremiumSilverTemplate() {
        super(
                "premium-silver",
                "Premium Silver",
                "Regal silver and gray design matching Royal Gold aesthetic. Perfect for a sophisticated and elegant look.",
                "#475569",  // Slate gray (matching Royal structure)
                "#CBD5E1",  // Silver
                "#F5F5F5",  // Light gray background
                true        // Premium template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-premium-silver bg-gradient-to-br from-gray-700 to-gray-800";
    }
}
