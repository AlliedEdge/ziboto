package com.ziboto.backend.gallery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a file in a gallery.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Entity
@Table(name = "gallery_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "gallery_id", nullable = false)
    private UUID galleryId;
    
    @Column(name = "file_id", nullable = false)
    private UUID fileId;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;
    
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;
    
    @PrePersist
    protected void onCreate() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}
