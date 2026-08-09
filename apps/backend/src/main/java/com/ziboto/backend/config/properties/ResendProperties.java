package com.ziboto.backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Resend email service configuration properties.
 *
 * <p>All values are driven by environment variables so secrets never
 * live in committed config files.</p>
 *
 * <h2>Required env vars:</h2>
 * <ul>
 *   <li>{@code RESEND_API_KEY}   – API key from the Resend dashboard</li>
 *   <li>{@code RESEND_FROM_EMAIL} – Verified sender address, e.g. {@code noreply@yourdomain.com}</li>
 * </ul>
 *
 * <h2>Optional env vars (sensible defaults provided):</h2>
 * <ul>
 *   <li>{@code RESEND_FROM_NAME}         – Display name shown to recipients (default: "Ziboto")</li>
 *   <li>{@code RESEND_VERIFICATION_URL}  – Base URL for email-verification deep-links</li>
 *   <li>{@code RESEND_RESET_URL}         – Base URL for password-reset deep-links</li>
 *   <li>{@code RESEND_SUPPORT_EMAIL}     – Reply-to / support contact shown in templates</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.email")
public class ResendProperties {

    /** Resend secret API key (re_xxxx). Never log or expose this value. */
    private String apiKey;

    /** Verified sender address that appears in the "From" header. */
    private String fromEmail;

    /** Human-readable display name that appears alongside the sender address. */
    private String fromName = "Ziboto";

    /**
     * Base URL used to build email-verification callback links.
     * The OTP code is appended as a query parameter: {@code ?token=<otp>&email=<email>}.
     * Example: {@code https://app.ziboto.com/verify-email}
     */
    private String verificationUrl;

    /**
     * Base URL used to build password-reset callback links.
     * The OTP code is appended as a query parameter: {@code ?token=<otp>&email=<email>}.
     * Example: {@code https://app.ziboto.com/reset-password}
     */
    private String resetPasswordUrl;

    /** Support / reply-to email address shown at the bottom of transactional emails. */
    private String supportEmail;

    /**
     * Returns the full "From" header value, combining display name and address.
     * E.g. {@code "Ziboto <noreply@ziboto.com>"}.
     */
    public String getFromHeader() {
        return fromName + " <" + fromEmail + ">";
    }
}
