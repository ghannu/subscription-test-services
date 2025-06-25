package com.subscription.service;

import com.subscription.dto.UserDto;
import com.subscription.dto.UpdateUserRoleRequest;
import com.subscription.exception.UnauthorizedException;
import com.subscription.exception.UserNotFoundException;
import com.subscription.exception.InvalidOperationException;
import com.subscription.model.*;
import com.subscription.repository.UserRepository;
import com.subscription.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    
    public List<UserDto> getAllUsersInOrganization(Long organizationId) {
        List<User> users = userRepository.findByOrganizationId(organizationId);
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public UserDto getUserById(Long userId, Long organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        if (!user.getOrganization().getId().equals(organizationId)) {
            throw new UnauthorizedException("User does not belong to the specified organization");
        }
        
        return convertToDto(user);
    }
    
    public UserDto updateUserRole(Long userId, UpdateUserRoleRequest request, User currentUser) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        // Check if current user can manage the target user
        if (!currentUser.canManageUser(userToUpdate)) {
            throw new UnauthorizedException("You don't have permission to manage this user");
        }
        
        // Check if users belong to the same organization
        if (!currentUser.getOrganization().getId().equals(userToUpdate.getOrganization().getId())) {
            throw new UnauthorizedException("Users must belong to the same organization");
        }
        
        UserRole newRole = request.getRole();
        UserRole oldRole = userToUpdate.getRole();
        
        // Validate role change
        validateRoleChange(userToUpdate, newRole, currentUser);
        
        // Update the role
        userToUpdate.setRole(newRole);
        User savedUser = userRepository.save(userToUpdate);
        
        log.info("User role updated: {} -> {} for user: {}", oldRole, newRole, userToUpdate.getEmail());
        
        return convertToDto(savedUser);
    }
    
    public void removeUser(Long userId, User currentUser) {
        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        // Check if current user can manage the target user
        if (!currentUser.canManageUser(userToRemove)) {
            throw new UnauthorizedException("You don't have permission to remove this user");
        }
        
        // Check if users belong to the same organization
        if (!currentUser.getOrganization().getId().equals(userToRemove.getOrganization().getId())) {
            throw new UnauthorizedException("Users must belong to the same organization");
        }
        
        // Check if removing the user would leave the organization without admins
        if (userToRemove.isAdmin() || userToRemove.isUnpaidAdmin()) {
            long adminCount = userRepository.countAdminsByOrganizationId(userToRemove.getOrganization().getId());
            if (adminCount <= 1) {
                throw new InvalidOperationException("Cannot remove the last admin from the organization");
            }
        }
        
        userRepository.delete(userToRemove);
        log.info("User removed: {} by user: {}", userToRemove.getEmail(), currentUser.getEmail());
    }
    
    public UserDto createUser(User user, String rawPassword) {
        // Check if user already exists in the organization
        if (userRepository.existsByEmailAndOrganizationId(user.getEmail(), user.getOrganization().getId())) {
            throw new InvalidOperationException("User with this email already exists in the organization");
        }
        
        // Encode password
        user.setPassword(passwordEncoder.encode(rawPassword));
        
        // Set default status if not provided
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        
        User savedUser = userRepository.save(user);
        log.info("User created: {} in organization: {}", savedUser.getEmail(), savedUser.getOrganization().getName());
        
        return convertToDto(savedUser);
    }
    
    public UserDto updateUserStatus(Long userId, UserStatus status, User currentUser) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        // Check if current user can manage the target user
        if (!currentUser.canManageUser(userToUpdate)) {
            throw new UnauthorizedException("You don't have permission to manage this user");
        }
        
        // Check if users belong to the same organization
        if (!currentUser.getOrganization().getId().equals(userToUpdate.getOrganization().getId())) {
            throw new UnauthorizedException("Users must belong to the same organization");
        }
        
        // Prevent deactivating the last admin
        if ((userToUpdate.isAdmin() || userToUpdate.isUnpaidAdmin()) && status == UserStatus.INACTIVE) {
            long adminCount = userRepository.countAdminsByOrganizationId(userToUpdate.getOrganization().getId());
            if (adminCount <= 1) {
                throw new InvalidOperationException("Cannot deactivate the last admin from the organization");
            }
        }
        
        userToUpdate.setStatus(status);
        User savedUser = userRepository.save(userToUpdate);
        
        log.info("User status updated: {} -> {} for user: {}", userToUpdate.getStatus(), status, userToUpdate.getEmail());
        
        return convertToDto(savedUser);
    }
    
    private void validateRoleChange(User userToUpdate, UserRole newRole, User currentUser) {
        UserRole oldRole = userToUpdate.getRole();
        
        // If the role is not changing, no validation needed
        if (oldRole == newRole) {
            return;
        }
        
        // Check if this would be the last admin being demoted
        if ((oldRole == UserRole.ADMIN || oldRole == UserRole.UNPAID_ADMIN) && 
            (newRole == UserRole.MEMBER || newRole == UserStatus.INACTIVE)) {
            
            long adminCount = userRepository.countAdminsByOrganizationId(userToUpdate.getOrganization().getId());
            if (adminCount <= 1) {
                throw new InvalidOperationException("Cannot demote the last admin from the organization");
            }
        }
        
        // Unpaid admin cannot promote someone to admin
        if (currentUser.isUnpaidAdmin() && newRole == UserRole.ADMIN) {
            throw new UnauthorizedException("Unpaid admin cannot promote users to admin role");
        }
    }
    
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .organizationId(user.getOrganization().getId())
                .organizationName(user.getOrganization().getName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
} 