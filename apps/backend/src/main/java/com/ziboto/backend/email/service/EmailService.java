package com.ziboto.backend.email.service;

/**
 * Transactional email service interface.
 *
 * <p>All methods are fire-and-forget from the caller's perspective — they log
 * failures internally and never propagate checked exceptions upward so that an
 * email-send failure does not roll back the surrounding database transaction.</p>
 */
public interface EmailService {

    /**
     * Send an OTP code to the given address for email verification.
     *
     * @param toEmail   recipient address
     * @param username  display name shown in the email body
     * @param otp       6-digit code to embed
     */
    void sendEmailVerification(String toEmail, String username, String otp);

    /**
     * Send an OTP code to the given address for password reset.
     *
     * @param toEmail   recipient address
     * @param username  display name shown in the email body
     * @param otp       6-digit code to embed
     */
    void sendPasswordResetEmail(String toEmail, String username, String otp);

    /**
     * Send a welcome / account-confirmed email after successful verification.
     *
     * @param toEmail  recipient address
     * @param username display name
     */
    void sendWelcomeEmail(String toEmail, String username);
}
