package com.subscription.controller;

import com.subscription.dto.*;
import com.subscription.model.Organization;
import com.subscription.model.User;
import com.subscription.model.UserRole;
import com.subscription.service.AadhaarVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aadhaar")
@RequiredArgsConstructor
@Slf4j
public class AadhaarVerificationController {

    private final AadhaarVerificationService aadhaarVerificationService;

    @PostMapping("/generate-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateOtp(@Valid @RequestBody AadhaarOtpRequest request) {
        try {
            log.info("Generating OTP for Aadhaar verification");
            
            Map<String, String> result = aadhaarVerificationService.generateOtp(request);
            
            return ResponseEntity.ok(ApiResponse.success("OTP generated successfully", result));
            
        } catch (Exception e) {
            log.error("Error generating OTP: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to generate OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AadhaarVerificationResponse>> verifyOtp(@Valid @RequestBody AadhaarOtpVerifyRequest request) {
        try {
            log.info("Verifying OTP for Aadhaar verification");
            
            AadhaarVerificationResponse response = aadhaarVerificationService.verifyOtp(request);
            
            if (response.isVerified()) {
                return ResponseEntity.ok(ApiResponse.success("Aadhaar verification successful", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Aadhaar verification failed: " + response.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("Error verifying OTP: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to verify OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AadhaarVerificationResponse>> verifyAadhaar(@Valid @RequestBody AadhaarVerificationRequest request) {
        try {
            log.info("Performing Aadhaar verification");
            
            // Mock current user (should come from security context)
            User currentUser = User.builder()
                    .id(1L)
                    .email("user@example.com")
                    .organization(Organization.builder().id(1L).name("Test Org").build())
                    .role(UserRole.ADMIN)
                    .build();
            
            AadhaarVerificationResponse response = aadhaarVerificationService.verifyAadhaar(request, currentUser);
            
            if (response.isVerified()) {
                return ResponseEntity.ok(ApiResponse.success("Aadhaar verification successful", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Aadhaar verification failed: " + response.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("Error verifying Aadhaar: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to verify Aadhaar: " + e.getMessage()));
        }
    }

    @GetMapping("/transaction-id")
    public ResponseEntity<ApiResponse<String>> generateTransactionId() {
        try {
            String transactionId = aadhaarVerificationService.generateTransactionId();
            return ResponseEntity.ok(ApiResponse.success("Transaction ID generated", transactionId));
        } catch (Exception e) {
            log.error("Error generating transaction ID: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to generate transaction ID: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{verificationId}")
    public ResponseEntity<ApiResponse<AadhaarVerificationResponse>> getVerificationStatus(@PathVariable String verificationId) {
        try {
            log.info("Getting verification status for ID: {}", verificationId);
            
            AadhaarVerificationResponse response = aadhaarVerificationService.getVerificationById(verificationId);
            
            return ResponseEntity.ok(ApiResponse.success("Verification status retrieved", response));
            
        } catch (Exception e) {
            log.error("Error getting verification status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get verification status: " + e.getMessage()));
        }
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<ApiResponse<List<AadhaarVerificationResponse>>> getVerificationHistory(@PathVariable Long userId) {
        try {
            log.info("Getting verification history for user: {}", userId);
            
            List<AadhaarVerificationResponse> history = aadhaarVerificationService.getVerificationHistory(userId);
            
            return ResponseEntity.ok(ApiResponse.success("Verification history retrieved", history));
            
        } catch (Exception e) {
            log.error("Error getting verification history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get verification history: " + e.getMessage()));
        }
    }
} 