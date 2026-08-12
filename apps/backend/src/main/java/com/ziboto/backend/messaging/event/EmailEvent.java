package com.ziboto.backend.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for async email sending via RabbitMQ.
 * 
 * <p>Decouples email sending from HTTP request processing,
 * improving response times and reliability.</p>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {
    
    /**
     * Event unique identifier.
     */
    private UUID eventId;
    
    /**
     * Email type (verification, password-reset, welcome, etc).
     */
    private EmailType emailType;
    
    /**
     * Recipient email address.
     */
    private String toEmail;
    
    /**
     * Recipient name (for personalization).
     */
    private String recipientName;
    
    /**
     * Email subject.
     */
    private String subject;
    
    /**
     * OTP code (for verification/reset emails).
     */
    private String otpCode;
    
    /**
     * Additional context data (JSON string).
     */
    private String contextData;
    
    /**
     * Event timestamp.
     */
    private LocalDateTime timestamp;
    
    /**
     * Retry count (for failed deliveries).
     */
    private Integer retryCount;
    
    /**
     * Email types supported by the system.
     */
    public enum EmailType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        WELCOME,
        FILE_SHARED,
        STORAGE_QUOTA_WARNING,
        ACCOUNT_LOCKED,
        PASSWORD_CHANGED
    }
}
