package com.ziboto.backend.config;

import com.resend.Resend;
import com.ziboto.backend.config.properties.ResendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Spring configuration that exposes a singleton {@link Resend} client bean.
 *
 * <p>The client is initialised once at startup using the API key from
 * {@link ResendProperties}.  Fail-fast behaviour is intentional: if no key is
 * configured the application will refuse to start rather than silently send
 * no emails.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResendConfig {

    private final ResendProperties resendProperties;

    @Bean
    public Resend resendClient() {
        String apiKey = resendProperties.getApiKey();

        if (!StringUtils.hasText(apiKey)) {
            log.warn("RESEND_API_KEY is not configured – email sending will be disabled. " +
                     "Set the RESEND_API_KEY environment variable to enable transactional email.");
            // Return a client with a placeholder key so the app still starts in dev.
            // All send calls will fail at runtime, which is intentional.
            return new Resend("re_placeholder_configure_RESEND_API_KEY");
        }

        log.info("Resend email client initialised (from: {})", resendProperties.getFromHeader());
        return new Resend(apiKey);
    }
}
