package com.biodatamaker.controller;

import com.biodatamaker.entity.User;
import com.biodatamaker.service.PaymentService;
import com.biodatamaker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Razorpay checkout for unlocking bio-data PDF downloads. All endpoints work for
 * anonymous visitors (the paywall is keyed off the bio-data, not the user).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /** Price + whether payment is actually required for this bio-data. */
    @GetMapping("/quote/{bioDataId}")
    public Map<String, Object> quote(@PathVariable Long bioDataId) {
        return paymentService.quote(bioDataId, currentUserOrNull());
    }

    /** Create a Razorpay order; response feeds the Razorpay Checkout widget. */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> checkout(@RequestBody Map<String, String> body) {
        Long bioDataId = parseBioDataId(body.get("bioDataId"));
        return paymentService.createCheckout(
                bioDataId,
                currentUserOrNull(),
                body.get("name"),
                body.get("email"),
                body.get("phone"),
                Boolean.parseBoolean(body.get("deliverByEmail")));
    }

    /** Razorpay server-to-server webhook. Must stay public + signature-verified. */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }

    /** Polled by the SPA after checkout closes; reconciles against Razorpay if needed. */
    @GetMapping("/status/{bioDataId}")
    public Map<String, Object> status(@PathVariable Long bioDataId) {
        return paymentService.status(bioDataId);
    }

    private Long parseBioDataId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bioDataId is required");
        }
    }

    private User currentUserOrNull() {
        return SecurityUtils.getCurrentUser().orElse(null);
    }
}
