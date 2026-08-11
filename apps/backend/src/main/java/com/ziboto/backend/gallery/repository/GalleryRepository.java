package com.ziboto.backend.gallery.repository;

import com.ziboto.backend.gallery.entity.Gallery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Gallery entity.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Repository
public interface GalleryRepository extends JpaRepository<Gallery, UUID> {
    
    /**
     * Find gallery by slug.
     */
    Optional<Gallery> findBySlug(String slug);
    
    /**
     * Check if slug exists.
     */
    boolean existsBySlug(String slug);
    
    /**
     * Find all galleries for a user.
     */
    Page<Gallery> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find public galleries.
     */
    Page<Gallery> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Increment view count.
     */
    @Modifying
    @Query("UPDATE Gallery g SET g.viewCount = g.viewCount + 1 WHERE g.id = :galleryId")
    void incrementViewCount(@Param("galleryId") UUID galleryId);
    
    /**
     * Count galleries for a user.
     */
    long countByUserId(Long userId);
}
