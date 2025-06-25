package com.subscription.repository;

import com.subscription.model.User;
import com.subscription.model.UserRole;
import com.subscription.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmailAndOrganizationId(String email, Long organizationId);
    
    List<User> findByOrganizationId(Long organizationId);
    
    List<User> findByOrganizationIdAndRoleIn(Long organizationId, List<UserRole> roles);
    
    @Query("SELECT u FROM User u WHERE u.organization.id = :organizationId AND (u.role = 'ADMIN' OR u.role = 'UNPAID_ADMIN')")
    List<User> findAdminsByOrganizationId(@Param("organizationId") Long organizationId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :organizationId AND (u.role = 'ADMIN' OR u.role = 'UNPAID_ADMIN')")
    long countAdminsByOrganizationId(@Param("organizationId") Long organizationId);
    
    boolean existsByEmailAndOrganizationId(String email, Long organizationId);
    
    List<User> findByOrganizationIdAndStatus(Long organizationId, UserStatus status);
} 