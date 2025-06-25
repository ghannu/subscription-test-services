package com.subscription.service;

import com.subscription.dto.InviteUserRequest;
import com.subscription.exception.InvalidOperationException;
import com.subscription.exception.UnauthorizedException;
import com.subscription.model.*;
import com.subscription.repository.InvitationRepository;
import com.subscription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvitationService {
    
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    
    @Value("${app.invitation.expiration-hours:24}")
    private int invitationExpirationHours;
    
    @Value("${app.invitation.base-url}")
    private String invitationBaseUrl;
    
    public Invitation inviteUser(InviteUserRequest request, User currentUser) {
        // Check if current user can invite users
        if (!currentUser.isAdmin() && !currentUser.isUnpaidAdmin()) {
            throw new UnauthorizedException("Only admins can invite users");
        }
        
        // Check if user already exists in the organization
        if (userRepository.existsByEmailAndOrganizationId(request.getEmail(), currentUser.getOrganization().getId())) {
            throw new InvalidOperationException("User with this email already exists in the organization");
        }
        
        // Check if there's already a pending invitation for this email
        Optional<Invitation> existingInvitation = invitationRepository.findByEmailAndOrganizationId(
                request.getEmail(), currentUser.getOrganization().getId());
        
        if (existingInvitation.isPresent() && existingInvitation.get().isValid()) {
            throw new InvalidOperationException("An invitation for this email is already pending");
        }
        
        // Create new invitation
        Invitation invitation = Invitation.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .organization(currentUser.getOrganization())
                .invitedBy(currentUser)
                .expiresAt(LocalDateTime.now().plusHours(invitationExpirationHours))
                .build();
        
        Invitation savedInvitation = invitationRepository.save(invitation);
        
        // Send invitation email
        sendInvitationEmail(savedInvitation);
        
        log.info("Invitation sent to {} by user: {}", request.getEmail(), currentUser.getEmail());
        
        return savedInvitation;
    }
    
    public List<Invitation> getPendingInvitations(Long organizationId) {
        return invitationRepository.findByOrganizationIdAndStatus(organizationId, InvitationStatus.PENDING);
    }
    
    public void cancelInvitation(Long invitationId, User currentUser) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvalidOperationException("Invitation not found"));
        
        // Check if current user can cancel this invitation
        if (!invitation.getInvitedBy().getId().equals(currentUser.getId()) && 
            !currentUser.isAdmin()) {
            throw new UnauthorizedException("You don't have permission to cancel this invitation");
        }
        
        // Check if invitation belongs to the same organization
        if (!invitation.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new UnauthorizedException("Invitation does not belong to your organization");
        }
        
        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
        
        log.info("Invitation cancelled: {} by user: {}", invitation.getEmail(), currentUser.getEmail());
    }
    
    public Invitation getInvitationByToken(String token) {
        return invitationRepository.findByToken(token)
                .orElseThrow(() -> new InvalidOperationException("Invalid invitation token"));
    }
    
    public void acceptInvitation(String token, String username, String password) {
        Invitation invitation = getInvitationByToken(token);
        
        if (!invitation.isValid()) {
            throw new InvalidOperationException("Invitation is not valid or has expired");
        }
        
        // Create new user
        User newUser = User.builder()
                .username(username)
                .email(invitation.getEmail())
                .firstName(invitation.getFirstName())
                .lastName(invitation.getLastName())
                .role(invitation.getRole())
                .organization(invitation.getOrganization())
                .status(UserStatus.ACTIVE)
                .build();
        
        // Save user (password will be encoded in UserService)
        userRepository.save(newUser);
        
        // Mark invitation as accepted
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
        
        log.info("Invitation accepted: {} by user: {}", invitation.getEmail(), username);
    }
    
    private void sendInvitationEmail(Invitation invitation) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(invitation.getEmail());
            message.setSubject("You've been invited to join " + invitation.getOrganization().getName());
            
            String invitationUrl = invitationBaseUrl + "/accept-invitation?token=" + invitation.getToken();
            
            message.setText(String.format(
                "Hello %s %s,\n\n" +
                "You have been invited to join %s as a %s.\n\n" +
                "Click the following link to accept the invitation:\n%s\n\n" +
                "This invitation will expire in %d hours.\n\n" +
                "Best regards,\n%s",
                invitation.getFirstName(),
                invitation.getLastName(),
                invitation.getOrganization().getName(),
                invitation.getRole().name().toLowerCase(),
                invitationUrl,
                invitationExpirationHours,
                invitation.getInvitedBy().getFullName()
            ));
            
            mailSender.send(message);
            log.info("Invitation email sent to: {}", invitation.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to send invitation email to: {}", invitation.getEmail(), e);
            // Don't throw exception as the invitation is still created
        }
    }
    
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void expireOldInvitations() {
        LocalDateTime now = LocalDateTime.now();
        List<Invitation> expiredInvitations = invitationRepository.findExpiredInvitations(now);
        
        for (Invitation invitation : expiredInvitations) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            log.info("Expired invitation: {}", invitation.getEmail());
        }
    }
} 