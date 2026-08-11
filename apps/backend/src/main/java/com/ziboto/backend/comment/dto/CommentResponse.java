package com.ziboto.backend.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for file comments.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    
    private UUID id;
    private UUID fileId;
    private Long userId;
    private String username;
    private String userAvatar;
    private UUID parentId;
    private String content;
    private List<Long> mentions;
    private Boolean isEdited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long replyCount;
    private List<CommentResponse> replies; // Nested replies
    private String timeAgo;
}
