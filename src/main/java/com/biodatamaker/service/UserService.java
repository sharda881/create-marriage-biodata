package com.biodatamaker.service;

import com.biodatamaker.dto.RegistrationDTO;
import com.biodatamaker.dto.UserDTO;
import com.biodatamaker.entity.User;
import com.biodatamaker.exception.ResourceNotFoundException;
import com.biodatamaker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for user management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user with email/password
     */
    @Transactional
    public User registerUser(RegistrationDTO registrationDTO) {
        // Check if email already exists
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Check if phone already exists (if provided)
        if (registrationDTO.getPhone() != null &&
                !registrationDTO.getPhone().isBlank() &&
                userRepository.existsByPhone(registrationDTO.getPhone())) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        User user = User.builder()
                .name(registrationDTO.getName())
                .email(registrationDTO.getEmail())
                .phone(registrationDTO.getPhone())
                .password(passwordEncoder.encode(registrationDTO.getPassword()))
                .provider("local")
                .role(User.Role.USER)
                .enabled(true)
                .emailVerified(false) // Can implement email verification later
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());
        return savedUser;
    }

    /**
     * Create or update user from OAuth2 login
     */
    @Transactional
    public User processOAuthUser(String email, String name, String provider, String providerId, String photoUrl) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            // Update profile photo if available
            if (photoUrl != null && !photoUrl.isBlank()) {
                user.setProfilePhotoUrl(photoUrl);
            }
            return userRepository.save(user);
        }

        // Create new user
        User newUser = User.builder()
                .email(email)
                .name(name)
                .provider(provider)
                .providerId(providerId)
                .profilePhotoUrl(photoUrl)
                .role(User.Role.USER)
                .enabled(true)
                .emailVerified(true) // OAuth users are considered verified
                .lastLoginAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New OAuth user created: {} via {}", email, provider);
        return savedUser;
    }

    /**
     * Find user by ID
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by email or phone
     */
    public Optional<User> findByEmailOrPhone(String emailOrPhone) {
        return userRepository.findByEmailOrPhone(emailOrPhone, emailOrPhone);
    }

    /**
     * Get user DTO by ID
     */
    public UserDTO getUserDTO(Long id) {
        return UserDTO.fromEntity(findById(id));
    }

    /**
     * Update user profile
     */
    @Transactional
    public User updateProfile(Long userId, String name, String phone) {
        User user = findById(userId);

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }

        if (phone != null && !phone.isBlank()) {
            // Check if phone is used by another user
            Optional<User> existingUser = userRepository.findByPhone(phone);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new IllegalArgumentException("Phone number already in use");
            }
            user.setPhone(phone);
        }

        return userRepository.save(user);
    }

    /**
     * Update last login timestamp
     */
    @Transactional
    public void updateLastLogin(Long userId) {
        User user = findById(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Get all users (for admin)
     */
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Count total users
     */
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * Count users by role
     */
    public long countUsersByRole(User.Role role) {
        return userRepository.countByRole(role);
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin(Long userId) {
        return findById(userId).getRole() == User.Role.ADMIN;
    }

    /**
     * Make user an admin
     */
    @Transactional
    public void makeAdmin(Long userId) {
        User user = findById(userId);
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
        log.info("User {} promoted to admin", userId);
    }

    /**
     * Disable a user
     */
    @Transactional
    public void disableUser(Long userId) {
        User user = findById(userId);
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User {} disabled", userId);
    }

    /**
     * Enable a user
     */
    @Transactional
    public void enableUser(Long userId) {
        User user = findById(userId);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("User {} enabled", userId);
    }
}
