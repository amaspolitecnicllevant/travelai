package com.travelai.domain.search;

import com.travelai.domain.auth.User;
import com.travelai.domain.auth.UserRepository;
import com.travelai.domain.trip.TripRepository;
import com.travelai.domain.trip.TripService;
import com.travelai.domain.trip.dto.TripResponse;
import com.travelai.domain.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * SearchController — cerca global de viatges i usuaris.
 *
 * Endpoints:
 *   GET /api/v1/search?q={query}&type={users|trips|all}
 *
 * La cerca és paginada i insensible a majúscules/minúscules.
 * Viatges: cerca en title, destination i description (PUBLIC + no esborrats).
 * Usuaris: cerca en username i name (actius + no esborrats).
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripService tripService;

    /**
     * Cerca global paginada.
     *
     * @param q       Terme de cerca (mínim 1 caràcter)
     * @param type    Filtre: "users", "trips" o "all" (per defecte "all")
     * @param pageable Paginació (mida per defecte: 20)
     * @return SearchResponse amb pàgines de viatges i/o usuaris
     */
    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String type,
            @PageableDefault(size = 20) Pageable pageable) {

        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String query = q.trim();
        log.debug("SearchController: cerca '{}' type={}", query, type);

        Page<TripResponse> trips = Page.empty(pageable);
        Page<UserProfileResponse> users = Page.empty(pageable);

        if ("all".equalsIgnoreCase(type) || "trips".equalsIgnoreCase(type)) {
            trips = tripRepository.searchPublic(query, pageable)
                    .map(tripService::toResponse);
        }

        if ("all".equalsIgnoreCase(type) || "users".equalsIgnoreCase(type)) {
            users = userRepository.search(query, pageable)
                    .map(this::toUserProfileResponse);
        }

        return ResponseEntity.ok(new SearchResponse(users, trips));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getBio(),
                user.getAvatarUrl(),
                null,   // email no exposat (GDPR)
                0L,     // followers count — no carregat en cerca
                0L,     // following count — no carregat en cerca
                0L,     // trips count — no carregat en cerca
                false,  // isFollowing — no disponible en cerca anònima
                user.getCreatedAt()
        );
    }
}
