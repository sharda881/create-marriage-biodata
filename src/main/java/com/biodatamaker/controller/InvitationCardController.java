package com.biodatamaker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for Wedding Invitation Card feature (Coming Soon)
 */
@Controller
@RequestMapping("/invitation-card")
public class InvitationCardController {

    /**
     * Display the Coming Soon page for Wedding Invitation Card
     */
    @GetMapping
    public String showComingSoon() {
        return "invitation/coming-soon";
    }

    /**
     * Alias route for invitation card
     */
    @GetMapping("/create")
    public String createInvitation() {
        // For now, redirect to coming soon page
        return "redirect:/invitation-card";
    }
}
