package com.biodatamaker.service;

import com.biodatamaker.dto.PaymentDTO;
import com.biodatamaker.entity.BioData;
import com.biodatamaker.entity.PaymentTransaction;
import com.biodatamaker.entity.User;
import com.biodatamaker.exception.ResourceNotFoundException;
import com.biodatamaker.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for payment transaction management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final BioDataService bioDataService;
    private final SystemConfigService configService;

    /**
     * Submit a payment for verification
     */
    @Transactional
    public PaymentTransaction submitPayment(User user, Long bioDataId, String transactionReferenceId) {
        BioData bioData = bioDataService.getBioDataForUser(bioDataId, user);

        // Check if transaction ID already exists
        if (paymentRepository.existsByTransactionReferenceId(transactionReferenceId)) {
            throw new IllegalArgumentException("Transaction ID has already been submitted");
        }

        // Check if there's already a pending or approved payment for this bio-data
        Optional<PaymentTransaction> existingPayment = paymentRepository.findByBioData(bioData);
        if (existingPayment.isPresent()) {
            PaymentTransaction existing = existingPayment.get();
            if (existing.getStatus() == PaymentTransaction.PaymentStatus.APPROVED) {
                throw new IllegalArgumentException("Payment already approved for this bio-data");
            }
            if (existing.getStatus() == PaymentTransaction.PaymentStatus.PENDING) {
                throw new IllegalArgumentException("A payment is already pending for this bio-data");
            }
        }

        // Get price from config
        String priceStr = configService.getDownloadPrice();
        BigDecimal amount = new BigDecimal(priceStr);

        PaymentTransaction payment = PaymentTransaction.builder()
                .user(user)
                .bioData(bioData)
                .transactionReferenceId(transactionReferenceId.trim().toUpperCase())
                .amount(amount)
                .status(PaymentTransaction.PaymentStatus.PENDING)
                .paymentMethod("UPI")
                .build();

        PaymentTransaction saved = paymentRepository.save(payment);
        log.info("Payment submitted for bio-data {} by user {}: {}", bioDataId, user.getId(), transactionReferenceId);
        return saved;
    }

    /**
     * Approve a payment (admin action)
     */
    @Transactional
    public PaymentTransaction approvePayment(Long paymentId, User admin, String notes) {
        PaymentTransaction payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentTransaction.PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Payment is not in pending status");
        }

        payment.setStatus(PaymentTransaction.PaymentStatus.APPROVED);
        payment.setVerifiedBy(admin);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setAdminNotes(notes);

        // Mark the bio-data as paid
        bioDataService.markAsPaid(payment.getBioData().getId());

        PaymentTransaction saved = paymentRepository.save(payment);
        log.info("Payment {} approved by admin {}", paymentId, admin.getId());
        return saved;
    }

    /**
     * Reject a payment (admin action)
     */
    @Transactional
    public PaymentTransaction rejectPayment(Long paymentId, User admin, String reason) {
        PaymentTransaction payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentTransaction.PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Payment is not in pending status");
        }

        payment.setStatus(PaymentTransaction.PaymentStatus.REJECTED);
        payment.setVerifiedBy(admin);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setAdminNotes(reason);

        PaymentTransaction saved = paymentRepository.save(payment);
        log.info("Payment {} rejected by admin {}: {}", paymentId, admin.getId(), reason);
        return saved;
    }

    /**
     * Get payment by ID
     */
    public PaymentTransaction getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    }

    /**
     * Get all pending payments (for admin)
     */
    public List<PaymentDTO> getPendingPayments() {
        return paymentRepository.findPendingPaymentsWithDetails(PaymentTransaction.PaymentStatus.PENDING)
                .stream()
                .map(PaymentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments by status
     */
    public List<PaymentDTO> getPaymentsByStatus(PaymentTransaction.PaymentStatus status) {
        return paymentRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(PaymentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get payments for a user
     */
    public List<PaymentDTO> getUserPayments(User user) {
        return paymentRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(PaymentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get payment for a bio-data
     */
    public Optional<PaymentTransaction> getPaymentForBioData(Long bioDataId) {
        return paymentRepository.findByBioDataId(bioDataId);
    }

    /**
     * Check if bio-data has approved payment
     */
    public boolean hasApprovedPayment(Long bioDataId) {
        BioData bioData = bioDataService.getBioDataById(bioDataId);
        return paymentRepository.existsByBioDataAndStatus(bioData, PaymentTransaction.PaymentStatus.APPROVED);
    }

    /**
     * Count pending payments
     */
    public long countPendingPayments() {
        return paymentRepository.countByStatus(PaymentTransaction.PaymentStatus.PENDING);
    }

    /**
     * Get total approved payments amount
     */
    public BigDecimal getTotalApprovedAmount() {
        return paymentRepository.getTotalAmountByStatus(PaymentTransaction.PaymentStatus.APPROVED);
    }

    /**
     * Get download price
     */
    public String getDownloadPrice() {
        return configService.getDownloadPrice();
    }

    /**
     * Get UPI ID
     */
    public String getUpiId() {
        return configService.getPaymentUpiId();
    }
}
