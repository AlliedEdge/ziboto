package com.ziboto.backend.gallery.repository;

import com.ziboto.backend.gallery.entity.GalleryFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for GalleryFile entity.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Repository
public interface GalleryFileRepository extends JpaRepository<GalleryFile, UUID> {
    
    /**
     * Find all files in a gallery ordered by display order.
     */
    List<GalleryFile> findByGalleryIdOrderByDisplayOrderAsc(UUID galleryId);
    
    /**
     * Find a specific gallery-file combination.
     */
    Optional<GalleryFile> findByGalleryIdAndFileId(UUID galleryId, UUID fileId);
    
    /**
     * Check if file exists in gallery.
     */
    boolean existsByGalleryIdAndFileId(UUID galleryId, UUID fileId);
    
    /**
     * Count files in a gallery.
     */
    long countByGalleryId(UUID galleryId);
    
    /**
     * Delete all files from a gallery.
     */
    @Modifying
    @Query("DELETE FROM GalleryFile gf WHERE gf.galleryId = :galleryId")
    void deleteByGalleryId(@Param("galleryId") UUID galleryId);
    
    /**
     * Get max display order for a gallery.
     */
    @Query("SELECT COALESCE(MAX(gf.displayOrder), 0) FROM GalleryFile gf WHERE gf.galleryId = :galleryId")
    Integer getMaxDisplayOrder(@Param("galleryId") UUID galleryId);
}
