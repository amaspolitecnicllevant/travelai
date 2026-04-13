package com.travelai.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GeoapifyService — obté llocs d'interès (POI) reals per a una destinació.
 *
 * Usa:
 * - Nominatim (OSM) per geocodificar la destinació → lat/lon (gratuït, sense clau)
 * - Geoapify Places API per obtenir POIs en un radi de 10 km (3.000 crèdits/dia gratuïts)
 *
 * Els POIs s'injecten al prompt de la IA perquè generi activitats amb llocs reals
 * (noms i adreces reals) en lloc d'inventar-los.
 *
 * Degrada graciosament: si no hi ha clau o falla la crida, retorna cadena buida.
 */
@Service
@Slf4j
public class GeoapifyService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1";
    private static final String PLACES_URL =
            "https://api.geoapify.com/v2/places" +
            "?categories=%s&filter=circle:%s,%s,10000&limit=%d&apiKey=%s";

    // Categories útils per a itineraris turístics
    private static final String CATEGORIES =
            "tourism.sightseeing,tourism.attraction,entertainment.museum," +
            "catering.restaurant,natural.park,commercial.shopping_mall";

    private static final int MAX_POIS = 20;

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public GeoapifyService(ObjectMapper objectMapper,
                           @Value("${geoapify.api-key:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey       = apiKey;
    }

    /**
     * Retorna un bloc de text amb POIs reals per injectar al prompt de la IA.
     * Retorna cadena buida si no hi ha clau o falla.
     *
     * @param destination nom del destí (ex: "Tokyo, Japan" o "Barcelona")
     */
    public String getPoisContext(String destination) {
        if (destination == null || destination.isBlank() || apiKey.isBlank()) {
            return "";
        }

        String cacheKey = destination.toLowerCase().trim();
        return cache.computeIfAbsent(cacheKey, k -> {
            try {
                return fetchAndFormat(destination);
            } catch (Exception e) {
                log.warn("GeoapifyService: no s'ha pogut obtenir POIs per '{}': {}",
                        destination, e.getMessage());
                return "";
            }
        });
    }

    // ── internal ─────────────────────────────────────────────────────────────

    private String fetchAndFormat(String destination) throws Exception {
        double[] coords = geocode(destination);
        if (coords == null) {
            log.debug("GeoapifyService: no s'han pogut geocodificar les coordenades per '{}'", destination);
            return "";
        }

        String placesJson = fetchPlaces(coords[1], coords[0]); // lon, lat
        if (placesJson == null) return "";

        return formatContext(placesJson);
    }

    private double[] geocode(String destination) throws Exception {
        // Use first part only for geocoding: "Barcelona, Catalonia, Spain" → "Barcelona"
        String query   = destination.split(",")[0].trim();
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(NOMINATIM_URL.formatted(encoded)))
                .header("User-Agent", "TravelAI/1.0 (travelai@example.com)")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode results = objectMapper.readTree(resp.body());
        if (!results.isArray() || results.isEmpty()) return null;

        JsonNode first = results.get(0);
        return new double[]{
            first.get("lat").asDouble(),
            first.get("lon").asDouble()
        };
    }

    private String fetchPlaces(double lon, double lat) throws Exception {
        String url = PLACES_URL.formatted(CATEGORIES, lon, lat, MAX_POIS, apiKey);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            log.warn("GeoapifyService: API key invàlida o límit superat (HTTP {})", resp.statusCode());
            return null;
        }
        if (resp.statusCode() != 200) {
            log.debug("GeoapifyService: HTTP {} en fetchPlaces", resp.statusCode());
            return null;
        }
        return resp.body();
    }

    private String formatContext(String json) throws Exception {
        JsonNode root     = objectMapper.readTree(json);
        JsonNode features = root.path("features");
        if (!features.isArray() || features.isEmpty()) return "";

        // Group by category
        Map<String, List<String>> byCategory = new LinkedHashMap<>();
        for (JsonNode f : features) {
            JsonNode props   = f.path("properties");
            String name      = props.path("name").asText("").trim();
            String address   = props.path("formatted").asText("").trim();
            String categories = props.path("categories").toString();

            if (name.isBlank()) continue;

            String cat = resolveCategory(categories);
            String entry = address.isBlank() ? name : name + " (" + shortenAddress(address) + ")";
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(entry);
        }

        if (byCategory.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("Real points of interest nearby:\n");
        byCategory.forEach((cat, places) -> {
            sb.append("- ").append(cat).append(": ")
              .append(String.join(", ", places))
              .append("\n");
        });
        sb.append("Use these real place names and addresses in the itinerary when relevant.\n");
        return sb.toString();
    }

    private String resolveCategory(String categories) {
        if (categories.contains("museum"))      return "Museums";
        if (categories.contains("sightseeing") || categories.contains("attraction")) return "Sightseeing";
        if (categories.contains("restaurant") || categories.contains("catering"))    return "Restaurants";
        if (categories.contains("park") || categories.contains("natural"))           return "Parks & Nature";
        if (categories.contains("shopping"))    return "Shopping";
        return "Points of interest";
    }

    private String shortenAddress(String address) {
        // Keep only first two parts: "Museu Picasso, Carrer Montcada 15, Barcelona" → "Carrer Montcada 15, Barcelona"
        String[] parts = address.split(",");
        if (parts.length <= 2) return address;
        return parts[0].trim() + ", " + parts[1].trim();
    }
}
