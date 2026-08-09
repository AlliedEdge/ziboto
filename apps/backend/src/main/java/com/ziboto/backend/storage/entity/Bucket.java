package com.ziboto.backend.storage.entity;

import com.ziboto.backend.common.entity.BaseEntity;
import com.ziboto.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "buckets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bucket extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @Column(nullable = false)
    private Boolean isPublic = false;
    
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private BucketStatus status = BucketStatus.ACTIVE;
}
