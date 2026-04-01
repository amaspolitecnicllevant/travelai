package com.travelai.domain.trip;

import com.travelai.domain.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByOwnerAndDeletedAtNull(User owner);

    @Query("SELECT t FROM Trip t WHERE t.visibility = 'PUBLIC' AND t.deletedAt IS NULL")
    Page<Trip> findPublicTrips(Pageable pageable);

    @Query("SELECT t FROM Trip t WHERE t.destination ILIKE %:destination% AND t.visibility = 'PUBLIC' AND t.deletedAt IS NULL")
    Page<Trip> findByDestination(@Param("destination") String destination, Pageable pageable);

    Optional<Trip> findByIdAndDeletedAtNull(UUID id);

    Page<Trip> findByOwnerAndDeletedAtNull(User owner, Pageable pageable);
}
