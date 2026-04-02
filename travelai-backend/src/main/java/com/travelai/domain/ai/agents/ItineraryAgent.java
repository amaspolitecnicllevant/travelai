package com.travelai.domain.ai.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelai.domain.ai.AiException;
import com.travelai.domain.ai.DayPlan;
import com.travelai.domain.ai.ItineraryParser;
import com.travelai.domain.ai.OllamaService;
import com.travelai.domain.trip.Itinerary;
import com.travelai.domain.trip.ItineraryRepository;
import com.travelai.domain.trip.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

/**
 * ItineraryAgent — genera un itinerari complet per a un viatge cridant OllamaService.
 * El stream retorna chunks de text raw; quan es completa, parseja i desa a BD.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ItineraryAgent {

    private final OllamaService ollamaService;
    private final ItineraryParser itineraryParser;
    private final ItineraryRepository itineraryRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Ets un planificador de viatges expert.
            NOMÉS respons amb JSON vàlid, sense text addicional, sense markdown, \
            sense blocs de codi. La resposta comença amb { i acaba amb }.
            Genera un itinerari detallat en JSON per a %s durant %d dies.
            Formato: {"days": [{"day": 1, "title": "...", "activities": \
            [{"time": "09:00", "name": "...", "description": "...", "duration": "2h", "cost": 0}]}]}
            Inclou almenys 3 activitats per dia. Les hores en format HH:mm.
            """;

    /**
     * Genera l'itinerari en streaming. Quan el flux es completa, parseja i persisteix a la BD.
     *
     * @param trip el viatge per al qual es genera l'itinerari
     * @return Flux de chunks de text (SSE payload)
     */
    public Flux<String> generate(Trip trip) {
        int days = computeDays(trip);
        String systemPrompt = SYSTEM_PROMPT.formatted(trip.getDestination(), days);
        String userPrompt = buildUserPrompt(trip);

        log.info("ItineraryAgent: generant itinerari per a '{}' ({} dies)", trip.getDestination(), days);

        StringBuilder fullResponse = new StringBuilder();

        return ollamaService.streamChat(systemPrompt, userPrompt)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> persistItinerary(trip, fullResponse.toString(), days))
                .doOnError(e -> log.error("Error generant itinerari per trip {}: {}", trip.getId(), e.getMessage()));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private int computeDays(Trip trip) {
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            long computed = trip.getStartDate().until(trip.getEndDate()).getDays() + 1;
            return (int) Math.max(1, Math.min(computed, 30));
        }
        return 3; // valor per defecte si no hi ha dates
    }

    private String buildUserPrompt(Trip trip) {
        StringBuilder sb = new StringBuilder();
        sb.append("Destino: ").append(trip.getDestination()).append("\n");
        sb.append("Duración: ").append(computeDays(trip)).append(" días\n");
        if (trip.getDescription() != null && !trip.getDescription().isBlank()) {
            sb.append("Descripción del viaje: ").append(trip.getDescription()).append("\n");
        }
        if (trip.getStartDate() != null) {
            sb.append("Fecha de inicio: ").append(trip.getStartDate()).append("\n");
        }
        sb.append("Genera el itinerari complet en JSON.");
        return sb.toString();
    }

    private void persistItinerary(Trip trip, String fullJson, int days) {
        try {
            List<DayPlan> dayPlans = itineraryParser.parse(fullJson);
            LocalDate baseDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now();

            for (int i = 0; i < dayPlans.size(); i++) {
                DayPlan plan = dayPlans.get(i);
                int dayNumber = plan.dayNumber() > 0 ? plan.dayNumber() : (i + 1);
                LocalDate dayDate = baseDate.plusDays(dayNumber - 1L);

                // Guardar {title, activities} per poder mostrar el títol al frontend
                String title = plan.title() != null ? plan.title() : "Dia " + dayNumber;
                var dayContent = java.util.Map.of("title", title, "activities", plan.activities() != null ? plan.activities() : java.util.List.of());
                String contentJson = objectMapper.writeValueAsString(dayContent);

                Itinerary itinerary = itineraryRepository
                        .findByTripAndDayNumber(trip, dayNumber)
                        .map(existing -> {
                            existing.setContentJson(contentJson);
                            existing.setGeneratedByAi(true);
                            existing.setVersion(existing.getVersion() + 1);
                            existing.setDate(dayDate);
                            return existing;
                        })
                        .orElseGet(() -> Itinerary.builder()
                                .trip(trip)
                                .dayNumber(dayNumber)
                                .date(dayDate)
                                .contentJson(contentJson)
                                .generatedByAi(true)
                                .version(1)
                                .build());

                itineraryRepository.save(itinerary);
            }

            log.info("ItineraryAgent: {} dies desats per trip {}", dayPlans.size(), trip.getId());
        } catch (Exception e) {
            log.error("ItineraryAgent: error persistint itinerari per trip {}: {}", trip.getId(), e.getMessage());
            throw new AiException("Error desant l'itinerari generat: " + e.getMessage());
        }
    }
}
