package com.ziboto.backend.comment.repository;

import com.ziboto.backend.comment.entity.FileComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for FileComment entity.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Repository
public interface FileCommentRepository extends JpaRepository<FileComment, UUID> {
    
    /**
     * Find all comments for a file (paginated).
     */
    Page<FileComment> findByFileIdOrderByCreatedAtDesc(UUID fileId, Pageable pageable);
    
    /**
     * Find root comments (no parent) for a file.
     */
    Page<FileComment> findByFileIdAndParentIdIsNullOrderByCreatedAtDesc(UUID fileId, Pageable pageable);
    
    /**
     * Find replies to a comment.
     */
    List<FileComment> findByParentIdOrderByCreatedAtAsc(UUID parentId);
    
    /**
     * Count comments for a file.
     */
    long countByFileId(UUID fileId);
    
    /**
     * Count replies to a comment.
     */
    long countByParentId(UUID parentId);
    
    /**
     * Find comments by user.
     */
    Page<FileComment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Delete all comments for a file.
     */
    void deleteByFileId(UUID fileId);
    
    /**
     * Check if user has permission to edit comment.
     */
    boolean existsByIdAndUserId(UUID id, Long userId);
    
    /**
     * Find comments mentioning a user.
     */
    @Query("SELECT c FROM FileComment c WHERE :userId MEMBER OF c.mentions ORDER BY c.createdAt DESC")
    Page<FileComment> findCommentsMentioningUser(@Param("userId") Long userId, Pageable pageable);
}
