package com.biodatamaker.dto;

/**
 * Response body for login / register: the JWT access token plus the current user.
 */
public record AuthResponse(String token, UserDTO user) {
}
