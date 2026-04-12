package com.travelai.domain.trip;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("SELECT c FROM Comment c WHERE c.trip.id = :tripId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    Page<Comment> findByTripId(@Param("tripId") UUID tripId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Comment> findActiveById(@Param("id") UUID id);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.trip.id = :tripId AND c.deletedAt IS NULL")
    long countByTripId(@Param("tripId") UUID tripId);
}
