package com.travelai.domain.user;

import com.travelai.domain.auth.User;
import com.travelai.domain.auth.UserRepository;
import com.travelai.domain.trip.TripRepository;
import com.travelai.domain.trip.Visibility;
import com.travelai.domain.trip.dto.TripResponse;
import com.travelai.domain.trip.TripService;
import com.travelai.domain.user.dto.UpdateProfileRequest;
import com.travelai.domain.user.dto.UserProfileResponse;
import com.travelai.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final TripRepository tripRepository;
    private final TripService tripService;

    // ── Perfils ──────────────────────────────────────────────────────────────

    /**
     * Retorna el perfil públic d'un usuari per username.
     * No inclou l'email.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = findActiveByUsernameOrThrow(username);
        return buildPublicProfile(user, null);
    }

    /**
     * Retorna el perfil públic d'un usuari, indicant si el viewer el segueix.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username, User viewer) {
        User user = findActiveByUsernameOrThrow(username);
        return buildPublicProfile(user, viewer);
    }

    /**
     * Retorna el perfil propi de l'usuari autenticat (inclou email).
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(User user) {
        long followers = followRepository.countFollowers(user.getId());
        long following = followRepository.countFollowing(user.getId());
        long trips     = tripRepository.countByOwnerAndDeletedAtNull(user);

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getEmail(),  // camp privat — visible per a l'usuari mateix
                followers,
                following,
                trips,
                false,
                user.getCreatedAt()
        );
    }

    /**
     * Actualitza name i bio de l'usuari autenticat.
     */
    @Transactional
    public UserProfileResponse updateMyProfile(User user, UpdateProfileRequest req) {
        if (req.name() != null) user.setName(req.name());
        if (req.bio()  != null) user.setBio(req.bio());
        userRepository.save(user);
        log.info("UserService: perfil actualitzat per a '{}'", user.getUsername());
        return getMyProfile(user);
    }

    /**
     * Retorna els viatges públics d'un usuari paginats.
     */
    @Transactional(readOnly = true)
    public Page<TripResponse> getPublicTrips(String username, Pageable pageable) {
        User user = findActiveByUsernameOrThrow(username);
        return tripRepository.findByOwnerAndVisibilityAndDeletedAtNull(user, Visibility.PUBLIC, pageable)
                .map(tripService::toResponse);
    }

    // ── Follows ──────────────────────────────────────────────────────────────

    /**
     * L'usuari follower segueix l'usuari amb targetUsername.
     */
    @Transactional
    public void follow(User follower, String targetUsername) {
        User target = findActiveByUsernameOrThrow(targetUsername);
        if (follower.getId().equals(target.getId())) {
            throw new IllegalArgumentException("No et pots seguir a tu mateix");
        }
        if (!followRepository.existsByFollowerIdAndFollowingId(follower.getId(), target.getId())) {
            Follow follow = Follow.builder()
                    .followerId(follower.getId())
                    .followingId(target.getId())
                    .build();
            followRepository.save(follow);
            log.info("UserService: '{}' ara segueix '{}'", follower.getUsername(), targetUsername);
        }
    }

    /**
     * L'usuari follower deixa de seguir l'usuari amb targetUsername.
     */
    @Transactional
    public void unfollow(User follower, String targetUsername) {
        User target = findActiveByUsernameOrThrow(targetUsername);
        followRepository.deleteByFollowerIdAndFollowingId(follower.getId(), target.getId());
        log.info("UserService: '{}' ha deixat de seguir '{}'", follower.getUsername(), targetUsername);
    }

    /**
     * Comprova si followerId segueix followingId.
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User findActiveByUsernameOrThrow(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "Usuari no trobat: " + username));
    }

    private UserProfileResponse buildPublicProfile(User user, User viewer) {
        long followers = followRepository.countFollowers(user.getId());
        long following = followRepository.countFollowing(user.getId());
        long trips     = tripRepository.countByOwnerAndDeletedAtNull(user);
        boolean isFollowing = viewer != null &&
                followRepository.existsByFollowerIdAndFollowingId(viewer.getId(), user.getId());

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getBio(),
                user.getAvatarUrl(),
                null,  // email no exposat en perfil públic (GDPR)
                followers,
                following,
                trips,
                isFollowing,
                user.getCreatedAt()
        );
    }
}
