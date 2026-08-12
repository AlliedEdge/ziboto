package com.ziboto.backend.duplicate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.duplicate.entity.DuplicateGroup;

/**
 * Repository for DuplicateGroup entity.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface DuplicateGroupRepository extends JpaRepository<DuplicateGroup, UUID> {
    
    /**
     * Find duplicate group by content hash.
     */
    Optional<DuplicateGroup> findByContentHash(String contentHash);
    
    /**
     * Check if duplicate group exists for hash.
     */
    boolean existsByContentHash(String contentHash);
    
    /**
     * Find all unreviewed duplicate groups.
     */
    @Query("SELECT dg FROM DuplicateGroup dg WHERE dg.reviewed = false ORDER BY dg.potentialSavingsBytes DESC")
    Page<DuplicateGroup> findUnreviewed(Pageable pageable);
    
    /**
     * Find all reviewed duplicate groups.
     */
    @Query("SELECT dg FROM DuplicateGroup dg WHERE dg.reviewed = true ORDER BY dg.reviewedAt DESC")
    Page<DuplicateGroup> findReviewed(Pageable pageable);
    
    /**
     * Find duplicate groups ordered by potential savings.
     */
    @Query("SELECT dg FROM DuplicateGroup dg ORDER BY dg.potentialSavingsBytes DESC")
    Page<DuplicateGroup> findAllOrderedBySavings(Pageable pageable);
    
    /**
     * Find duplicate groups by mime type.
     */
    @Query("SELECT dg FROM DuplicateGroup dg WHERE dg.mimeType = :mimeType ORDER BY dg.potentialSavingsBytes DESC")
    List<DuplicateGroup> findByMimeType(@Param("mimeType") String mimeType);
    
    /**
     * Calculate total potential savings.
     */
    @Query("SELECT COALESCE(SUM(dg.potentialSavingsBytes), 0) FROM DuplicateGroup dg WHERE dg.reviewed = false")
    Long calculateTotalPotentialSavings();
    
    /**
     * Count unreviewed duplicate groups.
     */
    @Query("SELECT COUNT(dg) FROM DuplicateGroup dg WHERE dg.reviewed = false")
    Long countUnreviewed();
    
    /**
     * Find duplicate groups with savings above threshold.
     */
    @Query("SELECT dg FROM DuplicateGroup dg WHERE dg.potentialSavingsBytes > :minSavings ORDER BY dg.potentialSavingsBytes DESC")
    List<DuplicateGroup> findWithSavingsAbove(@Param("minSavings") Long minSavings);
}
