package com.biodatamaker.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for the application.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TemplateNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleTemplateNotFound(TemplateNotFoundException ex, Model model) {
        log.error("Template not found: {}", ex.getMessage());
        model.addAttribute("error", "Template not found");
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        log.error("Resource not found: {}", ex.getMessage());
        model.addAttribute("error", "Resource not found");
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(PaymentRequiredException.class)
    public String handlePaymentRequired(PaymentRequiredException ex, RedirectAttributes redirectAttributes) {
        log.info("Payment required for bio-data: {}", ex.getBioDataId());
        redirectAttributes.addFlashAttribute("paymentRequired", true);
        redirectAttributes.addFlashAttribute("bioDataId", ex.getBioDataId());
        return "redirect:/payment/checkout/" + ex.getBioDataId();
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorizedAccess(UnauthorizedAccessException ex, Model model) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        model.addAttribute("error", "Access Denied");
        model.addAttribute("message", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error occurred", ex);
        model.addAttribute("error", "Something went wrong");
        model.addAttribute("message", "An unexpected error occurred. Please try again later.");
        return "error/500";
    }
}
