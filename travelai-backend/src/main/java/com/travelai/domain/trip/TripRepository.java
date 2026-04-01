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

    /**
     * Feed personalitzat: trips públics ordenats per rating DESC, createdAt DESC.
     * Exclou els viatges del propi usuari autenticat.
     */
    @Query("""
        SELECT t FROM Trip t
        WHERE t.visibility = 'PUBLIC'
          AND t.deletedAt IS NULL
          AND t.owner.id <> :excludeOwnerId
        ORDER BY (
            SELECT COALESCE(AVG(r.score), 0) FROM Rating r WHERE r.trip = t
        ) DESC, t.createdAt DESC
        """)
    Page<Trip> findFeedExcludingOwner(@Param("excludeOwnerId") UUID excludeOwnerId, Pageable pageable);

    /**
     * Feed anònim (sense usuari autenticat): trips públics ordenats per rating DESC, createdAt DESC.
     */
    @Query("""
        SELECT t FROM Trip t
        WHERE t.visibility = 'PUBLIC'
          AND t.deletedAt IS NULL
        ORDER BY (
            SELECT COALESCE(AVG(r.score), 0) FROM Rating r WHERE r.trip = t
        ) DESC, t.createdAt DESC
        """)
    Page<Trip> findFeed(Pageable pageable);
}
