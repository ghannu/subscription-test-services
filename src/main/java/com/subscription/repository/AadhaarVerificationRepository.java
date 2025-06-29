package com.subscription.repository;

import com.subscription.model.AadhaarVerification;
import com.subscription.model.AadhaarVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AadhaarVerificationRepository extends JpaRepository<AadhaarVerification, Long> {
    
    Optional<AadhaarVerification> findByVerificationId(String verificationId);
    
    List<AadhaarVerification> findByUserId(Long userId);
    
    List<AadhaarVerification> findByStatus(AadhaarVerificationStatus status);
    
    List<AadhaarVerification> findByUserIdAndStatus(Long userId, AadhaarVerificationStatus status);
    
    Optional<AadhaarVerification> findByAadhaarNumberAndStatus(String aadhaarNumber, AadhaarVerificationStatus status);
    
    Optional<AadhaarVerification> findByTransactionId(String transactionId);
    
    @Query("SELECT av FROM AadhaarVerification av WHERE av.createdAt < :cutoffTime AND av.status = :status")
    List<AadhaarVerification> findExpiredVerifications(@Param("cutoffTime") LocalDateTime cutoffTime, 
                                                       @Param("status") AadhaarVerificationStatus status);
    
    @Query("SELECT COUNT(av) FROM AadhaarVerification av WHERE av.user.id = :userId AND av.status = 'SUCCESS'")
    long countSuccessfulVerificationsByUserId(@Param("userId") Long userId);
} 