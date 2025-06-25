package com.subscription.dto;

import com.subscription.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {
    
    @NotNull(message = "Role is required")
    private UserRole role;
} 