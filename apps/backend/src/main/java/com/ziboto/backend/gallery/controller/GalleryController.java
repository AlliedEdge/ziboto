package com.ziboto.backend.gallery.controller;

import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.gallery.dto.*;
import com.ziboto.backend.gallery.service.GalleryService;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for public galleries.
 * 
 * @author Ziboto Team
 * @since V3
 */
@RestController
@RequestMapping("/api/v1/galleries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public Galleries", description = "Shareable file collections")
public class GalleryController {
    
    private final GalleryService galleryService;
    private final UserRepository userRepository;
    
    /**
     * Create a new gallery.
     * 
     * POST /api/v1/galleries
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create gallery", description = "Create a new gallery")
    public ResponseEntity<ApiResponse<GalleryResponse>> createGallery(
            @Valid @RequestBody GalleryRequest request,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Create gallery request - userId: {}, title: {}", userId, request.getTitle());
        
        GalleryResponse response = galleryService.createGallery(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gallery created successfully", response));
    }
    
    /**
     * Get user's galleries.
     * 
     * GET /api/v1/galleries
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user galleries", description = "Get all galleries for the current user")
    public ResponseEntity<ApiResponse<Page<GalleryResponse>>> getUserGalleries(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Long userId = getUserId(authentication);
        log.info("Get user galleries request - userId: {}", userId);
        
        Page<GalleryResponse> galleries = galleryService.getUserGalleries(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(galleries));
    }
    
    /**
     * Get gallery by ID (owner only).
     * 
     * GET /api/v1/galleries/{galleryId}
     */
    @GetMapping("/{galleryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get gallery", description = "Get gallery by ID (owner only)")
    public ResponseEntity<ApiResponse<GalleryDetailResponse>> getGallery(
            @PathVariable UUID galleryId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Get gallery request - galleryId: {}, userId: {}", galleryId, userId);
        
        GalleryDetailResponse response = galleryService.getGalleryById(galleryId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get public gallery by slug.
     * 
     * GET /api/v1/galleries/public/{slug}
     */
    @GetMapping("/public/{slug}")
    @Operation(summary = "Get public gallery", description = "Get public gallery by slug (no auth required)")
    public ResponseEntity<ApiResponse<GalleryDetailResponse>> getPublicGallery(
            @PathVariable String slug,
            @RequestParam(required = false) String password) {
        
        log.info("Get public gallery request - slug: {}", slug);
        
        GalleryDetailResponse response = galleryService.getPublicGallery(slug, password);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Update a gallery.
     * 
     * PUT /api/v1/galleries/{galleryId}
     */
    @PutMapping("/{galleryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update gallery", description = "Update a gallery (owner only)")
    public ResponseEntity<ApiResponse<GalleryResponse>> updateGallery(
            @PathVariable UUID galleryId,
            @Valid @RequestBody GalleryRequest request,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Update gallery request - galleryId: {}, userId: {}", galleryId, userId);
        
        GalleryResponse response = galleryService.updateGallery(galleryId, userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Gallery updated successfully", response));
    }
    
    /**
     * Delete a gallery.
     * 
     * DELETE /api/v1/galleries/{galleryId}
     */
    @DeleteMapping("/{galleryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete gallery", description = "Delete a gallery (owner only)")
    public ResponseEntity<ApiResponse<Void>> deleteGallery(
            @PathVariable UUID galleryId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Delete gallery request - galleryId: {}, userId: {}", galleryId, userId);
        
        galleryService.deleteGallery(galleryId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Gallery deleted successfully", null));
    }
    
    /**
     * Add file to gallery.
     * 
     * POST /api/v1/galleries/{galleryId}/files
     */
    @PostMapping("/{galleryId}/files")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add file to gallery", description = "Add a file to a gallery")
    public ResponseEntity<ApiResponse<Void>> addFileToGallery(
            @PathVariable UUID galleryId,
            @Valid @RequestBody AddFileRequest request,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Add file to gallery request - galleryId: {}, fileId: {}", galleryId, request.getFileId());
        
        galleryService.addFileToGallery(galleryId, userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("File added to gallery successfully", null));
    }
    
    /**
     * Remove file from gallery.
     * 
     * DELETE /api/v1/galleries/{galleryId}/files/{fileId}
     */
    @DeleteMapping("/{galleryId}/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove file from gallery", description = "Remove a file from a gallery")
    public ResponseEntity<ApiResponse<Void>> removeFileFromGallery(
            @PathVariable UUID galleryId,
            @PathVariable UUID fileId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Remove file from gallery request - galleryId: {}, fileId: {}", galleryId, fileId);
        
        galleryService.removeFileFromGallery(galleryId, fileId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("File removed from gallery successfully", null));
    }
    
    /**
     * Reorder files in gallery.
     * 
     * PUT /api/v1/galleries/{galleryId}/files/reorder
     */
    @PutMapping("/{galleryId}/files/reorder")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reorder files", description = "Reorder files in a gallery")
    public ResponseEntity<ApiResponse<Void>> reorderFiles(
            @PathVariable UUID galleryId,
            @RequestBody List<UUID> fileIds,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Reorder files request - galleryId: {}, count: {}", galleryId, fileIds.size());
        
        galleryService.reorderFiles(galleryId, userId, fileIds);
        
        return ResponseEntity.ok(ApiResponse.success("Files reordered successfully", null));
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
