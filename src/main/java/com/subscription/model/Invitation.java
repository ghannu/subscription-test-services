package com.subscription.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invitation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    private String firstName;
    
    @NotBlank
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;
    
    @Column(unique = true)
    private String token;
    
    @Enumerated(EnumType.STRING)
    private InvitationStatus status;
    
    private LocalDateTime expiresAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime acceptedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (token == null) {
            token = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = InvitationStatus.PENDING;
        }
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return status == InvitationStatus.PENDING && !isExpired();
    }
} 