package com.subscription.controller;

import com.subscription.dto.ApiResponse;
import com.subscription.dto.InviteUserRequest;
import com.subscription.dto.InvitationDto;
import com.subscription.model.Invitation;
import com.subscription.model.Organization;
import com.subscription.model.User;
import com.subscription.model.UserRole;
import com.subscription.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;

    private InvitationDto toDto(Invitation invitation) {
        return InvitationDto.builder()
                .id(invitation.getId())
                .email(invitation.getEmail())
                .firstName(invitation.getFirstName())
                .lastName(invitation.getLastName())
                .role(invitation.getRole())
                .organizationId(invitation.getOrganization() != null ? invitation.getOrganization().getId() : null)
                .organizationName(invitation.getOrganization() != null ? invitation.getOrganization().getName() : null)
                .invitedById(invitation.getInvitedBy() != null ? invitation.getInvitedBy().getId() : null)
                .invitedByName(invitation.getInvitedBy() != null ? invitation.getInvitedBy().getFullName() : null)
                .token(invitation.getToken())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
                .acceptedAt(invitation.getAcceptedAt())
                .build();
    }

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<InvitationDto>> inviteUser(@Valid @RequestBody InviteUserRequest request) {
        try {
            // Mock current user (should come from security context)
            User currentUser = User.builder()
                    .id(1L)
                    .organization(Organization.builder().id(1L).name("Test Org").build())
                    .role(UserRole.ADMIN)
                    .build();
            Invitation invitation = invitationService.inviteUser(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Invitation sent successfully", toDto(invitation)));
        } catch (Exception e) {
            log.error("Error inviting user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to send invitation: " + e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<InvitationDto>>> getPendingInvitations(@RequestParam Long organizationId) {
        try {
            List<Invitation> invitations = invitationService.getPendingInvitations(organizationId);
            List<InvitationDto> dtos = invitations.stream().map(this::toDto).collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("Pending invitations fetched", dtos));
        } catch (Exception e) {
            log.error("Error fetching pending invitations: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch invitations: " + e.getMessage()));
        }
    }

    @PostMapping("/cancel/{invitationId}")
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(@PathVariable Long invitationId, @RequestParam Long organizationId) {
        try {
            // Mock current user (should come from security context)
            User currentUser = User.builder()
                    .id(1L)
                    .organization(Organization.builder().id(organizationId).build())
                    .role(UserRole.ADMIN)
                    .build();
            invitationService.cancelInvitation(invitationId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Invitation cancelled successfully"));
        } catch (Exception e) {
            log.error("Error cancelling invitation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to cancel invitation: " + e.getMessage()));
        }
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<ApiResponse<InvitationDto>> getInvitationByToken(@PathVariable String token) {
        try {
            Invitation invitation = invitationService.getInvitationByToken(token);
            return ResponseEntity.ok(ApiResponse.success("Invitation fetched", toDto(invitation)));
        } catch (Exception e) {
            log.error("Error fetching invitation by token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch invitation: " + e.getMessage()));
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@RequestParam String token, @RequestParam String username, @RequestParam String password) {
        try {
            invitationService.acceptInvitation(token, username, password);
            return ResponseEntity.ok(ApiResponse.success("Invitation accepted successfully"));
        } catch (Exception e) {
            log.error("Error accepting invitation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to accept invitation: " + e.getMessage()));
        }
    }
} 