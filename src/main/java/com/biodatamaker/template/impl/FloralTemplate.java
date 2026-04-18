package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Floral template with beautiful flower patterns and soft colors.
 * Features romantic design elements perfect for matrimonial profiles.
 */
@Component
public class FloralTemplate extends AbstractBioDataTemplate {

    public FloralTemplate() {
        super(
                "floral",
                "Floral Romance",
                "A beautiful design with delicate floral patterns and soft pastel colors. Ideal for a romantic touch.",
                "#C45B7D",  // Pink
                "#E88DA8",  // Light Pink
                "#FFF5F7",  // Very light pink
                false       // Free template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-floral bg-gradient-to-br from-pink-50 to-rose-50";
    }
}
