package com.biodatamaker.template;

/**
 * Visual theme for the shared PDF layouts. {@code layout} picks which Thymeleaf
 * document renders it: {@code stacked} (photo/name header + sections top to
 * bottom, {@code templates/biodata/pdf/document.html}) or {@code split} (dark
 * sidebar + icon-headed sections, {@code templates/biodata/pdf/document-split.html}).
 * Mirrors the React {@code TEMPLATE_THEMES} in the frontend so the downloaded PDF
 * matches the on-screen preview for every template.
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
        String headerStyle,   // pill | bar | underline | plain (stacked layout only)
        String flourish,      // small centered glyph above the name (may be null)
        String footerMotif,   // repeated glyph in the footer rule (may be null)
        String cornerStyle,   // key (default) | floral
        String headerMotif,   // flourish (default) | ganesha
        boolean colonRows,    // render a " : " separator between label and value
        String layout         // stacked (default) | split
) {
    /** Backwards-compatible constructor: key corners, flourish header, no colon, stacked layout. */
    public PdfTheme(String pageBg, String surfaceBg, String text, String muted, String accent,
                    String onAccent, String border, String deco, String fontFamily,
                    String headerStyle, String flourish, String footerMotif) {
        this(pageBg, surfaceBg, text, muted, accent, onAccent, border, deco, fontFamily,
                headerStyle, flourish, footerMotif, "key", "flourish", false, "stacked");
    }

    /** Backwards-compatible constructor for the ganesha/floral/colon-row templates (stacked layout). */
    public PdfTheme(String pageBg, String surfaceBg, String text, String muted, String accent,
                    String onAccent, String border, String deco, String fontFamily,
                    String headerStyle, String flourish, String footerMotif,
                    String cornerStyle, String headerMotif, boolean colonRows) {
        this(pageBg, surfaceBg, text, muted, accent, onAccent, border, deco, fontFamily,
                headerStyle, flourish, footerMotif, cornerStyle, headerMotif, colonRows, "stacked");
    }
}
