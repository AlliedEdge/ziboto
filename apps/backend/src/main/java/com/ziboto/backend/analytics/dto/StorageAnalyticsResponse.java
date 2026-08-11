package com.ziboto.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for storage analytics.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageAnalyticsResponse {
    
    private StorageOverview overview;
    private List<FileTypeBreakdown> fileTypeBreakdown;
    private List<StorageTrend> storageTrend;
    private List<MostAccessedFile> mostAccessedFiles;
    private List<ActivityByDay> activityByDay;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageOverview {
        private Long totalFiles;
        private Long totalSize;
        private String formattedTotalSize;
        private Long storageUsed;
        private String formattedStorageUsed;
        private Long storageQuota;
        private String formattedStorageQuota;
        private Double usagePercentage;
        private Long storageRemaining;
        private String formattedStorageRemaining;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileTypeBreakdown {
        private String fileExtension;
        private Long fileCount;
        private Long totalSize;
        private String formattedSize;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageTrend {
        private String date;
        private Long storageUsed;
        private String formattedSize;
        private Long fileCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MostAccessedFile {
        private String fileId;
        private String fileName;
        private Integer downloadCount;
        private Long fileSize;
        private String formattedSize;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityByDay {
        private String date;
        private Long uploads;
        private Long downloads;
        private Long totalActivity;
    }
}
