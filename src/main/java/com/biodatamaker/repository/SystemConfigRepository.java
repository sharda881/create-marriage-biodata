package com.biodatamaker.repository;

import com.biodatamaker.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SystemConfig entity operations.
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * Find config by key
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * Check if config key exists
     */
    boolean existsByConfigKey(String configKey);

    /**
     * Find all active configs
     */
    List<SystemConfig> findByIsActiveTrue();

    /**
     * Find all configs by value type
     */
    List<SystemConfig> findByValueType(String valueType);

    /**
     * Delete config by key
     */
    void deleteByConfigKey(String configKey);
}
