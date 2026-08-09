package com.ziboto.backend.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.auth.entity.EmailVerification;
import com.ziboto.backend.auth.entity.EmailVerification.VerificationType;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    /**
     * Find the most-recently created, still-active (not used, not expired) record
     * for a given (email, type) pair.
     *
     * <p>Used during OTP submission to check whether there is a pending request
     * before the user object has been loaded.</p>
     */
    @Query("""
           SELECT ev FROM EmailVerification ev
           WHERE ev.email = :email
             AND ev.type  = :type
             AND ev.used  = false
             AND ev.expiresAt > :now
           ORDER BY ev.createdAt DESC
           """)
    Optional<EmailVerification> findLatestActive(
            @Param("email") String email,
            @Param("type")  VerificationType type,
            @Param("now")   LocalDateTime now);

    /**
     * Find the most-recently created, still-active record for a given (userId, type) pair.
     *
     * <p>Used to enforce rate-limiting: a new OTP should not be issued if an
     * active one already exists and was created less than N minutes ago.</p>
     */
    @Query("""
           SELECT ev FROM EmailVerification ev
           WHERE ev.user.id = :userId
             AND ev.type    = :type
             AND ev.used    = false
             AND ev.expiresAt > :now
           ORDER BY ev.createdAt DESC
           """)
    Optional<EmailVerification> findLatestActiveByUserId(
            @Param("userId") Long userId,
            @Param("type")   VerificationType type,
            @Param("now")    LocalDateTime now);

    /**
     * Bulk-invalidate all active records for a (userId, type) pair.
     *
     * <p>Called before issuing a new OTP so that only one active record exists
     * at a time, preventing replay of old codes.</p>
     */
    @Modifying
    @Query("""
           UPDATE EmailVerification ev
           SET ev.used = true, ev.usedAt = :now
           WHERE ev.user.id = :userId
             AND ev.type    = :type
             AND ev.used    = false
           """)
    void invalidateAllActive(
            @Param("userId") Long userId,
            @Param("type")   VerificationType type,
            @Param("now")    LocalDateTime now);

    /**
     * Bulk-invalidate all active records for a (email, type) pair.
     *
     * <p>Used for pre-verification registrations where the user row does not
     * exist yet and therefore no userId FK is available.</p>
     */
    @Modifying
    @Query("""
           UPDATE EmailVerification ev
           SET ev.used = true, ev.usedAt = :now
           WHERE ev.email = :email
             AND ev.type  = :type
             AND ev.used  = false
           """)
    void invalidateAllActiveByEmail(
            @Param("email") String email,
            @Param("type")  VerificationType type,
            @Param("now")   LocalDateTime now);

    /**
     * Delete expired records older than a given threshold.
     *
     * <p>Intended to be called by a scheduled cleanup job to prevent unbounded
     * table growth.</p>
     */
    @Modifying
    @Query("""
           DELETE FROM EmailVerification ev
           WHERE ev.expiresAt < :threshold
           """)
    int deleteExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
