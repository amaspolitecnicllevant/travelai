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
            You are an expert travel planner. IMPORTANT: Always respond in the same language the user uses.
            Respond ONLY with valid JSON, no markdown, no code blocks, no explanations.
            Generate a %d-day itinerary for %s. Output ALL %d days without stopping early.
            Each day format: {"day":N,"title":"...","activities":[...]}
            Output all days as separate JSON objects one after another.
            Activity format: {"time":"HH:mm","endTime":"HH:mm","name":"...","description":"...","location":"address, city","cost":0,"category":"CULTURE","transportMode":"WALK","travelTime":"10 min"}
            Rules:
            - time of each activity = endTime of previous + travelTime (mathematically consistent)
            - transportMode: WALK, PUBLIC, CAR or TAXI
            - category: CULTURE, FOOD, LEISURE, TRANSPORT, NATURE, SHOPPING, SPORT
            - MEALS: breakfast 08-10h ("Breakfast at X"), lunch 13-15h ("Lunch at X"), dinner 20-22h ("Dinner at X"). Different restaurant each meal, near current area.
            - DAY 1: activity 1 = arrival at arrival point (TRANSPORT), activity 2 = transfer to accommodation (TRANSPORT), then tourist activities after arrival time only.
            - MIDDLE DAYS: start and end at accommodation.
            - LAST DAY: activities until departure time minus travel time minus 30min margin. Second-to-last activity = transfer to departure point (TRANSPORT). Last activity = departure from [point] (TRANSPORT).
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
                .doOnComplete(() -> {
                    try {
                        persistItinerary(trip, fullResponse.toString(), days);
                    } catch (Exception e) {
                        log.error("Error persistint itinerari per trip {}: {}", trip.getId(), e.getMessage());
                    }
                })
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
        int days = computeDays(trip);
        StringBuilder sb = new StringBuilder();
        sb.append("Destino: ").append(trip.getDestination()).append("\n");
        sb.append("Duración: ").append(days).append(" días\n");
        if (trip.getDescription() != null && !trip.getDescription().isBlank()) {
            sb.append("Descripción del viaje: ").append(trip.getDescription()).append("\n");
        }
        if (trip.getStartDate() != null) {
            sb.append("Fecha de inicio: ").append(trip.getStartDate()).append("\n");
        }
        if (trip.getArrivalLocation() != null && !trip.getArrivalLocation().isBlank()) {
            sb.append("Punto de llegada/salida del destino (aeropuerto, estación, etc.): ")
              .append(trip.getArrivalLocation()).append("\n");
        }
        if (trip.getAccommodationAddress() != null && !trip.getAccommodationAddress().isBlank()) {
            sb.append("Dirección del alojamiento: ").append(trip.getAccommodationAddress())
              .append(" — el día 1, PRIMERO ir al alojamiento a dejar el equipaje, ")
              .append("y DESPUÉS comenzar las actividades.\n");
        }
        if (trip.getArrivalTime() != null && !trip.getArrivalTime().isBlank()) {
            sb.append("HORA DE LLEGADA al destino (día 1): ").append(trip.getArrivalTime())
              .append(" — OBLIGATORIO: la primera actividad del día 1 empieza exactamente a las ")
              .append(trip.getArrivalTime())
              .append(". NO planifiques NINGUNA actividad antes de esta hora.\n");
        } else {
            sb.append("No se ha indicado hora de llegada — asume llegada a las 10:00 del día 1.\n");
        }
        if (trip.getDepartureTime() != null && !trip.getDepartureTime().isBlank()) {
            sb.append("Hora de salida del destino (día ").append(days).append("): ")
              .append(trip.getDepartureTime())
              .append(" — el último día, la ÚLTIMA actividad debe ser el trayecto de vuelta ")
              .append("(hacia el aeropuerto, estación o inicio del viaje en coche), ")
              .append("calculando el tiempo necesario para llegar con margen antes de las ")
              .append(trip.getDepartureTime()).append(".\n");
        } else {
            sb.append("El último día no tiene hora de salida fija, pero debe terminar con ")
              .append("la actividad 'Regreso a casa' o 'Salida hacia el punto de partida'.\n");
        }
        if (trip.getPreferredTransport() != null && !trip.getPreferredTransport().isBlank()) {
            sb.append("Medio de transporte preferido entre actividades: ")
              .append(trip.getPreferredTransport()).append("\n");
        }
        if (trip.getTripTypes() != null && !trip.getTripTypes().isEmpty()) {
            sb.append("Tipo de viaje: ").append(String.join(", ", trip.getTripTypes())).append("\n");
        }
        if (trip.getBudget() != null && !trip.getBudget().isBlank()) {
            sb.append("Presupuesto total del viaje: ").append(trip.getBudget()).append("€");
            int d = computeDays(trip);
            try {
                double perDay = Double.parseDouble(trip.getBudget()) / Math.max(1, d);
                sb.append(String.format(" (aprox. %.0f€/día)", perDay));
            } catch (NumberFormatException ignored) {}
            sb.append("\n");
        }
        if (trip.getBudgetLevel() != null && !trip.getBudgetLevel().isBlank()) {
            String label = switch (trip.getBudgetLevel()) {
                case "BUDGET"  -> "Econòmic — hostels, menjar local, transport públic";
                case "COMFORT" -> "Confortable — hotels 3*, restaurants equilibrats";
                case "LUXURY"  -> "Premium — hotels 4-5*, restaurants gastronomics";
                default        -> trip.getBudgetLevel();
            };
            sb.append("Nivel de presupuesto: ").append(label).append("\n");
        }
        sb.append("Genera el itinerari complet en JSON.");
        return sb.toString();
    }

    private void persistItinerary(Trip trip, String fullJson, int days) {
        try {
            int len = fullJson.length();
            log.info("ItineraryAgent: parsejant resposta de {} chars. Inici: [{}] Fi: [{}]",
                len,
                fullJson.substring(0, Math.min(120, len)).replace("\n", "\\n"),
                fullJson.substring(Math.max(0, len - 80)).replace("\n", "\\n"));
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
