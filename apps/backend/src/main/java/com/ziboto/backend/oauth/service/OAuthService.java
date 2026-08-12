package com.ziboto.backend.oauth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ziboto.backend.auth.dto.AuthResponse;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.oauth.dto.OAuthLoginResponse;
import com.ziboto.backend.oauth.entity.OAuthAccount;
import com.ziboto.backend.oauth.enums.OAuthProvider;
import com.ziboto.backend.oauth.repository.OAuthAccountRepository;
import com.ziboto.backend.security.JwtTokenProvider;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.entity.UserRole;
import com.ziboto.backend.user.entity.UserStatus;
import com.ziboto.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for OAuth authentication and account management.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {
    
    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Handle OAuth login/registration.
     * Creates new user if doesn't exist, otherwise logs in.
     */
    @Transactional
    public OAuthLoginResponse handleOAuthLogin(
            OAuthProvider provider,
            String providerUserId,
            String email,
            String name,
            String pictureUrl,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt) {
        
        log.info("Handling OAuth login: provider={}, email={}", provider, email);
        
        // Check if OAuth account already exists
        Optional<OAuthAccount> existingOAuth = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);
        
        if (existingOAuth.isPresent()) {
            // Existing OAuth account - login
            OAuthAccount oauthAccount = existingOAuth.get();
            User user = userRepository.findById(oauthAccount.getUserId())
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
            
            // Update OAuth account
            oauthAccount.setAccessToken(accessToken);
            oauthAccount.setRefreshToken(refreshToken);
            oauthAccount.setTokenExpiresAt(tokenExpiresAt);
            oauthAccount.setLastUsedAt(LocalDateTime.now());
            oauthAccount.setEmail(email);
            oauthAccount.setName(name);
            oauthAccount.setPictureUrl(pictureUrl);
            oauthAccountRepository.save(oauthAccount);
            
            // Generate JWT
            String jwt = jwtTokenProvider.generateToken(user.getUsername(), List.of(user.getRole().name()));
            
            log.info("OAuth login successful: userId={}", user.getId());
            
            return OAuthLoginResponse.builder()
                    .accessToken(jwt)
                    .tokenType("Bearer")
                    .expiresIn(3600L) // 1 hour
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .isNewUser(false)
                    .accountLinked(false)
                    .build();
        }
        
        // Check if user exists with this email
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            // User exists - link OAuth account
            User user = existingUser.get();
            
            OAuthAccount oauthAccount = OAuthAccount.builder()
                    .userId(user.getId())
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .email(email)
                    .name(name)
                    .pictureUrl(pictureUrl)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenExpiresAt(tokenExpiresAt)
                    .lastUsedAt(LocalDateTime.now())
                    .build();
            
            oauthAccountRepository.save(oauthAccount);
            
            String jwt = jwtTokenProvider.generateToken(user.getUsername(), List.of(user.getRole().name()));
            
            log.info("OAuth account linked to existing user: userId={}", user.getId());
            
            return OAuthLoginResponse.builder()
                    .accessToken(jwt)
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .isNewUser(false)
                    .accountLinked(true)
                    .build();
        }
        
        // New user - create account
        String username = generateUsernameFromEmail(email);
        String randomPassword = generateRandomPassword(); // Not used for OAuth login
        
        User newUser = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(randomPassword))
                .firstName(extractFirstName(name))
                .lastName(extractLastName(name))
                .role(UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true) // OAuth email is already verified
                .avatarUrl(pictureUrl)
                .storageQuota(5L * 1024 * 1024 * 1024) // 5GB
                .storageUsed(0L)
                .build();
        
        newUser = userRepository.save(newUser);
        
        // Create OAuth account
        OAuthAccount oauthAccount = OAuthAccount.builder()
                .userId(newUser.getId())
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .name(name)
                .pictureUrl(pictureUrl)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresAt(tokenExpiresAt)
                .lastUsedAt(LocalDateTime.now())
                .build();
        
        oauthAccountRepository.save(oauthAccount);
        
        String jwt = jwtTokenProvider.generateToken(newUser.getUsername(), List.of(newUser.getRole().name()));
        
        log.info("New user created via OAuth: userId={}, provider={}", newUser.getId(), provider);
        
        return OAuthLoginResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(newUser.getId())
                .email(newUser.getEmail())
                .username(newUser.getUsername())
                .isNewUser(true)
                .accountLinked(true)
                .build();
    }
    
    /**
     * Link OAuth account to existing user (when already logged in).
     */
    @Transactional
    public void linkOAuthAccount(
            Long userId,
            OAuthProvider provider,
            String providerUserId,
            String email,
            String name,
            String pictureUrl,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt) {
        
        log.info("Linking OAuth account: userId={}, provider={}", userId, provider);
        
        // Check if already linked
        if (oauthAccountRepository.existsByUserIdAndProvider(userId, provider)) {
            throw new BaseException(ErrorCode.CONFLICT, "OAuth account already linked");
        }
        
        // Check if provider account is already linked to another user
        if (oauthAccountRepository.existsByProviderAndProviderUserId(provider, providerUserId)) {
            throw new BaseException(ErrorCode.CONFLICT, "This OAuth account is already linked to another user");
        }
        
        OAuthAccount oauthAccount = OAuthAccount.builder()
                .userId(userId)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .name(name)
                .pictureUrl(pictureUrl)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresAt(tokenExpiresAt)
                .lastUsedAt(LocalDateTime.now())
                .build();
        
        oauthAccountRepository.save(oauthAccount);
        
        log.info("OAuth account linked successfully: userId={}, provider={}", userId, provider);
    }
    
    /**
     * Unlink OAuth account from user.
     */
    @Transactional
    public void unlinkOAuthAccount(Long userId, OAuthProvider provider) {
        log.info("Unlinking OAuth account: userId={}, provider={}", userId, provider);
        
        OAuthAccount oauthAccount = oauthAccountRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "OAuth account not found"));
        
        oauthAccountRepository.delete(oauthAccount);
        
        log.info("OAuth account unlinked: userId={}, provider={}", userId, provider);
    }
    
    /**
     * Get user's linked OAuth accounts.
     */
    @Transactional(readOnly = true)
    public List<String> getLinkedProviders(Long userId) {
        return oauthAccountRepository.findActiveByUserId(userId)
                .stream()
                .map(oa -> oa.getProvider().name())
                .collect(Collectors.toList());
    }
    
    /**
     * Check if user has specific OAuth provider linked.
     */
    @Transactional(readOnly = true)
    public boolean hasLinkedProvider(Long userId, OAuthProvider provider) {
        return oauthAccountRepository.existsByUserIdAndProvider(userId, provider);
    }
    
    // Helper methods
    
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }
    
    private String generateRandomPassword() {
        return java.util.UUID.randomUUID().toString();
    }
    
    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }
        String[] parts = fullName.split(" ");
        return parts[0];
    }
    
    private String extractLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }
        String[] parts = fullName.split(" ");
        if (parts.length > 1) {
            return String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        }
        return null;
    }
}
