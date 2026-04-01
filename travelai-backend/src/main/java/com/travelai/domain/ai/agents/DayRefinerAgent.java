package com.travelai.domain.ai.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelai.domain.ai.AiException;
import com.travelai.domain.ai.OllamaService;
import com.travelai.domain.trip.Itinerary;
import com.travelai.domain.trip.ItineraryRepository;
import com.travelai.domain.trip.Trip;
import com.travelai.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * DayRefinerAgent — modifica un dia específic d'un itinerari existent
 * d'acord amb les instruccions del prompt de l'usuari.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DayRefinerAgent {

    private final OllamaService ollamaService;
    private final ItineraryRepository itineraryRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Ets un expert en viatges. Modifica NOMÉS el dia %d del següent itinerari \
            segons les instruccions de l'usuari.
            NOMÉS respons amb JSON vàlid del dia modificat, sense text addicional, \
            sense markdown, sense blocs de codi.
            La resposta comença amb { i acaba amb }.
            Format del dia: {"day": %d, "title": "...", "activities": \
            [{"time": "09:00", "name": "...", "description": "...", "duration": "2h", "cost": 0}]}
            """;

    /**
     * Refina un dia específic de l'itinerari en streaming.
     * Quan el flux es completa, actualitza el registre d'itinerari a la BD.
     *
     * @param trip       el viatge que conté l'itinerari
     * @param dayNumber  el número de dia a refinar (1-based)
     * @param userPrompt instruccions de l'usuari per modificar el dia
     * @return Flux de chunks de text (SSE payload)
     */
    public Flux<String> refine(Trip trip, int dayNumber, String userPrompt) {
        Itinerary existing = itineraryRepository.findByTripAndDayNumber(trip, dayNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ITINERARY_DAY_NOT_FOUND",
                        "No s'ha trobat el dia %d de l'itinerari".formatted(dayNumber)));

        String systemPrompt = SYSTEM_PROMPT.formatted(dayNumber, dayNumber);
        String fullUserPrompt = buildUserPrompt(dayNumber, existing.getContentJson(), userPrompt);

        log.info("DayRefinerAgent: refinant dia {} del trip {} — prompt: {}",
                dayNumber, trip.getId(), userPrompt);

        StringBuilder fullResponse = new StringBuilder();

        return ollamaService.streamChat(systemPrompt, fullUserPrompt)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> persistRefinedDay(existing, fullResponse.toString()))
                .doOnError(e -> log.error("Error refinant dia {} del trip {}: {}",
                        dayNumber, trip.getId(), e.getMessage()));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String buildUserPrompt(int dayNumber, String existingContentJson, String userPrompt) {
        return """
                Itinerari actual del dia %d:
                %s

                Instruccions de l'usuari:
                %s

                Modifica el dia seguint les instruccions i retorna NOMÉS el JSON del dia modificat.
                """.formatted(dayNumber, existingContentJson, userPrompt);
    }

    private void persistRefinedDay(Itinerary itinerary, String refinedJson) {
        try {
            // Attempt to extract the activities array from the AI response JSON.
            // The AI may return the full day object {"day":N,"title":"...","activities":[...]}
            // or just the activities array. We store only activities in contentJson.
            String activitiesJson = extractActivities(refinedJson);

            itinerary.setContentJson(activitiesJson);
            itinerary.setGeneratedByAi(true);
            itinerary.setVersion(itinerary.getVersion() + 1);

            itineraryRepository.save(itinerary);
            log.info("DayRefinerAgent: dia {} actualitzat per trip {}",
                    itinerary.getDayNumber(), itinerary.getTrip().getId());
        } catch (Exception e) {
            log.error("DayRefinerAgent: error persistint dia {} per trip {}: {}",
                    itinerary.getDayNumber(), itinerary.getTrip().getId(), e.getMessage());
            throw new AiException("Error desant el dia refinat: " + e.getMessage());
        }
    }

    /**
     * Intenta extreure l'array d'activitats del JSON retornat per la IA.
     * Si el JSON és un objecte dia complet, en extreu el camp "activities".
     * Si ja és un array, el retorna directament.
     */
    private String extractActivities(String rawJson) {
        try {
            var node = objectMapper.readTree(rawJson);
            if (node.has("activities")) {
                return objectMapper.writeValueAsString(node.get("activities"));
            }
            if (node.isArray()) {
                return rawJson.strip();
            }
            // Fallback: store as-is and let downstream parsing handle it
            return rawJson.strip();
        } catch (Exception e) {
            log.warn("DayRefinerAgent: no s'ha pogut parsejar el JSON refinat, guardant raw: {}", e.getMessage());
            return rawJson.strip();
        }
    }
}
