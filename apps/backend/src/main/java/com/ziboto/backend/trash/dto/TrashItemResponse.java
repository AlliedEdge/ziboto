package com.ziboto.backend.trash.dto;

import com.ziboto.backend.trash.enums.TrashItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for trash items.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrashItemResponse {
    
    private UUID id;
    private TrashItemType itemType;
    private String name;
    private Long size;
    private String formattedSize;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private LocalDateTime autoDeleteAt;
    private Integer daysUntilAutoDelete;
    private String timeInTrash;
}
