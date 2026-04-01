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
 * EditorAgent — edita l'itinerari complet d'un viatge seguint les instruccions del prompt de l'usuari.
 * Carrega l'itinerari existent de la BD com a context, envia el prompt a Ollama en streaming
 * i quan el flux es completa parseja i persisteix els dies modificats.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EditorAgent {

    private final OllamaService ollamaService;
    private final ItineraryParser itineraryParser;
    private final ItineraryRepository itineraryRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Eres un asistente de viajes experto. El usuario quiere modificar su itinerario.
            Aplica EXACTAMENTE las instrucciones del usuario al itinerario existente.
            Mantén los días que no se mencionan tal como están.
            Devuelve el itinerario COMPLETO en JSON con la misma estructura.
            SOLO responde con JSON válido, sin texto adicional, sin markdown, sin bloques de código.
            La respuesta empieza con { y acaba con }.
            Formato: {"days": [{"day": 1, "title": "...", "activities": \
            [{"time": "09:00", "name": "...", "description": "...", "duration": "2h", "cost": 0}]}]}
            """;

    /**
     * Edita l'itinerari complet en streaming seguint el prompt de l'usuari.
     * Quan el flux es completa, parseja i persisteix els dies modificats a la BD.
     *
     * @param trip       el viatge que conté l'itinerari a editar
     * @param userPrompt instruccions de l'usuari (ex: "fer el dia 2 més relaxat")
     * @return Flux de chunks de text (SSE payload)
     */
    public Flux<String> edit(Trip trip, String userPrompt) {
        List<Itinerary> existingDays = itineraryRepository.findByTripOrderByDayNumber(trip);
        String currentItinerary = buildCurrentItineraryJson(existingDays);

        String fullUserPrompt = buildUserPrompt(currentItinerary, userPrompt);

        log.info("EditorAgent: editant itinerari del trip {} — prompt: {}", trip.getId(), userPrompt);

        StringBuilder fullResponse = new StringBuilder();

        return ollamaService.streamChat(SYSTEM_PROMPT, fullUserPrompt)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> persistEditedItinerary(trip, fullResponse.toString(), existingDays))
                .doOnError(e -> log.error("EditorAgent: error editant itinerari del trip {}: {}",
                        trip.getId(), e.getMessage()));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String buildCurrentItineraryJson(List<Itinerary> days) {
        if (days.isEmpty()) {
            return "{\"days\": []}";
        }
        StringBuilder sb = new StringBuilder("{\"days\": [");
        for (int i = 0; i < days.size(); i++) {
            Itinerary day = days.get(i);
            sb.append("{\"day\": ").append(day.getDayNumber())
              .append(", \"activities\": ").append(day.getContentJson()).append("}");
            if (i < days.size() - 1) sb.append(", ");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String buildUserPrompt(String currentItinerary, String userPrompt) {
        return """
                Itinerario actual:
                %s

                Instrucciones del usuario:
                %s

                Aplica exactamente las instrucciones y devuelve el itinerario COMPLETO en JSON.
                """.formatted(currentItinerary, userPrompt);
    }

    private void persistEditedItinerary(Trip trip, String fullJson, List<Itinerary> originalDays) {
        try {
            List<DayPlan> dayPlans = itineraryParser.parse(fullJson);
            LocalDate baseDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now();

            for (int i = 0; i < dayPlans.size(); i++) {
                DayPlan plan = dayPlans.get(i);
                int dayNumber = plan.dayNumber() > 0 ? plan.dayNumber() : (i + 1);
                LocalDate dayDate = baseDate.plusDays(dayNumber - 1L);

                String contentJson = objectMapper.writeValueAsString(plan.activities());

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

            log.info("EditorAgent: {} dies actualitzats per trip {}", dayPlans.size(), trip.getId());
        } catch (Exception e) {
            log.error("EditorAgent: error persistint itinerari editat per trip {}: {}",
                    trip.getId(), e.getMessage());
            throw new AiException("Error desant l'itinerari editat: " + e.getMessage());
        }
    }
}
