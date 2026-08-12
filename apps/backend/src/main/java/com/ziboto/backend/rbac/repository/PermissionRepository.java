package com.ziboto.backend.rbac.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.rbac.entity.Permission;

/**
 * Repository for Permission entity.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * Find permission by name.
     */
    Optional<Permission> findByName(String name);
    
    /**
     * Find permissions by resource.
     */
    List<Permission> findByResource(String resource);
    
    /**
     * Find permissions by resource and action.
     */
    Optional<Permission> findByResourceAndAction(String resource, String action);
    
    /**
     * Find all permissions for a resource (including wildcards).
     */
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource OR p.name = CONCAT(:resource, ':*')")
    List<Permission> findByResourceIncludingWildcard(@Param("resource") String resource);
    
    /**
     * Check if permission exists by name.
     */
    boolean existsByName(String name);
    
    /**
     * Find permissions by names.
     */
    @Query("SELECT p FROM Permission p WHERE p.name IN :names")
    Set<Permission> findByNameIn(@Param("names") List<String> names);
}
