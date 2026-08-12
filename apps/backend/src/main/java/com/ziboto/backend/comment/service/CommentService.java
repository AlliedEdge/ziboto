package com.ziboto.backend.comment.service;

import com.ziboto.backend.activity.enums.ActivityType;
import com.ziboto.backend.activity.enums.EntityType;
import com.ziboto.backend.activity.service.ActivityService;
import com.ziboto.backend.comment.dto.CommentRequest;
import com.ziboto.backend.comment.dto.CommentResponse;
import com.ziboto.backend.comment.entity.FileComment;
import com.ziboto.backend.comment.repository.FileCommentRepository;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing file comments.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    
    private final FileCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    
    /**
     * Add a comment to a file.
     */
    @Transactional
    public CommentResponse addComment(UUID fileId, Long userId, CommentRequest request) {
        log.info("Adding comment - fileId: {}, userId: {}", fileId, userId);
        
        // Verify parent comment exists if provided
        if (request.getParentId() != null) {
            if (!commentRepository.existsById(request.getParentId())) {
                throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Parent comment not found");
            }
        }
        
        FileComment comment = FileComment.builder()
                .fileId(fileId)
                .userId(userId)
                .parentId(request.getParentId())
                .content(request.getContent())
                .mentions(request.getMentions())
                .isEdited(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        FileComment saved = commentRepository.save(comment);
        
        // Log activity
        activityService.logActivity(
                userId,
                ActivityType.COMMENT_ADDED,
                EntityType.FILE,
                fileId,
                "Comment",
                "added"
        );
        
        log.info("Comment added successfully - commentId: {}", saved.getId());
        
        return mapToResponse(saved);
    }
    
    /**
     * Get comments for a file with threading.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getFileComments(UUID fileId, Pageable pageable) {
        // Get root comments
        Page<FileComment> rootComments = commentRepository
                .findByFileIdAndParentIdIsNullOrderByCreatedAtDesc(fileId, pageable);
        
        return rootComments.map(comment -> {
            CommentResponse response = mapToResponse(comment);
            
            // Load replies
            List<FileComment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId());
            response.setReplies(replies.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList()));
            
            return response;
        });
    }
    
    /**
     * Update a comment.
     */
    @Transactional
    public CommentResponse updateComment(UUID commentId, Long userId, CommentRequest request) {
        log.info("Updating comment - commentId: {}, userId: {}", commentId, userId);
        
        FileComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Comment not found"));
        
        // Verify ownership
        if (!comment.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot edit another user's comment");
        }
        
        comment.setContent(request.getContent());
        comment.setMentions(request.getMentions());
        comment.setIsEdited(true);
        comment.setUpdatedAt(LocalDateTime.now());
        
        FileComment updated = commentRepository.save(comment);
        
        log.info("Comment updated successfully - commentId: {}", commentId);
        
        return mapToResponse(updated);
    }
    
    /**
     * Delete a comment.
     */
    @Transactional
    public void deleteComment(UUID commentId, Long userId) {
        log.info("Deleting comment - commentId: {}, userId: {}", commentId, userId);
        
        FileComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Comment not found"));
        
        // Verify ownership
        if (!comment.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot delete another user's comment");
        }
        
        // Log activity
        activityService.logActivity(
                userId,
                ActivityType.COMMENT_DELETED,
                EntityType.FILE,
                comment.getFileId(),
                "Comment",
                "deleted"
        );
        
        commentRepository.delete(comment);
        
        log.info("Comment deleted successfully - commentId: {}", commentId);
    }
    
    /**
     * Get comment count for a file.
     */
    @Transactional(readOnly = true)
    public long getCommentCount(UUID fileId) {
        return commentRepository.countByFileId(fileId);
    }
    
    /**
     * Get comments mentioning a user.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsMentioningUser(Long userId, Pageable pageable) {
        Page<FileComment> comments = commentRepository.findCommentsMentioningUser(userId, pageable);
        return comments.map(this::mapToResponse);
    }
    
    /**
     * Map FileComment to response DTO.
     */
    private CommentResponse mapToResponse(FileComment comment) {
        User user = userRepository.findById(comment.getUserId()).orElse(null);
        
        long replyCount = commentRepository.countByParentId(comment.getId());
        
        return CommentResponse.builder()
                .id(comment.getId())
                .fileId(comment.getFileId())
                .userId(comment.getUserId())
                .username(user != null ? user.getUsername() : "Unknown")
                .userAvatar(user != null ? user.getProfilePicture() : null)
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .mentions(comment.getMentions())
                .isEdited(comment.getIsEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replyCount(replyCount)
                .timeAgo(formatTimeAgo(comment.getCreatedAt()))
                .build();
    }
    
    /**
     * Format time ago.
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        
        if (seconds < 60) return seconds + "s ago";
        if (seconds < 3600) return (seconds / 60) + "m ago";
        if (seconds < 86400) return (seconds / 3600) + "h ago";
        if (seconds < 2592000) return (seconds / 86400) + "d ago";
        if (seconds < 31536000) return (seconds / 2592000) + "mo ago";
        return (seconds / 31536000) + "y ago";
    }
}
