package com.biodatamaker.controller;

import com.biodatamaker.dto.BioDataDTO;
import com.biodatamaker.dto.PaymentDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.PaymentTransaction;
import com.biodatamaker.entity.User;
import com.biodatamaker.service.BioDataService;
import com.biodatamaker.service.PaymentService;
import com.biodatamaker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Payment endpoints: QR-code checkout info, manual UPI transaction submission,
 * status polling and history. Payment always requires an authenticated user.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final BioDataService bioDataService;

    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping("/checkout/{bioDataId}")
    public Map<String, Object> checkout(@PathVariable Long bioDataId) {
        User user = requireUser();
        BioData bioData = bioDataService.getBioDataForUser(bioDataId, user);

        Map<String, Object> response = new HashMap<>();
        response.put("bioData", BioDataDTO.fromEntity(bioData));
        response.put("alreadyPaid", Boolean.TRUE.equals(bioData.getIsPaid()));
        response.put("price", paymentService.getDownloadPrice());
        response.put("upiId", paymentService.getUpiId());
        response.put("qrCodeUrl", baseUrl + "/images/payment_qr.png");

        Optional<PaymentTransaction> existing = paymentService.getPaymentForBioData(bioDataId);
        boolean pending = existing.isPresent()
                && existing.get().getStatus() == PaymentTransaction.PaymentStatus.PENDING;
        response.put("pendingPayment", pending);
        existing.filter(p -> pending).ifPresent(p -> response.put("existingTransaction", PaymentDTO.fromEntity(p)));
        return response;
    }

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDTO submit(@RequestBody Map<String, String> body) {
        User user = requireUser();
        Long bioDataId = parseBioDataId(body.get("bioDataId"));
        String transactionId = body.getOrDefault("transactionId", "").trim();

        if (transactionId.length() < 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid transaction ID. Please enter a valid UPI transaction ID.");
        }
        PaymentTransaction payment = paymentService.submitPayment(user, bioDataId, transactionId);
        return PaymentDTO.fromEntity(payment);
    }

    @GetMapping("/{id}")
    public PaymentDTO get(@PathVariable Long id) {
        return PaymentDTO.fromEntity(ownedPayment(id));
    }

    @GetMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable Long id) {
        PaymentTransaction payment = ownedPayment(id);
        Map<String, Object> response = new HashMap<>();
        response.put("payment", PaymentDTO.fromEntity(payment));
        response.put("canDownload", payment.getStatus() == PaymentTransaction.PaymentStatus.APPROVED);
        return response;
    }

    @GetMapping("/history")
    public List<PaymentDTO> history() {
        return paymentService.getUserPayments(requireUser());
    }

    private PaymentTransaction ownedPayment(Long id) {
        User user = requireUser();
        PaymentTransaction payment = paymentService.getPaymentById(id);
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your payment");
        }
        return payment;
    }

    private Long parseBioDataId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bioDataId is required");
        }
    }

    private User requireUser() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
