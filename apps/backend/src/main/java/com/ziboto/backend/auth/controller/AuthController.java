package com.ziboto.backend.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ziboto.backend.auth.dto.AuthenticationResponse;
import com.ziboto.backend.auth.dto.ForgotPasswordRequest;
import com.ziboto.backend.auth.dto.LoginRequest;
import com.ziboto.backend.auth.dto.RefreshTokenRequest;
import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.auth.dto.ResetPasswordRequest;
import com.ziboto.backend.auth.dto.SendVerificationEmailRequest;
import com.ziboto.backend.auth.dto.VerifyEmailRequest;
import com.ziboto.backend.auth.dto.VerifyTokenResponse;
import com.ziboto.backend.auth.service.AuthService;
import com.ziboto.backend.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication REST controller.
 * 
 * <p>Provides endpoints for user authentication operations:</p>
 * <ul>
 *   <li>POST /api/v1/auth/register               - Register new user</li>
 *   <li>POST /api/v1/auth/login                  - Login with credentials</li>
 *   <li>POST /api/v1/auth/logout                 - Logout and revoke tokens</li>
 *   <li>POST /api/v1/auth/refresh                - Refresh access token</li>
 *   <li>GET  /api/v1/auth/verify                 - Verify token validity</li>
 *   <li>POST /api/v1/auth/email/send-verification - Send email-verification OTP</li>
 *   <li>POST /api/v1/auth/email/verify           - Confirm email with OTP</li>
 *   <li>POST /api/v1/auth/password/forgot        - Request password-reset OTP</li>
 *   <li>POST /api/v1/auth/password/reset         - Complete password reset</li>
 * </ul>
 *   <li>POST /api/v1/auth/refresh - Refresh access token</li>
 *   <li>GET /api/v1/auth/verify - Verify token validity</li>
 * </ul>
 * 
 * <p>All business logic is delegated to AuthService.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Register a new user account.
     *
     * <p>Creates the account in PENDING state and sends an email verification OTP.
     * No JWT tokens are returned — the client must call POST /email/verify with
     * the OTP to activate the account and receive tokens.</p>
     *
     * @param request     registration details including username, email, and password
     * @param httpRequest HTTP request for extracting client IP address
     * @return 202 Accepted with no token payload
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates the account in PENDING state and sends a 6-digit email verification OTP. " +
                      "No tokens are issued until POST /email/verify is called with the correct OTP."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "202",
            description = "Account created — verification email sent"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid registration data"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Username or email already exists"
        )
    })
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = extractClientIpAddress(httpRequest);
        log.info("Registration request received for username: {} from IP: {}", request.getUsername(), ipAddress);

        authService.register(request, ipAddress);

        log.info("Registration pending email verification: {}", request.getUsername());
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "Account created. Please check your email for a verification code.", null));
    }
    
    /**
     * Authenticate user with credentials.
     * 
     * @param request login credentials (username/email and password)
     * @param httpRequest HTTP request for extracting client IP address
     * @return authentication response with JWT tokens and user information
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticate user with username/email and password. Returns JWT tokens upon successful authentication."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid credentials"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Too many login attempts"
        )
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = extractClientIpAddress(httpRequest);
        log.info("Login request received for user: {} from IP: {}", request.getUsernameOrEmail(), ipAddress);
        
        AuthenticationResponse response = authService.login(request, ipAddress);
        
        log.info("User logged in successfully: {}", request.getUsernameOrEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    /**
     * Refresh access token using refresh token.
     * 
     * @param request refresh token request
     * @param httpRequest HTTP request for extracting client IP address
     * @return new authentication response with fresh JWT tokens
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Obtain a new access token using a valid refresh token. The old refresh token is invalidated and a new one is issued."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
        )
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = extractClientIpAddress(httpRequest);
        log.info("Token refresh request received from IP: {}", ipAddress);
        
        AuthenticationResponse response = authService.refreshToken(request, ipAddress);
        
        log.info("Token refreshed successfully for user");
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }
    
    /**
     * Logout user and revoke tokens.
     * 
     * @param authorizationHeader Authorization header containing JWT token (optional)
     * @return success response
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Logout the authenticated user and revoke their tokens. The access token is blacklisted and refresh tokens are invalidated."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Logout successful"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        )
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Bearer token in format: Bearer <token>", required = false)
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        
        try {
            // If authorization header is present, extract token and perform full logout
            if (StringUtils.hasText(authorizationHeader)) {
                String accessToken = extractTokenFromHeader(authorizationHeader);
                
                // Get username from security context
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() 
                        && !"anonymousUser".equals(authentication.getName())) {
                    String username = authentication.getName();
                    log.info("Logout request received for user: {}", username);
                    authService.logout(accessToken, username);
                    log.info("User logged out successfully: {}", username);
                } else {
                    log.debug("Logout request with token but no authenticated user");
                }
            } else {
                log.debug("Logout request without authorization header - client-side logout only");
            }
            
            return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
        } catch (Exception e) {
            log.error("Error during logout", e);
            // Still return success to allow client-side logout
            return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
        }
    }
    
    /**
     * Verify JWT access token validity.
     * 
     * @param authorizationHeader Authorization header containing JWT token
     * @return token verification response with token details
     */
    @GetMapping("/verify")
    @Operation(
        summary = "Verify token",
        description = "Verify the validity of a JWT access token. Returns token details if valid."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token verified",
            content = @Content(schema = @Schema(implementation = VerifyTokenResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid or expired token"
        )
    })
    public ResponseEntity<ApiResponse<VerifyTokenResponse>> verifyToken(
            @Parameter(description = "Bearer token in format: Bearer <token>", required = true)
            @RequestHeader("Authorization") String authorizationHeader) {
        
        String accessToken = extractTokenFromHeader(authorizationHeader);
        
        log.debug("Token verification request received");
        
        VerifyTokenResponse response = authService.verifyAccessToken(accessToken);
        
        if (response.getValid()) {
            log.debug("Token verified successfully for user: {}", response.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Token is valid", response));
        } else {
            log.debug("Token verification failed: {}", response.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(response.getMessage(), response));
        }
    }
    
    // =========================================================================
    // Email verification endpoints
    // =========================================================================

    /**
     * Send (or re-send) an email-verification OTP.
     * Safe to call again after signup if the user never received / lost the code.
     */
    @PostMapping("/email/send-verification")
    @Operation(
        summary = "Send email verification OTP",
        description = "Sends a 6-digit OTP to the registered email address. " +
                      "Rate-limited: only one active OTP is allowed at a time."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Verification email sent"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "No account found for that email"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "Email is already verified"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429", description = "OTP already sent — please wait before requesting again")
    })
    public ResponseEntity<ApiResponse<Void>> sendEmailVerification(
            @Valid @RequestBody SendVerificationEmailRequest request) {

        log.info("Send email verification request for: {}", request.getEmail());
        authService.sendEmailVerification(request);
        return ResponseEntity.ok(
                ApiResponse.success("Verification email sent. Please check your inbox.", null));
    }

    /**
     * Confirm email ownership by submitting the OTP from the verification email.
     * On success the account is activated (PENDING → ACTIVE) and JWT tokens are returned.
     */
    @PostMapping("/email/verify")
    @Operation(
        summary = "Verify email with OTP",
        description = "Submit the 6-digit OTP that was emailed to confirm ownership of the address. " +
                      "Activates the account and returns JWT tokens so the user can enter the app immediately."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Email verified — account activated, tokens returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid or expired OTP"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "No account found for that email"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "Email is already verified")
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        log.info("Email verification submission for: {}", request.getEmail());
        AuthenticationResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified. Welcome to Ziboto!", response));
    }

    // =========================================================================
    // Forgot / reset password endpoints
    // =========================================================================

    /**
     * Initiate a password-reset flow.
     * Always returns 200 to prevent user enumeration.
     */
    @PostMapping("/password/forgot")
    @Operation(
        summary = "Forgot password — request reset OTP",
        description = "Sends a 6-digit password-reset OTP to the registered email address. " +
                      "Always returns 200 regardless of whether the account exists."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Reset email dispatched (if account exists)")
    })
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("Forgot password request for email: {}", request.getEmail());
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists for that email, a reset code has been sent.", null));
    }

    /**
     * Complete the password-reset flow using the OTP from the reset email.
     */
    @PostMapping("/password/reset")
    @Operation(
        summary = "Reset password with OTP",
        description = "Provide the 6-digit OTP and a new password to complete the reset. " +
                      "All existing sessions are revoked on success."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Password reset successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid or expired OTP"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "No account found for that email")
    })
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("Password reset submission for: {}", request.getEmail());
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset successfully. Please log in with your new password.", null));
    }

    /**
     * Extract JWT token from Authorization header.
     * 
     * @param authorizationHeader Authorization header value
     * @return JWT token string without Bearer prefix
     * @throws IllegalArgumentException if header format is invalid
     */
    private String extractTokenFromHeader(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new IllegalArgumentException("Authorization header is missing");
        }
        
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        
        throw new IllegalArgumentException("Invalid Authorization header format. Expected: Bearer <token>");
    }
    
    /**
     * Extract client IP address from HTTP request.
     * 
     * <p>Checks multiple headers in order:</p>
     * <ol>
     *   <li>X-Forwarded-For (proxy/load balancer)</li>
     *   <li>X-Real-IP (nginx proxy)</li>
     *   <li>Remote address from request</li>
     * </ol>
     * 
     * @param request HTTP servlet request
     * @return client IP address
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        
        if (!StringUtils.hasText(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        
        if (!StringUtils.hasText(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // X-Forwarded-For can contain multiple IPs, take the first one
        if (StringUtils.hasText(ipAddress) && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return StringUtils.hasText(ipAddress) ? ipAddress : "unknown";
    }
}
