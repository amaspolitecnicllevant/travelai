package com.travelai.domain.ai.agents;

import com.travelai.domain.ai.OllamaService;
import com.travelai.domain.trip.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * ActivityAgent — suggereix activitats addicionals per a un dia concret del viatge.
 *
 * El stream retorna chunks de text JSON amb un array d'activitats suggerides.
 * Temperatura 0.8 (creativitat) configurada a OllamaService via system prompt explícit.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityAgent {

    private final OllamaService ollamaService;

    private static final String SYSTEM_PROMPT = """
            Ets un expert local en %s.
            Suggereix 3 activitats de tipus %s per afegir al dia %d de l'itinerari.
            NOMÉS respons amb JSON vàlid, sense text addicional, sense markdown, sense blocs de codi.
            La resposta és un array JSON que comença amb [ i acaba amb ].
            Format de cada activitat:
            {"name": "...", "description": "...", "time": "HH:mm", "duration": "Xh", "cost": 0, "location": "...", "type": "CULTURE|FOOD|LEISURE|SIGHTSEEING|ADVENTURE"}
            """;

    /**
     * Suggereix activitats en streaming per a un dia concret i una categoria específica.
     *
     * @param trip      el viatge de referència (necessitem el destí)
     * @param dayNumber número del dia de l'itinerari
     * @param category  categoria de les activitats (CULTURE, FOOD, LEISURE, SIGHTSEEING, ADVENTURE)
     * @return Flux de chunks de text (SSE payload)
     */
    public Flux<String> suggest(Trip trip, int dayNumber, String category) {
        String systemPrompt = SYSTEM_PROMPT.formatted(
                trip.getDestination(),
                category.toUpperCase(),
                dayNumber
        );
        String userPrompt = buildUserPrompt(trip, dayNumber, category);

        log.info("ActivityAgent: suggerint activitats '{}' per dia {} de trip '{}'",
                category, dayNumber, trip.getDestination());

        return ollamaService.streamChat(systemPrompt, userPrompt)
                .doOnError(e -> log.error(
                        "ActivityAgent: error suggerint activitats per trip {} dia {}: {}",
                        trip.getId(), dayNumber, e.getMessage()));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String buildUserPrompt(Trip trip, int dayNumber, String category) {
        return "Destí: %s\nDia: %d\nCategoria d'activitats: %s\nGenera les 3 activitats en JSON."
                .formatted(trip.getDestination(), dayNumber, category.toUpperCase());
    }
}
