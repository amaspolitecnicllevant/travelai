package com.travelai.domain.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataDeletionRequestRepository extends JpaRepository<DataDeletionRequest, UUID> {

    Optional<DataDeletionRequest> findByUserIdAndStatus(UUID userId, DataDeletionRequest.DeletionStatus status);

    boolean existsByUserIdAndStatus(UUID userId, DataDeletionRequest.DeletionStatus status);

    /** Retorna sol·licituds vençudes i pendents per al purge scheduler */
    @Query("SELECT r FROM DataDeletionRequest r WHERE r.status = 'PENDING' AND r.scheduledPurgeAt <= :now")
    List<DataDeletionRequest> findDuePurges(Instant now);
}
