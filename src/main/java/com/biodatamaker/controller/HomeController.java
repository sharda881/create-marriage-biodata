package com.biodatamaker.controller;

import com.biodatamaker.config.OAuth2AvailabilityConfig.OAuth2Availability;
import com.biodatamaker.dto.RegistrationDTO;
import com.biodatamaker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for home and landing pages.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final OAuth2Availability oAuth2Availability;

    /**
     * Landing page - redirects to dashboard if authenticated
     */
    @GetMapping("/")
    public String home() {
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    /**
     * Login page
     */
    @GetMapping("/login")
    public String login(Model model) {
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("googleOAuthEnabled", oAuth2Availability.googleEnabled());
        return "auth/login";
    }

    /**
     * Registration page
     */
    @GetMapping("/register")
    public String register(Model model) {
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registration", new RegistrationDTO());
        model.addAttribute("googleOAuthEnabled", oAuth2Availability.googleEnabled());
        return "auth/register";
    }
}
