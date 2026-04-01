package com.travelai.domain.ai.agents;

import com.travelai.domain.ai.OllamaService;
import com.travelai.domain.trip.Itinerary;
import com.travelai.domain.trip.ItineraryRepository;
import com.travelai.domain.trip.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI agent that estimates the total cost of a trip's itinerary.
 * Streams a JSON response via Ollama following the BudgetAgent system prompt.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetAgent {

    private static final String SYSTEM_PROMPT = """
        Eres un experto en viajes y presupuestos. Analiza el siguiente itinerario y estima el coste total desglosado por días y categorías (alojamiento, comida, transporte, actividades).
        Devuelve JSON: {"totalEstimate": 0, "currency": "EUR", "days": [{"day": 1, "subtotal": 0, "breakdown": {"accommodation": 0, "food": 0, "transport": 0, "activities": 0}}]}
        SOLO responde con JSON válido, sin texto adicional, sin markdown, sin bloques de código.
        """;

    private final OllamaService ollamaService;
    private final ItineraryRepository itineraryRepository;

    /**
     * Streams a budget estimate for the given trip.
     * The user message is built from the trip metadata and its itinerary content.
     *
     * @param trip the trip entity to analyse
     * @return Flux of SSE text chunks containing the streamed JSON budget
     */
    public Flux<String> estimate(Trip trip) {
        String userMessage = buildUserMessage(trip);
        log.debug("BudgetAgent — estimating budget for trip {} ({})", trip.getId(), trip.getDestination());
        return ollamaService.streamChat(SYSTEM_PROMPT, userMessage);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private String buildUserMessage(Trip trip) {
        List<Itinerary> itineraries = itineraryRepository.findByTripOrderByDayNumber(trip);

        StringBuilder sb = new StringBuilder();
        sb.append("Destino: ").append(trip.getDestination()).append("\n");
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            sb.append("Fechas: ").append(trip.getStartDate()).append(" → ").append(trip.getEndDate()).append("\n");
        }
        sb.append("Número de días con itinerario: ").append(itineraries.size()).append("\n\n");

        if (itineraries.isEmpty()) {
            sb.append("No hay itinerario generado todavía. Estima un presupuesto medio para este destino.");
        } else {
            sb.append("Itinerario:\n");
            for (Itinerary day : itineraries) {
                sb.append("Día ").append(day.getDayNumber());
                if (day.getDate() != null) {
                    sb.append(" (").append(day.getDate()).append(")");
                }
                sb.append(":\n").append(day.getContentJson()).append("\n\n");
            }
        }

        return sb.toString();
    }
}
