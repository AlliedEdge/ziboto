package com.ziboto.backend.duplicate.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.duplicate.entity.DuplicateFile;

/**
 * Repository for DuplicateFile entity.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface DuplicateFileRepository extends JpaRepository<DuplicateFile, UUID> {
    
    /**
     * Find all files in a duplicate group.
     */
    @Query("SELECT df FROM DuplicateFile df WHERE df.groupId = :groupId ORDER BY df.uploadedAt ASC")
    List<DuplicateFile> findByGroupId(@Param("groupId") UUID groupId);
    
    /**
     * Find duplicate files for a user.
     */
    @Query("SELECT df FROM DuplicateFile df WHERE df.userId = :userId ORDER BY df.uploadedAt DESC")
    List<DuplicateFile> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find files marked for deletion in a group.
     */
    @Query("SELECT df FROM DuplicateFile df WHERE df.groupId = :groupId AND df.markedForDeletion = true")
    List<DuplicateFile> findMarkedForDeletionByGroupId(@Param("groupId") UUID groupId);
    
    /**
     * Find original file in a group.
     */
    @Query("SELECT df FROM DuplicateFile df WHERE df.groupId = :groupId AND df.isOriginal = true")
    DuplicateFile findOriginalByGroupId(@Param("groupId") UUID groupId);
    
    /**
     * Count duplicate files for a user.
     */
    @Query("SELECT COUNT(df) FROM DuplicateFile df WHERE df.userId = :userId AND df.isOriginal = false")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * Mark duplicates for deletion (keep original).
     */
    @Modifying
    @Query("UPDATE DuplicateFile df SET df.markedForDeletion = true WHERE df.groupId = :groupId AND df.isOriginal = false")
    int markDuplicatesForDeletion(@Param("groupId") UUID groupId);
    
    /**
     * Unmark all files in a group.
     */
    @Modifying
    @Query("UPDATE DuplicateFile df SET df.markedForDeletion = false WHERE df.groupId = :groupId")
    int unmarkAllInGroup(@Param("groupId") UUID groupId);
    
    /**
     * Delete entries by group ID.
     */
    @Modifying
    @Query("DELETE FROM DuplicateFile df WHERE df.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") UUID groupId);
}
