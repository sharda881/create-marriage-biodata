package com.biodatamaker.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PUT /api/profile}.
 */
public record ProfileUpdateRequest(
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters") String name,
        @Pattern(regexp = "^$|^[0-9]{10}$", message = "Phone number must be 10 digits") String phone
) {
}
