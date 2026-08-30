package com.biodatamaker.controller;

import com.biodatamaker.dto.InvitationCardDTO;
import com.biodatamaker.service.InvitationService;
import com.biodatamaker.template.InvitationThemeRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Wedding invitation card maker — stateless. Pick a theme, post the card details,
 * get back a PNG (or PDF).
 */
@RestController
@RequestMapping("/api/invitation-card")
@RequiredArgsConstructor
public class InvitationCardController {

    private final InvitationService invitationService;
    private final InvitationThemeRegistry themes;

    /** Available card themes. */
    @GetMapping
    public Object themes() {
        return java.util.Map.of("status", "AVAILABLE", "themes", themes.all());
    }

    /** Render the card. {@code ?format=pdf} for a print-ready A5 PDF, otherwise PNG. */
    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@Valid @RequestBody InvitationCardDTO card,
                                           @RequestParam(defaultValue = "png") String format) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] bytes = pdf ? invitationService.renderPdf(card) : invitationService.renderPng(card);
        String base = "invitation_"
                + (card.groomName() + "_" + card.brideName()).replaceAll("\\s+", "_");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + base + (pdf ? ".pdf" : ".png") + "\"")
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.IMAGE_PNG)
                .body(bytes);
    }
}
