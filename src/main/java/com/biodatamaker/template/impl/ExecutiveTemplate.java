package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Executive — a modern two-column resume-style layout: a black sidebar with
 * photo + personal-info panel on the left, icon-headed sections (family,
 * education, hobbies, career) and a contact footer on the right.
 */
@Component
public class ExecutiveTemplate extends AbstractBioDataTemplate {

    public ExecutiveTemplate() {
        super(
                "executive",
                "Executive",
                "A modern two-column resume-style layout: a black sidebar with your photo "
                        + "and personal info, icon-headed sections and a contact footer.",
                "#111111", // Black accent
                "#6b7280", // Grey
                "#FFFFFF", // White background
                true        // Premium template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-executive";
    }
}
