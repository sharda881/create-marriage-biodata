package com.biodatamaker.dto;

import com.biodatamaker.entity.PaymentTransaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Payment related operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {

    private Long id;

    @NotNull(message = "BioData ID is required")
    private Long bioDataId;

    @NotBlank(message = "Transaction Reference ID is required")
    @Pattern(regexp = "^[A-Za-z0-9]{12,35}$", message = "Invalid UPI Transaction ID format")
    private String transactionReferenceId;

    private BigDecimal amount;

    private PaymentTransaction.PaymentStatus status;

    private String paymentMethod;

    private String adminNotes;

    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;

    // Additional display fields
    private String userName;
    private String userEmail;
    private String bioDataFullName;

    /**
     * Create DTO from Entity
     */
    public static PaymentDTO fromEntity(PaymentTransaction entity) {
        PaymentDTO dto = PaymentDTO.builder()
                .id(entity.getId())
                .bioDataId(entity.getBioData().getId())
                .transactionReferenceId(entity.getTransactionReferenceId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .paymentMethod(entity.getPaymentMethod())
                .adminNotes(entity.getAdminNotes())
                .createdAt(entity.getCreatedAt())
                .verifiedAt(entity.getVerifiedAt())
                .build();

        if (entity.getUser() != null) {
            dto.setUserName(entity.getUser().getName());
            dto.setUserEmail(entity.getUser().getEmail());
        }

        if (entity.getBioData() != null) {
            dto.setBioDataFullName(entity.getBioData().getFullName());
        }

        return dto;
    }
}
