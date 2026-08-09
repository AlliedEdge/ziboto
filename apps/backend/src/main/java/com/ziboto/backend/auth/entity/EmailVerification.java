package com.ziboto.backend.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ziboto.backend.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks the lifecycle of an email-verification or password-reset OTP request.
 *
 * <p>The actual 6-digit OTP value lives in Redis (via {@code OtpCacheService})
 * and expires automatically according to the configured TTL.  This entity exists
 * so the API can:</p>
 * <ul>
 *   <li>tell the caller when their last OTP was issued (rate-limit UX)</li>
 *   <li>mark a token as {@code used} once consumed (prevent replay if Redis
 *       somehow still has the key)</li>
 *   <li>provide an audit trail independent of Redis eviction</li>
 * </ul>
 *
 * <p>One row is created every time an OTP is sent.  Old rows for the same
 * (user, type) pair are soft-superseded — only the latest non-used row is
 * considered active in queries.</p>
 */
@Entity
@Table(
    name = "email_verifications",
    indexes = {
        @Index(name = "idx_ev_user_type",  columnList = "user_id, type"),
        @Index(name = "idx_ev_email_type", columnList = "email, type"),
        @Index(name = "idx_ev_expires_at", columnList = "expiresAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The user this verification belongs to.
     *
     * <p>Nullable for the initial registration flow: the {@code User} row is not
     * written to the DB until the OTP is successfully verified, so there is no
     * FK target yet.  Once the account is activated, this field is populated on
     * the (now-used) record during the {@code verifyEmail} transaction.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * Denormalised email snapshot.
     * Stored here so the record remains useful even if the user later changes
     * their address, and to allow lookups before the user object is loaded.
     */
    @Column(nullable = false, length = 100)
    private String email;

    /** Discriminates between EMAIL_VERIFICATION and PASSWORD_RESET OTPs. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationType type;

    /** {@code true} once the OTP has been successfully consumed. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Hard expiry independent of Redis TTL.
     * Must be set to {@code createdAt + otpTtlMinutes} so the two stores stay
     * in sync.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Populated when {@link #used} is set to {@code true}. */
    @Column
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (used == null) {
            used = false;
        }
    }

    // -------------------------------------------------------------------------
    // Convenience helpers
    // -------------------------------------------------------------------------

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !used && !isExpired();
    }

    /**
     * OTP purpose — mirrors {@code OtpCacheService.OtpPurpose} for the two
     * email-related purposes.
     */
    public enum VerificationType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }
}
