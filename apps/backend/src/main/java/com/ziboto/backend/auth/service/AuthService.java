package com.ziboto.backend.auth.service;

import com.ziboto.backend.auth.dto.AuthenticationResponse;
import com.ziboto.backend.auth.dto.ForgotPasswordRequest;
import com.ziboto.backend.auth.dto.LoginRequest;
import com.ziboto.backend.auth.dto.RefreshTokenRequest;
import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.auth.dto.ResetPasswordRequest;
import com.ziboto.backend.auth.dto.SendVerificationEmailRequest;
import com.ziboto.backend.auth.dto.VerifyEmailRequest;
import com.ziboto.backend.auth.dto.VerifyTokenResponse;

/**
 * Authentication service interface.
 *
 * <p>Provides core authentication operations:</p>
 * <ul>
 *   <li>User registration</li>
 *   <li>User login with credentials</li>
 *   <li>Token refresh</li>
 *   <li>Logout (token revocation)</li>
 *   <li>Token verification</li>
 *   <li>Email verification (OTP send + confirm)</li>
 *   <li>Forgot / reset password (OTP send + confirm)</li>
 * </ul>
 */
public interface AuthService {

    /**
     * Register a new user.
     *
     * @param request    registration request with user details
     * @param ipAddress  client IP address for tracking
     * @return authentication response with tokens and user info
     */
    AuthenticationResponse register(RegisterRequest request, String ipAddress);

    /**
     * Authenticate user with credentials.
     *
     * @param request    login request with username/email and password
     * @param ipAddress  client IP address for security tracking
     * @return authentication response with tokens and user info
     */
    AuthenticationResponse login(LoginRequest request, String ipAddress);

    /**
     * Refresh access token using refresh token.
     *
     * @param request    refresh token request
     * @param ipAddress  client IP address
     * @return new authentication response with fresh tokens
     */
    AuthenticationResponse refreshToken(RefreshTokenRequest request, String ipAddress);

    /**
     * Logout user and revoke tokens.
     *
     * @param accessToken  JWT access token to revoke
     * @param username     username of the user logging out
     */
    void logout(String accessToken, String username);

    /**
     * Verify if access token is valid.
     *
     * @param token JWT access token to verify
     * @return verification response with token details
     */
    VerifyTokenResponse verifyAccessToken(String token);

    // -------------------------------------------------------------------------
    // Email verification
    // -------------------------------------------------------------------------

    /**
     * Send (or re-send) an email-verification OTP to the address on the account.
     *
     * <p>Rate-limited: a new OTP is issued only if no active one already exists
     * for this user.</p>
     *
     * @param request  contains the email address to verify
     */
    void sendEmailVerification(SendVerificationEmailRequest request);

    /**
     * Confirm email ownership by submitting the OTP that was emailed.
     *
     * <p>On success the account is promoted from PENDING to ACTIVE,
     * {@code emailVerified} is set to {@code true}, and a full
     * {@link AuthenticationResponse} (with JWT tokens) is returned so the
     * client can enter the application immediately without a separate login.</p>
     *
     * @param request  contains email + 6-digit OTP
     * @return authentication response with tokens and activated user info
     */
    AuthenticationResponse verifyEmail(VerifyEmailRequest request);

    // -------------------------------------------------------------------------
    // Forgot / reset password
    // -------------------------------------------------------------------------

    /**
     * Initiate a password-reset flow by sending a 6-digit OTP to the
     * registered email address.
     *
     * <p>To prevent user enumeration the method always returns successfully
     * even when no account is found for the given email.</p>
     *
     * @param request  contains the email address
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Complete the password-reset flow by validating the OTP and setting
     * a new password.
     *
     * @param request  contains email, OTP, and new password
     */
    void resetPassword(ResetPasswordRequest request);
}

