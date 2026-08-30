package com.biodatamaker.template;

/**
 * Visual theme for the single shared PDF layout ({@code templates/biodata/pdf/document.html}).
 * Mirrors the React {@code TEMPLATE_THEMES} in the frontend so the downloaded PDF matches
 * the on-screen preview for every template.
 */
public record PdfTheme(
        String pageBg,
        String surfaceBg,
        String text,
        String muted,
        String accent,
        String onAccent,
        String border,
        String deco,
        String fontFamily,
        String headerStyle,   // pill | bar | underline | plain
        String flourish,      // small centered glyph above the name (may be null)
        String footerMotif,   // repeated glyph in the footer rule (may be null)
        String cornerStyle,   // key (default) | floral
        String headerMotif,   // flourish (default) | ganesha
        boolean colonRows     // render a " : " separator between label and value
) {
    /** Backwards-compatible constructor: key corners, flourish header, no colon. */
    public PdfTheme(String pageBg, String surfaceBg, String text, String muted, String accent,
                    String onAccent, String border, String deco, String fontFamily,
                    String headerStyle, String flourish, String footerMotif) {
        this(pageBg, surfaceBg, text, muted, accent, onAccent, border, deco, fontFamily,
                headerStyle, flourish, footerMotif, "key", "flourish", false);
    }
}
