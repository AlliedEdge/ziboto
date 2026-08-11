package com.ziboto.backend.share.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.messaging.event.NotificationEvent;
import com.ziboto.backend.messaging.publisher.EventPublisher;
import com.ziboto.backend.share.dto.*;
import com.ziboto.backend.share.entity.FileShare;
import com.ziboto.backend.share.entity.ShareLink;
import com.ziboto.backend.share.enums.ShareStatus;
import com.ziboto.backend.share.repository.FileShareRepository;
import com.ziboto.backend.share.repository.ShareLinkRepository;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing file sharing.
 * 
 * <p>Handles:</p>
 * <ul>
 *   <li>Direct user-to-user file sharing</li>
 *   <li>Public share link generation</li>
 *   <li>Permission validation</li>
 *   <li>Share acceptance/revocation</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {
    
    private final FileShareRepository fileShareRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    
    @Value("${app.share-link.base-url:http://localhost:5173/share}")
    private String shareLinkBaseUrl;
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;
    
    // -------------------------------------------------------------------------
    // File Sharing (User to User)
    // -------------------------------------------------------------------------
    
    /**
     * Share a file with another user.
     */
    @Transactional
    public FileShareResponse shareFile(UUID fileId, Long ownerId, CreateFileShareRequest request, String ownerUsername) {
        log.info("Sharing file: fileId={}, owner={}, recipient={}", fileId, ownerId, request.getRecipientEmail());
        
        // Validate file ownership
        FileMetadata file = fileMetadataRepository.findByIdAndUserId(fileId, ownerId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found or access denied"));
        
        // Find recipient user
        User recipient = userRepository.findByEmail(request.getRecipientEmail())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Recipient user not found"));
        
        // Cannot share with self
        if (recipient.getId().equals(ownerId)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Cannot share file with yourself");
        }
        
        // Check if already shared
        fileShareRepository.findByFileIdAndSharedWithUserId(fileId, recipient.getId())
                .ifPresent(existing -> {
                    if (existing.getStatus() != ShareStatus.DECLINED) {
                        throw new BaseException(ErrorCode.CONFLICT, "File already shared with this user");
                    }
                });
        
        // Create share
        FileShare share = FileShare.builder()
                .fileId(fileId)
                .ownerId(ownerId)
                .sharedWithUserId(recipient.getId())
                .permission(request.getPermission())
                .status(ShareStatus.PENDING)
                .message(request.getMessage())
                .expiresAt(request.getExpiresAt())
                .createdBy(ownerUsername)
                .updatedBy(ownerUsername)
                .build();
        
        share = fileShareRepository.save(share);
        log.info("File shared successfully: shareId={}", share.getId());
        
        // Send notification to recipient
        try {
            NotificationEvent notification = NotificationEvent.builder()
                    .userId(recipient.getId())
                    .type(NotificationEvent.NotificationType.FILE_SHARED)
                    .title("New File Shared With You")
                    .message(String.format("%s shared '%s' with you", ownerUsername, file.getFileName()))
                    .relatedEntityId(fileId)
                    .relatedEntityType("FILE")
                    .priority(NotificationEvent.Priority.NORMAL)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            eventPublisher.publishNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send share notification", e);
        }
        
        return buildFileShareResponse(share, file, recipient);
    }
    
    /**
     * Accept a file share invitation.
     */
    @Transactional
    public FileShareResponse acceptShare(UUID shareId, Long userId, String username) {
        log.info("Accepting share: shareId={}, userId={}", shareId, userId);
        
        FileShare share = fileShareRepository.findById(shareId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Share not found"));
        
        // Verify user is the recipient
        if (!share.getSharedWithUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Not authorized to accept this share");
        }
        
        // Check if already accepted/declined/revoked
        if (share.getStatus() != ShareStatus.PENDING) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Share is not pending");
        }
        
        // Accept share
        share.setStatus(ShareStatus.ACCEPTED);
        share.setAcceptedAt(LocalDateTime.now());
        share.setUpdatedBy(username);
        
        share = fileShareRepository.save(share);
        log.info("Share accepted: shareId={}", shareId);
        
        FileMetadata file = fileMetadataRepository.findById(share.getFileId())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        
        return buildFileShareResponse(share, file, recipient);
    }
    
    /**
     * Decline a file share invitation.
     */
    @Transactional
    public void declineShare(UUID shareId, Long userId, String username) {
        log.info("Declining share: shareId={}, userId={}", shareId, userId);
        
        FileShare share = fileShareRepository.findById(shareId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Share not found"));
        
        if (!share.getSharedWithUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Not authorized to decline this share");
        }
        
        if (share.getStatus() != ShareStatus.PENDING) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Share is not pending");
        }
        
        share.setStatus(ShareStatus.DECLINED);
        share.setUpdatedBy(username);
        fileShareRepository.save(share);
        
        log.info("Share declined: shareId={}", shareId);
    }
    
    /**
     * Revoke a file share (by owner).
     */
    @Transactional
    public void revokeShare(UUID shareId, Long ownerId, String username) {
        log.info("Revoking share: shareId={}, ownerId={}", shareId, ownerId);
        
        FileShare share = fileShareRepository.findById(shareId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Share not found"));
        
        if (!share.getOwnerId().equals(ownerId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Not authorized to revoke this share");
        }
        
        share.setStatus(ShareStatus.REVOKED);
        share.setUpdatedBy(username);
        fileShareRepository.save(share);
        
        log.info("Share revoked: shareId={}", shareId);
    }
    
    /**
     * Get files shared with a user.
     */
    @Transactional(readOnly = true)
    public Page<FileShareResponse> getFilesSharedWithMe(Long userId, Pageable pageable) {
        Page<FileShare> shares = fileShareRepository.findBySharedWithUserIdOrderByCreatedAtDesc(userId, pageable);
        return shares.map(this::buildFileShareResponse);
    }
    
    /**
     * Get files shared by a user.
     */
    @Transactional(readOnly = true)
    public Page<FileShareResponse> getFilesSharedByMe(Long userId, Pageable pageable) {
        Page<FileShare> shares = fileShareRepository.findByOwnerIdOrderByCreatedAtDesc(userId, pageable);
        return shares.map(this::buildFileShareResponse);
    }
    
    // -------------------------------------------------------------------------
    // Share Links (Public/Anonymous)
    // -------------------------------------------------------------------------
    
    /**
     * Create a public share link for a file.
     */
    @Transactional
    public ShareLinkResponse createShareLink(UUID fileId, Long ownerId, CreateShareLinkRequest request, String username) {
        log.info("Creating share link: fileId={}, owner={}", fileId, ownerId);
        
        // Validate file ownership
        FileMetadata file = fileMetadataRepository.findByIdAndUserId(fileId, ownerId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found or access denied"));
        
        // Generate unique token
        String token = generateUniqueToken();
        
        // Hash password if provided
        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }
        
        // Create share link
        ShareLink shareLink = ShareLink.builder()
                .fileId(fileId)
                .ownerId(ownerId)
                .token(token)
                .permission(request.getPermission())
                .passwordHash(passwordHash)
                .expiresAt(request.getExpiresAt())
                .maxDownloads(request.getMaxDownloads())
                .createdBy(username)
                .updatedBy(username)
                .build();
        
        shareLink = shareLinkRepository.save(shareLink);
        log.info("Share link created: linkId={}, token={}", shareLink.getId(), token);
        
        return buildShareLinkResponse(shareLink, file);
    }
    
    /**
     * Get share link by token (for public access).
     */
    @Transactional(readOnly = true)
    public ShareLinkResponse getShareLink(String token) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Share link not found"));
        
        FileMetadata file = fileMetadataRepository.findById(shareLink.getFileId())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        return buildShareLinkResponse(shareLink, file);
    }
    
    /**
     * Revoke a share link.
     */
    @Transactional
    public void revokeShareLink(UUID linkId, Long ownerId) {
        log.info("Revoking share link: linkId={}, ownerId={}", linkId, ownerId);
        
        ShareLink shareLink = shareLinkRepository.findById(linkId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Share link not found"));
        
        if (!shareLink.getOwnerId().equals(ownerId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Not authorized to revoke this link");
        }
        
        shareLinkRepository.delete(shareLink);
        log.info("Share link revoked: linkId={}", linkId);
    }
    
    /**
     * Get share links for a file.
     */
    @Transactional(readOnly = true)
    public List<ShareLinkResponse> getShareLinksForFile(UUID fileId, Long ownerId) {
        // Verify file ownership
        fileMetadataRepository.findByIdAndUserId(fileId, ownerId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found or access denied"));
        
        List<ShareLink> shareLinks = shareLinkRepository.findByFileIdOrderByCreatedAtDesc(fileId);
        
        FileMetadata file = fileMetadataRepository.findById(fileId).orElseThrow();
        
        return shareLinks.stream()
                .map(link -> buildShareLinkResponse(link, file))
                .collect(Collectors.toList());
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    private String generateUniqueToken() {
        String token;
        do {
            byte[] randomBytes = new byte[TOKEN_LENGTH];
            SECURE_RANDOM.nextBytes(randomBytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        } while (shareLinkRepository.existsByToken(token));
        
        return token;
    }
    
    private FileShareResponse buildFileShareResponse(FileShare share) {
        FileMetadata file = fileMetadataRepository.findById(share.getFileId()).orElse(null);
        User recipient = userRepository.findById(share.getSharedWithUserId()).orElse(null);
        return buildFileShareResponse(share, file, recipient);
    }
    
    private FileShareResponse buildFileShareResponse(FileShare share, FileMetadata file, User recipient) {
        User owner = userRepository.findById(share.getOwnerId()).orElse(null);
        
        String ownerFullName = owner != null ? 
            (owner.getFirstName() != null && owner.getLastName() != null ? 
                owner.getFirstName() + " " + owner.getLastName() : owner.getUsername()) : null;
        
        String recipientFullName = recipient != null ? 
            (recipient.getFirstName() != null && recipient.getLastName() != null ? 
                recipient.getFirstName() + " " + recipient.getLastName() : recipient.getUsername()) : null;
        
        return FileShareResponse.builder()
                .id(share.getId())
                .fileId(share.getFileId())
                .fileName(file != null ? file.getFileName() : null)
                .ownerId(share.getOwnerId())
                .ownerUsername(owner != null ? owner.getUsername() : null)
                .ownerFullName(ownerFullName)
                .sharedWithUserId(share.getSharedWithUserId())
                .sharedWithUsername(recipient != null ? recipient.getUsername() : null)
                .sharedWithFullName(recipientFullName)
                .sharedWithEmail(recipient != null ? recipient.getEmail() : null)
                .permission(share.getPermission())
                .status(share.getStatus())
                .message(share.getMessage())
                .expiresAt(share.getExpiresAt())
                .acceptedAt(share.getAcceptedAt())
                .createdAt(share.getCreatedAt())
                .expired(share.isExpired())
                .active(share.isActive())
                .build();
    }
    
    private ShareLinkResponse buildShareLinkResponse(ShareLink shareLink, FileMetadata file) {
        String shareUrl = shareLinkBaseUrl + "/" + shareLink.getToken();
        
        return ShareLinkResponse.builder()
                .id(shareLink.getId())
                .fileId(shareLink.getFileId())
                .fileName(file != null ? file.getFileName() : null)
                .ownerId(shareLink.getOwnerId())
                .token(shareLink.getToken())
                .shareUrl(shareUrl)
                .permission(shareLink.getPermission())
                .status(shareLink.getStatus())
                .passwordProtected(shareLink.isPasswordProtected())
                .expiresAt(shareLink.getExpiresAt())
                .maxDownloads(shareLink.getMaxDownloads())
                .downloadCount(shareLink.getDownloadCount())
                .lastAccessedAt(shareLink.getLastAccessedAt())
                .lastAccessedIp(shareLink.getLastAccessedIp())
                .createdAt(shareLink.getCreatedAt())
                .expired(shareLink.isExpired())
                .downloadLimitReached(shareLink.isDownloadLimitReached())
                .active(shareLink.isActive())
                .build();
    }
}
