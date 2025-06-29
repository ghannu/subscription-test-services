package com.subscription.service;

import com.subscription.dto.AadhaarOtpRequest;
import com.subscription.dto.AadhaarOtpVerifyRequest;
import com.subscription.dto.AadhaarVerificationRequest;
import com.subscription.dto.AadhaarVerificationResponse;
import com.subscription.exception.InvalidOperationException;
import com.subscription.model.AadhaarVerification;
import com.subscription.model.AadhaarVerificationStatus;
import com.subscription.model.User;
import com.subscription.repository.AadhaarVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AadhaarVerificationService {

    private final RestTemplate restTemplate;
    private final AadhaarVerificationRepository aadhaarVerificationRepository;

    @Value("${aadhaar.api.base-url:https://api.uidai.gov.in}")
    private String aadhaarApiBaseUrl;

    @Value("${aadhaar.api.client-id}")
    private String clientId;

    @Value("${aadhaar.api.client-secret}")
    private String clientSecret;

    @Value("${aadhaar.api.app-id}")
    private String appId;

    @Value("${aadhaar.api.consent-text:Y}")
    private String consentText;

    /**
     * Generate OTP for Aadhaar verification
     */
    public Map<String, String> generateOtp(AadhaarOtpRequest request) {
        try {
            log.info("Generating OTP for Aadhaar number: {}", request.getAadhaarNumber());

            if (request.getAadhaarNumber() == null || request.getAadhaarNumber().isEmpty()) {
                throw new InvalidOperationException("Aadhaar number is required");
            }

            AadhaarVerification verification = AadhaarVerification.builder()
                    .verificationId(UUID.randomUUID().toString())
                    .aadhaarNumber(request.getAadhaarNumber())
                    .transactionId(request.getTransactionId())
                    .status(AadhaarVerificationStatus.PENDING)
                    .verificationMethod("OTP")
                    .build();

            aadhaarVerificationRepository.save(verification);

            Map<String, Object> payload = new HashMap<>();
            payload.put("uid", request.getAadhaarNumber());
            payload.put("txnId", request.getTransactionId());
            payload.put("consent", request.getConsent() != null ? request.getConsent() : consentText);
            payload.put("purpose", request.getPurpose() != null ? request.getPurpose() : "Authentication");

            HttpHeaders headers = createAuthHeaders();

            String url = aadhaarApiBaseUrl + "/v1/otp";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, String> result = new HashMap<>();
                result.put("transactionId", request.getTransactionId());
                result.put("verificationId", verification.getVerificationId());
                result.put("status", "SUCCESS");
                result.put("message", "OTP sent successfully");
                
                log.info("OTP generated successfully for transaction: {}", request.getTransactionId());
                return result;
            } else {
                verification.setStatus(AadhaarVerificationStatus.FAILED);
                verification.setErrorMessage("Failed to generate OTP");
                aadhaarVerificationRepository.save(verification);
                throw new InvalidOperationException("Failed to generate OTP");
            }

        } catch (Exception e) {
            log.error("Error generating OTP for Aadhaar: {}", request.getAadhaarNumber(), e);
            throw new InvalidOperationException("Failed to generate OTP: " + e.getMessage());
        }
    }

    /**
     * Verify OTP and get Aadhaar details
     */
    public AadhaarVerificationResponse verifyOtp(AadhaarOtpVerifyRequest request) {
        try {
            log.info("Verifying OTP for Aadhaar number: {}", request.getAadhaarNumber());

            AadhaarVerification verification = aadhaarVerificationRepository
                    .findByTransactionId(request.getTransactionId())
                    .orElseThrow(() -> new InvalidOperationException("Verification record not found"));

            if (verification.getStatus() != AadhaarVerificationStatus.PENDING) {
                throw new InvalidOperationException("Verification is not in pending status");
            }

            verification.setStatus(AadhaarVerificationStatus.IN_PROGRESS);
            aadhaarVerificationRepository.save(verification);

            if (request.getOtp() == null || request.getOtp().length() != 6) {
                throw new InvalidOperationException("Invalid OTP format");
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("uid", request.getAadhaarNumber());
            payload.put("txnId", request.getTransactionId());
            payload.put("otp", request.getOtp());

            HttpHeaders headers = createAuthHeaders();

            String url = aadhaarApiBaseUrl + "/v1/otp/verify";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                String name = (String) responseBody.get("name");
                String dob = (String) responseBody.get("dob");
                String gender = (String) responseBody.get("gender");
                
                verification.setStatus(AadhaarVerificationStatus.SUCCESS);
                verification.setVerifiedAt(LocalDateTime.now());
                verification.setNameMatch("100");
                verification.setDobMatch("100");
                aadhaarVerificationRepository.save(verification);

                AadhaarVerificationResponse verificationResponse = AadhaarVerificationResponse.builder()
                        .verified(true)
                        .verificationId(verification.getVerificationId())
                        .aadhaarNumber(request.getAadhaarNumber())
                        .name(name)
                        .dateOfBirth(dob)
                        .gender(gender)
                        .address((String) responseBody.get("address"))
                        .photo((String) responseBody.get("photo"))
                        .verifiedAt(LocalDateTime.now())
                        .verificationMethod("OTP")
                        .nameMatch("100")
                        .dobMatch("100")
                        .build();

                log.info("Aadhaar verification successful for: {}", request.getAadhaarNumber());
                return verificationResponse;
            } else {
                verification.setStatus(AadhaarVerificationStatus.FAILED);
                verification.setErrorMessage("OTP verification failed");
                aadhaarVerificationRepository.save(verification);
                throw new InvalidOperationException("OTP verification failed");
            }

        } catch (Exception e) {
            log.error("Error verifying OTP for Aadhaar: {}", request.getAadhaarNumber(), e);
            
            return AadhaarVerificationResponse.builder()
                    .verified(false)
                    .errorMessage("OTP verification failed: " + e.getMessage())
                    .errorCode("OTP_VERIFICATION_FAILED")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Basic Aadhaar verification (demo mode for development)
     */
    public AadhaarVerificationResponse verifyAadhaar(AadhaarVerificationRequest request, User user) {
        try {
            log.info("Verifying Aadhaar for user: {} with Aadhaar: {}", 
                    user.getEmail(), request.getAadhaarNumber());

            if (user == null) {
                throw new InvalidOperationException("User is required");
            }

            AadhaarVerification verification = AadhaarVerification.builder()
                    .verificationId(UUID.randomUUID().toString())
                    .aadhaarNumber(request.getAadhaarNumber())
                    .user(user)
                    .status(AadhaarVerificationStatus.IN_PROGRESS)
                    .verificationMethod("DEMO")
                    .build();

            aadhaarVerificationRepository.save(verification);

            Thread.sleep(1000);

            if (!request.getAadhaarNumber().matches("^[0-9]{12}$")) {
                verification.setStatus(AadhaarVerificationStatus.FAILED);
                verification.setErrorMessage("Invalid Aadhaar number format");
                aadhaarVerificationRepository.save(verification);
                throw new InvalidOperationException("Invalid Aadhaar number format");
            }

            boolean isVerified = simulateVerification(request);

            if (isVerified) {
                verification.setStatus(AadhaarVerificationStatus.SUCCESS);
                verification.setVerifiedAt(LocalDateTime.now());
                verification.setNameMatch("100");
                verification.setDobMatch("100");
                aadhaarVerificationRepository.save(verification);

                user.setAadhaarNumber(request.getAadhaarNumber());
                user.setAadhaarVerified(true);
                user.setAadhaarVerificationId(verification.getVerificationId());
                user.setAadhaarVerifiedAt(LocalDateTime.now());

                AadhaarVerificationResponse response = AadhaarVerificationResponse.builder()
                        .verified(true)
                        .verificationId(verification.getVerificationId())
                        .aadhaarNumber(request.getAadhaarNumber())
                        .name(request.getName())
                        .dateOfBirth(request.getDateOfBirth())
                        .gender(request.getGender())
                        .address(request.getAddress())
                        .verifiedAt(LocalDateTime.now())
                        .verificationMethod("DEMO")
                        .nameMatch("100")
                        .dobMatch("100")
                        .build();

                log.info("Aadhaar verification successful for user: {}", user.getEmail());
                return response;
            } else {
                verification.setStatus(AadhaarVerificationStatus.FAILED);
                verification.setErrorMessage("Aadhaar verification failed");
                aadhaarVerificationRepository.save(verification);

                return AadhaarVerificationResponse.builder()
                        .verified(false)
                        .errorMessage("Aadhaar verification failed")
                        .errorCode("VERIFICATION_FAILED")
                        .verifiedAt(LocalDateTime.now())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error verifying Aadhaar for user: {}", user.getEmail(), e);
            throw new InvalidOperationException("Aadhaar verification failed: " + e.getMessage());
        }
    }

    /**
     * Get verification history for a user
     */
    public List<AadhaarVerificationResponse> getVerificationHistory(Long userId) {
        List<AadhaarVerification> verifications = aadhaarVerificationRepository.findByUserId(userId);
        
        return verifications.stream()
                .map(this::toVerificationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get verification by ID
     */
    public AadhaarVerificationResponse getVerificationById(String verificationId) {
        if (verificationId == null || verificationId.trim().isEmpty()) {
            throw new InvalidOperationException("Verification ID is required");
        }
        
        AadhaarVerification verification = aadhaarVerificationRepository
                .findByVerificationId(verificationId)
                .orElseThrow(() -> new InvalidOperationException("Verification not found"));
        
        return toVerificationResponse(verification);
    }

    /**
     * Generate a transaction ID for Aadhaar operations
     */
    public String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + "12345";
    }

    /**
     * Mask Aadhaar number for logging (show only first 4 and last 4 digits)
     */
    private String maskAadhaar(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.length() < 8) {
            return "****";
        }
        return aadhaarNumber.substring(0, 4) + "****" + aadhaarNumber.substring(aadhaarNumber.length() - 4);
    }

    /**
     * Create authentication headers for Aadhaar API
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-Client-ID", clientId);
        headers.set("X-Client-Secret", clientSecret);
        headers.set("X-App-ID", appId);
        headers.set("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        return headers;
    }

    /**
     * Simulate verification for demo purposes
     */
    private boolean simulateVerification(AadhaarVerificationRequest request) {
        return true;
    }

    /**
     * Convert AadhaarVerification entity to AadhaarVerificationResponse DTO
     */
    private AadhaarVerificationResponse toVerificationResponse(AadhaarVerification verification) {
        return AadhaarVerificationResponse.builder()
                .verified(verification.getStatus() == AadhaarVerificationStatus.SUCCESS)
                .verificationId(verification.getVerificationId())
                .aadhaarNumber(verification.getAadhaarNumber())
                .verifiedAt(verification.getVerifiedAt())
                .verificationMethod(verification.getVerificationMethod())
                .nameMatch(verification.getNameMatch())
                .dobMatch(verification.getDobMatch())
                .faceScore(verification.getFaceScore())
                .addressMatch(verification.getAddressMatch())
                .errorMessage(verification.getErrorMessage())
                .errorCode(verification.getErrorCode())
                .build();
    }
} 