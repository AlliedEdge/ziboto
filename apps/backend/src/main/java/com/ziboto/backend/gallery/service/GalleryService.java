package com.ziboto.backend.gallery.service;

import com.ziboto.backend.activity.enums.ActivityType;
import com.ziboto.backend.activity.enums.EntityType;
import com.ziboto.backend.activity.service.ActivityService;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.gallery.dto.*;
import com.ziboto.backend.gallery.entity.Gallery;
import com.ziboto.backend.gallery.entity.GalleryFile;
import com.ziboto.backend.gallery.repository.GalleryFileRepository;
import com.ziboto.backend.gallery.repository.GalleryRepository;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing public galleries.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GalleryService {
    
    private final GalleryRepository galleryRepository;
    private final GalleryFileRepository galleryFileRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    /**
     * Create a new gallery.
     */
    @Transactional
    public GalleryResponse createGallery(Long userId, GalleryRequest request) {
        log.info("Creating gallery - userId: {}, title: {}", userId, request.getTitle());
        
        // Generate unique slug
        String slug = generateUniqueSlug(request.getTitle());
        
        Gallery gallery = Gallery.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .slug(slug)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .passwordProtected(request.getPasswordProtected() != null ? request.getPasswordProtected() : false)
                .theme(request.getTheme() != null ? request.getTheme() : "default")
                .layout(request.getLayout() != null ? request.getLayout() : "grid")
                .viewCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Hash password if provided
        if (Boolean.TRUE.equals(request.getPasswordProtected()) && request.getPassword() != null) {
            gallery.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        
        Gallery saved = galleryRepository.save(gallery);
        
        // Log activity
        activityService.logActivity(
                userId,
                ActivityType.GALLERY_CREATED,
                EntityType.GALLERY,
                saved.getId(),
                "Gallery",
                saved.getTitle()
        );
        
        log.info("Gallery created successfully - galleryId: {}, slug: {}", saved.getId(), saved.getSlug());
        
        return mapToResponse(saved);
    }
    
    /**
     * Get user's galleries.
     */
    @Transactional(readOnly = true)
    public Page<GalleryResponse> getUserGalleries(Long userId, Pageable pageable) {
        log.info("Getting galleries for user: {}", userId);
        
        Page<Gallery> galleries = galleryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return galleries.map(gallery -> {
            GalleryResponse response = mapToResponse(gallery);
            response.setFileCount(galleryFileRepository.countByGalleryId(gallery.getId()));
            return response;
        });
    }
    
    /**
     * Get public gallery by slug.
     */
    @Transactional
    public GalleryDetailResponse getPublicGallery(String slug, String password) {
        log.info("Getting public gallery - slug: {}", slug);
        
        Gallery gallery = galleryRepository.findBySlug(slug)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Gallery not found"));
        
        // Check if gallery is public
        if (!Boolean.TRUE.equals(gallery.getIsPublic())) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Gallery is not public");
        }
        
        // Verify password if protected
        if (Boolean.TRUE.equals(gallery.getPasswordProtected())) {
            if (password == null || !passwordEncoder.matches(password, gallery.getPasswordHash())) {
                throw new BaseException(ErrorCode.UNAUTHORIZED, "Invalid gallery password");
            }
        }
        
        // Increment view count
        galleryRepository.incrementViewCount(gallery.getId());
        
        // Load files
        List<GalleryFile> galleryFiles = galleryFileRepository.findByGalleryIdOrderByDisplayOrderAsc(gallery.getId());
        
        List<GalleryDetailResponse.GalleryFileResponse> fileResponses = galleryFiles.stream()
                .map(gf -> {
                    FileMetadata file = fileMetadataRepository.findById(gf.getFileId()).orElse(null);
                    if (file == null) return null;
                    
                    return GalleryDetailResponse.GalleryFileResponse.builder()
                            .galleryFileId(gf.getId())
                            .fileId(file.getId())
                            .filename(file.getFilename())
                            .contentType(file.getContentType())
                            .fileSize(file.getFileSize())
                            .s3Key(file.getS3Key())
                            .thumbnailUrl(file.getThumbnailUrl())
                            .previewUrl(file.getPreviewUrl())
                            .displayOrder(gf.getDisplayOrder())
                            .caption(gf.getCaption())
                            .addedAt(gf.getAddedAt())
                            .build();
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());
        
        User user = userRepository.findById(gallery.getUserId()).orElse(null);
        
        return GalleryDetailResponse.builder()
                .id(gallery.getId())
                .userId(gallery.getUserId())
                .username(user != null ? user.getUsername() : "Unknown")
                .title(gallery.getTitle())
                .description(gallery.getDescription())
                .slug(gallery.getSlug())
                .isPublic(gallery.getIsPublic())
                .passwordProtected(gallery.getPasswordProtected())
                .theme(gallery.getTheme())
                .layout(gallery.getLayout())
                .viewCount(gallery.getViewCount())
                .createdAt(gallery.getCreatedAt())
                .updatedAt(gallery.getUpdatedAt())
                .publicUrl(baseUrl + "/gallery/" + gallery.getSlug())
                .files(fileResponses)
                .build();
    }
    
    /**
     * Get gallery by ID (owner only).
     */
    @Transactional(readOnly = true)
    public GalleryDetailResponse getGalleryById(UUID galleryId, Long userId) {
        log.info("Getting gallery - galleryId: {}, userId: {}", galleryId, userId);
        
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Gallery not found"));
        
        // Verify ownership
        if (!gallery.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Access denied to this gallery");
        }
        
        // Load files
        List<GalleryFile> galleryFiles = galleryFileRepository.findByGalleryIdOrderByDisplayOrderAsc(gallery.getId());
        
        List<GalleryDetailResponse.GalleryFileResponse> fileResponses = galleryFiles.stream()
                .map(gf -> {
                    FileMetadata file = fileMetadataRepository.findById(gf.getFileId()).orElse(null);
                    if (file == null) return null;
                    
                    return GalleryDetailResponse.GalleryFileResponse.builder()
                            .galleryFileId(gf.getId())
                            .fileId(file.getId())
                            .filename(file.getFilename())
                            .contentType(file.getContentType())
                            .fileSize(file.getFileSize())
                            .s3Key(file.getS3Key())
                            .thumbnailUrl(file.getThumbnailUrl())
                            .previewUrl(file.getPreviewUrl())
                            .displayOrder(gf.getDisplayOrder())
                            .caption(gf.getCaption())
                            .addedAt(gf.getAddedAt())
                            .build();
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());
        
        User user = userRepository.findById(gallery.getUserId()).orElse(null);
        
        return GalleryDetailResponse.builder()
                .id(gallery.getId())
                .userId(gallery.getUserId())
                .username(user != null ? user.getUsername() : "Unknown")
                .title(gallery.getTitle())
                .description(gallery.getDescription())
                .slug(gallery.getSlug())
                .isPublic(gallery.getIsPublic())
                .passwordProtected(gallery.getPasswordProtected())
                .theme(gallery.getTheme())
                .layout(gallery.getLayout())
                .viewCount(gallery.getViewCount())
                .createdAt(gallery.getCreatedAt())
                .updatedAt(gallery.getUpdatedAt())
                .publicUrl(baseUrl + "/gallery/" + gallery.getSlug())
                .files(fileResponses)
                .build();
    }
    
    /**
     * Update a gallery.
     */
    @Transactional
    public GalleryResponse updateGallery(UUID galleryId, Long userId, GalleryRequest request) {
        log.info("Updating gallery - galleryId: {}, userId: {}", galleryId, userId);
        
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Gallery not found"));
        
        // Verify ownership
        if (!gallery.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot edit another user's gallery");
        }
        
        // Update fields
        if (request.getTitle() != null) {
            gallery.setTitle(request.getTitle());
            // Regenerate slug if title changed
            if (!gallery.getTitle().equals(request.getTitle())) {
                gallery.setSlug(generateUniqueSlug(request.getTitle()));
            }
        }
        if (request.getDescription() != null) {
            gallery.setDescription(request.getDescription());
        }
        if (request.getIsPublic() != null) {
            gallery.setIsPublic(request.getIsPublic());
        }
        if (request.getPasswordProtected() != null) {
            gallery.setPasswordProtected(request.getPasswordProtected());
        }
        if (request.getPassword() != null && Boolean.TRUE.equals(request.getPasswordProtected())) {
            gallery.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getTheme() != null) {
            gallery.setTheme(request.getTheme());
        }
        if (request.getLayout() != null) {
            gallery.setLayout(request.getLayout());
        }
        
        gallery.setUpdatedAt(LocalDateTime.now());
        
        Gallery updated = galleryRepository.save(gallery);
        
        log.info("Gallery updated successfully - galleryId: {}", galleryId);
        
        return mapToResponse(updated);
    }
    
    /**
     * Delete a gallery.
     */
    @Transactional
    public void deleteGallery(UUID galleryId, Long userId) {
        log.info("Deleting gallery - galleryId: {}, userId: {}", galleryId, userId);
        
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Gallery not found"));
        
        // Verify ownership
        if (!gallery.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot delete another user's gallery");
        }
        
        // Delete all gallery files first
        galleryFileRepository.deleteByGalleryId(galleryId);
        
        // Delete gallery
        galleryRepository.delete(gallery);
        
        // Log activity
        activityService.logActivity(
                userId,
                ActivityType.GALLERY_DELETED,
                EntityType.GALLERY,
                galleryId,
                "Gallery",
                gallery.getTitle()
        );
        
        log.info("Gallery deleted successfully - galleryId: {}", galleryId);
    }
    
    /**
     * Add file to gallery.
     */
    @Transactional
    public void addFileToGallery(UUID galleryId, Long userId, AddFileRequest request) {
        log.info("Adding file to gallery - galleryId: {}, fileId: {}", galleryId, request.getFileId());
        
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Gallery not found"));
        
        // Verify ownership
        if (!gallery.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot edit another user's gallery");
        }
        
        // Verify file exists and user owns it
        FileMetadata file = fileMetadataRepository.findById(request.getFileId())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        if (!file.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot add another user's file");
        }
        
        // Check if file already in gallery
        if (galleryFileRepository.existsByGalleryIdAndFileId(galleryId, request.getFileId())) {
            throw new BaseException(ErrorCode.DUPLICATE_RESOURCE, "File already in gallery");
        }
        
        // Get next display order
        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            displayOrder = galleryFileRepository.getMaxDisplayOrder(galleryId) + 1;
        }
        
        GalleryFile galleryFile = GalleryFile.builder()
                .galleryId(galleryId)
                .fileId(request.getFileId())
                .displayOrder(displayOrder)
                .caption(request.getCaption())
                .addedAt(LocalDateTime.now())
                .build();
        
        galleryFileRepository.save(galleryFile);
        
        log.info("File added to gallery successfully - galleryFileId: {}", galleryFile.getId());
    }
    
    /**
     * Remove file from gallery.
     */
    @Transactional
    public void removeFileFromGallery(UUID galleryId, UUID fileId, Long userId) {
        log.info("Removing file from gallery - galleryId: {}, fileId: {}", galleryId, fileId);
        
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Gallery not found"));
        
        // Verify ownership
        if (!gallery.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot edit another user's gallery");
        }
        
        GalleryFile galleryFile = galleryFileRepository.findByGalleryIdAndFileId(galleryId, fileId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not in gallery"));
        
        galleryFileRepository.delete(galleryFile);
        
        log.info("File removed from gallery successfully");
    }
    
    /**
     * Reorder files in gallery.
     */
    @Transactional
    public void reorderFiles(UUID galleryId, Long userId, List<UUID> fileIds) {
        log.info("Reordering files in gallery - galleryId: {}, count: {}", galleryId, fileIds.size());
        
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Gallery not found"));
        
        // Verify ownership
        if (!gallery.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot edit another user's gallery");
        }
        
        // Update display orders
        for (int i = 0; i < fileIds.size(); i++) {
            UUID fileId = fileIds.get(i);
            GalleryFile galleryFile = galleryFileRepository.findByGalleryIdAndFileId(galleryId, fileId)
                    .orElse(null);
            
            if (galleryFile != null) {
                galleryFile.setDisplayOrder(i);
                galleryFileRepository.save(galleryFile);
            }
        }
        
        log.info("Files reordered successfully");
    }
    
    /**
     * Generate unique slug from title.
     */
    private String generateUniqueSlug(String title) {
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        
        String slug = baseSlug;
        int counter = 0;
        
        while (galleryRepository.existsBySlug(slug)) {
            counter++;
            slug = baseSlug + "-" + counter;
        }
        
        return slug;
    }
    
    /**
     * Map Gallery to response DTO.
     */
    private GalleryResponse mapToResponse(Gallery gallery) {
        User user = userRepository.findById(gallery.getUserId()).orElse(null);
        
        return GalleryResponse.builder()
                .id(gallery.getId())
                .userId(gallery.getUserId())
                .username(user != null ? user.getUsername() : "Unknown")
                .title(gallery.getTitle())
                .description(gallery.getDescription())
                .slug(gallery.getSlug())
                .isPublic(gallery.getIsPublic())
                .passwordProtected(gallery.getPasswordProtected())
                .theme(gallery.getTheme())
                .layout(gallery.getLayout())
                .viewCount(gallery.getViewCount())
                .createdAt(gallery.getCreatedAt())
                .updatedAt(gallery.getUpdatedAt())
                .publicUrl(baseUrl + "/gallery/" + gallery.getSlug())
                .build();
    }
}
