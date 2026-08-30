package com.biodatamaker.config;

import com.biodatamaker.entity.User;
import com.biodatamaker.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * On successful Google login, mint a JWT for the resolved DB user and redirect the
 * browser back to the SPA at {@code <frontend>/oauth/callback#token=<jwt>}.
 * The token is placed in the URL fragment so it never reaches server logs / Referer headers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String target = frontendUrl + "/login?error=oauth";

        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            User user = oAuth2User.getAttribute("dbUser");
            if (user != null) {
                String token = jwtService.generateToken(user);
                target = frontendUrl + "/oauth/callback#token="
                        + URLEncoder.encode(token, StandardCharsets.UTF_8);
                log.info("OAuth2 login success for {}, redirecting to SPA", user.getEmail());
            }
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
