package com.biodatamaker.controller;

import com.biodatamaker.dto.PaymentDTO;
import com.biodatamaker.dto.UserDTO;
import com.biodatamaker.entity.PaymentTransaction;
import com.biodatamaker.entity.User;
import com.biodatamaker.service.BioDataService;
import com.biodatamaker.service.PaymentService;
import com.biodatamaker.service.SystemConfigService;
import com.biodatamaker.service.UserService;
import com.biodatamaker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Admin API: dashboard stats, payment verification, user list and system config.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final PaymentService paymentService;
    private final UserService userService;
    private final BioDataService bioDataService;
    private final SystemConfigService configService;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        List<PaymentDTO> pending = paymentService.getPendingPayments();
        return Map.of(
                "totalUsers", userService.countUsers(),
                "pendingPayments", paymentService.countPendingPayments(),
                "totalRevenue", paymentService.getTotalApprovedAmount(),
                "recentPending", pending.stream().limit(5).toList()
        );
    }

    @GetMapping("/payments")
    public Map<String, Object> payments(@RequestParam(defaultValue = "PENDING") String status) {
        PaymentTransaction.PaymentStatus paymentStatus;
        try {
            paymentStatus = PaymentTransaction.PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            paymentStatus = PaymentTransaction.PaymentStatus.PENDING;
        }
        return Map.of(
                "payments", paymentService.getPaymentsByStatus(paymentStatus),
                "currentStatus", paymentStatus.name(),
                "pendingCount", paymentService.countPendingPayments()
        );
    }

    @GetMapping("/payments/{id}")
    public Map<String, Object> paymentDetails(@PathVariable Long id) {
        PaymentTransaction payment = paymentService.getPaymentById(id);
        return Map.of(
                "payment", PaymentDTO.fromEntity(payment),
                "bioData", com.biodatamaker.dto.BioDataDTO.fromEntity(payment.getBioData()),
                "user", UserDTO.fromEntity(payment.getUser())
        );
    }

    @PostMapping("/payments/{id}/approve")
    public PaymentDTO approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return PaymentDTO.fromEntity(paymentService.approvePayment(id, currentAdmin(), notes));
    }

    @PostMapping("/payments/{id}/reject")
    public PaymentDTO reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        if (reason == null || reason.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
        }
        return PaymentDTO.fromEntity(paymentService.rejectPayment(id, currentAdmin(), reason));
    }

    @GetMapping("/users")
    public List<UserDTO> users() {
        return userService.getAllUsers();
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "configs", configService.getAllActiveConfigs(),
                "freeLimit", configService.getFreeLimitCount(),
                "downloadPrice", configService.getDownloadPrice(),
                "paywallEnabled", configService.isPaywallEnabled()
        );
    }

    @PostMapping("/config")
    public Map<String, Object> updateConfig(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        String value = body.get("value");
        if (key == null || key.isBlank() || value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key and value are required");
        }
        configService.saveConfig(key, value, null, null);
        return config();
    }

    private User currentAdmin() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not authenticated"));
    }
}
