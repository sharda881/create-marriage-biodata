package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Divine Maroon — deep maroon and gold with a Shree Ganeshaya invocation, floral
 * corner flourishes and a Ganesha motif watermark. A rich, devotional
 * wedding-invitation look with "Label : Value" detail rows.
 */
@Component
public class DivineTemplate extends AbstractBioDataTemplate {

    public DivineTemplate() {
        super(
                "divine",
                "Divine Maroon",
                "Deep maroon and gold with a Shree Ganeshaya invocation, floral corner "
                        + "flourishes and a Ganesha motif watermark. A rich, devotional look.",
                "#D4A24E", // Gold accent
                "#C9A66B", // Muted gold
                "#3D0E1F", // Deep maroon background
                true        // Premium template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-divine";
    }
}
