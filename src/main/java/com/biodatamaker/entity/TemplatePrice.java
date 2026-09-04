package com.biodatamaker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin-set price for a single bio-data template, keyed by the template id
 * ({@code modern}, {@code royal}, …). A template with no row falls back to a
 * computed default (free templates → 0, premium → {@code download_price}).
 */
@Entity
@Table(name = "template_price")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplatePrice {

    @Id
    @Column(name = "template_id", length = 64)
    private String templateId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
