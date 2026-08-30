package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Golden Heritage — ivory background with ornate copper-gold floral corners and a
 * Ganesha invocation header. A classic Indian wedding-invitation look.
 */
@Component
public class GoldTemplate extends AbstractBioDataTemplate {

    public GoldTemplate() {
        super(
                "gold",
                "Golden Heritage",
                "Ivory background with ornate golden floral borders and a Shree Ganeshaya "
                        + "invocation. A timeless, traditional wedding-invitation style.",
                "#9A5B2E",  // Copper brown
                "#C9922F",  // Gold
                "#FFFFFF",  // White / ivory
                true        // Premium template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-gold";
    }
}
