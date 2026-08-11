package com.ziboto.backend.activity.dto;

import com.ziboto.backend.activity.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for activity summary.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummaryResponse {
    
    private Long totalActivities;
    private Map<ActivityType, Long> activityCounts;
    private LocalDateTime mostRecentActivity;
    private LocalDateTime oldestActivity;
    private Integer daysActive;
    private Double averageActivitiesPerDay;
}
