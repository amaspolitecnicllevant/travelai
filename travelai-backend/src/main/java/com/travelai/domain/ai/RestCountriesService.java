package com.travelai.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RestCountriesService — obté moneda, idioma i zona horària d'un país
 * via l'API pública restcountries.com (gratuïta, sense API key, sense límits).
 *
 * La informació s'injecta als prompts de la IA perquè els itineraris
 * mostrin preus en la moneda correcta i horaris ajustats al fus horari.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RestCountriesService {

    private final ObjectMapper objectMapper;

    private static final String API_URL =
            "https://restcountries.com/v3.1/name/%s?fields=name,currencies,languages,timezones,capital";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Retorna un bloc de text amb moneda, idioma i zona horària per injectar al prompt.
     * Retorna cadena buida en cas d'error (degradació graciosa).
     *
     * @param destination nom del destí (ex: "Tokyo, Japan" o "Barcelona, Catalonia, Spain")
     */
    public String getCountryContext(String destination) {
        if (destination == null || destination.isBlank()) return "";

        String country = extractCountry(destination);
        if (country.isBlank()) return "";

        String cacheKey = country.toLowerCase();
        return cache.computeIfAbsent(cacheKey, k -> {
            try {
                return fetchCountryContext(country);
            } catch (Exception e) {
                log.warn("RestCountriesService: no s'ha pogut obtenir context per '{}': {}",
                        country, e.getMessage());
                return "";
            }
        });
    }

    // ── internal ─────────────────────────────────────────────────────────────

    /**
     * Extreu el país del destí:
     * "Barcelona, Catalonia, Spain" → "Spain"
     * "Tokyo, Japan"               → "Japan"
     * "Paris"                      → "Paris"  (intentarem igualment)
     */
    private String extractCountry(String destination) {
        String[] parts = destination.split(",");
        return parts[parts.length - 1].trim();
    }

    private String fetchCountryContext(String country) throws Exception {
        String encoded = URLEncoder.encode(country, StandardCharsets.UTF_8);
        String url     = API_URL.formatted(encoded);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 404) {
            log.debug("RestCountriesService: país no trobat '{}'", country);
            return "";
        }
        if (resp.statusCode() != 200) {
            log.debug("RestCountriesService: HTTP {} per '{}'", resp.statusCode(), country);
            return "";
        }

        JsonNode root = objectMapper.readTree(resp.body());
        if (!root.isArray() || root.isEmpty()) return "";

        JsonNode c = root.get(0);
        return buildContext(c);
    }

    private String buildContext(JsonNode country) {
        List<String> lines = new ArrayList<>();

        // Currency: "Euro (EUR, €)"
        JsonNode currencies = country.path("currencies");
        if (currencies.isObject()) {
            currencies.fields().forEachRemaining(entry -> {
                String code   = entry.getKey();
                String name   = entry.getValue().path("name").asText("");
                String symbol = entry.getValue().path("symbol").asText("");
                if (!name.isBlank()) {
                    lines.add("Local currency: %s (%s%s)".formatted(
                            name, code, symbol.isBlank() ? "" : ", " + symbol));
                }
            });
        }

        // Languages: "Official languages: Spanish, Catalan"
        JsonNode languages = country.path("languages");
        if (languages.isObject()) {
            List<String> langs = new ArrayList<>();
            languages.fields().forEachRemaining(e -> langs.add(e.getValue().asText()));
            if (!langs.isEmpty()) {
                lines.add("Official languages: " + String.join(", ", langs));
            }
        }

        // Timezone: "Timezone: UTC+01:00"
        JsonNode timezones = country.path("timezones");
        if (timezones.isArray() && !timezones.isEmpty()) {
            lines.add("Timezone: " + timezones.get(0).asText());
        }

        // Capital: "Capital: Madrid"
        String capital = country.path("capital").isArray() && !country.path("capital").isEmpty()
                ? country.path("capital").get(0).asText("") : "";
        if (!capital.isBlank()) {
            lines.add("Capital city: " + capital);
        }

        if (lines.isEmpty()) return "";

        return "Country information:\n" + String.join("\n", lines) + "\n";
    }
}
