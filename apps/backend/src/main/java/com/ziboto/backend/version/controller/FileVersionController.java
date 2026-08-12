package com.ziboto.backend.version.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import com.ziboto.backend.version.dto.CreateVersionRequest;
import com.ziboto.backend.version.dto.FileVersionResponse;
import com.ziboto.backend.version.dto.VersionCompareResponse;
import com.ziboto.backend.version.service.FileVersionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for file versioning operations.
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET /api/v1/versions/{fileId} - Get version history</li>
 *   <li>GET /api/v1/versions/{fileId}/{versionNumber} - Get specific version</li>
 *   <li>POST /api/v1/versions/{fileId} - Create new version</li>
 *   <li>POST /api/v1/versions/{fileId}/snapshot - Create snapshot</li>
 *   <li>POST /api/v1/versions/{fileId}/restore/{versionNumber} - Restore version</li>
 *   <li>GET /api/v1/versions/{fileId}/compare - Compare versions</li>
 *   <li>DELETE /api/v1/versions/{fileId}/cleanup - Apply retention policy</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
public class FileVersionController {
    
    private final FileVersionService versionService;
    private final UserRepository userRepository;
    
    // -------------------------------------------------------------------------
    // Version History
    // -------------------------------------------------------------------------
    
    /**
     * Get version history for a file.
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Page<FileVersionResponse>> getVersionHistory(
            @PathVariable UUID fileId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        Page<FileVersionResponse> versions = versionService.getVersionHistory(fileId, user.getId(), pageable);
        
        return ResponseEntity.ok(versions);
    }
    
    /**
     * Get specific version.
     */
    @GetMapping("/{fileId}/{versionNumber}")
    public ResponseEntity<FileVersionResponse> getVersion(
            @PathVariable UUID fileId,
            @PathVariable Integer versionNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        FileVersionResponse version = versionService.getVersion(fileId, versionNumber, user.getId());
        
        return ResponseEntity.ok(version);
    }
    
    /**
     * Get latest version.
     */
    @GetMapping("/{fileId}/latest")
    public ResponseEntity<FileVersionResponse> getLatestVersion(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        FileVersionResponse version = versionService.getLatestVersion(fileId, user.getId());
        
        return ResponseEntity.ok(version);
    }
    
    // -------------------------------------------------------------------------
    // Version Creation
    // -------------------------------------------------------------------------
    
    /**
     * Create new version by uploading updated file.
     */
    @PostMapping("/{fileId}")
    public ResponseEntity<FileVersionResponse> createNewVersion(
            @PathVariable UUID fileId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeDescription", required = false) String changeDescription,
            @RequestParam(value = "versionTag", required = false) String versionTag,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        
        CreateVersionRequest request = CreateVersionRequest.builder()
                .changeDescription(changeDescription)
                .versionTag(versionTag)
                .build();
        
        FileVersionResponse version = versionService.createNewVersion(
                fileId, user.getId(), file, request, user.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }
    
    /**
     * Create version snapshot (without uploading new file).
     */
    @PostMapping("/{fileId}/snapshot")
    public ResponseEntity<FileVersionResponse> createSnapshot(
            @PathVariable UUID fileId,
            @Valid @RequestBody CreateVersionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        FileVersionResponse version = versionService.createVersionSnapshot(
                fileId, user.getId(), request, user.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }
    
    // -------------------------------------------------------------------------
    // Version Restoration
    // -------------------------------------------------------------------------
    
    /**
     * Restore a previous version.
     */
    @PostMapping("/{fileId}/restore/{versionNumber}")
    public ResponseEntity<FileVersionResponse> restoreVersion(
            @PathVariable UUID fileId,
            @PathVariable Integer versionNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        FileVersionResponse version = versionService.restoreVersion(
                fileId, versionNumber, user.getId(), user.getUsername());
        
        return ResponseEntity.ok(version);
    }
    
    // -------------------------------------------------------------------------
    // Version Comparison
    // -------------------------------------------------------------------------
    
    /**
     * Compare two versions.
     */
    @GetMapping("/{fileId}/compare")
    public ResponseEntity<VersionCompareResponse> compareVersions(
            @PathVariable UUID fileId,
            @RequestParam("oldVersion") Integer oldVersion,
            @RequestParam("newVersion") Integer newVersion,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        VersionCompareResponse comparison = versionService.compareVersions(
                fileId, oldVersion, newVersion, user.getId());
        
        return ResponseEntity.ok(comparison);
    }
    
    // -------------------------------------------------------------------------
    // Version Management
    // -------------------------------------------------------------------------
    
    /**
     * Apply retention policy (keep last N versions).
     */
    @DeleteMapping("/{fileId}/cleanup")
    public ResponseEntity<Void> applyRetentionPolicy(
            @PathVariable UUID fileId,
            @RequestParam(value = "maxVersions", defaultValue = "50") int maxVersions,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        versionService.applyRetentionPolicy(fileId, user.getId(), maxVersions);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Delete old versions (older than N days).
     */
    @DeleteMapping("/{fileId}/cleanup/old")
    public ResponseEntity<Void> deleteOldVersions(
            @PathVariable UUID fileId,
            @RequestParam(value = "daysToKeep", defaultValue = "90") int daysToKeep,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        versionService.deleteOldVersions(fileId, user.getId(), daysToKeep);
        
        return ResponseEntity.noContent().build();
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
