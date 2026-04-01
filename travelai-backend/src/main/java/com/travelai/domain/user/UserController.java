package com.travelai.domain.user;

import com.travelai.domain.auth.User;
import com.travelai.domain.trip.dto.TripResponse;
import com.travelai.domain.user.dto.UpdateProfileRequest;
import com.travelai.domain.user.dto.UserProfileResponse;
import com.travelai.domain.user.dto.UserStatsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * UserController — endpoints de perfil d'usuari i sistema de follows.
 *
 * Endpoints públics:
 *   GET  /api/v1/users/{username}        → perfil públic
 *   GET  /api/v1/users/{username}/trips  → viatges públics d'un usuari
 *
 * Endpoints autenticats:
 *   GET  /api/v1/users/me                → perfil propi
 *   PUT  /api/v1/users/me                → actualitzar perfil
 *   POST /api/v1/users/{username}/follow → seguir un usuari
 *   DELETE /api/v1/users/{username}/follow → deixar de seguir
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── Perfil propi (autenticat) ────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getMyProfile(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(user, request));
    }

    // ── Perfil públic ────────────────────────────────────────────────────────

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getProfile(
            @PathVariable String username,
            @AuthenticationPrincipal User viewer) {

        UserProfileResponse profile = viewer != null
                ? userService.getProfile(username, viewer)
                : userService.getProfile(username);

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{username}/trips")
    public ResponseEntity<Page<TripResponse>> getPublicTrips(
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getPublicTrips(username, pageable));
    }

    @GetMapping("/{username}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(
            @PathVariable String username) {
        return ResponseEntity.ok(userService.getUserStats(username));
    }

    // ── Follows ──────────────────────────────────────────────────────────────

    @PostMapping("/{username}/follow")
    public ResponseEntity<Void> follow(
            @PathVariable String username,
            @AuthenticationPrincipal User user) {
        userService.follow(user, username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{username}/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable String username,
            @AuthenticationPrincipal User user) {
        userService.unfollow(user, username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<String> uploadAvatar(
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal User user) {
        String url = userService.uploadAvatar(user.getId(), file);
        return ResponseEntity.ok(url);
    }
}
