package com.subscription.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(unique = true)
    private String username;
    
    @NotBlank
    @Email
    @Column(unique = true)
    private String email;
    
    @NotBlank
    private String password;
    
    @NotBlank
    private String firstName;
    
    @NotBlank
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    public boolean isUnpaidAdmin() {
        return role == UserRole.UNPAID_ADMIN;
    }
    
    public boolean isMember() {
        return role == UserRole.MEMBER;
    }
    
    public boolean canManageUser(User targetUser) {
        if (this.equals(targetUser)) {
            return false; // Can't manage yourself
        }
        
        if (isAdmin()) {
            return true; // Admin can manage anyone
        }
        
        if (isUnpaidAdmin()) {
            return !targetUser.isAdmin(); // Unpaid admin can't manage admin
        }
        
        return false; // Member can't manage anyone
    }
} 