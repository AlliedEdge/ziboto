package com.ziboto.backend.share.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.share.entity.ShareLink;
import com.ziboto.backend.share.enums.ShareLinkStatus;

/**
 * Repository for ShareLink entities.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {
    
    /**
     * Find share link by token.
     */
    Optional<ShareLink> findByToken(String token);
    
    /**
     * Find all share links for a file.
     */
    List<ShareLink> findByFileIdOrderByCreatedAtDesc(UUID fileId);
    
    /**
     * Find all share links created by a user.
     */
    Page<ShareLink> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    
    /**
     * Find share links by status.
     */
    Page<ShareLink> findByOwnerIdAndStatusOrderByCreatedAtDesc(
        Long ownerId, ShareLinkStatus status, Pageable pageable);
    
    /**
     * Count active share links for a file.
     */
    long countByFileIdAndStatus(UUID fileId, ShareLinkStatus status);
    
    /**
     * Check if token exists.
     */
    boolean existsByToken(String token);
    
    /**
     * Find expired links that haven't been marked as EXPIRED.
     */
    @Query("SELECT sl FROM ShareLink sl " +
           "WHERE sl.expiresAt IS NOT NULL " +
           "AND sl.expiresAt < CURRENT_TIMESTAMP " +
           "AND sl.status = 'ACTIVE'")
    List<ShareLink> findExpiredLinks();
    
    /**
     * Find links that reached download limit.
     */
    @Query("SELECT sl FROM ShareLink sl " +
           "WHERE sl.maxDownloads IS NOT NULL " +
           "AND sl.downloadCount >= sl.maxDownloads " +
           "AND sl.status = 'ACTIVE'")
    List<ShareLink> findLinksReachedDownloadLimit();
    
    /**
     * Find active share link by token.
     */
    @Query("SELECT sl FROM ShareLink sl " +
           "WHERE sl.token = :token " +
           "AND sl.status = 'ACTIVE' " +
           "AND (sl.expiresAt IS NULL OR sl.expiresAt > CURRENT_TIMESTAMP) " +
           "AND (sl.maxDownloads IS NULL OR sl.downloadCount < sl.maxDownloads)")
    Optional<ShareLink> findActiveShareLinkByToken(@Param("token") String token);
    
    /**
     * Delete all share links for a file.
     */
    void deleteByFileId(UUID fileId);
}
