package com.biodatamaker.service;

import com.biodatamaker.entity.SystemConfig;
import com.biodatamaker.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing system configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final SystemConfigRepository configRepository;

    // Default configuration values
    private static final int DEFAULT_FREE_LIMIT = 2;
    private static final boolean DEFAULT_PAYWALL_ENABLED = true;
    private static final String DEFAULT_DOWNLOAD_PRICE = "99";

    /**
     * Get the free bio-data download limit
     */
    public int getFreeLimitCount() {
        return configRepository.findByConfigKey(SystemConfig.FREE_LIMIT_COUNT)
                .map(config -> {
                    Integer value = config.getAsInteger();
                    return value != null ? value : DEFAULT_FREE_LIMIT;
                })
                .orElse(DEFAULT_FREE_LIMIT);
    }

    /**
     * Check if global paywall is enabled
     */
    public boolean isPaywallEnabled() {
        return configRepository.findByConfigKey(SystemConfig.GLOBAL_PAYWALL_ENABLED)
                .map(SystemConfig::getAsBoolean)
                .orElse(DEFAULT_PAYWALL_ENABLED);
    }

    /**
     * Get the download price
     */
    public String getDownloadPrice() {
        return configRepository.findByConfigKey(SystemConfig.DOWNLOAD_PRICE)
                .map(SystemConfig::getConfigValue)
                .orElse(DEFAULT_DOWNLOAD_PRICE);
    }

    /**
     * Get UPI ID for payments
     */
    public String getPaymentUpiId() {
        return configRepository.findByConfigKey(SystemConfig.PAYMENT_UPI_ID)
                .map(SystemConfig::getConfigValue)
                .orElse("your-upi-id@bank");
    }

    /**
     * Check if maintenance mode is enabled
     */
    public boolean isMaintenanceMode() {
        return configRepository.findByConfigKey(SystemConfig.MAINTENANCE_MODE)
                .map(SystemConfig::getAsBoolean)
                .orElse(false);
    }

    /**
     * Get configuration by key
     */
    public Optional<SystemConfig> getConfig(String key) {
        return configRepository.findByConfigKey(key);
    }

    /**
     * Get all active configurations
     */
    public List<SystemConfig> getAllActiveConfigs() {
        return configRepository.findByIsActiveTrue();
    }

    /**
     * Create or update a configuration
     */
    @Transactional
    public SystemConfig saveConfig(String key, String value, String description, String valueType) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElse(SystemConfig.builder()
                        .configKey(key)
                        .build());

        config.setConfigValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        if (valueType != null) {
            config.setValueType(valueType);
        }

        return configRepository.save(config);
    }

    /**
     * Initialize default configurations
     */
    @Transactional
    public void initializeDefaultConfigs() {
        createConfigIfNotExists(SystemConfig.FREE_LIMIT_COUNT, String.valueOf(DEFAULT_FREE_LIMIT),
                "Number of free bio-data downloads allowed per user", "INTEGER");

        createConfigIfNotExists(SystemConfig.GLOBAL_PAYWALL_ENABLED, String.valueOf(DEFAULT_PAYWALL_ENABLED),
                "Whether the paywall is globally enabled", "BOOLEAN");

        createConfigIfNotExists(SystemConfig.DOWNLOAD_PRICE, DEFAULT_DOWNLOAD_PRICE,
                "Price per bio-data download in INR", "STRING");

        createConfigIfNotExists(SystemConfig.PAYMENT_UPI_ID, "your-upi-id@bank",
                "UPI ID for receiving payments", "STRING");

        createConfigIfNotExists(SystemConfig.MAINTENANCE_MODE, "false",
                "Whether the application is in maintenance mode", "BOOLEAN");

        createConfigIfNotExists(SystemConfig.MAX_UPLOAD_SIZE_MB, "5",
                "Maximum file upload size in MB", "INTEGER");

        log.info("System configurations initialized");
    }

    private void createConfigIfNotExists(String key, String value, String description, String valueType) {
        if (!configRepository.existsByConfigKey(key)) {
            SystemConfig config = SystemConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .description(description)
                    .valueType(valueType)
                    .isActive(true)
                    .build();
            configRepository.save(config);
            log.debug("Created config: {} = {}", key, value);
        }
    }
}
