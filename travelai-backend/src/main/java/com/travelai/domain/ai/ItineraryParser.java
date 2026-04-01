package com.travelai.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ItineraryParser {

    private final ObjectMapper objectMapper;

    public List<DayPlan> parse(String json) {
        try {
            String clean = cleanJson(json);
            JsonNode root = objectMapper.readTree(clean);
            JsonNode daysNode = root.has("days") ? root.get("days") : root;
            return objectMapper.readerForListOf(DayPlan.class).readValue(daysNode);
        } catch (Exception e) {
            log.error("Error parsejant JSON d'itinerari: {}", json, e);
            throw new AiException("No s'ha pogut parsejar l'itinerari generat: " + e.getMessage());
        }
    }

    public String toJson(List<DayPlan> plans) {
        try {
            return objectMapper.writeValueAsString(Map.of("days", plans));
        } catch (Exception e) {
            throw new AiException("Error serialitzant itinerari: " + e.getMessage());
        }
    }

    /** Elimina possibles blocs markdown que Ollama pot afegir malgrat les instruccions. */
    private String cleanJson(String raw) {
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                s = s.substring(firstNewline + 1, lastFence).strip();
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            s = s.substring(start, end + 1);
        }
        return s;
    }
}
