package com.travelai.domain.ai;

import com.travelai.domain.ai.agents.ActivityAgent;
import com.travelai.domain.ai.agents.BudgetAgent;
import com.travelai.domain.ai.agents.DayRefinerAgent;
import com.travelai.domain.ai.agents.EditorAgent;
import com.travelai.domain.ai.agents.ExperienceAgent;
import com.travelai.domain.ai.agents.ItineraryAgent;
import com.travelai.domain.ai.agents.SocialAgent;
import com.travelai.domain.trip.ItineraryRepository;
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
 * Format SSE unificat (compatible amb useAiStream.js al frontend):
 *   data: {"type":"chunk","content":"<text parcial>"}
 *   data: {"type":"complete"}
 *   data: {"type":"day_complete","dayNumber":N}
 *   data: {"type":"error","message":"<missatge>"}
 *
 * Endpoints:
 *   POST /api/v1/ai/trips/{id}/generate                    → genera itinerari complet
 *   POST /api/v1/ai/trips/{id}/days/{day}/refine           → refina un dia específic
 *   POST /api/v1/ai/trips/{id}/days/{day}/activities/suggest → suggereix activitats
 *   POST /api/v1/ai/trips/{id}/edit                        → edita itinerari per prompt
 *   POST /api/v1/ai/trips/{id}/optimize-social             → optimitza per feedback social
 *   GET  /api/v1/ai/trips/{id}/budget-estimate             → estima pressupost
 *   POST /api/v1/ai/trips/{id}/enrich                      → afegeix experiències locals
 *   POST /api/v1/ai/trips/{id}/refine-all                  → refina tots els dies
 */
