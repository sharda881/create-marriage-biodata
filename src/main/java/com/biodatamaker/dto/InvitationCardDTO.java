package com.biodatamaker.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for generating a wedding invitation card. Stateless — nothing is
 * persisted; the card is rendered to an image / PDF on demand.
 */
public record InvitationCardDTO(
        String templateId,
        String invocation,          // e.g. "|| Shree Ganeshaya Namaha ||"
        String inviteLine,          // e.g. "You are invited to the wedding of"
        @NotBlank String groomName,
        @NotBlank String brideName,
        String weddingDate,         // ISO yyyy-MM-dd (rendered as "December | 30 | 2023")
        String weddingTime,         // free text, e.g. "12:30 pm onwards"
        String venueName,
        String venueAddress,
        String regardsLabel,        // e.g. "Regards"
        String hostedBy,            // family / hosts
        String extraEvents          // optional free text (Sangeet / Mehendi lines)
) {
}
