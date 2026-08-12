package com.ziboto.backend.preview.service;

import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.exception.UnauthorizedException;
import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.preview.dto.PreviewRequest;
import com.ziboto.backend.preview.dto.PreviewResponse;
import com.ziboto.backend.preview.dto.PreviewStatsResponse;
import com.ziboto.backend.preview.entity.FilePreview;
import com.ziboto.backend.preview.enums.PreviewStatus;
import com.ziboto.backend.preview.enums.PreviewType;
import com.ziboto.backend.preview.repository.FilePreviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for file preview generation and management.
 * 
 * Supports:
 * - Thumbnail generation for images
 * - PDF previews
 * - Video thumbnails
 * - Audio waveforms
 * - Document previews
 * - Code syntax highlighting
 * 
 * @author Ziboto Team
 * @since V3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreviewService {
    
    private final FilePreviewRepository previewRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final JdbcTemplate jdbcTemplate;
    
    // Supported MIME types for preview generation
    private static final Map<PreviewType, Set<String>> SUPPORTED_MIME_TYPES = Map.of(
        PreviewType.IMAGE, Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"),
        PreviewType.THUMBNAIL, Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "video/mp4", "video/avi", "application/pdf"),
        PreviewType.PDF, Set.of("application/pdf"),
        PreviewType.VIDEO, Set.of("video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv"),
        PreviewType.AUDIO, Set.of("audio/mpeg", "audio/wav", "audio/ogg", "audio/mp3"),
        PreviewType.DOCUMENT, Set.of("application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
                                     "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        PreviewType.CODE, Set.of("text/plain", "text/x-java-source", "application/javascript", "text/html", "text/css")
    );
    
    /**
     * Generate or retrieve preview for a file.
     */
    @Transactional
    public PreviewResponse generatePreview(Long userId, PreviewRequest request) {
        log.info("Generate preview request - userId: {}, fileId: {}, type: {}", 
                userId, request.getFileId(), request.getPreviewType());
        
        // Validate file ownership
        FileMetadata file = fileMetadataRepository.findById(request.getFileId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + request.getFileId()));
        
        if (!file.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to generate preview for this file");
        }
        
        // Check if preview already exists
        Optional<FilePreview> existingPreview = previewRepository
                .findByFileIdAndPreviewType(request.getFileId(), request.getPreviewType());
        
        if (existingPreview.isPresent()) {
            FilePreview preview = existingPreview.get();
            if (preview.getExpiresAt() == null || preview.getExpiresAt().isAfter(LocalDateTime.now())) {
                log.info("Returning cached preview - previewId: {}", preview.getId());
                return mapToResponse(preview, file);
            } else {
                // Delete expired preview
                previewRepository.delete(preview);
            }
        }
        
        // Check if file type supports preview
        if (!isPreviewSupported(file.getMimeType(), request.getPreviewType())) {
            log.warn("Preview not supported - mimeType: {}, previewType: {}", 
                    file.getMimeType(), request.getPreviewType());
            return PreviewResponse.builder()
                    .fileId(request.getFileId())
                    .previewType(request.getPreviewType())
                    .status(PreviewStatus.NOT_SUPPORTED)
                    .errorMessage("Preview type " + request.getPreviewType() + " is not supported for " + file.getMimeType())
                    .build();
        }
        
        // Generate preview based on type
        FilePreview preview;
        try {
            preview = generatePreviewByType(file, request);
            preview = previewRepository.save(preview);
            log.info("Preview generated successfully - previewId: {}, type: {}", 
                    preview.getId(), preview.getPreviewType());
        } catch (Exception e) {
            log.error("Failed to generate preview - fileId: {}, type: {}", 
                    request.getFileId(), request.getPreviewType(), e);
            return PreviewResponse.builder()
                    .fileId(request.getFileId())
                    .previewType(request.getPreviewType())
                    .status(PreviewStatus.FAILED)
                    .errorMessage("Preview generation failed: " + e.getMessage())
                    .build();
        }
        
        return mapToResponse(preview, file);
    }
    
    /**
     * Get preview by file ID and type.
     */
    @Transactional(readOnly = true)
    public PreviewResponse getPreview(Long userId, UUID fileId, PreviewType previewType) {
        log.info("Get preview - userId: {}, fileId: {}, type: {}", userId, fileId, previewType);
        
        // Validate file ownership
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        
        if (!file.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to view preview for this file");
        }
        
        FilePreview preview = previewRepository.findByFileIdAndPreviewType(fileId, previewType)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Preview not found for file " + fileId + " with type " + previewType));
        
        // Check expiration
        if (preview.getExpiresAt() != null && preview.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Preview has expired");
        }
        
        return mapToResponse(preview, file);
    }
    
    /**
     * Get all previews for a file.
     */
    @Transactional(readOnly = true)
    public List<PreviewResponse> getFilePreviews(Long userId, UUID fileId) {
        log.info("Get file previews - userId: {}, fileId: {}", userId, fileId);
        
        // Validate file ownership
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        
        if (!file.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to view previews for this file");
        }
        
        List<FilePreview> previews = previewRepository.findByFileId(fileId);
        
        // Filter out expired previews
        return previews.stream()
                .filter(p -> p.getExpiresAt() == null || p.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(p -> mapToResponse(p, file))
                .collect(Collectors.toList());
    }
    
    /**
     * Delete preview.
     */
    @Transactional
    public void deletePreview(Long userId, UUID previewId) {
        log.info("Delete preview - userId: {}, previewId: {}", userId, previewId);
        
        FilePreview preview = previewRepository.findById(previewId)
                .orElseThrow(() -> new ResourceNotFoundException("Preview not found: " + previewId));
        
        // Validate file ownership
        FileMetadata file = fileMetadataRepository.findById(preview.getFileId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + preview.getFileId()));
        
        if (!file.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to delete this preview");
        }
        
        previewRepository.delete(preview);
        log.info("Preview deleted successfully - previewId: {}", previewId);
    }
    
    /**
     * Delete all previews for a file.
     */
    @Transactional
    public void deleteFilePreviews(Long userId, UUID fileId) {
        log.info("Delete file previews - userId: {}, fileId: {}", userId, fileId);
        
        // Validate file ownership
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        
        if (!file.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to delete previews for this file");
        }
        
        previewRepository.deleteByFileId(fileId);
        log.info("All previews deleted for file - fileId: {}", fileId);
    }
    
    /**
     * Get preview statistics.
     */
    @Transactional(readOnly = true)
    public PreviewStatsResponse getPreviewStats() {
        log.info("Get preview statistics");
        
        String sql = "SELECT * FROM get_preview_stats()";
        
        List<PreviewStatsResponse.PreviewTypeStats> statsByType = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> PreviewStatsResponse.PreviewTypeStats.builder()
                    .previewType(rs.getString("preview_type"))
                    .count(rs.getLong("count"))
                    .totalSize(rs.getLong("total_size"))
                    .avgSize(rs.getLong("avg_size"))
                    .build()
        );
        
        long totalPreviews = statsByType.stream().mapToLong(PreviewStatsResponse.PreviewTypeStats::getCount).sum();
        long totalSize = statsByType.stream().mapToLong(PreviewStatsResponse.PreviewTypeStats::getTotalSize).sum();
        
        return PreviewStatsResponse.builder()
                .totalPreviews(totalPreviews)
                .totalSize(totalSize)
                .statsByType(statsByType)
                .build();
    }
    
    /**
     * Scheduled task to cleanup expired previews.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredPreviews() {
        log.info("Starting cleanup of expired previews");
        
        int deleted = previewRepository.deleteExpiredPreviews(LocalDateTime.now());
        
        log.info("Expired previews cleanup completed - deleted: {}", deleted);
    }
    
    /**
     * Generate preview based on type.
     */
    private FilePreview generatePreviewByType(FileMetadata file, PreviewRequest request) {
        FilePreview.FilePreviewBuilder builder = FilePreview.builder()
                .fileId(file.getId())
                .previewType(request.getPreviewType())
                .createdAt(LocalDateTime.now());
        
        switch (request.getPreviewType()) {
            case THUMBNAIL:
                return generateThumbnail(file, request, builder);
            case IMAGE:
                return generateImagePreview(file, request, builder);
            case PDF:
                return generatePdfPreview(file, request, builder);
            case VIDEO:
                return generateVideoPreview(file, request, builder);
            case AUDIO:
                return generateAudioPreview(file, request, builder);
            case DOCUMENT:
                return generateDocumentPreview(file, request, builder);
            case CODE:
                return generateCodePreview(file, request, builder);
            default:
                throw new IllegalArgumentException("Unsupported preview type: " + request.getPreviewType());
        }
    }
    
    /**
     * Generate thumbnail (small image preview).
     */
    private FilePreview generateThumbnail(FileMetadata file, PreviewRequest request, 
                                          FilePreview.FilePreviewBuilder builder) {
        // In real implementation, would use image processing library (e.g., Thumbnailator)
        // or call external service (e.g., Lambda function)
        
        int width = request.getWidth() != null ? request.getWidth() : 200;
        int height = request.getHeight() != null ? request.getHeight() : 200;
        
        // Mock implementation - would generate actual thumbnail
        String thumbnailUrl = generateMockPreviewUrl(file.getId(), "thumbnail", width, height);
        
        return builder
                .previewUrl(thumbnailUrl)
                .width(width)
                .height(height)
                .fileSize(estimatePreviewSize(width, height))
                .expiresAt(LocalDateTime.now().plusDays(30)) // Cache for 30 days
                .build();
    }
    
    /**
     * Generate full image preview.
     */
    private FilePreview generateImagePreview(FileMetadata file, PreviewRequest request, 
                                            FilePreview.FilePreviewBuilder builder) {
        // In real implementation, would process image (resize, optimize, format conversion)
        
        int width = request.getWidth() != null ? request.getWidth() : 1200;
        int height = request.getHeight() != null ? request.getHeight() : 1200;
        
        String previewUrl = generateMockPreviewUrl(file.getId(), "image", width, height);
        
        return builder
                .previewUrl(previewUrl)
                .width(width)
                .height(height)
                .fileSize(estimatePreviewSize(width, height))
                .build();
    }
    
    /**
     * Generate PDF preview.
     */
    private FilePreview generatePdfPreview(FileMetadata file, PreviewRequest request, 
                                          FilePreview.FilePreviewBuilder builder) {
        // In real implementation, would use PDF library (e.g., Apache PDFBox)
        
        int page = request.getPage() != null ? request.getPage() : 1;
        String previewUrl = generateMockPreviewUrl(file.getId(), "pdf", page, 0);
        
        return builder
                .previewUrl(previewUrl)
                .width(800)
                .height(1000)
                .pageCount(estimatePdfPages(file.getFileSize()))
                .fileSize(estimatePreviewSize(800, 1000))
                .build();
    }
    
    /**
     * Generate video preview/thumbnail.
     */
    private FilePreview generateVideoPreview(FileMetadata file, PreviewRequest request, 
                                            FilePreview.FilePreviewBuilder builder) {
        // In real implementation, would use video processing library (e.g., FFmpeg)
        
        String previewUrl = generateMockPreviewUrl(file.getId(), "video", 640, 360);
        
        return builder
                .previewUrl(previewUrl)
                .width(640)
                .height(360)
                .duration(estimateVideoDuration(file.getFileSize()))
                .fileSize(estimatePreviewSize(640, 360))
                .build();
    }
    
    /**
     * Generate audio waveform preview.
     */
    private FilePreview generateAudioPreview(FileMetadata file, PreviewRequest request, 
                                            FilePreview.FilePreviewBuilder builder) {
        // In real implementation, would generate waveform visualization
        
        String previewUrl = generateMockPreviewUrl(file.getId(), "audio", 800, 200);
        
        return builder
                .previewUrl(previewUrl)
                .width(800)
                .height(200)
                .duration(estimateAudioDuration(file.getFileSize()))
                .fileSize(50000L) // Waveform images are typically small
                .build();
    }
    
    /**
     * Generate document preview.
     */
    private FilePreview generateDocumentPreview(FileMetadata file, PreviewRequest request, 
                                               FilePreview.FilePreviewBuilder builder) {
        // In real implementation, would use document processing library (e.g., Apache POI)
        
        int page = request.getPage() != null ? request.getPage() : 1;
        String previewUrl = generateMockPreviewUrl(file.getId(), "document", page, 0);
        
        return builder
                .previewUrl(previewUrl)
                .width(800)
                .height(1000)
                .pageCount(estimateDocumentPages(file.getFileSize()))
                .fileSize(estimatePreviewSize(800, 1000))
                .build();
    }
    
    /**
     * Generate code preview with syntax highlighting.
     */
    private FilePreview generateCodePreview(FileMetadata file, PreviewRequest request, 
                                           FilePreview.FilePreviewBuilder builder) {
        // In real implementation, would apply syntax highlighting
        
        String previewUrl = generateMockPreviewUrl(file.getId(), "code", 0, 0);
        
        return builder
                .previewUrl(previewUrl)
                .fileSize(file.getFileSize()) // Code preview is similar size to original
                .build();
    }
    
    /**
     * Check if preview type is supported for MIME type.
     */
    private boolean isPreviewSupported(String mimeType, PreviewType previewType) {
        Set<String> supportedTypes = SUPPORTED_MIME_TYPES.get(previewType);
        return supportedTypes != null && supportedTypes.contains(mimeType);
    }
    
    /**
     * Generate mock preview URL (in real implementation, would upload to S3).
     */
    private String generateMockPreviewUrl(UUID fileId, String type, int param1, int param2) {
        return String.format("/api/v1/previews/files/%s/%s/%d/%d", fileId, type, param1, param2);
    }
    
    /**
     * Estimate preview file size based on dimensions.
     */
    private Long estimatePreviewSize(int width, int height) {
        // Rough estimate: width * height * 3 bytes (RGB) / compression ratio (assume 10:1)
        return (long) (width * height * 3 / 10);
    }
    
    /**
     * Estimate PDF page count based on file size.
     */
    private Integer estimatePdfPages(Long fileSize) {
        // Rough estimate: 100KB per page
        return (int) (fileSize / 100000) + 1;
    }
    
    /**
     * Estimate video duration based on file size.
     */
    private Integer estimateVideoDuration(Long fileSize) {
        // Rough estimate: 1MB per second (varies by quality)
        return (int) (fileSize / 1000000);
    }
    
    /**
     * Estimate audio duration based on file size.
     */
    private Integer estimateAudioDuration(Long fileSize) {
        // Rough estimate: 128 kbps = 16KB per second
        return (int) (fileSize / 16000);
    }
    
    /**
     * Estimate document page count based on file size.
     */
    private Integer estimateDocumentPages(Long fileSize) {
        // Rough estimate: 50KB per page
        return (int) (fileSize / 50000) + 1;
    }
    
    /**
     * Map FilePreview entity to response DTO.
     */
    private PreviewResponse mapToResponse(FilePreview preview, FileMetadata file) {
        PreviewResponse.PreviewResponseBuilder builder = PreviewResponse.builder()
                .id(preview.getId())
                .fileId(preview.getFileId())
                .previewType(preview.getPreviewType())
                .status(PreviewStatus.COMPLETED)
                .previewUrl(preview.getPreviewUrl())
                .width(preview.getWidth())
                .height(preview.getHeight())
                .duration(preview.getDuration())
                .pageCount(preview.getPageCount())
                .fileSize(preview.getFileSize())
                .createdAt(preview.getCreatedAt())
                .expiresAt(preview.getExpiresAt());
        
        // Convert byte array to Base64 for inline previews
        if (preview.getPreviewData() != null) {
            String base64 = Base64.getEncoder().encodeToString(preview.getPreviewData());
            builder.previewData(base64);
        }
        
        return builder.build();
    }
}
