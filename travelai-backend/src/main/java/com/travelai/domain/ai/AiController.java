package com.travelai.domain.ai;

import com.travelai.domain.ai.agents.DayRefinerAgent;
import com.travelai.domain.ai.agents.ItineraryAgent;
import com.travelai.domain.auth.User;
import com.travelai.domain.trip.Trip;
import com.travelai.domain.trip.TripService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * AiController — endpoints SSE per a la generació i refinament d'itineraris via IA.
 *
 * Endpoints:
 *   POST /api/v1/ai/trips/{id}/generate          → genera itinerari complet (streaming)
 *   POST /api/v1/ai/trips/{id}/days/{day}/refine → refina un dia específic (streaming)
 */
@RestController
@RequestMapping("/api/v1/ai/trips")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final TripService tripService;
    private final ItineraryAgent itineraryAgent;
    private final DayRefinerAgent dayRefinerAgent;

    // ── DTO ──────────────────────────────────────────────────────────────────

    public record RefineRequest(
            @NotBlank @Size(max = 1000) String prompt
    ) {}

    // ── Endpoints ────────────────────────────────────────────────────────────

    /**
     * Genera un itinerari complet per a un viatge en format SSE (streaming).
     * El client rep chunks de text JSON a mesura que Ollama els genera.
     * Quan el stream finalitza, l'itinerari queda desat a la BD.
     */
    @PostMapping(value = "/{id}/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateItinerary(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Trip trip = tripService.findActiveOrThrow(id);
        assertOwner(trip, user);

        log.info("AiController: generant itinerari per trip {} (usuari: {})", id, user.getUsername());

        return itineraryAgent.generate(trip)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("chunk")
                        .data(chunk)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("complete")
                                .data("{\"status\":\"done\"}")
                                .build()))
                .onErrorResume(e -> {
                    log.error("AiController: error en generació per trip {}: {}", id, e.getMessage());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("{\"message\":\"" + escapeJson(e.getMessage()) + "\"}")
                            .build());
                });
    }

    /**
     * Refina un dia específic de l'itinerari en format SSE (streaming).
     * Rep el prompt de l'usuari que descriu els canvis desitjats.
     * Quan el stream finalitza, el dia modificat queda desat a la BD.
     */
    @PostMapping(value = "/{id}/days/{day}/refine", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> refineDay(
            @PathVariable UUID id,
            @PathVariable int day,
            @Valid @RequestBody RefineRequest request,
            @AuthenticationPrincipal User user) {

        Trip trip = tripService.findActiveOrThrow(id);
        assertOwner(trip, user);

        log.info("AiController: refinant dia {} del trip {} (usuari: {})", day, id, user.getUsername());

        return dayRefinerAgent.refine(trip, day, request.prompt())
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("chunk")
                        .data(chunk)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("complete")
                                .data("{\"status\":\"done\",\"day\":" + day + "}")
                                .build()))
                .onErrorResume(e -> {
                    log.error("AiController: error en refinament del dia {} per trip {}: {}", day, id, e.getMessage());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("{\"message\":\"" + escapeJson(e.getMessage()) + "\"}")
                            .build());
                });
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void assertOwner(Trip trip, User requester) {
        if (!trip.getOwner().getId().equals(requester.getId())) {
            throw new AccessDeniedException("No tens permisos per modificar aquest viatge");
        }
    }

    /** Escapa caràcters especials per incloure el missatge en un JSON inline. */
    private String escapeJson(String message) {
        if (message == null) return "Error desconegut";
        return message.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "");
    }
}
