package com.biodatamaker.config;

import com.biodatamaker.config.OAuth2AvailabilityConfig.OAuth2Availability;
import com.biodatamaker.security.JwtAuthenticationFilter;
import com.biodatamaker.service.CustomUserDetailsService;
import com.biodatamaker.service.OAuth2UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless security configuration for the REST API.
 * Authentication is via JWT bearer tokens (see {@link JwtAuthenticationFilter}).
 * Google OAuth2 login is still handled server-side and hands a freshly minted JWT
 * back to the SPA (see {@code OAuth2AuthenticationSuccessHandler}).
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
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public SecurityConfig(
            @Lazy CustomUserDetailsService userDetailsService,
            @Lazy OAuth2UserService oAuth2UserService,
            OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
            PasswordEncoder passwordEncoder,
            OAuth2Availability oAuth2Availability,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.passwordEncoder = passwordEncoder;
        this.oAuth2Availability = oAuth2Availability;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, e) ->
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        // Public API endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/templates/**",
                                "/api/invitation-card/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/uploads/**",
                                "/images/**",
                                "/favicon.svg",
                                "/error",
                                "/h2-console/**"
                        ).permitAll()
                        // Anonymous bio-data flow (create / edit / preview / download without login)
                        .requestMatchers(HttpMethod.POST, "/api/biodata").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/biodata/*").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/biodata/*").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/biodata/*/photo",
                                "/api/biodata/*/complete").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/biodata/*/template").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/biodata/*/preview-data",
                                "/api/biodata/*/needs-payment",
                                "/api/biodata/*/download").permitAll()
                        // Payment flow (anonymous checkout + Razorpay webhook)
                        .requestMatchers(HttpMethod.GET,
                                "/api/payments/quote/*",
                                "/api/payments/status/*").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/payments/checkout",
                                "/api/payments/webhook").permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Everything else needs a valid token
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        if (oAuth2Availability.googleEnabled()) {
            log.info("Configuring OAuth2 login with Google");
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler((request, response, exception) ->
                            response.sendRedirect(frontendUrl + "/login?error=oauth"))
            );
        } else {
            log.info("OAuth2 login is disabled - no valid credentials configured");
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // The configured SPA origin, any localhost port for local dev, and any
        // *.vercel.app deployment (production + preview builds).
        config.setAllowedOriginPatterns(List.of(
                frontendUrl,
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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
