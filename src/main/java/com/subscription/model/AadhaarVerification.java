package com.subscription.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "aadhaar_verifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AadhaarVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "verification_id", unique = true)
    private String verificationId;
    
    @Column(name = "aadhaar_number")
    private String aadhaarNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    private AadhaarVerificationStatus status;
    
    @Column(name = "verification_method")
    private String verificationMethod; // OTP, Biometric, etc.
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "name_match")
    private String nameMatch;
    
    @Column(name = "dob_match")
    private String dobMatch;
    
    @Column(name = "face_score")
    private String faceScore;
    
    @Column(name = "address_match")
    private String addressMatch;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "error_code")
    private String errorCode;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = AadhaarVerificationStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 