package com.biodatamaker.template;

/**
 * Visual theme for a wedding invitation card. Mirrored on the frontend
 * ({@code src/invitation/themes.ts}).
 */
public record InvitationTheme(
        String id,
        String name,
        String panelBg,       // main panel background (css color or gradient)
        String pageBg,         // area outside the panel
        String ink,            // body text colour on the panel
        String accent,         // headings / names / rules
        String script,         // colour for the couple's names (script font)
        String border,         // ornate frame colour
        boolean woodTexture    // overlay a subtle wood grain on the panel
) {
}
