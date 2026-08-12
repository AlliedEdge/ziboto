package com.ziboto.backend.messaging.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.ziboto.backend.config.RabbitMQConfig;
import com.ziboto.backend.email.service.EmailService;
import com.ziboto.backend.messaging.event.EmailEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for email events.
 * 
 * <p>Processes async email sending requests, decoupling email delivery
 * from HTTP request processing for better performance and reliability.</p>
 * 
 * <p>Email sending failures are logged but don't crash the consumer.
 * The EmailService already handles Resend API failures gracefully.</p>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {
    
    private final EmailService emailService;
    
    /**
     * Process email event and send email via EmailService.
     * 
     * @param event email event
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleEmail(EmailEvent event) {
        log.info("Processing email event: type={}, to={}, eventId={}", 
                 event.getEmailType(), event.getToEmail(), event.getEventId());
        
        try {
            switch (event.getEmailType()) {
                case EMAIL_VERIFICATION:
                    emailService.sendEmailVerification(
                        event.getToEmail(),
                        event.getRecipientName(),
                        event.getOtpCode()
                    );
                    break;
                    
                case PASSWORD_RESET:
                    emailService.sendPasswordResetEmail(
                        event.getToEmail(),
                        event.getRecipientName(),
                        event.getOtpCode()
                    );
                    break;
                    
                case WELCOME:
                    emailService.sendWelcomeEmail(
                        event.getToEmail(),
                        event.getRecipientName()
                    );
                    break;
                    
                case FILE_SHARED:
                    // TODO V2: Implement file shared email
                    log.warn("FILE_SHARED email not yet implemented: to={}", event.getToEmail());
                    break;
                    
                case STORAGE_QUOTA_WARNING:
                    // TODO V2: Implement quota warning email
                    log.warn("STORAGE_QUOTA_WARNING email not yet implemented: to={}", event.getToEmail());
                    break;
                    
                case ACCOUNT_LOCKED:
                    // TODO V2: Implement account locked email
                    log.warn("ACCOUNT_LOCKED email not yet implemented: to={}", event.getToEmail());
                    break;
                    
                case PASSWORD_CHANGED:
                    // TODO V2: Implement password changed email
                    log.warn("PASSWORD_CHANGED email not yet implemented: to={}", event.getToEmail());
                    break;
                    
                default:
                    log.error("Unknown email type: {}", event.getEmailType());
            }
            
            log.info("Email event processed successfully: type={}, to={}", 
                     event.getEmailType(), event.getToEmail());
            
        } catch (Exception e) {
            log.error("Failed to process email event: type={}, to={}", 
                      event.getEmailType(), event.getToEmail(), e);
            
            // Check if we should retry
            if (event.getRetryCount() < 3) {
                log.info("Will retry email event: retryCount={}", event.getRetryCount());
                throw e; // Trigger RabbitMQ retry
            } else {
                log.error("Max retries exceeded for email event: type={}, to={}", 
                          event.getEmailType(), event.getToEmail());
                // Don't throw - let it go to DLQ
            }
        }
    }
}
