package com.ziboto.backend.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating/updating comments.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    
    @NotBlank(message = "Comment content is required")
    @Size(max = 5000, message = "Comment cannot exceed 5000 characters")
    private String content;
    
    private UUID parentId; // For threaded replies
    
    private List<Long> mentions; // User IDs mentioned in comment
}
