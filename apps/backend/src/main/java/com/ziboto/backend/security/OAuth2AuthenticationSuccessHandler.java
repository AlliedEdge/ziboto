package com.ziboto.backend.security;

import tools.jackson.databind.ObjectMapper;
import com.ziboto.backend.auth.dto.AuthenticationResponse;
import com.ziboto.backend.security.JwtTokenProvider;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.entity.UserRole;
import com.ziboto.backend.user.entity.UserStatus;
import com.ziboto.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 Authentication Success Handler.
 * 
 * <p>Handles successful OAuth2 authentication by:</p>
 * <ul>
 *   <li>Finding or creating user in database</li>
 *   <li>Generating JWT tokens</li>
 *   <li>Redirecting to frontend with tokens</li>
 * </ul>
 * 
 * <p>This handler integrates OAuth2 authentication with the existing JWT-based system,
 * ensuring OAuth2 users receive the same JWT tokens as traditional email/password users.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    
    @Value("${app.oauth.redirect-url:http://localhost:5173/oauth/callback}")
    private String frontendRedirectUrl;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            log.error("Authentication is not OAuth2AuthenticationToken");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication type");
            return;
        }
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        
        log.info("OAuth2 authentication successful for provider: {}", registrationId);
        
        try {
            // Process Google OAuth2 user
            if ("google".equals(registrationId)) {
                User user = processGoogleUser(oAuth2User);
                
                // Generate JWT tokens
                String accessToken = jwtTokenProvider.generateToken(user.getUsername(), 
                    List.of(user.getRole().name()));
                String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
                
                log.info("JWT tokens generated for OAuth2 user: {}", user.getUsername());
                
                // Redirect to frontend with tokens
                String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                        .queryParam("accessToken", accessToken)
                        .queryParam("refreshToken", refreshToken)
                        .build()
                        .toUriString();
                
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            } else {
                log.error("Unsupported OAuth2 provider: {}", registrationId);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported OAuth2 provider");
            }
        } catch (Exception e) {
            log.error("Error processing OAuth2 authentication", e);
            
            // Redirect to frontend with error
            String errorRedirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .queryParam("error", "oauth_failed")
                    .queryParam("message", e.getMessage())
                    .build()
                    .toUriString();
            
            getRedirectStrategy().sendRedirect(request, response, errorRedirectUrl);
        }
    }
    
    /**
     * Process Google OAuth2 user.
     * Finds existing user by Google ID or email, or creates new user.
     * 
     * @param oAuth2User OAuth2 user principal
     * @return application User entity
     */
    private User processGoogleUser(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        String googleId = (String) attributes.get("sub"); // Google's unique user ID
        String email = (String) attributes.get("email");
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");
        String picture = (String) attributes.get("picture");
        Boolean emailVerified = (Boolean) attributes.get("email_verified");
        
        log.debug("Processing Google user - googleId: {}, email: {}", googleId, email);
        
        // Try to find existing user by Google ID
        User user = userRepository.findByGoogleId(googleId)
                .orElse(null);
        
        if (user != null) {
            log.info("Found existing user by Google ID: {}", user.getUsername());
            
            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            
            // Update profile picture if changed
            if (picture != null && !picture.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(picture);
            }
            
            return userRepository.save(user);
        }
        
        // Try to find existing user by email (link Google to existing account)
        user = userRepository.findByEmail(email)
                .orElse(null);
        
        if (user != null) {
            log.info("Linking Google account to existing user: {}", user.getUsername());
            
            // Link Google ID to existing account
            user.setGoogleId(googleId);
            user.setOauthProvider("google");
            user.setEmailVerified(emailVerified != null ? emailVerified : true);
            user.setLastLoginAt(LocalDateTime.now());
            
            // Update profile picture if not set
            if (picture != null && user.getAvatarUrl() == null) {
                user.setAvatarUrl(picture);
            }
            
            return userRepository.save(user);
        }
        
        // Create new user
        log.info("Creating new user from Google account: {}", email);
        
        // Generate unique username from email
        String username = generateUniqueUsername(email);
        
        User newUser = User.builder()
                .username(username)
                .email(email)
                .googleId(googleId)
                .oauthProvider("google")
                .firstName(givenName)
                .lastName(familyName)
                .avatarUrl(picture)
                .emailVerified(emailVerified != null ? emailVerified : true)
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE) // OAuth users are active immediately
                .storageQuota(10737418240L) // 10GB default
                .storageUsed(0L)
                .lastLoginAt(LocalDateTime.now())
                .build();
        
        return userRepository.save(newUser);
    }
    
    /**
     * Generate unique username from email.
     * If username exists, append numbers until unique.
     * 
     * @param email user email
     * @return unique username
     */
    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
        String username = baseUsername;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }
}
