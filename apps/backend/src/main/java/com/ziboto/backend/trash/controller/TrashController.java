package com.ziboto.backend.trash.controller;

import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.trash.dto.TrashItemResponse;
import com.ziboto.backend.trash.dto.TrashSummaryResponse;
import com.ziboto.backend.trash.service.TrashService;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for trash bin operations.
 * 
 * @author Ziboto Team
 * @since V3
 */
@RestController
@RequestMapping("/api/v1/trash")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trash Bin", description = "Soft delete and recovery operations")
public class TrashController {
    
    private final TrashService trashService;
    private final UserRepository userRepository;
    
    /**
     * Get trash items for current user.
     * 
     * GET /api/v1/trash
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get trash items", description = "Get all items in trash bin")
    public ResponseEntity<ApiResponse<Page<TrashItemResponse>>> getTrash(
            Authentication authentication,
            @PageableDefault(size = 50) Pageable pageable) {
        
        Long userId = getUserId(authentication);
        log.info("Get trash request - userId: {}", userId);
        
        Page<TrashItemResponse> trash = trashService.getUserTrash(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(trash));
    }
    
    /**
     * Get trash summary statistics.
     * 
     * GET /api/v1/trash/summary
     */
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get trash summary", description = "Get trash statistics (count, size)")
    public ResponseEntity<ApiResponse<TrashSummaryResponse>> getTrashSummary(
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Get trash summary - userId: {}", userId);
        
        TrashSummaryResponse summary = trashService.getTrashSummary(userId);
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    /**
     * Restore file from trash.
     * 
     * POST /api/v1/trash/files/{fileId}/restore
     */
    @PostMapping("/files/{fileId}/restore")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Restore file", description = "Restore a file from trash")
    public ResponseEntity<ApiResponse<Void>> restoreFile(
            @PathVariable UUID fileId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Restore file request - fileId: {}, userId: {}", fileId, userId);
        
        trashService.restoreFile(fileId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("File restored successfully", null));
    }
    
    /**
     * Restore folder from trash.
     * 
     * POST /api/v1/trash/folders/{folderId}/restore
     */
    @PostMapping("/folders/{folderId}/restore")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Restore folder", description = "Restore a folder from trash")
    public ResponseEntity<ApiResponse<Void>> restoreFolder(
            @PathVariable UUID folderId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Restore folder request - folderId: {}, userId: {}", folderId, userId);
        
        trashService.restoreFolder(folderId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Folder restored successfully", null));
    }
    
    /**
     * Permanently delete file from trash.
     * 
     * DELETE /api/v1/trash/files/{fileId}
     */
    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete file permanently", description = "Permanently delete a file from trash")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteFile(
            @PathVariable UUID fileId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Permanent delete file request - fileId: {}, userId: {}", fileId, userId);
        
        trashService.permanentlyDeleteFile(fileId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("File permanently deleted", null));
    }
    
    /**
     * Empty entire trash.
     * 
     * DELETE /api/v1/trash/empty
     */
    @DeleteMapping("/empty")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Empty trash", description = "Permanently delete all items in trash")
    public ResponseEntity<ApiResponse<Void>> emptyTrash(Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Empty trash request - userId: {}", userId);
        
        trashService.emptyTrash(userId);
        
        return ResponseEntity.ok(ApiResponse.success("Trash emptied successfully", null));
    }
    
    /**
     * Extract user ID from authentication.
     */
    private Long getUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with username: " + username
                ));
        
        return user.getId();
    }
}
