package com.biodatamaker.controller;

import com.biodatamaker.config.OAuth2AvailabilityConfig.OAuth2Availability;
import com.biodatamaker.dto.AuthResponse;
import com.biodatamaker.dto.LoginRequest;
import com.biodatamaker.dto.RegistrationDTO;
import com.biodatamaker.dto.UserDTO;
import com.biodatamaker.entity.User;
import com.biodatamaker.security.JwtService;
import com.biodatamaker.service.UserService;
import com.biodatamaker.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Authentication endpoints for the SPA: register, login (JWT), current user, OAuth status.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OAuth2Availability oAuth2Availability;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegistrationDTO registration) {
        if (!registration.passwordsMatch()) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        User user = userService.registerUser(registration);
        return new AuthResponse(jwtService.generateToken(user), UserDTO.fromEntity(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = userService.findByEmailOrPhone(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        userService.updateLastLogin(user.getId());
        return new AuthResponse(jwtService.generateToken(user), UserDTO.fromEntity(user));
    }

    @GetMapping("/me")
    public UserDTO me() {
        User user = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
        return userService.getUserDTO(user.getId());
    }

    @GetMapping("/oauth-status")
    public ResponseEntity<Map<String, Boolean>> oauthStatus() {
        return ResponseEntity.ok(Map.of("googleEnabled", oAuth2Availability.googleEnabled()));
    }
}
