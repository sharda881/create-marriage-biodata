package com.biodatamaker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for OAuth2.
 * Used to determine if OAuth2 should be enabled based on credential availability.
 */
@Configuration
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2Properties {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if Google OAuth2 credentials are properly configured.
     */
    public static boolean hasValidGoogleCredentials(String clientId, String clientSecret) {
        return clientId != null 
                && !clientId.isBlank() 
                && !clientId.equals("your-google-client-id")
                && clientSecret != null 
                && !clientSecret.isBlank() 
                && !clientSecret.equals("your-google-client-secret");
    }
}
