package com.ziboto.backend.version.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for file version information.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileVersionResponse {
    
    private UUID id;
    private UUID fileId;
    private Integer versionNumber;
    private String versionLabel; // Tag or "v{number}"
    
    // File metadata snapshot
    private String fileName;
    private Long fileSize;
    private String formattedFileSize;
    private String mimeType;
    private String fileExtension;
    private String sha256Hash;
    
    // Version metadata
    private String changeDescription;
    private String versionTag;
    
    // Storage info
    private String storageKey;
    private String storageLocation;
    
    // Audit
    private LocalDateTime createdAt;
    private String createdBy;
    
    // Flags
    private Boolean isLatest;
    private Boolean isInitialVersion;
}