@RestController
@RequestMapping("/api/v1/ai/trips")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final TripService tripService;
    private final ItineraryAgent itineraryAgent;
    private final DayRefinerAgent dayRefinerAgent;
    private final ActivityAgent activityAgent;
    private final EditorAgent editorAgent;
    private final SocialAgent socialAgent;
    private final BudgetAgent budgetAgent;
    private final ExperienceAgent experienceAgent;
    private final ItineraryRepository itineraryRepository;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record RefineRequest(
            @NotBlank @Size(max = 1000) String prompt
    ) {}

    public record ActivitySuggestRequest(
            @NotBlank @Size(max = 50) String category
    ) {}

    public record EditRequest(
            @NotBlank @Size(max = 2000) String prompt
    ) {}

    // ── Endpoints ────────────────────────────────────────────────────────────

    /**
     * Genera un itinerari complet per a un viatge en format SSE (streaming).
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
                .map(chunk -> sseData("{\"type\":\"chunk\",\"content\":\"" + escapeJson(chunk) + "\"}"))
                .concatWith(Flux.just(sseData("{\"type\":\"complete\"}")))
                .onErrorResume(e -> {
                    log.error("AiController: error en generació per trip {}: {}", id, e.getMessage());
                    return Flux.just(sseData("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                });
    }

    /**
     * Refina un dia específic de l'itinerari en format SSE (streaming).
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
                .map(chunk -> sseData("{\"type\":\"chunk\",\"content\":\"" + escapeJson(chunk) + "\"}"))
                .concatWith(Flux.just(
                        sseData("{\"type\":\"day_complete\",\"dayNumber\":" + day + "}"),
                        sseData("{\"type\":\"complete\"}")))
                .onErrorResume(e -> {
                    log.error("AiController: error en refinament del dia {} per trip {}: {}", day, id, e.getMessage());
                    return Flux.just(sseData("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                });
    }

    /**
     * Suggereix activitats d'una categoria per a un dia específic del viatge.
     */
    @PostMapping(value = "/{id}/days/{day}/activities/suggest", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> suggestActivities(
            @PathVariable UUID id,
            @PathVariable int day,
            @Valid @RequestBody ActivitySuggestRequest request,
            @AuthenticationPrincipal User user) {

        Trip trip = tripService.findActiveOrThrow(id);
        assertOwner(trip, user);

        log.info("AiController: suggerint activitats '{}' per dia {} del trip {} (usuari: {})",
                request.category(), day, id, user.getUsername());

        return activityAgent.suggest(trip, day, request.category())
                .map(chunk -> sseData("{\"type\":\"chunk\",\"content\":\"" + escapeJson(chunk) + "\"}"))
                .concatWith(Flux.just(sseData("{\"type\":\"complete\"}")))
                .onErrorResume(e -> {
                    log.error("AiController: error suggerint activitats per trip {} dia {}: {}", id, day, e.getMessage());
                    return Flux.just(sseData("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                });
    }

    /**
     * Edita l'itinerari complet del viatge seguint el prompt de l'usuari en format SSE (streaming).
     * Quan el stream finalitza, els dies modificats queden desats a la BD.
     */
    @PostMapping(value = "/{id}/edit", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> editItinerary(
            @PathVariable UUID id,
            @Valid @RequestBody EditRequest request,
            @AuthenticationPrincipal User user) {

        Trip trip = tripService.findActiveOrThrow(id);
        assertOwner(trip, user);

        log.info("AiController: editant itinerari del trip {} (usuari: {})", id, user.getUsername());

        return editorAgent.edit(trip, request.prompt())
                .map(chunk -> sseData("{\"type\":\"chunk\",\"content\":\"" + escapeJson(chunk) + "\"}"))
                .concatWith(Flux.just(sseData("{\"type\":\"complete\"}")))
                .onErrorResume(e -> {
                    log.error("AiController: error en edició del trip {}: {}", id, e.getMessage());
                    return Flux.just(sseData("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                });
    }

    /**
     * Optimitza l'itinerari del viatge basant-se en el feedback social (ratings i comentaris).
     * Quan el stream finalitza, els dies optimitzats queden desats a la BD.
     */
    @PostMapping(value = "/{id}/optimize-social", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> optimizeSocial(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Trip trip = tripService.findActiveOrThrow(id);
        assertOwner(trip, user);

        log.info("AiController: optimitzant socialment l'itinerari del trip {} (usuari: {})",
                id, user.getUsername());

        return socialAgent.optimize(trip)
                .map(chunk -> sseData("{\"type\":\"chunk\",\"content\":\"" + escapeJson(chunk) + "\"}"))
                .concatWith(Flux.just(sseData("{\"type\":\"complete\"}")))
                .onErrorResume(e -> {
                    log.error("AiController: error en optimització social del trip {}: {}", id, e.getMessage());
                    return Flux.just(sseData("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                });
    }

    /**
     * Estima el pressupost total del viatge en format SSE (streaming).
     * Utilitza BudgetAgent per analitzar l'itinerari actual i calcular costos per dia.
     */
    @GetMapping(value = "/{id}/budget-estimate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> budgetEstimate(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Trip trip = tripService.findActiveOrThrow(id);
        assertOwner(trip, user);

        log.info("AiController: estimant pressupost per trip {} (usuari: {})", id, user.getUsername());

        return budgetAgent.estimate(trip)
                .map(chunk -> sseData("{\"type\":\"chunk\",\"content\":\"" + escapeJson(chunk) + "\"}"))
                .concatWith(Flux.just(sseData("{\"type\":\"complete\"}")))
                .onErrorResume(e -> {
                    log.error("AiController: error en estimació de pressupost per trip {}: {}", id, e.getMessage());
                    return Flux.just(sseData("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                });
    }

    /**
     * Enriqueix l'itinerari existent amb experiències locals en format SSE (streaming).
     * Utilitza ExperienceAgent per afegir restaurants, mercats, festivals i tradicions locals.
     */
    @PostMapping(value = "/{id}/enrich", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> enrichItinerary(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Trip trip = tripService.findActiveOrThrow(id);
        assertOwner(trip, user);

        log.info("AiController: enriquint itinerari per trip {} (usuari: {})", id, user.getUsername());

        return experienceAgent.enrich(trip)
                .map(chunk -> sseData("{\"type\":\"chunk\",\"content\":\"" + escapeJson(chunk) + "\"}"))
                .concatWith(Flux.just(sseData("{\"type\":\"complete\"}")))
                .onErrorResume(e -> {
                    log.error("AiController: error enriquint itinerari per trip {}: {}", id, e.getMessage());
                    return Flux.just(sseData("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                });
    }

    /**
     * Refina tots els dies de l'itinerari amb el prompt donat en format SSE (streaming).
     * Itera tots els dies seqüencialment, emetent un event day_complete per cada dia finalitzat.
     */
    @PostMapping(value = "/{id}/refine-all", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> refineAll(
            @PathVariable UUID id,
            @Valid @RequestBody RefineRequest request,
            @AuthenticationPrincipal User user) {

        Trip trip = tripService.findActiveOrThrow(id);
        assertOwner(trip, user);

        log.info("AiController: refinant tots els dies del trip {} (usuari: {})", id, user.getUsername());

        return Flux.fromIterable(itineraryRepository.findByTripOrderByDayNumber(trip))
                .concatMap(day -> {
                    int dayNumber = day.getDayNumber();
                    return dayRefinerAgent.refine(trip, dayNumber, request.prompt())
                            .map(chunk -> sseData("{\"type\":\"chunk\",\"content\":\"" + escapeJson(chunk) + "\"}"))
                            .concatWith(Flux.just(
                                    sseData("{\"type\":\"day_complete\",\"dayNumber\":" + dayNumber + "}")));
                })
                .concatWith(Flux.just(sseData("{\"type\":\"complete\"}")))
                .onErrorResume(e -> {
                    log.error("AiController: error en refine-all per trip {}: {}", id, e.getMessage());
                    return Flux.just(sseData("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                });
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void assertOwner(Trip trip, User requester) {
        if (!trip.getOwner().getId().equals(requester.getId())) {
            throw new AccessDeniedException("No tens permisos per modificar aquest viatge");
        }
    }

    /**
     * Crea un ServerSentEvent amb el payload data donat (format JSON-envelope).
     * L'event type no s'envia explícitament perquè el frontend llegeix només el camp "data:"
     * i descodifica el type del JSON intern.
     */
    private ServerSentEvent<String> sseData(String jsonPayload) {
        return ServerSentEvent.<String>builder()
                .data(jsonPayload)
                .build();
    }

    /** Escapa caràcters especials per incloure text en un string JSON inline. */
    private String escapeJson(String message) {
        if (message == null) return "Error desconegut";
        return message.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "");
    }
}
