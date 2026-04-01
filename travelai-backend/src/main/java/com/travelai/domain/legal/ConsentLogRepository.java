package com.travelai.domain.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentLogRepository extends JpaRepository<ConsentLog, UUID> {

    List<ConsentLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // JPQL no suporta LIMIT — usem findFirst via mètode derivat
    Optional<ConsentLog> findFirstByUserIdAndConsentTypeOrderByCreatedAtDesc(UUID userId, String consentType);
}
