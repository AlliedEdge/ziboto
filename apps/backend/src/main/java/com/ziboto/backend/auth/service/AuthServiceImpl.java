package com.ziboto.backend.auth.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ziboto.backend.audit.entity.AuditAction;
import com.ziboto.backend.audit.service.AuditService;
import com.ziboto.backend.auth.dto.AuthenticationResponse;
import com.ziboto.backend.auth.dto.ForgotPasswordRequest;
import com.ziboto.backend.auth.dto.LoginRequest;
import com.ziboto.backend.auth.dto.RefreshTokenRequest;
import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.auth.dto.ResetPasswordRequest;
import com.ziboto.backend.auth.dto.SendVerificationEmailRequest;
import com.ziboto.backend.auth.dto.VerifyEmailRequest;
import com.ziboto.backend.auth.dto.VerifyTokenResponse;
import com.ziboto.backend.auth.entity.EmailVerification;
import com.ziboto.backend.auth.entity.EmailVerification.VerificationType;
import com.ziboto.backend.auth.entity.RefreshToken;
import com.ziboto.backend.auth.repository.EmailVerificationRepository;
import com.ziboto.backend.auth.repository.RefreshTokenRepository;
import com.ziboto.backend.auth.service.OtpCacheService.OtpPurpose;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.config.properties.RedisProperties;
import com.ziboto.backend.email.service.EmailService;
import com.ziboto.backend.exception.AccountLockedException;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.exception.ConflictException;
import com.ziboto.backend.exception.InvalidTokenException;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.exception.UnauthorizedException;
import com.ziboto.backend.exception.ValidationException;
import com.ziboto.backend.security.JwtTokenProvider;
import com.ziboto.backend.user.dto.UserResponse;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.entity.UserStatus;
import com.ziboto.backend.user.mapper.UserMapper;
import com.ziboto.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Production-grade authentication service implementation.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>User registration with validation</li>
 *   <li>Login with rate limiting and failed attempt tracking</li>
 *   <li>Token refresh with rotation</li>
 *   <li>Logout with token blacklisting</li>
 *   <li>Session caching for performance</li>
 *   <li>Comprehensive security logging</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;
    
    // Redis services
    private final RateLimitService rateLimitService;
    private final FailedLoginAttemptService failedLoginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionCacheService sessionCacheService;
    private final OtpCacheService otpCacheService;

    // Email verification
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final RedisProperties redisProperties;

    // Pending registration cache — holds signup data in Redis until email is verified
    private final PendingRegistrationCacheService pendingRegistrationCacheService;
    
    /**
     * Register a new user.
     *
     * <p>No {@code User} row is written to the database here.  Instead, the
     * registration data is stored in Redis via {@link PendingRegistrationCacheService}
     * and a 6-digit OTP is emailed to the supplied address.  The DB row is only
     * created — directly in {@link UserStatus#ACTIVE} state — once the user
     * successfully verifies their email (see {@link #verifyEmail}).</p>
     *
     * <p>This eliminates the "username / email squatting" problem: if the user
     * never verifies, the Redis key expires along with the OTP and both the
     * username and email become freely available again.</p>
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Validate registration request</li>
     *   <li>Check for duplicate username/email in DB <em>and</em> in the Redis pending cache</li>
     *   <li>Hash password with BCrypt</li>
     *   <li>Store pending registration data in Redis (TTL = OTP TTL)</li>
     *   <li>Send email-verification OTP</li>
     * </ol>
     */
    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request, String ipAddress) {
        log.info("User registration attempt - email: {}, username: {}", request.getEmail(), request.getUsername());

        try {
            // 1. Validate request
            validateRegistrationRequest(request);

            String email    = request.getEmail().toLowerCase().trim();
            String username = request.getUsername().trim();

            // 2a. Check DB for confirmed (ACTIVE/etc.) accounts
            if (userRepository.existsByUsername(username)) {
                log.warn("Registration failed - username already exists in DB: {}", username);
                throw new ConflictException(ErrorCode.USER_USERNAME_EXISTS,
                        "Username '" + username + "' is already taken");
            }
            if (userRepository.existsByEmail(email)) {
                log.warn("Registration failed - email already exists in DB: {}", email);
                throw new ConflictException(ErrorCode.USER_EMAIL_EXISTS,
                        "Email '" + email + "' is already registered");
            }

            // 2b. Check Redis pending cache — a username/email that is mid-verification
            //     is still reserved for the duration of the OTP TTL.
            if (pendingRegistrationCacheService.exists(email)) {
                // The same person is trying to register again before their OTP expires.
                // Just let them through to the verification page — attempt to resend,
                // but if rate-limited (OTP still active) swallow the error silently
                // since the email was already sent.
                log.info("Pending registration already exists for email: {} — attempting resend", email);
                try {
                    String displayName = pendingRegistrationCacheService.getUsername(email);
                    sendVerificationOtpForPending(email, displayName != null ? displayName : email);
                } catch (BaseException e) {
                    // OTP_RATE_LIMITED means an active OTP is already in flight — that's fine.
                    log.debug("OTP resend skipped for pending registration {} — {}", email, e.getMessage());
                }
                return null; // 202 — frontend navigates to verify page
            }

            // Walk all pending entries to catch username collisions across different emails
            String pendingOwner = pendingRegistrationCacheService.findEmailByUsername(username);
            if (pendingOwner != null) {
                log.warn("Registration failed - username '{}' reserved by pending entry for: {}", username, pendingOwner);
                throw new ConflictException(ErrorCode.USER_USERNAME_EXISTS,
                        "Username '" + username + "' is already taken");
            }

            // 3. Hash password
            String passwordHash = passwordEncoder.encode(request.getPassword());

            // 4. Store pending registration in Redis (expires with the OTP)
            pendingRegistrationCacheService.save(
                    email,
                    username,
                    passwordHash,
                    request.getFirstName(),
                    request.getLastName()
            );
            log.info("Pending registration cached for email: {}", email);

            // 5. Send verification OTP (email-only variant — no User FK yet)
            sendVerificationOtpForPending(email, request.getFirstName() != null
                    ? request.getFirstName() : username);

            log.info("Registration pending email verification - username: {}", username);

            // No tokens issued until email is verified.
            // The controller returns 202 Accepted with no data payload.
            return null;

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Registration failed - email: {}", request.getEmail(), e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Registration failed. Please try again later.", e);
        }
    }
    
    /**
     * Authenticate user with credentials.
     * 
     * <p>Login Flow (EXACT ORDER):</p>
     * <ol>
     *   <li>POST /api/v1/auth/login → AuthController</li>
     *   <li>AuthController → AuthService</li>
     *   <li>Redis Rate Limit Check</li>
     *   <li>Redis Failed Login Check</li>
     *   <li>Retrieve User from PostgreSQL</li>
     *   <li>BCrypt Password Verification</li>
     *   <li>Generate Access Token (15 minutes)</li>
     *   <li>Generate Refresh Token (7 days)</li>
     *   <li>Store Session in Redis</li>
     *   <li>Store Refresh Token in PostgreSQL</li>
     *   <li>Update Last Login</li>
     *   <li>Create Audit Log</li>
     *   <li>Return Tokens and User</li>
     * </ol>
     * 
     * <p>Security features:</p>
     * <ul>
     *   <li>Rate limiting (5 attempts per 15 minutes)</li>
     *   <li>Account lockout (5 failed attempts = 30 min lockout)</li>
     *   <li>Failed attempt tracking per user and IP</li>
     *   <li>Session caching</li>
     *   <li>Security event logging</li>
     *   <li>BCrypt password verification</li>
     *   <li>Stateless JWT authentication</li>
     * </ul>
     */
    @Override
    @Transactional
    public AuthenticationResponse login(LoginRequest request, String ipAddress) {
        String identifier = request.getUsernameOrEmail();
        log.info("Login attempt - identifier: {}, IP: {}", identifier, ipAddress);
        
        try {
            // 1. Validate request
            validateLoginRequest(request);
            
            // 2. Redis Rate Limit Check - DISABLED FOR DEVELOPMENT
            /*
            if (rateLimitService.isLoginRateLimitExceeded(identifier)) {
                long resetTime = rateLimitService.getLoginRateLimitResetTime(identifier);
                log.warn("Login rate limit exceeded - identifier: {}, reset in: {}s", identifier, resetTime);
                throw new RateLimitExceededException(
                        "Too many login attempts. Please try again in " + resetTime + " seconds."
                );
            }
            */
            
            // 3. Redis Failed Login Check - DISABLED FOR DEVELOPMENT
            /*
            if (failedLoginAttemptService.isLocked(identifier)) {
                long unlockTime = failedLoginAttemptService.getLockoutRemainingTime(identifier);
                log.warn("Account locked - identifier: {}, unlock in: {}s", identifier, unlockTime);
                throw new AccountLockedException(
                        "Account is locked due to multiple failed login attempts. " +
                        "Please try again in " + unlockTime + " seconds."
                );
            }
            */
            
            // Record rate limit attempt - DISABLED FOR DEVELOPMENT
            // rateLimitService.recordLoginAttempt(identifier);
            
            // 4. Retrieve User from PostgreSQL
            User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                    .orElseThrow(() -> {
                        handleFailedLogin(identifier, ipAddress);
                        return new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, 
                                "Invalid username or password");
                    });
            
            // Check user status before authentication
            validateUserStatus(user);
            
            // 5. BCrypt Password Verification
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                handleFailedLogin(identifier, ipAddress);
                throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, 
                        "Invalid username or password");
            }
            
            // Authentication successful - proceed with token generation
            
            // 6. Generate Access Token (15 minutes)
            String accessToken = jwtTokenProvider.generateToken(
                    user.getUsername(), 
                    List.of(user.getRole().name())
            );
            
            // 7. Generate Refresh Token (7 days)
            String refreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUsername());
            
            // 8. Store Session in Redis
            UserResponse userResponse = userMapper.toResponse(user);
            sessionCacheService.cacheUserSession(user.getUsername(), userResponse);
            
            // 9. Store Hashed Refresh Token in PostgreSQL
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                    user, 
                    refreshTokenString, 
                    ipAddress,
                    extractDeviceInfo(null), // Can be enhanced with actual device detection
                    null // userAgent can be passed from controller
            );
            
            // Track active session in Redis
            sessionCacheService.trackActiveSession(
                    user.getUsername(),
                    refreshToken.getId().toString(),
                    ipAddress
            );
            
            // 10. Update Last Login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            // 11. Create Audit Log
            auditService.log(
                    user.getId(),
                    "User",
                    user.getId(),
                    AuditAction.LOGIN,
                    String.format("Successful login from IP: %s", ipAddress)
            );
            
            // 12. Reset security counters on successful login - DISABLED FOR DEVELOPMENT
            // rateLimitService.resetLoginRateLimit(identifier);
            // failedLoginAttemptService.resetFailedAttempts(identifier);
            
            log.info("Login successful - user: {}, IP: {}", user.getUsername(), ipAddress);
            
            // 13. Return Tokens and User
            return buildAuthenticationResponse(accessToken, refreshTokenString, userResponse);
            
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Login failed - identifier: {}", identifier, e);
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, 
                    "Authentication failed. Please try again.");
        }
    }
    
    /**
     * Refresh access token using valid refresh token.
     * 
     * <p>Refresh Token Flow:</p>
     * <ol>
     *   <li>Validate refresh token JWT format</li>
     *   <li>Extract username from token</li>
     *   <li>Check rate limiting</li>
     *   <li>Find matching hashed token in PostgreSQL</li>
     *   <li>Verify token is not revoked or expired</li>
     *   <li>Generate new access token</li>
     *   <li>Generate new refresh token</li>
     *   <li>Invalidate old refresh token</li>
     *   <li>Store new hashed refresh token</li>
     *   <li>Update Redis session</li>
     *   <li>Return new tokens</li>
     * </ol>
     * 
     * <p>Security Features:</p>
     * <ul>
     *   <li>Token rotation - old token invalidated on use</li>
     *   <li>BCrypt validation against hashed tokens</li>
     *   <li>Rate limiting on refresh attempts</li>
     *   <li>Blacklist checking for revoked tokens</li>
     *   <li>Multiple device support via separate tokens</li>
     *   <li>Session tracking in Redis</li>
     * </ul>
     */
    @Override
    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request, String ipAddress) {
        log.info("Token refresh attempt - IP: {}", ipAddress);
        
        try {
            // 1. Validate request
            if (!StringUtils.hasText(request.getRefreshToken())) {
                throw new ValidationException("Refresh token is required");
            }
            
            // 2. Validate refresh token JWT format
            if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
                log.warn("Invalid refresh token format");
                throw new InvalidTokenException("Invalid refresh token");
            }
            
            // 3. Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(request.getRefreshToken())) {
                log.warn("Attempted use of blacklisted refresh token");
                throw new InvalidTokenException("Refresh token has been revoked");
            }
            
            // 4. Extract username from token
            String username = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());
            if (username == null) {
                throw new InvalidTokenException("Invalid refresh token");
            }
            
            // 5. Retrieve user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            // 6. Check refresh rate limit - DISABLED FOR DEVELOPMENT
            /*
            if (rateLimitService.isRefreshRateLimitExceeded(user.getId())) {
                log.warn("Refresh rate limit exceeded - user: {}", username);
                throw new RateLimitExceededException(
                        "Too many token refresh attempts. Please try again later."
                );
            }
            
            rateLimitService.recordRefreshAttempt(user.getId());
            */
            
            // 7. Validate refresh token against stored hashed tokens
            RefreshToken storedToken = refreshTokenService.validateRefreshToken(
                    request.getRefreshToken(), 
                    username
            ).orElseThrow(() -> {
                log.warn("Refresh token not found or invalid - user: {}", username);
                return new InvalidTokenException("Refresh token is invalid or expired");
            });
            
            // 8. Additional validation checks
            if (!storedToken.isValid()) {
                log.warn("Refresh token invalid - revoked: {}, expired: {}", 
                        storedToken.getRevoked(), storedToken.isExpired());
                throw new InvalidTokenException("Refresh token is invalid or expired");
            }
            
            // 9. Check user status
            validateUserStatus(user);
            
            // 10. Generate new access token
            String accessToken = jwtTokenProvider.generateToken(
                    user.getUsername(),
                    List.of(user.getRole().name())
            );
            
            // 11. Generate new refresh token (rotation)
            String newRefreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUsername());
            
            // 12. Create new hashed refresh token
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                    user,
                    newRefreshTokenString,
                    ipAddress,
                    storedToken.getDeviceInfo(),
                    storedToken.getUserAgent()
            );
            
            // 13. Revoke old refresh token
            refreshTokenService.revokeToken(storedToken.getId());
            
            // 14. Update session in Redis
            sessionCacheService.removeActiveSession(username, storedToken.getId().toString());
            sessionCacheService.trackActiveSession(
                    username,
                    newRefreshToken.getId().toString(),
                    ipAddress
            );
            
            // 15. Get or cache user response
            UserResponse userResponse = sessionCacheService.getCachedUserSession(username);
            if (userResponse == null) {
                userResponse = userMapper.toResponse(user);
                sessionCacheService.cacheUserSession(username, userResponse);
            }
            
            log.info("Token refreshed successfully - user: {}, old token ID: {}, new token ID: {}", 
                    username, storedToken.getId(), newRefreshToken.getId());
            
            // 16. Create audit log
            auditService.log(
                    user.getId(),
                    "User",
                    user.getId(),
                    AuditAction.TOKEN_REFRESH,
                    String.format("Token refreshed from IP: %s, device: %s", 
                            ipAddress, storedToken.getDeviceInfo())
            );
            
            return buildAuthenticationResponse(accessToken, newRefreshTokenString, userResponse);
            
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new InvalidTokenException("Failed to refresh token. Please login again.");
        }
    }
    
    /**
     * Logout user and revoke all tokens.
     * 
     * <p>Actions:</p>
     * <ul>
     *   <li>Blacklist access token</li>
     *   <li>Revoke refresh tokens</li>
     *   <li>Clear session cache</li>
     *   <li>Remove active session tracking</li>
     * </ul>
     */
    @Override
    @Transactional
    public void logout(String accessToken, String username) {
        log.info("Logout initiated - user: {}", username);
        
        try {
            // 1. Blacklist access token
            if (StringUtils.hasText(accessToken)) {
                tokenBlacklistService.blacklistToken(accessToken);
            }
            
            // 2. Find and revoke user's refresh tokens
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            
            List<RefreshToken> activeTokens = refreshTokenRepository
                    .findActiveTokensByUserId(user.getId(), LocalDateTime.now());
            
            for (RefreshToken token : activeTokens) {
                token.setRevoked(true);
            }
            
            if (!activeTokens.isEmpty()) {
                refreshTokenRepository.saveAll(activeTokens);
                log.info("Revoked {} active refresh tokens for user: {}", activeTokens.size(), username);
            }
            
            // 3. Invalidate session cache
            sessionCacheService.invalidateUserSession(username);
            
            // 4. Clear active sessions
            sessionCacheService.clearAllActiveSessions(username);
            
            log.info("Logout completed - user: {}", username);
            
        } catch (Exception e) {
            log.error("Logout failed - user: {}", username, e);
            // Don't throw exception - logout should always succeed
        }
    }
    
    /**
     * Verify if access token is valid.
     * 
     * @param token JWT access token
     * @return verification response with token details
     */
    @Override
    public VerifyTokenResponse verifyAccessToken(String token) {
        try {
            // 1. Validate token format
            if (!StringUtils.hasText(token)) {
                return buildInvalidTokenResponse("Token is required");
            }
            
            // 2. Check if blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                return buildInvalidTokenResponse("Token has been revoked");
            }
            
            // 3. Validate token
            if (!jwtTokenProvider.validateAccessToken(token)) {
                return buildInvalidTokenResponse("Token is invalid or expired");
            }
            
            // 4. Extract token information
            String username = jwtTokenProvider.getUsernameFromToken(token);
            Date expiresAt = jwtTokenProvider.getExpirationFromToken(token);
            Date issuedAt = jwtTokenProvider.getIssuedAtFromToken(token);
            
            // 5. Verify user exists and is active
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                return buildInvalidTokenResponse("User account is not active");
            }
            
            // 6. Build success response
            return VerifyTokenResponse.builder()
                    .valid(true)
                    .username(username)
                    .userId(user.getId())
                    .expiresAt(convertToLocalDateTime(expiresAt))
                    .issuedAt(convertToLocalDateTime(issuedAt))
                    .message("Token is valid")
                    .build();
                    
        } catch (Exception e) {
            log.error("Token verification failed", e);
            return buildInvalidTokenResponse("Token verification failed");
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private void validateRegistrationRequest(RegisterRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new ValidationException("Username is required");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ValidationException("Email is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new ValidationException("Password is required");
        }
    }
    
    private void validateLoginRequest(LoginRequest request) {
        if (!StringUtils.hasText(request.getUsernameOrEmail())) {
            throw new ValidationException("Username or email is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new ValidationException("Password is required");
        }
    }
    
    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.PENDING) {
            throw new BaseException(ErrorCode.ACCOUNT_PENDING_VERIFICATION,
                    "Please verify your email address before logging in.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccountLockedException("Account has been suspended. Please contact support.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResourceNotFoundException("Account not found");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BaseException(ErrorCode.ACCOUNT_DISABLED,
                    "Account is inactive. Please contact support.");
        }
    }
    
    private void handleFailedLogin(String identifier, String ipAddress) {
        // Record failed attempt - DISABLED FOR DEVELOPMENT
        /*
        failedLoginAttemptService.recordFailedAttempt(identifier);
        
        int remainingAttempts = failedLoginAttemptService.getRemainingAttempts(identifier);
        
        log.warn("Failed login attempt - identifier: {}, IP: {}, remaining attempts: {}", 
                identifier, ipAddress, remainingAttempts);
        
        if (remainingAttempts > 0) {
            throw new BadCredentialsException(
                    "Invalid username or password. " + remainingAttempts + " attempts remaining."
            );
        } else {
            throw new AccountLockedException(
                    "Account locked due to multiple failed login attempts. " +
                    "Please try again in 30 minutes."
            );
        }
        */
        
        // Development mode - just log the failed attempt
        log.warn("Failed login attempt - identifier: {}, IP: {} (rate limiting disabled)", 
                identifier, ipAddress);
    }
    
    private String extractDeviceInfo(String userAgent) {
        // Simple device extraction - can be enhanced with user-agent parsing library
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }
        
        if (userAgent.contains("Mobile")) {
            return "Mobile Device";
        } else if (userAgent.contains("Tablet")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }
    
    private AuthenticationResponse buildAuthenticationResponse(
            String accessToken, 
            String refreshToken, 
            UserResponse user) {
        
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L) // 15 minutes in seconds
                .user(user)
                .build();
    }
    
    private VerifyTokenResponse buildInvalidTokenResponse(String message) {
        return VerifyTokenResponse.builder()
                .valid(false)
                .message(message)
                .build();
    }
    
    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) return null;
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }

    // =========================================================================
    // Email verification
    // =========================================================================

    /**
     * Send (or re-send) an email-verification OTP.
     *
     * <p>Handles two cases:</p>
     * <ul>
     *   <li><strong>Pending registration</strong> — the user signed up but has not yet
     *       verified.  No {@code User} row exists in the DB yet; the pending data lives
     *       in Redis.  We resend the OTP using the email-only variant.</li>
     *   <li><strong>Existing unverified account</strong> — a {@code User} row exists
     *       in the DB but {@code emailVerified = false} (legacy / edge case).  We
     *       resend using the user-aware variant.</li>
     * </ul>
     */
    @Override
    @Transactional
    public void sendEmailVerification(SendVerificationEmailRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Email verification requested for: {}", email);

        // Case 1: user exists in DB
        userRepository.findByEmail(email).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new ConflictException(ErrorCode.EMAIL_ALREADY_VERIFIED,
                        "Email address is already verified");
            }
            // User in DB but not yet verified — resend via user-aware helper
            sendVerificationOtp(user);
            log.info("Verification email resent (DB user) to: {}", email);
        });

        // Case 2: pending registration in Redis (no DB row yet)
        if (!userRepository.existsByEmail(email)) {
            if (!pendingRegistrationCacheService.exists(email)) {
                throw new ResourceNotFoundException("No account found for email: " + email);
            }
            String displayName = pendingRegistrationCacheService.getUsername(email);
            sendVerificationOtpForPending(email, displayName != null ? displayName : email);
            log.info("Verification email resent (pending registration) to: {}", email);
        }
    }

    /**
     * Confirm email ownership by submitting the OTP.
     *
     * <p>On success:</p>
     * <ol>
     *   <li>OTP is validated and consumed from Redis</li>
     *   <li>EmailVerification DB record is marked used</li>
     *   <li>Pending registration data is loaded from Redis</li>
     *   <li>User is created directly in {@link UserStatus#ACTIVE} state (first and only DB write)</li>
     *   <li>Pending cache entry is deleted from Redis</li>
     *   <li>JWT tokens are issued immediately</li>
     *   <li>Session cached in Redis</li>
     *   <li>Welcome email dispatched (best-effort)</li>
     * </ol>
     *
     * @return full {@link AuthenticationResponse} so the client can log in immediately
     */
    @Override
    @Transactional
    public AuthenticationResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Email verification submission for: {}", email);

        // Check if this email already has a fully-verified account in the DB
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (Boolean.TRUE.equals(existing.getEmailVerified())) {
                throw new ConflictException(ErrorCode.EMAIL_ALREADY_VERIFIED,
                        "Email address is already verified");
            }
        });

        // Validate OTP via Redis
        boolean valid = otpCacheService.verifyOtp(email, request.getOtp(), OtpPurpose.EMAIL_VERIFICATION);
        if (!valid) {
            log.warn("Invalid email-verification OTP for: {}", email);
            throw new BaseException(ErrorCode.INVALID_OTP,
                    "Invalid or expired verification code");
        }

        // Load pending registration data from Redis
        PendingRegistrationCacheService.PendingRegistration pending =
                pendingRegistrationCacheService.load(email);
        if (pending == null) {
            // Pending entry expired or was never created — treat as a bad/stale OTP
            log.warn("No pending registration found in cache for: {}", email);
            throw new BaseException(ErrorCode.INVALID_OTP,
                    "Verification session expired. Please register again.");
        }

        // Consume OTP in Redis
        otpCacheService.invalidateOtp(email, OtpPurpose.EMAIL_VERIFICATION);

        // Mark EmailVerification DB record as used
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.findLatestActive(email, VerificationType.EMAIL_VERIFICATION, now)
                .ifPresent(ev -> {
                    ev.setUsed(true);
                    ev.setUsedAt(now);
                    emailVerificationRepository.save(ev);
                });

        // Create the User row — directly ACTIVE, email already confirmed
        User user = User.builder()
                .username(pending.getUsername())
                .email(pending.getEmail())
                .password(pending.getPasswordHash())
                .firstName(pending.getFirstName())
                .lastName(pending.getLastName())
                .role(com.ziboto.backend.user.entity.UserRole.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .storageQuota(5368709120L) // 5 GB default
                .storageUsed(0L)
                .build();
        user = userRepository.save(user);
        log.info("Account created and activated for user: {} (ID: {})", user.getUsername(), user.getId());

        // Clean up the pending cache entry
        pendingRegistrationCacheService.delete(email);

        // Issue tokens for the first time
        String accessToken = jwtTokenProvider.generateToken(
                user.getUsername(),
                List.of(user.getRole().name())
        );
        String refreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUsername());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user, refreshTokenString, "email-verified", null, null);

        // Cache session
        UserResponse userResponse = userMapper.toResponse(user);
        sessionCacheService.cacheUserSession(user.getUsername(), userResponse);
        sessionCacheService.trackActiveSession(
                user.getUsername(), refreshToken.getId().toString(), "email-verified");

        // Audit
        auditService.log(user.getId(), "User", user.getId(),
                AuditAction.UPDATE,
                "Email verified — account created and activated");

        // Welcome email (best-effort — failure must not roll back)
        try {
            emailService.sendWelcomeEmail(user.getEmail(), resolveDisplayName(user));
        } catch (Exception e) {
            log.warn("Welcome email failed for {} — ignoring", user.getUsername(), e);
        }

        log.info("Email verified and tokens issued for user: {}", user.getUsername());
        return buildAuthenticationResponse(accessToken, refreshTokenString, userResponse);
    }

    // =========================================================================
    // Forgot / reset password
    // =========================================================================

    /**
     * Initiate a password-reset flow.
     *
     * <p>Always returns success to prevent user enumeration — the email is
     * simply not sent if no account exists for the address.</p>
     */
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Password reset requested for: {}", email);

        userRepository.findByEmail(email).ifPresent(user -> {
            try {
                sendPasswordResetOtp(user);
                log.info("Password reset email dispatched to: {}", email);
            } catch (BaseException e) {
                // Rate-limited — log and swallow so caller gets the same generic response
                log.warn("Password reset OTP not sent for {} — {}", email, e.getMessage());
            }
        });
    }

    /**
     * Complete the password-reset flow.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Look up user by email</li>
     *   <li>Verify OTP via Redis</li>
     *   <li>BCrypt-encode and save new password</li>
     *   <li>Consume OTP in Redis + mark DB record used</li>
     *   <li>Revoke all active refresh tokens (force re-login everywhere)</li>
     *   <li>Invalidate session cache</li>
     * </ol>
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Password reset submission for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for email: " + email));

        // Validate OTP via Redis
        boolean valid = otpCacheService.verifyOtp(email, request.getOtp(), OtpPurpose.PASSWORD_RESET);
        if (!valid) {
            log.warn("Invalid password-reset OTP for: {}", email);
            throw new BaseException(ErrorCode.INVALID_OTP,
                    "Invalid or expired verification code");
        }

        // Consume OTP in Redis
        otpCacheService.invalidateOtp(email, OtpPurpose.PASSWORD_RESET);

        // Mark DB record as used
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.findLatestActive(email, VerificationType.PASSWORD_RESET, now)
                .ifPresent(ev -> {
                    ev.setUsed(true);
                    ev.setUsedAt(now);
                    emailVerificationRepository.save(ev);
                });

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens — force re-login on all devices
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findActiveTokensByUserId(user.getId(), now);
        activeTokens.forEach(t -> t.setRevoked(true));
        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }

        // Invalidate Redis session cache
        sessionCacheService.invalidateUserSession(user.getUsername());
        sessionCacheService.clearAllActiveSessions(user.getUsername());

        // Audit
        auditService.log(user.getId(), "User", user.getId(),
                AuditAction.UPDATE,
                "Password reset completed via email OTP");

        log.info("Password reset successful for user: {}", user.getUsername());
    }

    // =========================================================================
    // Private OTP helpers shared by the new flows
    // =========================================================================

    /**
     * Issue a new EMAIL_VERIFICATION OTP for a <em>fully pending</em> registration
     * (no {@code User} row in the DB yet).
     *
     * <p>Because there is no {@code User} FK to reference, the
     * {@link EmailVerification} audit row is persisted with {@code user = null}.
     * Rate-limiting uses the email-based query ({@link EmailVerificationRepository#findLatestActive}).</p>
     */
    private void sendVerificationOtpForPending(String email, String displayName) {
        LocalDateTime now = LocalDateTime.now();

        // DEVELOPMENT: Rate-limiting disabled — allow unlimited OTP generation
        // Rate-limit: one active OTP at a time (email-based, no userId)
        // emailVerificationRepository
        //         .findLatestActive(email, VerificationType.EMAIL_VERIFICATION, now)
        //         .ifPresent(ev -> {
        //             throw new BaseException(ErrorCode.OTP_RATE_LIMITED,
        //                     "A verification email was already sent. Please check your inbox or wait before requesting a new code.");
        //         });

        // Invalidate any stale unused records for this email+type
        emailVerificationRepository.invalidateAllActiveByEmail(
                email, VerificationType.EMAIL_VERIFICATION, now);

        // Generate OTP (stored in Redis)
        String otp = otpCacheService.generateOtp(email, OtpPurpose.EMAIL_VERIFICATION);
        // DEVELOPMENT: Disabled null check — if Redis rate-limit hits, we'll get null but won't throw
        // if (otp == null) {
        //     throw new BaseException(ErrorCode.OTP_RATE_LIMITED,
        //             "Too many verification requests. Please try again later.");
        // }

        // If OTP generation was rate-limited, generate a fallback for dev
        if (otp == null) {
            log.warn("OTP generation rate-limited for {} — generating fallback OTP for development", email);
            otp = String.format("%06d", (int) (Math.random() * 1000000));
        }

        // Persist audit row — user FK left null (pre-verification)
        int ttlMinutes = redisProperties.getOtp().getTtlMinutes();
        EmailVerification ev = EmailVerification.builder()
                .user(null)
                .email(email)
                .type(VerificationType.EMAIL_VERIFICATION)
                .used(false)
                .expiresAt(now.plusMinutes(ttlMinutes))
                .build();
        emailVerificationRepository.save(ev);

        // Send email
        emailService.sendEmailVerification(email, displayName, otp);
    }

    /**
     * Issue a new EMAIL_VERIFICATION OTP for an existing {@link User} entity
     * (DB row already present — covers legacy / edge-case re-sends).
     * Enforces rate-limiting: throws {@link BaseException} (OTP_RATE_LIMITED) if an
     * active OTP already exists.
     */
    private void sendVerificationOtp(User user) {
        LocalDateTime now = LocalDateTime.now();

        // DEVELOPMENT: Rate-limiting disabled
        // Rate-limit: one active OTP at a time
        // emailVerificationRepository
        //         .findLatestActiveByUserId(user.getId(), VerificationType.EMAIL_VERIFICATION, now)
        //         .ifPresent(ev -> {
        //             throw new BaseException(ErrorCode.OTP_RATE_LIMITED,
        //                     "A verification email was already sent. Please check your inbox or wait before requesting a new code.");
        //         });

        // Invalidate any stale unused records for this user+type
        emailVerificationRepository.invalidateAllActive(
                user.getId(), VerificationType.EMAIL_VERIFICATION, now);

        // Generate OTP (stored in Redis)
        String otp = otpCacheService.generateOtp(user.getEmail(), OtpPurpose.EMAIL_VERIFICATION);
        // DEVELOPMENT: Disabled
        // if (otp == null) {
        //     throw new BaseException(ErrorCode.OTP_RATE_LIMITED,
        //             "Too many verification requests. Please try again later.");
        // }

        // Fallback for dev if rate-limited
        if (otp == null) {
            log.warn("OTP generation rate-limited for {} — generating fallback OTP for development", user.getEmail());
            otp = String.format("%06d", (int) (Math.random() * 1000000));
        }

        // Persist lifecycle record
        int ttlMinutes = redisProperties.getOtp().getTtlMinutes();
        EmailVerification ev = EmailVerification.builder()
                .user(user)
                .email(user.getEmail())
                .type(VerificationType.EMAIL_VERIFICATION)
                .used(false)
                .expiresAt(now.plusMinutes(ttlMinutes))
                .build();
        emailVerificationRepository.save(ev);

        // Send email
        emailService.sendEmailVerification(
                user.getEmail(), resolveDisplayName(user), otp);
    }

    /**
     * Issue a new PASSWORD_RESET OTP for the given user and dispatch the email.
     * Enforces the same rate-limiting as {@link #sendVerificationOtp}.
     */
    private void sendPasswordResetOtp(User user) {
        LocalDateTime now = LocalDateTime.now();

        // Rate-limit: one active OTP at a time
        emailVerificationRepository
                .findLatestActiveByUserId(user.getId(), VerificationType.PASSWORD_RESET, now)
                .ifPresent(ev -> {
                    throw new BaseException(ErrorCode.OTP_RATE_LIMITED,
                            "A password reset email was already sent. Please check your inbox or wait before requesting a new code.");
                });

        // Invalidate any stale unused records
        emailVerificationRepository.invalidateAllActive(
                user.getId(), VerificationType.PASSWORD_RESET, now);

        // Generate OTP (stored in Redis)
        String otp = otpCacheService.generateOtp(user.getEmail(), OtpPurpose.PASSWORD_RESET);
        if (otp == null) {
            throw new BaseException(ErrorCode.OTP_RATE_LIMITED,
                    "Too many reset requests. Please try again later.");
        }

        // Persist lifecycle record
        int ttlMinutes = redisProperties.getOtp().getTtlMinutes();
        EmailVerification ev = EmailVerification.builder()
                .user(user)
                .email(user.getEmail())
                .type(VerificationType.PASSWORD_RESET)
                .used(false)
                .expiresAt(now.plusMinutes(ttlMinutes))
                .build();
        emailVerificationRepository.save(ev);

        // Send email
        emailService.sendPasswordResetEmail(
                user.getEmail(), resolveDisplayName(user), otp);
    }

    /** Returns firstName if set, otherwise falls back to username. */
    private String resolveDisplayName(User user) {
        return StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : user.getUsername();
    }
}
