package com.biodatamaker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Wedding Invitation Card feature - not built yet.
 */
@RestController
@RequestMapping("/api/invitation-card")
public class InvitationCardController {

    @GetMapping
    public Map<String, String> status() {
        return Map.of("status", "COMING_SOON");
    }
}
