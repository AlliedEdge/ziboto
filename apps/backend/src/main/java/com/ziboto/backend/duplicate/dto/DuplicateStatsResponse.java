package com.ziboto.backend.duplicate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for duplicate statistics.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateStatsResponse {
    
    // Counts
    private Long totalDuplicateGroups;
    private Long totalDuplicateFiles;
    private Long unreviewedGroups;
    
    // Savings
    private Long potentialSavingsBytes;
    private String formattedSavings;
    
    // User-specific (optional)
    private Long userDuplicateFiles;
    private Long userPotentialSavingsBytes;
    private String userFormattedSavings;
}
