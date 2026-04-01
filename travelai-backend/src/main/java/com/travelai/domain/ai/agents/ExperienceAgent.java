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

import java.util.List;

/**
 * ExperienceAgent — enriqueix un itinerari existent amb experiències locals.
 *
 * Per a cada dia de l'itinerari, afegeix 1-2 experiències locals úniques:
 * restaurants típics, mercats, festivals, tradicions culturals i gastronòmiques.
 *
 * El stream retorna chunks JSON; quan finalitza, persisteix l'itinerari enriquit a la BD.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExperienceAgent {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Eres un experto en cultura y gastronomía local de %s.
            SOLO respondes con JSON válido, sin texto adicional, sin markdown, sin bloques de código.
            La respuesta empieza con { y termina con }.
            Enriquece el siguiente itinerario añadiendo experiencias locales únicas: restaurantes típicos,
            mercados, festivales, tradiciones. Para cada día añade 1-2 experiencias locales.
            Devuelve el itinerario completo en JSON manteniendo la estructura original pero enriquecido.
            Formato de actividad: {"time":"HH:mm","name":"...","description":"...","duration":"1h","cost":0}
            """;

    private final OllamaService ollamaService;
    private final ItineraryParser itineraryParser;
    private final ItineraryRepository itineraryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Enriqueix l'itinerari del viatge amb experiències locals en streaming.
     * Quan el flux finalitza, persisteix l'itinerari enriquit a la BD.
     *
     * @param trip el viatge a enriquir
     * @return Flux de chunks de text JSON (SSE payload)
     */
    public Flux<String> enrich(Trip trip) {
        List<Itinerary> days = itineraryRepository.findByTripOrderByDayNumber(trip);

        if (days.isEmpty()) {
            return Flux.error(new AiException(
                    "No hi ha itinerari per enriquir. Genera primer l'itinerari del viatge."));
        }

        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(trip.getDestination());
        String userPrompt = buildUserPrompt(trip, days);

        log.info("ExperienceAgent: enriquint itinerari per a '{}' ({} dies)",
                trip.getDestination(), days.size());

        StringBuilder fullResponse = new StringBuilder();

        return ollamaService.streamChat(systemPrompt, userPrompt)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> persistEnrichedItinerary(trip, fullResponse.toString()))
                .doOnError(e -> log.error("ExperienceAgent: error enriquint itinerari per trip {}: {}",
                        trip.getId(), e.getMessage()));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String buildUserPrompt(Trip trip, List<Itinerary> days) {
        StringBuilder sb = new StringBuilder();
        sb.append("Destino: ").append(trip.getDestination()).append("\n");
        sb.append("Días del itinerario:\n");

        for (Itinerary day : days) {
            sb.append("Día ").append(day.getDayNumber());
            if (day.getDate() != null) {
                sb.append(" (").append(day.getDate()).append(")");
            }
            sb.append(": ").append(day.getContentJson()).append("\n");
        }

        sb.append("\nEnriquece este itinerario añadiendo experiencias locales únicas.");
        return sb.toString();
    }

    private void persistEnrichedItinerary(Trip trip, String fullJson) {
        try {
            List<DayPlan> dayPlans = itineraryParser.parse(fullJson);

            for (int i = 0; i < dayPlans.size(); i++) {
                DayPlan plan = dayPlans.get(i);
                int dayNumber = plan.dayNumber() > 0 ? plan.dayNumber() : (i + 1);
                String contentJson = objectMapper.writeValueAsString(plan.activities());

                itineraryRepository.findByTripAndDayNumber(trip, dayNumber)
                        .ifPresent(existing -> {
                            existing.setContentJson(contentJson);
                            existing.setVersion(existing.getVersion() + 1);
                            itineraryRepository.save(existing);
                        });
            }

            log.info("ExperienceAgent: {} dies enriquits per trip {}", dayPlans.size(), trip.getId());
        } catch (Exception e) {
            log.error("ExperienceAgent: error persistint itinerari enriquit per trip {}: {}",
                    trip.getId(), e.getMessage());
            throw new AiException("Error desant l'itinerari enriquit: " + e.getMessage());
        }
    }
}
