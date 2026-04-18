package com.biodatamaker.config;

import com.biodatamaker.entity.User;
import com.biodatamaker.repository.UserRepository;
import com.biodatamaker.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data initializer for development and testing.
 * Creates default admin user and initializes system configuration.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService configService;

    @Override
    public void run(String... args) {
        log.info("Initializing application data...");

        // Initialize system configuration
        configService.initializeDefaultConfigs();

        // Create default admin user if not exists
        createAdminUserIfNotExists();

        // Create demo user if not exists
        createDemoUserIfNotExists();

        log.info("Data initialization complete!");
    }

    private void createAdminUserIfNotExists() {
        String adminEmail = "admin@biodatamaker.app";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .name("Admin User")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@123"))
                    .provider("local")
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .emailVerified(true)
                    .build();

            userRepository.save(admin);
            log.info("Created admin user: {}", adminEmail);
        } else {
            log.info("Admin user already exists: {}", adminEmail);
        }
    }

    private void createDemoUserIfNotExists() {
        String demoEmail = "demo@biodatamaker.app";

        if (!userRepository.existsByEmail(demoEmail)) {
            User demoUser = User.builder()
                    .name("Demo User")
                    .email(demoEmail)
                    .password(passwordEncoder.encode("Demo@123"))
                    .provider("local")
                    .role(User.Role.USER)
                    .enabled(true)
                    .emailVerified(true)
                    .build();

            userRepository.save(demoUser);
            log.info("Created demo user: {}", demoEmail);
        } else {
            log.info("Demo user already exists: {}", demoEmail);
        }
    }
}
