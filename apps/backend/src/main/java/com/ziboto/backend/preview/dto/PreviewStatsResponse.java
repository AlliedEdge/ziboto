package com.ziboto.backend.preview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for preview statistics.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewStatsResponse {
    
    private Long totalPreviews;
    private Long totalSize;
    private List<PreviewTypeStats> statsByType;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewTypeStats {
        private String previewType;
        private Long count;
        private Long totalSize;
        private Long avgSize;
    }
}
