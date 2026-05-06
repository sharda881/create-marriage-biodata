package com.biodatamaker.template.impl;

import com.biodatamaker.template.AbstractBioDataTemplate;
import org.springframework.stereotype.Component;

/**
 * Ornate template with mauve/tan theme and decorative corner ornaments.
 * Features elegant border design with vintage styling.
 */
@Component
public class OrnateTemplate extends AbstractBioDataTemplate {

    public OrnateTemplate() {
        super(
                "ornate",
                "Ornate Mauve",
                "A sophisticated design with mauve background and ornate tan borders. Perfect for those seeking a vintage, elegant aesthetic.",
                "#927668",  // Mauve
                "#dec2b1",  // Tan
                "#927668",  // Mauve background
                false       // Free template
        );
    }

    @Override
    public String getCssClasses() {
        return "template-ornate bg-gradient-to-br from-stone-400 to-stone-500";
    }
}
