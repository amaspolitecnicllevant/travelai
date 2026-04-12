package com.travelai.domain.user;

import com.travelai.domain.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followingId = :userId")
    long countFollowers(@Param("userId") UUID userId);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followerId = :userId")
    long countFollowing(@Param("userId") UUID userId);

    /** Usuaris que segueixen userId (els seus seguidors). */
    @Query("SELECT f.follower FROM Follow f WHERE f.followingId = :userId ORDER BY f.createdAt DESC")
    Page<User> findFollowers(@Param("userId") UUID userId, Pageable pageable);

    /** Usuaris que userId segueix. */
    @Query("SELECT f.following FROM Follow f WHERE f.followerId = :userId ORDER BY f.createdAt DESC")
    Page<User> findFollowing(@Param("userId") UUID userId, Pageable pageable);
}
