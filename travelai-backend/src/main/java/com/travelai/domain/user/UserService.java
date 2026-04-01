package com.travelai.domain.user;

import com.travelai.domain.auth.User;
import com.travelai.domain.auth.UserRepository;
import com.travelai.domain.notification.NotificationService;
import com.travelai.domain.notification.NotificationType;
import com.travelai.domain.trip.TripRepository;
import com.travelai.domain.trip.Visibility;
import com.travelai.domain.trip.dto.TripResponse;
import com.travelai.domain.trip.TripService;
import com.travelai.domain.legal.DataDeletionRequest;
import com.travelai.domain.legal.DataDeletionRequestRepository;
import com.travelai.domain.trip.RatingRepository;
import com.travelai.domain.user.dto.UpdateProfileRequest;
import com.travelai.domain.user.dto.UserProfileResponse;
import com.travelai.domain.user.dto.UserStatsResponse;
import com.travelai.shared.exception.ResourceNotFoundException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final TripRepository tripRepository;
    private final RatingRepository ratingRepository;
    private final DataDeletionRequestRepository deletionRequestRepository;
    private final TripService tripService;
    private final NotificationService notificationService;
    private final MinioClient minioClient;

    private static final String AVATAR_BUCKET = "travelai-avatars";
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

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
     * Retorna el perfil propi de l'usuari autenticat (inclou email i deleteScheduledAt).
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(User user) {
        long followers = followRepository.countFollowers(user.getId());
        long following = followRepository.countFollowing(user.getId());
        long trips     = tripRepository.countByOwnerAndDeletedAtNull(user);

        java.time.Instant deleteScheduledAt = deletionRequestRepository
                .findByUserIdAndStatus(user.getId(), DataDeletionRequest.DeletionStatus.PENDING)
                .map(DataDeletionRequest::getScheduledPurgeAt)
                .orElse(null);

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
                user.getCreatedAt(),
                deleteScheduledAt
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

    /**
     * Retorna les estadístiques agregades d'un usuari per username.
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(String username) {
        User user = findActiveByUsernameOrThrow(username);
        long tripsCount    = tripRepository.countByOwnerAndDeletedAtNull(user);
        double avgRating   = ratingRepository.averageScoreByOwnerId(user.getId())
                .orElse(0.0);
        long followersCount = followRepository.countFollowers(user.getId());
        long followingCount = followRepository.countFollowing(user.getId());
        return new UserStatsResponse(tripsCount, avgRating, followersCount, followingCount);
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
            notificationService.notify(target.getId(), NotificationType.FOLLOW,
                    follower.getUsername() + " ha començat a seguir-te");
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

    @Transactional
    public String uploadAvatar(UUID userId, MultipartFile file) {
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("La imatge no pot superar els 5MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Format no permès. Usa JPEG, PNG o WebP");
        }
        String ext = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                : ".jpg";
        String objectName = userId + "/" + UUID.randomUUID() + ext;
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(AVATAR_BUCKET).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(AVATAR_BUCKET).build());
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(AVATAR_BUCKET)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Error pujant l'avatar: " + e.getMessage(), e);
        }
        String avatarUrl = "/api/v1/minio/" + AVATAR_BUCKET + "/" + objectName;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuari no trobat"));
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return avatarUrl;
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
                user.getCreatedAt(),
                null   // deleteScheduledAt no exposat en perfil públic (GDPR)
        );
    }
}
