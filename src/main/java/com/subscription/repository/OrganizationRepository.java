package com.subscription.repository;

import com.subscription.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    Optional<Organization> findByName(String name);
    
    boolean existsByName(String name);
} 