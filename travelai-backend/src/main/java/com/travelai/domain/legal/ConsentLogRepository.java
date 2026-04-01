package com.travelai.domain.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentLogRepository extends JpaRepository<ConsentLog, UUID> {

    List<ConsentLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT c FROM ConsentLog c WHERE c.user.id = :userId AND c.consentType = :type ORDER BY c.createdAt DESC LIMIT 1")
    java.util.Optional<ConsentLog> findLatestByUserIdAndType(UUID userId, String type);
}
