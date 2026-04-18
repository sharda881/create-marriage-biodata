package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Royal template with luxurious gold accents and regal design.
 * Features premium aesthetics with elegant typography and rich colors.
 */
@Component
public class RoyalTemplate extends AbstractBioDataTemplate {

    public RoyalTemplate() {
        super(
                "royal",
                "Royal Gold",
                "A luxurious design with gold accents and regal aesthetics. Perfect for making a grand impression.",
                "#722F37",  // Maroon
                "#C4A35A",  // Gold
                "#FDF8F0",  // Light cream
                true        // Premium template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-royal bg-gradient-to-br from-amber-50 to-yellow-50";
    }
}
