package com.biodatamaker.controller;

import com.biodatamaker.dto.PaymentDTO;
import com.biodatamaker.entity.PaymentTransaction;
import com.biodatamaker.entity.User;
import com.biodatamaker.service.BioDataService;
import com.biodatamaker.service.PaymentService;
import com.biodatamaker.service.SystemConfigService;
import com.biodatamaker.service.UserService;
import com.biodatamaker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for admin operations including payment verification.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final PaymentService paymentService;
    private final UserService userService;
    private final BioDataService bioDataService;
    private final SystemConfigService configService;

    /**
     * Admin dashboard
     */
    @GetMapping
    public String dashboard(Model model) {
        // Stats
        long totalUsers = userService.countUsers();
        long pendingPayments = paymentService.countPendingPayments();
        BigDecimal totalRevenue = paymentService.getTotalApprovedAmount();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("pendingPayments", pendingPayments);
        model.addAttribute("totalRevenue", totalRevenue);

        // Recent pending payments
        List<PaymentDTO> recentPending = paymentService.getPendingPayments();
        model.addAttribute("recentPending", recentPending.stream().limit(5).toList());

        return "admin/dashboard";
    }

    /**
     * Payment management page - list all pending payments
     */
    @GetMapping("/payments")
    public String payments(@RequestParam(defaultValue = "PENDING") String status, Model model) {
        PaymentTransaction.PaymentStatus paymentStatus;
        try {
            paymentStatus = PaymentTransaction.PaymentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            paymentStatus = PaymentTransaction.PaymentStatus.PENDING;
        }

        List<PaymentDTO> payments = paymentService.getPaymentsByStatus(paymentStatus);
        model.addAttribute("payments", payments);
        model.addAttribute("currentStatus", paymentStatus);
        model.addAttribute("pendingCount", paymentService.countPendingPayments());

        return "admin/payments";
    }

    /**
     * View payment details
     */
    @GetMapping("/payments/{id}")
    public String paymentDetails(@PathVariable Long id, Model model) {
        PaymentTransaction payment = paymentService.getPaymentById(id);
        model.addAttribute("payment", PaymentDTO.fromEntity(payment));
        model.addAttribute("bioData", payment.getBioData());
        model.addAttribute("user", payment.getUser());

        return "admin/payment-details";
    }

    /**
     * Approve a payment
     */
    @PostMapping("/payments/{id}/approve")
    public String approvePayment(@PathVariable Long id,
                                  @RequestParam(required = false) String notes,
                                  RedirectAttributes redirectAttributes) {
        User admin = getCurrentAdmin();

        try {
            paymentService.approvePayment(id, admin, notes);
            redirectAttributes.addFlashAttribute("success", "Payment approved successfully!");
            log.info("Payment {} approved by admin {}", id, admin.getId());
        } catch (Exception e) {
            log.error("Error approving payment {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error approving payment: " + e.getMessage());
        }

        return "redirect:/admin/payments";
    }

    /**
     * Reject a payment
     */
    @PostMapping("/payments/{id}/reject")
    public String rejectPayment(@PathVariable Long id,
                                 @RequestParam String reason,
                                 RedirectAttributes redirectAttributes) {
        User admin = getCurrentAdmin();

        if (reason == null || reason.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Rejection reason is required");
            return "redirect:/admin/payments/" + id;
        }

        try {
            paymentService.rejectPayment(id, admin, reason);
            redirectAttributes.addFlashAttribute("success", "Payment rejected.");
            log.info("Payment {} rejected by admin {}: {}", id, admin.getId(), reason);
        } catch (Exception e) {
            log.error("Error rejecting payment {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error rejecting payment: " + e.getMessage());
        }

        return "redirect:/admin/payments";
    }

    /**
     * User management page
     */
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    /**
     * System configuration page
     */
    @GetMapping("/config")
    public String config(Model model) {
        model.addAttribute("configs", configService.getAllActiveConfigs());
        model.addAttribute("freeLimit", configService.getFreeLimitCount());
        model.addAttribute("downloadPrice", configService.getDownloadPrice());
        model.addAttribute("paywallEnabled", configService.isPaywallEnabled());
        return "admin/config";
    }

    /**
     * Update system configuration
     */
    @PostMapping("/config/update")
    public String updateConfig(@RequestParam String key,
                               @RequestParam String value,
                               RedirectAttributes redirectAttributes) {
        try {
            configService.saveConfig(key, value, null, null);
            redirectAttributes.addFlashAttribute("success", "Configuration updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating configuration: " + e.getMessage());
        }
        return "redirect:/admin/config";
    }

    // Helper method
    private User getCurrentAdmin() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Admin not authenticated"));
    }
}
