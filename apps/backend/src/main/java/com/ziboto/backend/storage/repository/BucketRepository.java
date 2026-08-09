package com.ziboto.backend.storage.repository;

import com.ziboto.backend.storage.entity.Bucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BucketRepository extends JpaRepository<Bucket, Long> {
    
    @Query("SELECT COUNT(b) FROM Bucket b WHERE b.owner.id = :ownerId")
    Long countByOwnerId(Long ownerId);
}
