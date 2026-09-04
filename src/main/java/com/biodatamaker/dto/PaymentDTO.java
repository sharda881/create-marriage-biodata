package com.biodatamaker.dto;

import com.biodatamaker.entity.BioData;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flat payment view of a bio-data row (payment data now lives on {@code bio_data}).
 * Consumed by the admin payments screen and the user-facing status endpoint.
 */
public record PaymentDTO(
        Long bioDataId,
        String bioDataFullName,
        String templateId,
        String payerName,
        String payerEmail,
        String payerPhone,
        String userEmail,
        BigDecimal amount,
        BioData.PaymentStatus status,
        String razorpayOrderId,
        String razorpayPaymentId,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        boolean deliverByEmail,
        LocalDateTime pdfEmailedAt
) {
    public static PaymentDTO fromEntity(BioData b) {
        return new PaymentDTO(
                b.getId(),
                b.getFullName(),
                b.getSelectedTemplateId(),
                b.getPayerName(),
                b.getPayerEmail(),
                b.getPayerPhone(),
                b.getUser() != null ? b.getUser().getEmail() : null,
                b.getPaymentAmount(),
                b.getPaymentStatus(),
                b.getRazorpayOrderId(),
                b.getRazorpayPaymentId(),
                b.getPaidAt(),
                b.getCreatedAt(),
                Boolean.TRUE.equals(b.getDeliverByEmail()),
                b.getPdfEmailedAt()
        );
    }
}
