package com.ziboto.backend.auth.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ziboto.backend.cache.RedisService;
import com.ziboto.backend.config.properties.RedisProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages pending (pre-verification) registration data in Redis.
 *
 * <p>When a user submits the registration form we do <em>not</em> create a row
 * in the {@code users} table immediately.  Instead, all registration fields are
 * serialised into a Redis hash that expires at the same time as the email-
 * verification OTP.  The DB row is only created once the user successfully
 * verifies their email address (see {@link AuthServiceImpl#verifyEmail}).</p>
 *
 * <p>This eliminates the "username/email squatting" problem: if a user abandons
 * the flow before verifying, the Redis key simply expires and the
 * username/email become available to anyone else.</p>
 *
 * <h2>Redis key schema</h2>
 * <pre>
 *   pending_reg:{email}            →  Hash
 *       username      →  String
 *       passwordHash  →  String  (BCrypt)
 *       email         →  String
 *       firstName     →  String  (optional)
 *       lastName      →  String  (optional)
 *
 *   pending_reg_username:{username} →  String (email)   [reverse-lookup index]
 * </pre>
 *
 * <p>Both keys share the same TTL ({@code app.redis.otp.ttl-minutes}) so they
 * expire together with the OTP.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingRegistrationCacheService {

    private static final String KEY_PREFIX          = "pending_reg:";
    private static final String USERNAME_IDX_PREFIX = "pending_reg_username:";

    // Hash field names
    private static final String F_USERNAME      = "username";
    private static final String F_PASSWORD_HASH = "passwordHash";
    private static final String F_EMAIL         = "email";
    private static final String F_FIRST_NAME    = "firstName";
    private static final String F_LAST_NAME     = "lastName";

    private final RedisService redisService;
    private final RedisProperties redisProperties;

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Persist all registration fields for {@code email} in Redis and record a
     * username → email reverse-lookup index.
     *
     * <p>Any previous pending entry for the same email is silently overwritten
     * (e.g. user re-registers before the first OTP expires).</p>
     *
     * @param email        canonical (lower-cased, trimmed) email address
     * @param username     chosen username (already validated)
     * @param passwordHash BCrypt-encoded password
     * @param firstName    optional first name
     * @param lastName     optional last name
     */
    public void save(String email,
                     String username,
                     String passwordHash,
                     String firstName,
                     String lastName) {

        String key      = buildKey(email);
        String usernameIdxKey = buildUsernameIdxKey(username);
        Duration ttl    = Duration.ofMinutes(redisProperties.getOtp().getTtlMinutes());

        Map<String, Object> data = new HashMap<>();
        data.put(F_USERNAME,      username);
        data.put(F_PASSWORD_HASH, passwordHash);
        data.put(F_EMAIL,         email);
        if (firstName != null) data.put(F_FIRST_NAME, firstName);
        if (lastName  != null) data.put(F_LAST_NAME,  lastName);

        redisService.hashSetAll(key, data);
        redisService.expire(key, ttl);

        // Reverse index: username → email (so callers can check username collisions)
        redisService.set(usernameIdxKey, email, ttl);

        log.debug("Saved pending registration for email: {} username: {} (TTL {} min)",
                email, username, redisProperties.getOtp().getTtlMinutes());
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Return {@code true} if a non-expired pending entry exists for {@code email}.
     */
    public boolean exists(String email) {
        return redisService.exists(buildKey(email));
    }

    /**
     * Return the username reserved in the pending entry for {@code email},
     * or {@code null} if the entry has expired / never existed.
     */
    public String getUsername(String email) {
        Object val = redisService.hashGet(buildKey(email), F_USERNAME);
        return val != null ? val.toString() : null;
    }

    /**
     * Return the email address of a pending registration that has reserved
     * {@code username}, or {@code null} if no such entry exists.
     *
     * <p>Used during new registrations to detect username collisions with
     * in-flight (unverified) sign-ups.</p>
     */
    public String findEmailByUsername(String username) {
        Object val = redisService.get(buildUsernameIdxKey(username));
        return val != null ? val.toString() : null;
    }

    /**
     * Load a complete snapshot of the pending registration fields.
     *
     * @param email canonical email
     * @return populated {@link PendingRegistration}, or {@code null} if
     *         the entry is absent/expired
     */
    public PendingRegistration load(String email) {
        String key = buildKey(email);
        Map<String, Object> data = redisService.hashGetAll(key);

        if (data == null || !data.containsKey(F_USERNAME)) {
            return null;
        }

        return PendingRegistration.builder()
                .email(email)
                .username(str(data, F_USERNAME))
                .passwordHash(str(data, F_PASSWORD_HASH))
                .firstName(str(data, F_FIRST_NAME))
                .lastName(str(data, F_LAST_NAME))
                .build();
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /**
     * Remove the pending entry for {@code email} (called after successful
     * verification so the key doesn't linger until natural expiry).
     * Also removes the username reverse-lookup index.
     */
    public void delete(String email) {
        // Load username before deleting the main record so we can clean up the index
        String username = getUsername(email);

        redisService.delete(buildKey(email));
        if (username != null) {
            redisService.delete(buildUsernameIdxKey(username));
        }

        log.debug("Removed pending registration for email: {}", email);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildKey(String email) {
        return KEY_PREFIX + email;
    }

    private String buildUsernameIdxKey(String username) {
        return USERNAME_IDX_PREFIX + username.toLowerCase();
    }

    private static String str(Map<String, Object> map, String field) {
        Object val = map.get(field);
        return val != null ? val.toString() : null;
    }

    // -------------------------------------------------------------------------
    // Value object
    // -------------------------------------------------------------------------

    /**
     * Immutable snapshot of a pending registration entry read from Redis.
     */
    @lombok.Value
    @lombok.Builder
    public static class PendingRegistration {
        String email;
        String username;
        String passwordHash;
        String firstName;
        String lastName;
    }
}
