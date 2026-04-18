package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Premium template with sophisticated design and advanced features.
 * Features modern gradients, premium typography, and exclusive layouts.
 */
@Component
public class PremiumTemplate extends AbstractBioDataTemplate {

    public PremiumTemplate() {
        super(
                "premium",
                "Premium Elite",
                "An exclusive design with sophisticated gradients and premium aesthetics. Stand out from the crowd.",
                "#7C3AED",  // Violet
                "#A78BFA",  // Light Violet
                "#F5F3FF",  // Very light violet
                true        // Premium template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-premium bg-gradient-to-br from-violet-50 to-purple-50";
    }
}
