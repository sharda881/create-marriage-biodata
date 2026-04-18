package com.biodatamaker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to determine OAuth2 availability.
 * Makes OAuth2 features conditional based on whether valid credentials are provided.
 */
@Configuration
@Slf4j
public class OAuth2AvailabilityConfig {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Bean
    public OAuth2Availability oAuth2Availability() {
        boolean googleEnabled = isGoogleOAuth2Configured();
        
        if (googleEnabled) {
            log.info("Google OAuth2 is ENABLED - valid credentials detected");
        } else {
            log.info("Google OAuth2 is DISABLED - no valid credentials configured. " +
                    "Users can still login with email/password.");
        }
        
        return new OAuth2Availability(googleEnabled);
    }

    private boolean isGoogleOAuth2Configured() {
        return googleClientId != null
                && !googleClientId.isBlank()
                && !googleClientId.equals("your-google-client-id")
                && googleClientSecret != null
                && !googleClientSecret.isBlank()
                && !googleClientSecret.equals("your-google-client-secret");
    }

    /**
     * Simple record to hold OAuth2 availability status.
     */
    public record OAuth2Availability(boolean googleEnabled) {
        public boolean isAnyOAuth2Enabled() {
            return googleEnabled;
        }
    }
}
