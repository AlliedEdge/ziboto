package com.ziboto.backend.version.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for comparing two file versions.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionCompareResponse {
    
    private VersionInfo oldVersion;
    private VersionInfo newVersion;
    
    private Boolean contentChanged; // Based on SHA-256 hash
    private Boolean nameChanged;
    private Boolean sizeChanged;
    
    private Long sizeDifference; // Bytes
    private Long daysBetween;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionInfo {
        private Integer versionNumber;
        private String versionLabel;
        private String fileName;
        private Long fileSize;
        private String formattedFileSize;
        private String sha256Hash;
        private LocalDateTime createdAt;
        private String changeDescription;
    }
}
