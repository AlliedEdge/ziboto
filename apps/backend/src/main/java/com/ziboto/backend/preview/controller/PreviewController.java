package com.ziboto.backend.preview.controller;

import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.preview.dto.PreviewRequest;
import com.ziboto.backend.preview.dto.PreviewResponse;
import com.ziboto.backend.preview.dto.PreviewStatsResponse;
import com.ziboto.backend.preview.enums.PreviewType;
import com.ziboto.backend.preview.service.PreviewService;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for file preview generation and management.
 * 
 * @author Ziboto Team
 * @since V3
 */
@RestController
@RequestMapping("/api/v1/previews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Previews", description = "File preview generation and caching")
public class PreviewController {
    
    private final PreviewService previewService;
    private final UserRepository userRepository;
    
    /**
     * Generate or retrieve a preview for a file.
     * 
     * POST /api/v1/previews/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate preview", description = "Generate or retrieve a preview for a file")
    public ResponseEntity<ApiResponse<PreviewResponse>> generatePreview(
            @Valid @RequestBody PreviewRequest request,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Generate preview request - userId: {}, fileId: {}, type: {}", 
                userId, request.getFileId(), request.getPreviewType());
        
        PreviewResponse response = previewService.generatePreview(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Preview generated successfully", response));
    }
    
    /**
     * Get preview by file ID and type.
     * 
     * GET /api/v1/previews/files/{fileId}/{previewType}
     */
    @GetMapping("/files/{fileId}/{previewType}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get preview", description = "Get preview by file ID and type")
    public ResponseEntity<ApiResponse<PreviewResponse>> getPreview(
            @PathVariable UUID fileId,
            @PathVariable PreviewType previewType,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Get preview request - userId: {}, fileId: {}, type: {}", userId, fileId, previewType);
        
        PreviewResponse response = previewService.getPreview(userId, fileId, previewType);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get all previews for a file.
     * 
     * GET /api/v1/previews/files/{fileId}
     */
    @GetMapping("/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get file previews", description = "Get all previews for a file")
    public ResponseEntity<ApiResponse<List<PreviewResponse>>> getFilePreviews(
            @PathVariable UUID fileId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Get file previews request - userId: {}, fileId: {}", userId, fileId);
        
        List<PreviewResponse> response = previewService.getFilePreviews(userId, fileId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Delete a preview.
     * 
     * DELETE /api/v1/previews/{previewId}
     */
    @DeleteMapping("/{previewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete preview", description = "Delete a preview")
    public ResponseEntity<ApiResponse<Void>> deletePreview(
            @PathVariable UUID previewId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Delete preview request - userId: {}, previewId: {}", userId, previewId);
        
        previewService.deletePreview(userId, previewId);
        
        return ResponseEntity.ok(ApiResponse.success("Preview deleted successfully", null));
    }
    
    /**
     * Delete all previews for a file.
     * 
     * DELETE /api/v1/previews/files/{fileId}
     */
    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete file previews", description = "Delete all previews for a file")
    public ResponseEntity<ApiResponse<Void>> deleteFilePreviews(
            @PathVariable UUID fileId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Delete file previews request - userId: {}, fileId: {}", userId, fileId);
        
        previewService.deleteFilePreviews(userId, fileId);
        
        return ResponseEntity.ok(ApiResponse.success("All previews deleted successfully", null));
    }
    
    /**
     * Get preview statistics.
     * 
     * GET /api/v1/previews/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get preview statistics", description = "Get preview statistics (admin only)")
    public ResponseEntity<ApiResponse<PreviewStatsResponse>> getPreviewStats(
            Authentication authentication) {
        
        log.info("Get preview statistics request");
        
        PreviewStatsResponse response = previewService.getPreviewStats();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Manually trigger cleanup of expired previews.
     * 
     * POST /api/v1/previews/cleanup
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cleanup expired previews", description = "Manually trigger cleanup of expired previews (admin only)")
    public ResponseEntity<ApiResponse<Void>> cleanupExpiredPreviews(
            Authentication authentication) {
        
        log.info("Manual cleanup request");
        
        previewService.cleanupExpiredPreviews();
        
        return ResponseEntity.ok(ApiResponse.success("Expired previews cleaned up successfully", null));
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
