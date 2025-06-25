package com.subscription.repository;

import com.subscription.model.Invitation;
import com.subscription.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    
    Optional<Invitation> findByToken(String token);
    
    Optional<Invitation> findByEmailAndOrganizationId(String email, Long organizationId);
    
    List<Invitation> findByOrganizationId(Long organizationId);
    
    List<Invitation> findByOrganizationIdAndStatus(Long organizationId, InvitationStatus status);
    
    @Query("SELECT i FROM Invitation i WHERE i.organization.id = :organizationId AND i.status = 'PENDING' AND i.expiresAt > :now")
    List<Invitation> findValidInvitationsByOrganizationId(@Param("organizationId") Long organizationId, @Param("now") LocalDateTime now);
    
    @Query("SELECT i FROM Invitation i WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    List<Invitation> findExpiredInvitations(@Param("now") LocalDateTime now);
} 