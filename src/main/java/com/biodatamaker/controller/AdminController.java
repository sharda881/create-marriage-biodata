package com.biodatamaker.controller;

import com.biodatamaker.dto.BioDataDTO;
import com.biodatamaker.dto.PaymentDTO;
import com.biodatamaker.dto.UserDTO;
import com.biodatamaker.entity.BioData;
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
        List<PaymentDTO> pending = paymentService.listByStatus(BioData.PaymentStatus.PENDING);
        return Map.of(
                "totalUsers", userService.countUsers(),
                "pendingPayments", paymentService.countByStatus(BioData.PaymentStatus.PENDING),
                "totalRevenue", paymentService.totalRevenue(),
                "recentPending", pending.stream().limit(5).toList()
        );
    }

    @GetMapping("/payments")
    public Map<String, Object> payments(@RequestParam(defaultValue = "PAID") String status) {
        BioData.PaymentStatus paymentStatus;
        try {
            paymentStatus = BioData.PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            paymentStatus = BioData.PaymentStatus.PAID;
        }
        return Map.of(
                "payments", paymentService.listByStatus(paymentStatus),
                "currentStatus", paymentStatus.name(),
                "pendingCount", paymentService.countByStatus(BioData.PaymentStatus.PENDING)
        );
    }

    @GetMapping("/payments/{bioDataId}")
    public Map<String, Object> paymentDetails(@PathVariable Long bioDataId) {
        BioData bioData = bioDataService.getBioDataById(bioDataId);
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("payment", PaymentDTO.fromEntity(bioData));
        out.put("bioData", BioDataDTO.fromEntity(bioData));
        out.put("user", bioData.getUser() != null ? UserDTO.fromEntity(bioData.getUser()) : null);
        return out;
    }

    /** Admin override — force a bio-data to PAID (e.g. paid out-of-band via UPI). */
    @PostMapping("/payments/{bioDataId}/mark-paid")
    public PaymentDTO markPaid(@PathVariable Long bioDataId) {
        return paymentService.markPaidManually(bioDataId, currentAdmin());
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
