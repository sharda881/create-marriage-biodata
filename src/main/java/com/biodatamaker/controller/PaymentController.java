package com.biodatamaker.controller;

import com.biodatamaker.dto.PaymentDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.PaymentTransaction;
import com.biodatamaker.entity.User;
import com.biodatamaker.service.BioDataService;
import com.biodatamaker.service.PaymentService;
import com.biodatamaker.service.SystemConfigService;
import com.biodatamaker.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Controller for payment operations including QR code checkout and status checking.
 */
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final BioDataService bioDataService;
    private final SystemConfigService configService;

    /**
     * Show payment checkout page with QR code
     */
    @GetMapping("/checkout/{bioDataId}")
    public String checkout(@PathVariable Long bioDataId, Model model) {
        User currentUser = getCurrentUser();

        // Get bio-data
        BioData bioData = bioDataService.getBioDataForUser(bioDataId, currentUser);

        // Check if already paid
        if (bioData.getIsPaid()) {
            return "redirect:/biodata/download/" + bioDataId;
        }

        // Check for existing pending payment
        Optional<PaymentTransaction> existingPayment = paymentService.getPaymentForBioData(bioDataId);
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentTransaction.PaymentStatus.PENDING) {
            model.addAttribute("pendingPayment", true);
            model.addAttribute("existingTransaction", existingPayment.get());
        }

        model.addAttribute("bioData", bioData);
        model.addAttribute("price", paymentService.getDownloadPrice());
        model.addAttribute("upiId", paymentService.getUpiId());
        model.addAttribute("qrCodePath", "/images/payment_qr.png");

        return "payment/checkout";
    }

    /**
     * Submit payment transaction ID for verification
     */
    @PostMapping("/submit")
    public String submitPayment(@RequestParam Long bioDataId,
                                @RequestParam String transactionId,
                                RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();

        try {
            // Validate transaction ID format
            if (transactionId == null || transactionId.trim().length() < 12) {
                redirectAttributes.addFlashAttribute("error", "Invalid transaction ID. Please enter a valid UPI transaction ID.");
                return "redirect:/payment/checkout/" + bioDataId;
            }

            // Submit payment
            PaymentTransaction payment = paymentService.submitPayment(currentUser, bioDataId, transactionId.trim());

            redirectAttributes.addFlashAttribute("success", 
                "Payment submitted for verification! Transaction ID: " + payment.getTransactionReferenceId());
            redirectAttributes.addFlashAttribute("paymentId", payment.getId());

            return "redirect:/payment/status/" + payment.getId();

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/payment/checkout/" + bioDataId;
        } catch (Exception e) {
            log.error("Error submitting payment", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred. Please try again.");
            return "redirect:/payment/checkout/" + bioDataId;
        }
    }

    /**
     * Show payment status page
     */
    @GetMapping("/status/{paymentId}")
    public String paymentStatus(@PathVariable Long paymentId, Model model) {
        User currentUser = getCurrentUser();

        PaymentTransaction payment = paymentService.getPaymentById(paymentId);

        // Verify the payment belongs to current user
        if (!payment.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/dashboard";
        }

        model.addAttribute("payment", PaymentDTO.fromEntity(payment));
        model.addAttribute("bioData", payment.getBioData());

        // If approved, show download link
        if (payment.getStatus() == PaymentTransaction.PaymentStatus.APPROVED) {
            model.addAttribute("canDownload", true);
        }

        return "payment/status";
    }

    /**
     * List user's payment history
     */
    @GetMapping("/history")
    public String paymentHistory(Model model) {
        User currentUser = getCurrentUser();

        List<PaymentDTO> payments = paymentService.getUserPayments(currentUser);
        model.addAttribute("payments", payments);

        return "payment/history";
    }

    /**
     * Check payment status (AJAX endpoint)
     */
    @GetMapping("/check-status/{paymentId}")
    @ResponseBody
    public PaymentDTO checkPaymentStatus(@PathVariable Long paymentId) {
        User currentUser = getCurrentUser();

        PaymentTransaction payment = paymentService.getPaymentById(paymentId);

        // Verify ownership
        if (!payment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        return PaymentDTO.fromEntity(payment);
    }

    // Helper method
    private User getCurrentUser() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }
}
