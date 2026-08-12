package com.ziboto.backend.rbac.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.rbac.entity.Role;
import com.ziboto.backend.rbac.enums.RoleType;

/**
 * Repository for Role entity.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * Find role by name.
     */
    Optional<Role> findByName(RoleType name);
    
    /**
     * Get all roles ordered by level.
     */
    @Query("SELECT r FROM Role r ORDER BY r.level ASC")
    List<Role> findAllOrderedByLevel();
    
    /**
     * Get roles with level less than or equal to specified level.
     */
    @Query("SELECT r FROM Role r WHERE r.level <= :level ORDER BY r.level ASC")
    List<Role> findRolesWithLevelLessThanOrEqual(@Param("level") Integer level);
    
    /**
     * Get child roles (roles with parent = specified role).
     */
    @Query("SELECT r FROM Role r WHERE r.parentRole = :parentRole")
    List<Role> findByParentRole(@Param("parentRole") RoleType parentRole);
}
