package com.biodatamaker.controller;

import com.biodatamaker.dto.RegistrationDTO;
import com.biodatamaker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for authentication-related operations.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    /**
     * Show registration form
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registration", new RegistrationDTO());
        return "auth/register";
    }

    /**
     * Process registration
     */
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registration") RegistrationDTO registration,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        // Validate passwords match
        if (!registration.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "error.registration", "Passwords do not match");
            return "auth/register";
        }

        try {
            userService.registerUser(registration);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            log.error("Registration error", e);
            model.addAttribute("error", "An error occurred during registration. Please try again.");
            return "auth/register";
        }
    }
}
