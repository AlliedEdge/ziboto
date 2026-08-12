package com.ziboto.backend.comment.controller;

import com.ziboto.backend.comment.dto.CommentRequest;
import com.ziboto.backend.comment.dto.CommentResponse;
import com.ziboto.backend.comment.service.CommentService;
import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for file comments.
 * 
 * @author Ziboto Team
 * @since V3
 */
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Comments", description = "Commenting system for files")
public class CommentController {
    
    private final CommentService commentService;
    private final UserRepository userRepository;
    
    /**
     * Add a comment to a file.
     * 
     * POST /api/v1/comments/files/{fileId}
     */
    @PostMapping("/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add comment", description = "Add a comment to a file")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID fileId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Add comment request - fileId: {}, userId: {}", fileId, userId);
        
        CommentResponse response = commentService.addComment(fileId, userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", response));
    }
    
    /**
     * Get comments for a file.
     * 
     * GET /api/v1/comments/files/{fileId}
     */
    @GetMapping("/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get file comments", description = "Get all comments for a file with threading")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getFileComments(
            @PathVariable UUID fileId,
            @PageableDefault(size = 50) Pageable pageable) {
        
        log.info("Get comments request - fileId: {}", fileId);
        
        Page<CommentResponse> comments = commentService.getFileComments(fileId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
    
    /**
     * Update a comment.
     * 
     * PUT /api/v1/comments/{commentId}
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update comment", description = "Update a comment (owner only)")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Update comment request - commentId: {}, userId: {}", commentId, userId);
        
        CommentResponse response = commentService.updateComment(commentId, userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Comment updated successfully", response));
    }
    
    /**
     * Delete a comment.
     * 
     * DELETE /api/v1/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete comment", description = "Delete a comment (owner only)")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Delete comment request - commentId: {}, userId: {}", commentId, userId);
        
        commentService.deleteComment(commentId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }
    
    /**
     * Get comment count for a file.
     * 
     * GET /api/v1/comments/files/{fileId}/count
     */
    @GetMapping("/files/{fileId}/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get comment count", description = "Get total number of comments for a file")
    public ResponseEntity<ApiResponse<Long>> getCommentCount(@PathVariable UUID fileId) {
        
        long count = commentService.getCommentCount(fileId);
        
        return ResponseEntity.ok(ApiResponse.success(count));
    }
    
    /**
     * Get comments mentioning current user.
     * 
     * GET /api/v1/comments/mentions
     */
    @GetMapping("/mentions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get mentions", description = "Get comments mentioning the current user")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getMentions(
            Authentication authentication,
            @PageableDefault(size = 50) Pageable pageable) {
        
        Long userId = getUserId(authentication);
        log.info("Get mentions request - userId: {}", userId);
        
        Page<CommentResponse> mentions = commentService.getCommentsMentioningUser(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(mentions));
    }
    
    /**
     * Extract user ID from authentication.
     */
    private Long getUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with username: " + username
                ));
        
        return user.getId();
    }
}
