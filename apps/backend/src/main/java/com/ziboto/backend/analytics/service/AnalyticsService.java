package com.ziboto.backend.analytics.service;

import com.ziboto.backend.analytics.dto.StorageAnalyticsResponse;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for storage analytics and insights.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    
    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Get complete storage analytics for user.
     */
    @Transactional(readOnly = true)
    public StorageAnalyticsResponse getStorageAnalytics(Long userId, int days) {
        log.info("Getting storage analytics - userId: {}, days: {}", userId, days);
        
        return StorageAnalyticsResponse.builder()
                .overview(getStorageOverview(userId))
                .fileTypeBreakdown(getFileTypeBreakdown(userId))
                .storageTrend(getStorageTrend(userId, days))
                .mostAccessedFiles(getMostAccessedFiles(userId, 10))
                .activityByDay(getActivityByDay(userId, days))
                .build();
    }
    
    /**
     * Get storage overview.
     */
    private StorageAnalyticsResponse.StorageOverview getStorageOverview(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        
        long totalFiles = fileMetadataRepository.countByUserId(userId);
        long storageUsed = user.getStorageUsed();
        long storageQuota = user.getStorageQuota();
        double usagePercentage = storageQuota > 0 ? (double) storageUsed / storageQuota * 100 : 0;
        long storageRemaining = storageQuota - storageUsed;
        
        return StorageAnalyticsResponse.StorageOverview.builder()
                .totalFiles(totalFiles)
                .totalSize(storageUsed)
                .formattedTotalSize(formatBytes(storageUsed))
                .storageUsed(storageUsed)
                .formattedStorageUsed(formatBytes(storageUsed))
                .storageQuota(storageQuota)
                .formattedStorageQuota(formatBytes(storageQuota))
                .usagePercentage(usagePercentage)
                .storageRemaining(storageRemaining)
                .formattedStorageRemaining(formatBytes(storageRemaining))
                .build();
    }
    
    /**
     * Get file type breakdown.
     */
    private List<StorageAnalyticsResponse.FileTypeBreakdown> getFileTypeBreakdown(Long userId) {
        String sql = "SELECT * FROM get_storage_by_file_type(?)";
        
        List<StorageAnalyticsResponse.FileTypeBreakdown> breakdown = new ArrayList<>();
        
        jdbcTemplate.query(sql, rs -> {
            breakdown.add(StorageAnalyticsResponse.FileTypeBreakdown.builder()
                    .fileExtension(rs.getString("file_extension"))
                    .fileCount(rs.getLong("file_count"))
                    .totalSize(rs.getLong("total_size"))
                    .formattedSize(formatBytes(rs.getLong("total_size")))
                    .percentage(rs.getDouble("percentage"))
                    .build());
        }, userId);
        
        return breakdown;
    }
    
    /**
     * Get storage usage trend.
     */
    private List<StorageAnalyticsResponse.StorageTrend> getStorageTrend(Long userId, int days) {
        String sql = "SELECT * FROM get_storage_usage_trend(?, ?)";
        
        List<StorageAnalyticsResponse.StorageTrend> trend = new ArrayList<>();
        
        jdbcTemplate.query(sql, rs -> {
            trend.add(StorageAnalyticsResponse.StorageTrend.builder()
                    .date(rs.getDate("date").toString())
                    .storageUsed(rs.getLong("storage_used"))
                    .formattedSize(formatBytes(rs.getLong("storage_used")))
                    .fileCount(rs.getLong("file_count"))
                    .build());
        }, userId, days);
        
        return trend;
    }
    
    /**
     * Get most accessed files.
     */
    private List<StorageAnalyticsResponse.MostAccessedFile> getMostAccessedFiles(Long userId, int limit) {
        String sql = "SELECT * FROM get_most_accessed_files(?, ?)";
        
        List<StorageAnalyticsResponse.MostAccessedFile> files = new ArrayList<>();
        
        jdbcTemplate.query(sql, rs -> {
            files.add(StorageAnalyticsResponse.MostAccessedFile.builder()
                    .fileId(rs.getString("file_id"))
                    .fileName(rs.getString("file_name"))
                    .downloadCount(rs.getInt("download_count"))
                    .fileSize(rs.getLong("file_size"))
                    .formattedSize(formatBytes(rs.getLong("file_size")))
                    .build());
        }, userId, limit);
        
        return files;
    }
    
    /**
     * Get activity by day.
     */
    private List<StorageAnalyticsResponse.ActivityByDay> getActivityByDay(Long userId, int days) {
        String sql = "SELECT * FROM get_activity_by_day(?, ?)";
        
        List<StorageAnalyticsResponse.ActivityByDay> activity = new ArrayList<>();
        
        jdbcTemplate.query(sql, rs -> {
            long uploads = rs.getLong("uploads");
            long downloads = rs.getLong("downloads");
            
            activity.add(StorageAnalyticsResponse.ActivityByDay.builder()
                    .date(rs.getDate("date").toString())
                    .uploads(uploads)
                    .downloads(downloads)
                    .totalActivity(uploads + downloads)
                    .build());
        }, userId, days);
        
        return activity;
    }
    
    /**
     * Record storage snapshot for user.
     */
    @Transactional
    public void recordStorageSnapshot(Long userId) {
        String sql = "SELECT record_storage_snapshot(?)";
        jdbcTemplate.update(sql, userId);
        log.debug("Storage snapshot recorded for user: {}", userId);
    }
    
    /**
     * Format bytes to human-readable size.
     */
    private String formatBytes(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
