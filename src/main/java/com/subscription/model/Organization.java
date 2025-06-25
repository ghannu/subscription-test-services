package com.subscription.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(unique = true)
    private String name;
    
    private String description;
    
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public long getAdminCount() {
        return users.stream()
                .filter(user -> user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.UNPAID_ADMIN)
                .count();
    }
    
    public boolean hasAtLeastOneAdmin() {
        return getAdminCount() > 0;
    }
} 