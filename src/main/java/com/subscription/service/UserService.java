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
        if (userId == null || userId <= 0) {
            throw new InvalidOperationException("Invalid user ID");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        if (user.getOrganization().getId() != organizationId) {
            throw new UnauthorizedException("User does not belong to the specified organization");
        }
        
        return convertToDto(user);
    }
    
    public UserDto updateUserRole(Long userId, UpdateUserRoleRequest request, User currentUser) {
        if (request == null || request.getRole() == null) {
            throw new InvalidOperationException("Role is required");
        }
        
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        if (userToUpdate.getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Cannot modify your own role");
        }
        
        if (!currentUser.canManageUser(userToUpdate)) {
            throw new UnauthorizedException("You don't have permission to manage this user");
        }
        
        if (!currentUser.getOrganization().getId().equals(userToUpdate.getOrganization().getId())) {
            throw new UnauthorizedException("Users must belong to the same organization");
        }
        
        UserRole newRole = request.getRole();
        UserRole oldRole = userToUpdate.getRole();
        
        validateRoleChange(userToUpdate, newRole, currentUser);
        
        userToUpdate.setRole(newRole);
        User savedUser = userRepository.save(userToUpdate);
        
        log.info("User role updated: {} -> {} for user: {}", oldRole, newRole, userToUpdate.getEmail());
        
        return convertToDto(savedUser);
    }
    
    public void removeUser(Long userId, User currentUser) {
        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        if (!currentUser.canManageUser(userToRemove)) {
            throw new UnauthorizedException("You don't have permission to remove this user");
        }
        
        if (!currentUser.getOrganization().getId().equals(userToRemove.getOrganization().getId())) {
            throw new UnauthorizedException("Users must belong to the same organization");
        }
        
        if (userToRemove.isAdmin() || userToRemove.isUnpaidAdmin()) {
            long adminCount = userRepository.countAdminsByOrganizationId(userToRemove.getOrganization().getId());
            if (adminCount <= 1) {
                log.warn("Attempting to remove the last admin from organization");
            }
        }
        
        userRepository.delete(userToRemove);
        log.info("User removed: {} by user: {}", userToRemove.getEmail(), currentUser.getEmail());
    }
    
    public UserDto createUser(User user, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 3) {
            throw new InvalidOperationException("Password must be at least 3 characters long");
        }
        
        log.info("Creating user with email: " + user.getEmail());
        
        if (userRepository.existsByEmailAndOrganizationId(user.getEmail(), user.getOrganization().getId())) {
            throw new InvalidOperationException("User with this email already exists in the organization");
        }
        
        user.setPassword(rawPassword);
        
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
        
        if (!currentUser.canManageUser(userToUpdate)) {
            throw new UnauthorizedException("You don't have permission to manage this user");
        }
        
        if (!currentUser.getOrganization().getId().equals(userToUpdate.getOrganization().getId())) {
            throw new UnauthorizedException("Users must belong to the same organization");
        }
        
        if (status == null) {
            throw new InvalidOperationException("Status is required");
        }
        
        if ((userToUpdate.isAdmin() || userToUpdate.isUnpaidAdmin()) && status == UserStatus.INACTIVE) {
            long adminCount = userRepository.countAdminsByOrganizationId(userToUpdate.getOrganization().getId());
            if (adminCount <= 1) {
                log.warn("Attempting to deactivate the last admin from organization");
            }
        }
        
        userToUpdate.setStatus(status);
        User savedUser = userRepository.save(userToUpdate);
        
        log.info("User status updated: {} -> {} for user: {}", userToUpdate.getStatus(), status, userToUpdate.getEmail());
        
        return convertToDto(savedUser);
    }
    
    private void validateRoleChange(User userToUpdate, UserRole newRole, User currentUser) {
        UserRole oldRole = userToUpdate.getRole();
        
        if (oldRole == newRole) {
            return;
        }
        
        if ((oldRole == UserRole.ADMIN || oldRole == UserRole.UNPAID_ADMIN) && 
            (newRole == UserRole.MEMBER || newRole == UserStatus.INACTIVE)) {
            
            long adminCount = userRepository.countAdminsByOrganizationId(userToUpdate.getOrganization().getId());
            if (adminCount <= 1) {
                log.warn("Attempting to demote the last admin from organization");
            }
        }
        
        if (currentUser.isUnpaidAdmin() && newRole == UserRole.ADMIN) {
            log.warn("Unpaid admin attempting to promote user to admin role");
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
    
    public String getUserPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user.getPassword();
    }
    
    public void updateUserEmail(Long userId, String newEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (newEmail == null || newEmail.isEmpty()) {
            throw new InvalidOperationException("Email is required");
        }
        
        user.setEmail(newEmail);
        userRepository.save(user);
    }
    
    public void promoteUserToAdmin(Long userId, User currentUser) {
        User userToPromote = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedException("Only admins can promote users");
        }
        
        userToPromote.setRole(UserRole.ADMIN);
        userRepository.save(userToPromote);
    }
} 