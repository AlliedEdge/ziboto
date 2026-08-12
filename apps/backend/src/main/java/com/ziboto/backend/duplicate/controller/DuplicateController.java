package com.ziboto.backend.duplicate.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.ziboto.backend.duplicate.dto.DuplicateGroupResponse;
import com.ziboto.backend.duplicate.dto.DuplicateStatsResponse;
import com.ziboto.backend.duplicate.service.DuplicateDetectionService;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for duplicate file detection and management.
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>POST /api/v1/duplicates/scan - Scan for duplicates</li>
 *   <li>GET /api/v1/duplicates - Get all duplicate groups</li>
 *   <li>GET /api/v1/duplicates/unreviewed - Get unreviewed groups</li>
 *   <li>GET /api/v1/duplicates/{groupId} - Get specific group</li>
 *   <li>GET /api/v1/duplicates/my - Get user's duplicate groups</li>
 *   <li>GET /api/v1/duplicates/stats - Get statistics</li>
 *   <li>POST /api/v1/duplicates/{groupId}/mark-for-deletion - Mark duplicates</li>
 *   <li>POST /api/v1/duplicates/{groupId}/delete - Delete marked files</li>
 *   <li>POST /api/v1/duplicates/{groupId}/keep-all - Keep all</li>
 *   <li>POST /api/v1/duplicates/{groupId}/keep/{fileId} - Keep specific file</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/duplicates")
@RequiredArgsConstructor
public class DuplicateController {
    
    private final DuplicateDetectionService duplicateService;
    private final UserRepository userRepository;
    
    // -------------------------------------------------------------------------
    // Duplicate Detection
    // -------------------------------------------------------------------------
    
    /**
     * Scan all files for duplicates.
     */
    @PostMapping("/scan")
    public ResponseEntity<Void> scanForDuplicates(@AuthenticationPrincipal UserDetails userDetails) {
        duplicateService.scanAllDuplicates();
        return ResponseEntity.ok().build();
    }
    
    // -------------------------------------------------------------------------
    // Duplicate Groups
    // -------------------------------------------------------------------------
    
    /**
     * Get all duplicate groups (ordered by potential savings).
     */
    @GetMapping
    public ResponseEntity<Page<DuplicateGroupResponse>> getAllDuplicateGroups(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Page<DuplicateGroupResponse> groups = duplicateService.getAllDuplicateGroups(pageable);
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Get unreviewed duplicate groups.
     */
    @GetMapping("/unreviewed")
    public ResponseEntity<Page<DuplicateGroupResponse>> getUnreviewedGroups(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Page<DuplicateGroupResponse> groups = duplicateService.getUnreviewedGroups(pageable);
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Get specific duplicate group.
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<DuplicateGroupResponse> getDuplicateGroup(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        DuplicateGroupResponse group = duplicateService.getDuplicateGroup(groupId);
        return ResponseEntity.ok(group);
    }
    
    /**
     * Get current user's duplicate groups.
     */
    @GetMapping("/my")
    public ResponseEntity<List<DuplicateGroupResponse>> getMyDuplicateGroups(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        List<DuplicateGroupResponse> groups = duplicateService.getUserDuplicateGroups(user.getId());
        
        return ResponseEntity.ok(groups);
    }
    
    // -------------------------------------------------------------------------
    // Statistics
    // -------------------------------------------------------------------------
    
    /**
     * Get duplicate statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<DuplicateStatsResponse> getDuplicateStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        DuplicateStatsResponse stats = duplicateService.getDuplicateStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get user's duplicate statistics.
     */
    @GetMapping("/stats/my")
    public ResponseEntity<DuplicateStatsResponse> getMyDuplicateStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        DuplicateStatsResponse stats = duplicateService.getUserDuplicateStats(user.getId());
        
        return ResponseEntity.ok(stats);
    }
    
    // -------------------------------------------------------------------------
    // Duplicate Management
    // -------------------------------------------------------------------------
    
    /**
     * Mark duplicates for deletion (keep original).
     */
    @PostMapping("/{groupId}/mark-for-deletion")
    public ResponseEntity<Void> markDuplicatesForDeletion(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        duplicateService.markDuplicatesForDeletion(groupId, user.getUsername());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Delete marked duplicate files.
     */
    @PostMapping("/{groupId}/delete")
    public ResponseEntity<Integer> deleteMarkedDuplicates(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        int deleted = duplicateService.deleteMarkedDuplicates(groupId, user.getId());
        
        return ResponseEntity.ok(deleted);
    }
    
    /**
     * Keep all duplicates (don't delete).
     */
    @PostMapping("/{groupId}/keep-all")
    public ResponseEntity<Void> keepAllDuplicates(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        duplicateService.keepAllDuplicates(groupId, user.getUsername());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Keep specific file and delete others.
     */
    @PostMapping("/{groupId}/keep/{fileId}")
    public ResponseEntity<Void> keepSpecificFile(
            @PathVariable UUID groupId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        duplicateService.keepSpecificFile(groupId, fileId, user.getUsername());
        
        return ResponseEntity.ok().build();
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
