package com.biodatamaker.dto;

import com.biodatamaker.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for User entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;

    @Email(message = "Please provide a valid email address")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    private String provider;

    private User.Role role;

    private String profilePhotoUrl;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    private int bioDataCount;

    /**
     * Create DTO from Entity
     */
    public static UserDTO fromEntity(User entity) {
        return UserDTO.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .name(entity.getName())
                .provider(entity.getProvider())
                .role(entity.getRole())
                .profilePhotoUrl(entity.getProfilePhotoUrl())
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .lastLoginAt(entity.getLastLoginAt())
                .bioDataCount(entity.getBioDataCount())
                .build();
    }
}
