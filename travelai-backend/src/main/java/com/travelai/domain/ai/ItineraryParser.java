package com.travelai.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ItineraryParser {

    private final ObjectMapper objectMapper;

    public List<DayPlan> parse(String json) {
        try {
            String clean = cleanJson(json);
            JsonNode root = parseOrRepair(clean);
            JsonNode daysNode = root.has("days") ? root.get("days") : root;
            if (!daysNode.isArray()) {
                throw new AiException("S'esperava un array de dies");
            }
            List<DayPlan> result = new ArrayList<>();
            int idx = 0;
            for (JsonNode dayNode : daysNode) {
                idx++;
                DayPlan plan = parseDay(dayNode, idx);
                if (plan != null) result.add(plan);
            }
            if (result.isEmpty()) throw new AiException("No s'han trobat dies en el JSON generat");
            log.info("ItineraryParser: {} dies parsejats correctament", result.size());
            return result;
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsejant JSON d'itinerari: {}", e.getMessage());
            throw new AiException("No s'ha pogut parsejar l'itinerari generat: " + e.getMessage());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JsonNode parseOrRepair(String s) {
        try {
            return objectMapper.readTree(s);
        } catch (Exception e) {
            log.debug("JSON incomplet, intentant reparar: {}", e.getMessage());
            return tryRepair(s);
        }
    }

    private JsonNode tryRepair(String s) {
        // Count unmatched braces/brackets and close them
        int opens = 0, closes = 0, arrOpens = 0, arrCloses = 0;
        boolean inString = false;
        boolean escape = false;
        for (char c : s.toCharArray()) {
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') opens++;
            else if (c == '}') closes++;
            else if (c == '[') arrOpens++;
            else if (c == ']') arrCloses++;
        }
        StringBuilder sb = new StringBuilder(s);
        // Remove trailing incomplete token (comma, colon, partial key)
        String trimmed = s.stripTrailing();
        while (!trimmed.isEmpty()) {
            char last = trimmed.charAt(trimmed.length() - 1);
            if (last == ',' || last == ':') {
                trimmed = trimmed.substring(0, trimmed.length() - 1).stripTrailing();
            } else {
                break;
            }
        }
        sb = new StringBuilder(trimmed);
        // Close arrays first, then objects
        for (int i = 0; i < arrOpens - arrCloses; i++) sb.append(']');
        for (int i = 0; i < opens - closes; i++) sb.append('}');
        try {
            return objectMapper.readTree(sb.toString());
        } catch (Exception e) {
            throw new AiException("No s'ha pogut reparar el JSON generat: " + e.getMessage());
        }
    }

    private DayPlan parseDay(JsonNode node, int fallbackNumber) {
        if (!node.isObject()) return null;
        int dayNumber = intField(node, fallbackNumber, "dayNumber", "day", "dia", "number");
        String title  = strField(node, "Dia " + dayNumber, "title", "titulo", "titol", "name", "nom");
        List<Activity> activities = new ArrayList<>();
        JsonNode acts = node.has("activities")  ? node.get("activities")  :
                        node.has("actividades") ? node.get("actividades") : null;
        if (acts != null && acts.isArray()) {
            for (JsonNode a : acts) {
                Activity act = parseActivity(a);
                if (act != null) activities.add(act);
            }
        }
        return new DayPlan(dayNumber, null, title, activities);
    }

    private Activity parseActivity(JsonNode node) {
        if (!node.isObject()) return null;
        String time  = strField(node, null, "time", "hora", "horario");
        String name  = strField(node, "Activitat", "name", "title", "nombre", "activitat", "activity", "nom", "titol");
        String desc  = strField(node, null, "description", "descripcion", "descripció", "desc");
        String loc   = strField(node, null, "location", "lugar", "lloc", "place", "ubicacion");
        BigDecimal cost = costField(node, "estimatedCost", "cost", "precio", "preu", "price");
        Activity.Category cat = categoryField(node, "category", "type", "tipo", "tipus");
        return new Activity(time, name, desc, loc, cost, cat);
    }

    // ── field extractors ─────────────────────────────────────────────────────

    private String strField(JsonNode node, String def, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) return node.get(key).asText();
        }
        return def;
    }

    private int intField(JsonNode node, int def, String... keys) {
        for (String key : keys) {
            if (node.has(key) && node.get(key).isInt()) return node.get(key).asInt();
        }
        return def;
    }

    private BigDecimal costField(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                try { return new BigDecimal(node.get(key).asText()); } catch (Exception ignored) {}
            }
        }
        return BigDecimal.ZERO;
    }

    private Activity.Category categoryField(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return Activity.Category.fromString(node.get(key).asText());
            }
        }
        return Activity.Category.LEISURE;
    }

    /** Extreu el primer bloc JSON vàlid, elimina markdown si cal. */
    private String cleanJson(String raw) {
        String s = raw == null ? "" : raw.strip();
        // Strip markdown code block
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (nl > 0 && lastFence > nl) s = s.substring(nl + 1, lastFence).strip();
        }
        // Find outermost {...}
        int start = s.indexOf('{');
        int end   = s.lastIndexOf('}');
        if (start >= 0 && end > start) return s.substring(start, end + 1);
        if (start >= 0) return s.substring(start); // no closing brace → let repairJson fix it
        return s;
    }

    public String toJson(List<DayPlan> plans) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("days", plans));
        } catch (Exception e) {
            throw new AiException("Error serialitzant itinerari: " + e.getMessage());
        }
    }
}
