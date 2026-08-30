package com.biodatamaker.template;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The 10 PDF themes, kept in sync with the frontend's
 * {@code src/templates/registry.tsx TEMPLATE_THEMES}.
 */
@Component
public class PdfThemeRegistry {

    private static final String SERIF = "'Playfair Display', Georgia, 'Times New Roman', serif";
    private static final String SANS = "'Poppins', 'Helvetica Neue', Arial, sans-serif";

    private final Map<String, PdfTheme> themes = Map.ofEntries(
            Map.entry("traditional", new PdfTheme(
                    "#EADFC7", "#F5EBD8", "#2b2b2b", "#6b5b43", "#8B0000", "#ffffff",
                    "#8B4513", "#8B0000", SERIF, "bar", "ॐ", "✦")),
            Map.entry("royal", new PdfTheme(
                    "#EFE2C9", "#FDF8F0", "#3a2a20", "#8a6f52", "#722F37", "#ffffff",
                    "#C4A35A", "#C4A35A", SERIF, "pill", "❧", "✧")),
            Map.entry("modern", new PdfTheme(
                    "#E3ECF2", "#F5F8FA", "#1f2937", "#5b7488", "#2C5F7C", "#ffffff",
                    "#4A8DB7", "#2C5F7C", SANS, "underline", null, null)),
            Map.entry("elegant", new PdfTheme(
                    "#F0E6F4", "#FAF5FC", "#2e2433", "#7a6484", "#6B4C7B", "#ffffff",
                    "#9B6BA8", "#9B6BA8", SERIF, "pill", "❦", null)),
            Map.entry("floral", new PdfTheme(
                    "#FCE7EC", "#FFF5F7", "#3d2830", "#a56b7d", "#C45B7D", "#ffffff",
                    "#E88DA8", "#C45B7D", SERIF, "pill", "✿", "❀")),
            Map.entry("simple", new PdfTheme(
                    "#E6EFE8", "#F5F8F5", "#1f2b25", "#5a7566", "#3D5A4C", "#ffffff",
                    "#5B8A72", "#3D5A4C", SANS, "plain", null, null)),
            Map.entry("premium", new PdfTheme(
                    "#ECE6FB", "#F5F3FF", "#241c38", "#6d5f8c", "#7C3AED", "#ffffff",
                    "#A78BFA", "#7C3AED", SANS, "bar", "◆", null)),
            Map.entry("premium-silver", new PdfTheme(
                    "#E2E5E9", "#F5F5F5", "#26303c", "#64748b", "#475569", "#ffffff",
                    "#CBD5E1", "#64748b", SERIF, "pill", "✦", "✦")),
            Map.entry("premium-green", new PdfTheme(
                    "#1c3226", "#2C4A3A", "#f3ece0", "#c7d6c9", "#D4AF37", "#1c3226",
                    "#D4AF37", "#D4AF37", SERIF, "pill", "ॐ", "✦")),
            Map.entry("ornate", new PdfTheme(
                    "#6f594e", "#927668", "#f6efe9", "#e3d3c7", "#dec2b1", "#4a3a30",
                    "#dec2b1", "#dec2b1", SERIF, "bar", "❦", "❧")),
            Map.entry("gold", new PdfTheme(
                    "#F4ECDD", "#FFFFFF", "#33291d", "#7a6a54", "#9A5B2E", "#ffffff",
                    "#C9922F", "#B87333", SERIF, "plain", null, null,
                    "floral", "ganesha", true))
    );

    public PdfTheme themeFor(String templateId) {
        if (templateId == null) {
            return themes.get("traditional");
        }
        return themes.getOrDefault(templateId, themes.get("traditional"));
    }
}
