package com.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarVerificationResponse {
    
    private boolean verified;
    private String verificationId;
    private String aadhaarNumber;
    private String name;
    private String dateOfBirth;
    private String gender;
    private String address;
    private String photo;
    private String errorMessage;
    private String errorCode;
    private LocalDateTime verifiedAt;
    private String verificationMethod; // OTP, Biometric, etc.
    
    // Additional fields for detailed verification
    private String faceScore;
    private String addressMatch;
    private String nameMatch;
    private String dobMatch;
} 