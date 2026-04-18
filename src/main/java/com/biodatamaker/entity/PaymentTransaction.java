package com.biodatamaker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentTransaction entity for tracking payment submissions and approvals.
 * Uses manual QR code based payment verification.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biodata_id", nullable = false)
    private BioData bioData;

    /**
     * UPI Transaction Reference ID provided by user after payment
     */
    @Column(nullable = false)
    private String transactionReferenceId;

    /**
     * Amount paid (in INR)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * Payment status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Payment method used
     */
    @Column(nullable = false)
    @Builder.Default
    private String paymentMethod = "UPI";

    /**
     * Additional notes from admin during verification
     */
    @Column(length = 500)
    private String adminNotes;

    /**
     * Admin who approved/rejected the payment
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    /**
     * Timestamp when payment was verified
     */
    private LocalDateTime verifiedAt;

    /**
     * User's screenshot or proof reference (optional)
     */
    private String paymentProofPath;

    /**
     * Transaction creation timestamp
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Last update timestamp
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Payment status enumeration
     */
    public enum PaymentStatus {
        PENDING,    // Awaiting admin verification
        APPROVED,   // Payment verified and approved
        REJECTED    // Payment rejected (invalid transaction ID, etc.)
    }

    /**
     * Check if payment is successful
     */
    public boolean isSuccessful() {
        return status == PaymentStatus.APPROVED;
    }

    /**
     * Check if payment is still pending
     */
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
}
