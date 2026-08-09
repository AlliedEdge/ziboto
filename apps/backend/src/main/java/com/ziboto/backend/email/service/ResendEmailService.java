package com.ziboto.backend.email.service;

import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.ziboto.backend.config.properties.ResendProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link EmailService} implementation backed by the Resend transactional
 * email platform.
 *
 * <p>HTML templates are inlined here so the service has zero external
 * template-engine dependencies.  Each method builds a self-contained
 * HTML string and delegates to the {@link Resend} client.</p>
 *
 * <p>Failures are caught and logged — they are intentionally <em>not</em>
 * re-thrown so a Resend outage cannot roll back a user-registration
 * transaction.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResendEmailService implements EmailService {

    private final Resend resendClient;
    private final ResendProperties emailProps;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public void sendEmailVerification(String toEmail, String username, String otp) {
        String subject = "Verify your Ziboto account";
        String html = buildVerificationEmail(username, otp, toEmail);
        send(toEmail, subject, html, "email-verification");
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String username, String otp) {
        String subject = "Reset your Ziboto password";
        String html = buildPasswordResetEmail(username, otp);
        send(toEmail, subject, html, "password-reset");
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String username) {
        String subject = "Welcome to Ziboto!";
        String html = buildWelcomeEmail(username);
        send(toEmail, subject, html, "welcome");
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void send(String toEmail, String subject, String html, String emailType) {
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(emailProps.getFromHeader())
                    .to(toEmail)
                    .replyTo(emailProps.getSupportEmail())
                    .subject(subject)
                    .html(html)
                    .build();

            var response = resendClient.emails().send(options);
            log.info("Email sent [type={}, to={}, id={}]", emailType, toEmail, response.getId());

        } catch (ResendException e) {
            // Log but do not propagate — email failure must not roll back DB transactions.
            log.error("Failed to send {} email to {}: {}", emailType, toEmail, e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // HTML template builders
    // -------------------------------------------------------------------------

    private String buildVerificationEmail(String username, String otp, String email) {
        String verifyUrl = emailProps.getVerificationUrl()
                + "?token=" + otp
                + "&email=" + email;

        return baseTemplate(
                "Verify your email address",
                """
                <p style="margin:0 0 16px;color:#374151;font-size:16px;">
                    Hi <strong>%s</strong>,
                </p>
                <p style="margin:0 0 24px;color:#374151;font-size:15px;">
                    Thanks for signing up to Ziboto! Please verify your email address
                    by entering the code below or clicking the button.
                </p>
                %s
                <p style="margin:24px 0;text-align:center;">
                    <a href="%s"
                       style="display:inline-block;background:#4f46e5;color:#ffffff;
                              text-decoration:none;padding:12px 32px;border-radius:6px;
                              font-size:15px;font-weight:600;">
                        Verify my email
                    </a>
                </p>
                <p style="margin:0 0 8px;color:#6b7280;font-size:13px;">
                    This code expires in <strong>5 minutes</strong>.
                    If you did not create an account you can safely ignore this email.
                </p>
                """.formatted(escapeHtml(username), otpBox(otp), verifyUrl)
        );
    }

    private String buildPasswordResetEmail(String username, String otp) {
        String resetUrl = emailProps.getResetPasswordUrl()
                + "?token=" + otp;

        return baseTemplate(
                "Reset your password",
                """
                <p style="margin:0 0 16px;color:#374151;font-size:16px;">
                    Hi <strong>%s</strong>,
                </p>
                <p style="margin:0 0 24px;color:#374151;font-size:15px;">
                    We received a request to reset your Ziboto password.
                    Use the code below or click the button — it expires in
                    <strong>5 minutes</strong>.
                </p>
                %s
                <p style="margin:24px 0;text-align:center;">
                    <a href="%s"
                       style="display:inline-block;background:#4f46e5;color:#ffffff;
                              text-decoration:none;padding:12px 32px;border-radius:6px;
                              font-size:15px;font-weight:600;">
                        Reset my password
                    </a>
                </p>
                <p style="margin:0 0 8px;color:#6b7280;font-size:13px;">
                    If you did not request a password reset, you can safely ignore this email.
                    Your password will not change.
                </p>
                """.formatted(escapeHtml(username), otpBox(otp), resetUrl)
        );
    }

    private String buildWelcomeEmail(String username) {
        return baseTemplate(
                "Welcome to Ziboto!",
                """
                <p style="margin:0 0 16px;color:#374151;font-size:16px;">
                    Hi <strong>%s</strong>,
                </p>
                <p style="margin:0 0 24px;color:#374151;font-size:15px;">
                    Your email has been verified and your Ziboto account is ready.
                    You now have <strong>5 GB</strong> of cloud storage at your disposal.
                </p>
                <p style="margin:0 0 8px;color:#6b7280;font-size:13px;">
                    Questions? Reply to this email or contact us at
                    <a href="mailto:%s" style="color:#4f46e5;">%s</a>.
                </p>
                """.formatted(escapeHtml(username),
                              escapeHtml(emailProps.getSupportEmail()),
                              escapeHtml(emailProps.getSupportEmail()))
        );
    }

    /**
     * Renders a visually prominent OTP code box consistent across all email types.
     */
    private String otpBox(String otp) {
        // Space out individual digits for readability
        String spaced = String.join(" ", otp.split(""));
        return """
               <div style="margin:24px auto;text-align:center;">
                   <span style="display:inline-block;background:#f3f4f6;border:1px solid #e5e7eb;
                                border-radius:8px;padding:16px 32px;font-size:32px;font-weight:700;
                                letter-spacing:8px;color:#111827;font-family:monospace;">
                       %s
                   </span>
               </div>
               """.formatted(spaced);
    }

    /**
     * Wraps the provided {@code body} fragment in a responsive email shell.
     *
     * @param title  short heading shown inside the email card
     * @param body   pre-built HTML body content
     */
    private String baseTemplate(String title, String body) {
        return """
               <!DOCTYPE html>
               <html lang="en">
               <head>
                   <meta charset="UTF-8"/>
                   <meta name="viewport" content="width=device-width,initial-scale=1"/>
                   <title>%s</title>
               </head>
               <body style="margin:0;padding:0;background:#f9fafb;font-family:-apple-system,BlinkMacSystemFont,
                            'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;">
                   <table width="100%%" cellpadding="0" cellspacing="0" role="presentation"
                          style="background:#f9fafb;padding:40px 0;">
                       <tr>
                           <td align="center">
                               <table width="560" cellpadding="0" cellspacing="0" role="presentation"
                                      style="background:#ffffff;border-radius:8px;
                                             box-shadow:0 1px 3px rgba(0,0,0,.1);
                                             overflow:hidden;max-width:560px;width:100%%;">
                                   <!-- Header -->
                                   <tr>
                                       <td style="background:#4f46e5;padding:24px 32px;">
                                           <span style="color:#ffffff;font-size:20px;font-weight:700;
                                                        letter-spacing:-0.3px;">Ziboto</span>
                                       </td>
                                   </tr>
                                   <!-- Body -->
                                   <tr>
                                       <td style="padding:32px;">
                                           <h2 style="margin:0 0 24px;color:#111827;font-size:20px;font-weight:700;">
                                               %s
                                           </h2>
                                           %s
                                       </td>
                                   </tr>
                                   <!-- Footer -->
                                   <tr>
                                       <td style="background:#f9fafb;border-top:1px solid #e5e7eb;
                                                  padding:16px 32px;text-align:center;">
                                           <p style="margin:0;color:#9ca3af;font-size:12px;">
                                               © 2026 Ziboto · Cloud-native object storage
                                           </p>
                                       </td>
                                   </tr>
                               </table>
                           </td>
                       </tr>
                   </table>
               </body>
               </html>
               """.formatted(escapeHtml(title), escapeHtml(title), body);
    }

    /** Minimal HTML escaping for user-supplied values inserted into templates. */
    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
