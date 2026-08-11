package com.ziboto.backend.version.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.version.entity.FileVersion;

/**
 * Repository for FileVersion entity.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, UUID> {
    
    // -------------------------------------------------------------------------
    // Version History Queries
    // -------------------------------------------------------------------------
    
    /**
     * Get all versions for a file (ordered by version number descending).
     */
    @Query("SELECT v FROM FileVersion v WHERE v.fileId = :fileId ORDER BY v.versionNumber DESC")
    List<FileVersion> findByFileIdOrderByVersionNumberDesc(@Param("fileId") UUID fileId);
    
    /**
     * Get all versions for a file (paginated).
     */
    @Query("SELECT v FROM FileVersion v WHERE v.fileId = :fileId ORDER BY v.versionNumber DESC")
    Page<FileVersion> findByFileId(@Param("fileId") UUID fileId, Pageable pageable);
    
    /**
     * Get specific version by file ID and version number.
     */
    @Query("SELECT v FROM FileVersion v WHERE v.fileId = :fileId AND v.versionNumber = :versionNumber")
    Optional<FileVersion> findByFileIdAndVersionNumber(
            @Param("fileId") UUID fileId,
            @Param("versionNumber") Integer versionNumber);
    
    /**
     * Get latest version for a file.
     */
    @Query("SELECT v FROM FileVersion v WHERE v.fileId = :fileId ORDER BY v.versionNumber DESC LIMIT 1")
    Optional<FileVersion> findLatestVersionByFileId(@Param("fileId") UUID fileId);
    
    /**
     * Count versions for a file.
     */
    @Query("SELECT COUNT(v) FROM FileVersion v WHERE v.fileId = :fileId")
    Long countVersionsByFileId(@Param("fileId") UUID fileId);
    
    // -------------------------------------------------------------------------
    // User Queries
    // -------------------------------------------------------------------------
    
    /**
     * Get all versions by user (across all files).
     */
    @Query("SELECT v FROM FileVersion v WHERE v.userId = :userId ORDER BY v.createdAt DESC")
    Page<FileVersion> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Count total versions created by user.
     */
    @Query("SELECT COUNT(v) FROM FileVersion v WHERE v.userId = :userId")
    Long countVersionsByUserId(@Param("userId") Long userId);
    
    // -------------------------------------------------------------------------
    // Version Management Queries
    // -------------------------------------------------------------------------
    
    /**
     * Get versions by file ID that are older than specified date (for cleanup).
     */
    @Query("SELECT v FROM FileVersion v WHERE v.fileId = :fileId AND v.createdAt < :beforeDate ORDER BY v.createdAt ASC")
    List<FileVersion> findOldVersionsByFileId(
            @Param("fileId") UUID fileId,
            @Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Get all versions except the latest N for a file (for retention policy).
     */
    @Query(value = """
        SELECT v.* FROM file_versions v
        WHERE v.file_id = :fileId
        AND v.version_number NOT IN (
            SELECT version_number FROM file_versions
            WHERE file_id = :fileId
            ORDER BY version_number DESC
            LIMIT :keepCount
        )
        ORDER BY v.version_number ASC
        """, nativeQuery = true)
    List<FileVersion> findVersionsToDelete(
            @Param("fileId") UUID fileId,
            @Param("keepCount") int keepCount);
    
    /**
     * Delete old versions (for retention policy).
     */
    @Query("DELETE FROM FileVersion v WHERE v.fileId = :fileId AND v.versionNumber < :beforeVersion")
    void deleteOldVersions(
            @Param("fileId") UUID fileId,
            @Param("beforeVersion") Integer beforeVersion);
    
    // -------------------------------------------------------------------------
    // Deduplication Queries
    // -------------------------------------------------------------------------
    
    /**
     * Find versions with same content hash (for deduplication).
     */
    @Query("SELECT v FROM FileVersion v WHERE v.sha256Hash = :hash")
    List<FileVersion> findByHash(@Param("hash") String hash);
    
    /**
     * Check if content hash already exists (for deduplication).
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM FileVersion v WHERE v.sha256Hash = :hash")
    boolean existsByHash(@Param("hash") String hash);
    
    /**
     * Find duplicate content across all versions.
     */
    @Query("""
        SELECT v.sha256Hash, COUNT(v) as count
        FROM FileVersion v
        GROUP BY v.sha256Hash
        HAVING COUNT(v) > 1
        """)
    List<Object[]> findDuplicateHashes();
    
    // -------------------------------------------------------------------------
    // Version Tag Queries
    // -------------------------------------------------------------------------
    
    /**
     * Find versions by tag.
     */
    @Query("SELECT v FROM FileVersion v WHERE v.fileId = :fileId AND v.versionTag = :tag")
    Optional<FileVersion> findByFileIdAndTag(
            @Param("fileId") UUID fileId,
            @Param("tag") String tag);
    
    /**
     * Get all tagged versions for a file.
     */
    @Query("SELECT v FROM FileVersion v WHERE v.fileId = :fileId AND v.versionTag IS NOT NULL ORDER BY v.versionNumber DESC")
    List<FileVersion> findTaggedVersionsByFileId(@Param("fileId") UUID fileId);
    
    // -------------------------------------------------------------------------
    // Statistics Queries
    // -------------------------------------------------------------------------
    
    /**
     * Calculate total storage used by versions for a user.
     */
    @Query("SELECT SUM(v.fileSize) FROM FileVersion v WHERE v.userId = :userId")
    Long getTotalVersionStorageByUserId(@Param("userId") Long userId);
    
    /**
     * Calculate total storage used by versions for a file.
     */
    @Query("SELECT SUM(v.fileSize) FROM FileVersion v WHERE v.fileId = :fileId")
    Long getTotalVersionStorageByFileId(@Param("fileId") UUID fileId);
    
    /**
     * Get version count per file for a user.
     */
    @Query("""
        SELECT v.fileId, COUNT(v)
        FROM FileVersion v
        WHERE v.userId = :userId
        GROUP BY v.fileId
        """)
    List<Object[]> getVersionCountPerFile(@Param("userId") Long userId);
}
