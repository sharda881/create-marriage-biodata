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
                "Sophisticated silver and gray design with metallic accents. Perfect for a modern and elegant look.",
                "#2D3748",  // Dark gray
                "#C0C0C0",  // Silver
                "#2D3748",  // Dark gray background
                true        // Premium template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-premium-silver bg-gradient-to-br from-gray-700 to-gray-800";
    }
}
