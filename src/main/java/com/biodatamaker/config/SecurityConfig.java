package com.biodatamaker.config;

import com.biodatamaker.config.OAuth2AvailabilityConfig.OAuth2Availability;
import com.biodatamaker.service.CustomUserDetailsService;
import com.biodatamaker.service.OAuth2UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application.
 * Supports both Form Login (Email/Password) and OAuth2 (Google).
 * OAuth2 is only enabled when valid credentials are configured.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2Availability oAuth2Availability;

    public SecurityConfig(
            @Lazy CustomUserDetailsService userDetailsService,
            @Lazy OAuth2UserService oAuth2UserService,
            OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
            PasswordEncoder passwordEncoder,
            OAuth2Availability oAuth2Availability) {
        this.userDetailsService = userDetailsService;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.passwordEncoder = passwordEncoder;
        this.oAuth2Availability = oAuth2Availability;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF configuration
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                )
                // Frame options for H2 console
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/templates",
                                "/biodata/create",
                                "/biodata/save",
                                "/biodata/edit/**",
                                "/biodata/preview/**",
                                "/biodata/download/**",
                                "/biodata/*/upload-photo",
                                "/biodata/*/template",
                                "/invitation-card",
                                "/invitation-card/**",
                                "/uploads/**",
                                "/auth/**",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/h2-console/**",
                                "/favicon.ico"
                        ).permitAll()
                        // Admin endpoints
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // Form Login configuration
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .permitAll()
                )
                // Logout configuration
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // Authentication provider
                .authenticationProvider(authenticationProvider());

        // Only configure OAuth2 if valid credentials are available
        if (oAuth2Availability.googleEnabled()) {
            log.info("Configuring OAuth2 login with Google");
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(oAuth2UserService)
                    )
                    .successHandler(oAuth2SuccessHandler)
                    .failureUrl("/login?error=oauth")
            );
        } else {
            log.info("OAuth2 login is disabled - no valid credentials configured");
        }

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
