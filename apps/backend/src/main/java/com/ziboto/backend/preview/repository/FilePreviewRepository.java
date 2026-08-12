package com.ziboto.backend.preview.repository;

import com.ziboto.backend.preview.entity.FilePreview;
import com.ziboto.backend.preview.enums.PreviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FilePreview entity.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Repository
public interface FilePreviewRepository extends JpaRepository<FilePreview, UUID> {
    
    /**
     * Find preview by file ID and preview type.
     */
    Optional<FilePreview> findByFileIdAndPreviewType(UUID fileId, PreviewType previewType);
    
    /**
     * Find all previews for a file.
     */
    List<FilePreview> findByFileId(UUID fileId);
    
    /**
     * Delete previews for a file.
     */
    @Modifying
    @Query("DELETE FROM FilePreview fp WHERE fp.fileId = :fileId")
    void deleteByFileId(@Param("fileId") UUID fileId);
    
    /**
     * Delete expired previews.
     */
    @Modifying
    @Query("DELETE FROM FilePreview fp WHERE fp.expiresAt IS NOT NULL AND fp.expiresAt < :now")
    int deleteExpiredPreviews(@Param("now") LocalDateTime now);
    
    /**
     * Check if preview exists for file and type.
     */
    boolean existsByFileIdAndPreviewType(UUID fileId, PreviewType previewType);
}
