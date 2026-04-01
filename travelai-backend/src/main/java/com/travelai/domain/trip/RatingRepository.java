package com.travelai.domain.trip;

import com.travelai.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    List<Rating> findByTrip(Trip trip);

    Optional<Rating> findByTripAndUser(Trip trip, User user);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.trip = :trip")
    Optional<Double> averageScoreByTrip(@Param("trip") Trip trip);
}
