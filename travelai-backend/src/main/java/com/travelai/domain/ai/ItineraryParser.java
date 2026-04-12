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
            JsonNode daysNode = findDaysNode(root);
            if (daysNode == null || !daysNode.isArray()) {
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

    /**
     * Tries multiple common JSON structures that models may output.
     * Returns the first JsonNode that is an array of day objects, or null.
     */
    private JsonNode findDaysNode(JsonNode root) {
        // 1. Root is already an array
        if (root.isArray()) return root;
        // 2. root.days
        if (root.has("days") && root.get("days").isArray()) return root.get("days");
        // 3. root.itinerary (array or object with days)
        JsonNode itin = root.get("itinerary");
        if (itin != null) {
            if (itin.isArray()) return itin;
            if (itin.isObject() && itin.has("days") && itin.get("days").isArray()) return itin.get("days");
        }
        // 4. root.plan / root.schedule / root.agenda
        for (String key : new String[]{"plan", "schedule", "agenda", "trips", "viaje", "viatge"}) {
            JsonNode n = root.get(key);
            if (n != null && n.isArray()) return n;
            if (n != null && n.isObject() && n.has("days") && n.get("days").isArray()) return n.get("days");
        }
        // 5. Single-day object: wrap in list
        if (root.has("activities") || root.has("actividades")) {
            com.fasterxml.jackson.databind.node.ArrayNode arr =
                objectMapper.createArrayNode();
            arr.add(root);
            return arr;
        }
        // 6. Scan all array-valued fields
        root.fields().forEachRemaining(entry -> {});
        java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            JsonNode val = fields.next().getValue();
            if (val.isArray() && val.size() > 0 && val.get(0).isObject()) return val;
        }
        return null;
    }

    private JsonNode parseOrRepair(String s) {
        try {
            return objectMapper.readTree(s);
        } catch (Exception e) {
            log.debug("JSON incomplet, intentant reparar: {}", e.getMessage());
            return tryRepair(s);
        }
    }

    private JsonNode tryRepair(String s) {
        // Use a stack to track open brackets in order, so we close them in LIFO order
        java.util.Deque<Character> stack = new java.util.ArrayDeque<>();
        boolean inString = false;
        boolean escape = false;
        for (char c : s.toCharArray()) {
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{' || c == '[') stack.push(c);
            else if (c == '}') { if (!stack.isEmpty() && stack.peek() == '{') stack.pop(); }
            else if (c == ']') { if (!stack.isEmpty() && stack.peek() == '[') stack.pop(); }
        }
        // Remove trailing incomplete token (comma, colon, partial string)
        String trimmed = s.stripTrailing();
        while (!trimmed.isEmpty()) {
            char last = trimmed.charAt(trimmed.length() - 1);
            if (last == ',' || last == ':') {
                trimmed = trimmed.substring(0, trimmed.length() - 1).stripTrailing();
            } else {
                break;
            }
        }
        // Remove trailing partial string if unclosed quote
        if (inString) {
            int lastQuote = trimmed.lastIndexOf('"');
            if (lastQuote >= 0) trimmed = trimmed.substring(0, lastQuote).stripTrailing();
            while (!trimmed.isEmpty() && (trimmed.charAt(trimmed.length()-1) == ',' || trimmed.charAt(trimmed.length()-1) == ':'))
                trimmed = trimmed.substring(0, trimmed.length()-1).stripTrailing();
        }
        // Close in LIFO order (innermost first)
        StringBuilder sb = new StringBuilder(trimmed);
        while (!stack.isEmpty()) {
            sb.append(stack.pop() == '{' ? '}' : ']');
        }
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
        String time          = strField(node, null, "time", "hora", "horario", "arrivalTime");
        String endTime       = strField(node, null, "endTime", "end_time", "departureTime", "horaFi");
        String name          = strField(node, "Activitat", "name", "title", "nombre", "activitat", "activity", "nom", "titol");
        String desc          = strField(node, null, "description", "descripcion", "descripció", "desc");
        String loc           = strField(node, null, "location", "lugar", "lloc", "place", "ubicacion");
        BigDecimal cost      = costField(node, "estimatedCost", "cost", "precio", "preu", "price");
        Activity.Category cat = categoryField(node, "category", "type", "tipo", "tipus");
        String transport     = strField(node, null, "transportMode", "transport", "transport_mode", "mitja");
        String travelTime    = strField(node, null, "travelTime", "travel_time", "tempsDesplacament", "temps");
        return new Activity(time, endTime, name, desc, loc, cost, cat, transport, travelTime);
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
        // Prefer root array [...] if it starts before the first object
        int objStart = s.indexOf('{');
        int arrStart = s.indexOf('[');
        if (arrStart >= 0 && (objStart < 0 || arrStart < objStart)) {
            int end = s.lastIndexOf(']');
            if (end > arrStart) return s.substring(arrStart, end + 1);
            return s.substring(arrStart); // unclosed → repair
        }
        if (objStart < 0) return s;
        // Extract all root-level {...} objects (model may output one per day)
        return extractRootObjects(s, objStart);
    }

    /**
     * Scans the string for all root-level {...} objects.
     * If multiple are found, wraps them in [...] so the parser sees an array.
     * If only one, returns it as-is (possibly incomplete → tryRepair fixes it).
     */
    private String extractRootObjects(String s, int firstObjStart) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        int objectStart = -1;

        for (int i = firstObjStart; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') {
                if (depth == 0) objectStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    objects.add(s.substring(objectStart, i + 1));
                    objectStart = -1;
                }
            }
        }

        if (objects.size() > 1) {
            log.debug("cleanJson: {} objectes arrel trobats — embolicant en array", objects.size());
            return "[" + String.join(",", objects) + "]";
        }
        if (objects.size() == 1) return objects.get(0);
        // Nothing cleanly closed → return from first { to last } (truncated, let tryRepair fix)
        int end = s.lastIndexOf('}');
        if (end > firstObjStart) return s.substring(firstObjStart, end + 1);
        return s.substring(firstObjStart);
    }

    public String toJson(List<DayPlan> plans) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("days", plans));
        } catch (Exception e) {
            throw new AiException("Error serialitzant itinerari: " + e.getMessage());
        }
    }
}
