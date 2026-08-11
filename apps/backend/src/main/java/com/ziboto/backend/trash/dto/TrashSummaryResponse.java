package com.ziboto.backend.trash.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for trash summary statistics.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrashSummaryResponse {
    
    private Long totalItems;
    private Long totalSize;
    private String formattedTotalSize;
    private Long fileCount;
    private Long folderCount;
}
