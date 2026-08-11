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

import com.ziboto.backend.share.entity.FileShare;
import com.ziboto.backend.share.enums.ShareStatus;

/**
 * Repository for FileShare entities.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface FileShareRepository extends JpaRepository<FileShare, UUID> {
    
    /**
     * Find all shares for a specific file.
     */
    List<FileShare> findByFileId(UUID fileId);
    
    /**
     * Find specific share for a file and user.
     */
    Optional<FileShare> findByFileIdAndSharedWithUserId(UUID fileId, Long userId);
    
    /**
     * Find all files shared BY a user (as owner).
     */
    Page<FileShare> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    
    /**
     * Find all files shared WITH a user (as recipient).
     */
    Page<FileShare> findBySharedWithUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find files shared with user filtered by status.
     */
    Page<FileShare> findBySharedWithUserIdAndStatusOrderByCreatedAtDesc(
        Long userId, ShareStatus status, Pageable pageable);
    
    /**
     * Count pending share requests for a user.
     */
    long countBySharedWithUserIdAndStatus(Long userId, ShareStatus status);
    
    /**
     * Check if file is shared with specific user (any status except DECLINED).
     */
    @Query("SELECT CASE WHEN COUNT(fs) > 0 THEN true ELSE false END " +
           "FROM FileShare fs " +
           "WHERE fs.fileId = :fileId " +
           "AND fs.sharedWithUserId = :userId " +
           "AND fs.status != 'DECLINED'")
    boolean existsActiveShareForFileAndUser(@Param("fileId") UUID fileId, @Param("userId") Long userId);
    
    /**
     * Find active (accepted and not expired) share for file and user.
     */
    @Query("SELECT fs FROM FileShare fs " +
           "WHERE fs.fileId = :fileId " +
           "AND fs.sharedWithUserId = :userId " +
           "AND fs.status = 'ACCEPTED' " +
           "AND (fs.expiresAt IS NULL OR fs.expiresAt > CURRENT_TIMESTAMP)")
    Optional<FileShare> findActiveShareForFileAndUser(@Param("fileId") UUID fileId, @Param("userId") Long userId);
    
    /**
     * Find all expired shares that haven't been marked as EXPIRED yet.
     */
    @Query("SELECT fs FROM FileShare fs " +
           "WHERE fs.expiresAt IS NOT NULL " +
           "AND fs.expiresAt < CURRENT_TIMESTAMP " +
           "AND fs.status = 'ACCEPTED'")
    List<FileShare> findExpiredShares();
    
    /**
     * Delete all shares for a file (when file is deleted).
     */
    void deleteByFileId(UUID fileId);
}
