package com.ziboto.backend.share.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.ziboto.backend.share.dto.*;
import com.ziboto.backend.share.service.ShareService;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for file sharing operations.
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>POST /api/v1/shares/files/{fileId} - Share file with user</li>
 *   <li>GET /api/v1/shares/received - Get files shared with me</li>
 *   <li>GET /api/v1/shares/sent - Get files I shared</li>
 *   <li>POST /api/v1/shares/{shareId}/accept - Accept share</li>
 *   <li>POST /api/v1/shares/{shareId}/decline - Decline share</li>
 *   <li>DELETE /api/v1/shares/{shareId} - Revoke share</li>
 *   <li>POST /api/v1/shares/links/{fileId} - Create share link</li>
 *   <li>GET /api/v1/shares/links/{fileId} - Get share links for file</li>
 *   <li>DELETE /api/v1/shares/links/{linkId} - Revoke share link</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
public class ShareController {
    
    private final ShareService shareService;
    private final UserRepository userRepository;
    
    // -------------------------------------------------------------------------
    // File Sharing (User to User)
    // -------------------------------------------------------------------------
    
    /**
     * Share a file with another user.
     */
    @PostMapping("/files/{fileId}")
    public ResponseEntity<FileShareResponse> shareFile(
            @PathVariable UUID fileId,
            @Valid @RequestBody CreateFileShareRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        FileShareResponse response = shareService.shareFile(fileId, user.getId(), request, user.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get files shared with current user.
     */
    @GetMapping("/received")
    public ResponseEntity<Page<FileShareResponse>> getFilesSharedWithMe(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        Page<FileShareResponse> shares = shareService.getFilesSharedWithMe(user.getId(), pageable);
        
        return ResponseEntity.ok(shares);
    }
    
    /**
     * Get files shared by current user.
     */
    @GetMapping("/sent")
    public ResponseEntity<Page<FileShareResponse>> getFilesSharedByMe(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        Page<FileShareResponse> shares = shareService.getFilesSharedByMe(user.getId(), pageable);
        
        return ResponseEntity.ok(shares);
    }
    
    /**
     * Accept a file share invitation.
     */
    @PostMapping("/{shareId}/accept")
    public ResponseEntity<FileShareResponse> acceptShare(
            @PathVariable UUID shareId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        FileShareResponse response = shareService.acceptShare(shareId, user.getId(), user.getUsername());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Decline a file share invitation.
     */
    @PostMapping("/{shareId}/decline")
    public ResponseEntity<Void> declineShare(
            @PathVariable UUID shareId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        shareService.declineShare(shareId, user.getId(), user.getUsername());
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Revoke a file share (by owner).
     */
    @DeleteMapping("/{shareId}")
    public ResponseEntity<Void> revokeShare(
            @PathVariable UUID shareId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        shareService.revokeShare(shareId, user.getId(), user.getUsername());
        
        return ResponseEntity.noContent().build();
    }
    
    // -------------------------------------------------------------------------
    // Share Links (Public/Anonymous)
    // -------------------------------------------------------------------------
    
    /**
     * Create a public share link for a file.
     */
    @PostMapping("/links/{fileId}")
    public ResponseEntity<ShareLinkResponse> createShareLink(
            @PathVariable UUID fileId,
            @Valid @RequestBody CreateShareLinkRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        ShareLinkResponse response = shareService.createShareLink(fileId, user.getId(), request, user.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get share links for a file.
     */
    @GetMapping("/links/{fileId}")
    public ResponseEntity<List<ShareLinkResponse>> getShareLinksForFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        List<ShareLinkResponse> links = shareService.getShareLinksForFile(fileId, user.getId());
        
        return ResponseEntity.ok(links);
    }
    
    /**
     * Revoke a share link.
     */
    @DeleteMapping("/links/{linkId}")
    public ResponseEntity<Void> revokeShareLink(
            @PathVariable UUID linkId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        shareService.revokeShareLink(linkId, user.getId());
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get share link info by token (public access - no auth required).
     */
    @GetMapping("/public/{token}")
    public ResponseEntity<ShareLinkResponse> getPublicShareLink(@PathVariable String token) {
        ShareLinkResponse response = shareService.getShareLink(token);
        return ResponseEntity.ok(response);
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
