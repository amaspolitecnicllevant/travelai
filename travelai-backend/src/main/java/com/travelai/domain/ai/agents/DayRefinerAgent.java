package com.travelai.domain.ai.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelai.domain.ai.AiException;
import com.travelai.domain.ai.DayPlan;
import com.travelai.domain.ai.ItineraryParser;
import com.travelai.domain.ai.OllamaService;
import com.travelai.domain.trip.Itinerary;
import com.travelai.domain.trip.ItineraryRepository;
import com.travelai.domain.trip.Trip;
import com.travelai.shared.exception.ResourceNotFoundException;

import java.util.List;
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
    private final ItineraryParser itineraryParser;
    private final ItineraryRepository itineraryRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Ets un expert en viatges. Modifica NOMÉS el dia %d de l'itinerari \
            segons les instruccions de l'usuari.
            IMPORTANT: Respon SEMPRE en el mateix idioma que l'usuari fa servir al seu missatge.
            NOMÉS respons amb JSON vàlid del dia modificat, sense text addicional, \
            sense markdown, sense blocs de codi. La resposta comença amb { i acaba amb }.
            Format: {"day": %d, "title": "...", "activities": \
            [{"time": "09:00", "endTime": "11:00", "name": "...", "description": "...", \
            "location": "adreça real, ciutat", "cost": 0, "category": "CULTURE", \
            "transportMode": "WALK", "travelTime": "10 min a peu"}]}
            Regles que SEMPRE has de respectar:
            - L'hora d'inici (time) de cada activitat = endTime anterior + travelTime.
            - travelTime: temps real amb el mitjà de transport indicat.
            - transportMode: WALK, PUBLIC, CAR o TAXI.
            - category: CULTURE, FOOD, LEISURE, TRANSPORT, NATURE, SHOPPING, SPORT.
            - Cada àpat en un restaurant DIFERENT, proper a la zona visitada.
            - El dia ha de mantenir una ruta lògica geogràficament (sense salts innecessaris).
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
                .doOnComplete(() -> {
                    try {
                        persistRefinedDay(existing, fullResponse.toString(), dayNumber);
                    } catch (Exception e) {
                        log.error("Error persistint dia {} del trip {}: {}",
                                dayNumber, trip.getId(), e.getMessage());
                    }
                })
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

    private void persistRefinedDay(Itinerary itinerary, String refinedJson, int dayNumber) {
        // Use ItineraryParser for robust JSON cleaning, repair and field extraction
        List<DayPlan> plans = itineraryParser.parse(refinedJson);
        DayPlan plan = plans.isEmpty() ? null : plans.get(0);

        String title = (plan != null && plan.title() != null)
                ? plan.title()
                : "Dia " + dayNumber;
        var activities = (plan != null && plan.activities() != null)
                ? plan.activities()
                : java.util.List.of();

        // Store as {title, activities} — same format as ItineraryAgent
        try {
            String contentJson = objectMapper.writeValueAsString(
                    java.util.Map.of("title", title, "activities", activities));
            itinerary.setContentJson(contentJson);
            itinerary.setGeneratedByAi(true);
            itinerary.setVersion(itinerary.getVersion() + 1);
            itineraryRepository.save(itinerary);
            log.info("DayRefinerAgent: dia {} actualitzat per trip {}",
                    itinerary.getDayNumber(), itinerary.getTrip().getId());
        } catch (Exception e) {
            log.error("DayRefinerAgent: error serialitzant dia {} per trip {}: {}",
                    itinerary.getDayNumber(), itinerary.getTrip().getId(), e.getMessage());
            throw new AiException("Error desant el dia refinat: " + e.getMessage());
        }
    }
}
