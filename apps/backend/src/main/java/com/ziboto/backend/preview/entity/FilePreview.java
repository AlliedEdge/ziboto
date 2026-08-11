package com.ziboto.backend.preview.entity;

import com.ziboto.backend.preview.enums.PreviewType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a file preview.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Entity
@Table(name = "file_previews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePreview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "file_id", nullable = false)
    private UUID fileId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "preview_type", nullable = false)
    private PreviewType previewType;
    
    @Lob
    @Column(name = "preview_data")
    private byte[] previewData;
    
    @Column(name = "preview_url", length = 1000)
    private String previewUrl;
    
    @Column(name = "width")
    private Integer width;
    
    @Column(name = "height")
    private Integer height;
    
    @Column(name = "duration")
    private Integer duration;
    
    @Column(name = "page_count")
    private Integer pageCount;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
