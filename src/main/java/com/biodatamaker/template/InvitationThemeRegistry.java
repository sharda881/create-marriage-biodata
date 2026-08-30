package com.biodatamaker.template;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * The wedding-invitation card themes. Kept in sync with the frontend's
 * {@code src/invitation/themes.ts}.
 */
@Component
public class InvitationThemeRegistry {

    private final Map<String, InvitationTheme> themes = Map.of(
            "wooden", new InvitationTheme(
                    "wooden", "Rustic Wood & Roses",
                    "#3a2318", "#f3e7cf", "#f4e6c9", "#e8c98a", "#f0d9a8", "#c9a24a", true),
            "maroon", new InvitationTheme(
                    "maroon", "Royal Maroon",
                    "#4a0d16", "#2b0810", "#f2dcae", "#d9b45f", "#e9cf93", "#c8a24e", false),
            "blue", new InvitationTheme(
                    "blue", "Peacock Blue",
                    "#123a63", "#0b243f", "#e9e2c9", "#d7bd74", "#efe4bd", "#c9a54e", false),
            "ivory", new InvitationTheme(
                    "ivory", "Ivory & Gold",
                    "#faf3e3", "#efe4cd", "#5a4a2e", "#a9822f", "#8a6a24", "#b98f34", false)
    );

    public InvitationTheme themeFor(String id) {
        return id == null ? themes.get("wooden") : themes.getOrDefault(id, themes.get("wooden"));
    }

    public List<InvitationTheme> all() {
        return List.of(
                themes.get("wooden"), themes.get("maroon"),
                themes.get("blue"), themes.get("ivory"));
    }
}
