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

import com.ziboto.backend.share.entity.FolderShare;
import com.ziboto.backend.share.enums.ShareStatus;

/**
 * Repository for FolderShare entities.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface FolderShareRepository extends JpaRepository<FolderShare, UUID> {
    
    /**
     * Find all shares for a specific folder.
     */
    List<FolderShare> findByFolderId(UUID folderId);
    
    /**
     * Find specific share for a folder and user.
     */
    Optional<FolderShare> findByFolderIdAndSharedWithUserId(UUID folderId, Long userId);
    
    /**
     * Find all folders shared BY a user (as owner).
     */
    Page<FolderShare> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    
    /**
     * Find all folders shared WITH a user (as recipient).
     */
    Page<FolderShare> findBySharedWithUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find folders shared with user filtered by status.
     */
    Page<FolderShare> findBySharedWithUserIdAndStatusOrderByCreatedAtDesc(
        Long userId, ShareStatus status, Pageable pageable);
    
    /**
     * Count pending share requests for a user.
     */
    long countBySharedWithUserIdAndStatus(Long userId, ShareStatus status);
    
    /**
     * Find active (accepted and not expired) share for folder and user.
     */
    @Query("SELECT fs FROM FolderShare fs " +
           "WHERE fs.folderId = :folderId " +
           "AND fs.sharedWithUserId = :userId " +
           "AND fs.status = 'ACCEPTED' " +
           "AND (fs.expiresAt IS NULL OR fs.expiresAt > CURRENT_TIMESTAMP)")
    Optional<FolderShare> findActiveShareForFolderAndUser(
        @Param("folderId") UUID folderId, @Param("userId") Long userId);
    
    /**
     * Check if folder is shared with specific user (active share).
     */
    @Query("SELECT CASE WHEN COUNT(fs) > 0 THEN true ELSE false END " +
           "FROM FolderShare fs " +
           "WHERE fs.folderId = :folderId " +
           "AND fs.sharedWithUserId = :userId " +
           "AND fs.status = 'ACCEPTED' " +
           "AND (fs.expiresAt IS NULL OR fs.expiresAt > CURRENT_TIMESTAMP)")
    boolean hasActiveShareForFolderAndUser(@Param("folderId") UUID folderId, @Param("userId") Long userId);
    
    /**
     * Find all expired shares.
     */
    @Query("SELECT fs FROM FolderShare fs " +
           "WHERE fs.expiresAt IS NOT NULL " +
           "AND fs.expiresAt < CURRENT_TIMESTAMP " +
           "AND fs.status = 'ACCEPTED'")
    List<FolderShare> findExpiredShares();
    
    /**
     * Delete all shares for a folder.
     */
    void deleteByFolderId(UUID folderId);
}
