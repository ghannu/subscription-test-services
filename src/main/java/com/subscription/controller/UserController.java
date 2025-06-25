package com.subscription.controller;

import com.subscription.dto.ApiResponse;
import com.subscription.dto.CreateUserRequest;
import com.subscription.dto.UserDto;
import com.subscription.model.Organization;
import com.subscription.model.User;
import com.subscription.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            // Create User object from request
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .role(request.getRole())
                    .status(request.getStatus())
                    .organization(Organization.builder().id(request.getOrganizationId()).build())
                    .build();
            
            UserDto createdUser = userService.createUser(user, request.getPassword());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User created successfully", createdUser));
                    
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create user: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeUser(
            @PathVariable Long userId,
            @RequestParam Long organizationId) {
        try {
            // For now, we'll need to get the current user from security context
            // This is a simplified version - in a real application, you'd get the current user from security context
            User currentUser = User.builder()
                    .id(1L) // This should come from security context
                    .organization(Organization.builder().id(organizationId).build())
                    .role(com.subscription.model.UserRole.ADMIN) // This should come from security context
                    .build();
            
            userService.removeUser(userId, currentUser);
            
            return ResponseEntity.ok()
                    .body(ApiResponse.success("User removed successfully"));
                    
        } catch (Exception e) {
            log.error("Error removing user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to remove user: " + e.getMessage()));
        }
    }
} 