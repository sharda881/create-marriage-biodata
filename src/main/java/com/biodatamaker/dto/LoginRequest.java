package com.biodatamaker.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/login}. The username may be an email or phone.
 */
public record LoginRequest(
        @NotBlank(message = "Email or phone is required") String email,
        @NotBlank(message = "Password is required") String password
) {
}
