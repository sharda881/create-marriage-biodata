package com.biodatamaker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SystemConfig entity for storing application-wide configuration.
 * Feature flags and system settings are stored as key-value pairs.
 */
@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique configuration key
     */
    @Column(unique = true, nullable = false)
    private String configKey;

    /**
     * Configuration value (stored as String, parsed by application)
     */
    @Column(nullable = false)
    private String configValue;

    /**
     * Description of what this config does
     */
    @Column(length = 500)
    private String description;

    /**
     * Data type hint for parsing (STRING, INTEGER, BOOLEAN, DECIMAL)
     */
    @Column(nullable = false)
    @Builder.Default
    private String valueType = "STRING";

    /**
     * Whether this config is active
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Creation timestamp
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

    // ================== Predefined Config Keys ==================

    /**
     * Number of free bio-data downloads allowed per user
     */
    public static final String FREE_LIMIT_COUNT = "free_limit_count";

    /**
     * Whether the paywall is globally enabled
     */
    public static final String GLOBAL_PAYWALL_ENABLED = "global_paywall_enabled";

    /**
     * Price per bio-data download (in INR)
     */
    public static final String DOWNLOAD_PRICE = "download_price";

    /**
     * UPI ID for payments
     */
    public static final String PAYMENT_UPI_ID = "payment_upi_id";

    /**
     * Maintenance mode flag
     */
    public static final String MAINTENANCE_MODE = "maintenance_mode";

    /**
     * Maximum file upload size (in MB)
     */
    public static final String MAX_UPLOAD_SIZE_MB = "max_upload_size_mb";

    /**
     * Helper method to get value as Integer
     */
    public Integer getAsInteger() {
        try {
            return Integer.parseInt(configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Helper method to get value as Boolean
     */
    public Boolean getAsBoolean() {
        return Boolean.parseBoolean(configValue);
    }

    /**
     * Helper method to get value as Double
     */
    public Double getAsDouble() {
        try {
            return Double.parseDouble(configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
