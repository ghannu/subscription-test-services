package com.subscription.dto;

import com.subscription.model.InvitationStatus;
import com.subscription.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private Long organizationId;
    private String organizationName;
    private Long invitedById;
    private String invitedByName;
    private String token;
    private InvitationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
} 