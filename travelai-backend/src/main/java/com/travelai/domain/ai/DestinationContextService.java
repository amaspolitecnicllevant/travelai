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
import java.util.concurrent.ConcurrentHashMap;

/**
 * DestinationContextService — obté un resum de la destinació via Wikipedia API.
 *
 * Usa l'API pública de Wikipedia (gratuïta, sense API key) per obtenir
 * un extracte de la destinació i injectar-lo com a context als prompts de la IA.
 *
 * Degrada graciosament: si falla la crida, retorna cadena buida.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DestinationContextService {

    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Wikipedia REST API — retorna extracte en text pla (max ~500 chars)
    private static final String WIKI_URL =
            "https://en.wikipedia.org/api/rest_v1/page/summary/%s";

    // Caché per evitar crides repetides a la mateixa destinació
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Retorna un String amb informació bàsica de la destinació per injectar als prompts.
     * Retorna cadena buida en cas d'error (degradació graciosa).
     *
     * @param destination nom de la destinació (ex: "Barcelona", "Tokyo", "Rome")
     */
    public String getDestinationContext(String destination) {
        if (destination == null || destination.isBlank()) return "";

        String cacheKey = destination.toLowerCase().trim();
        return cache.computeIfAbsent(cacheKey, k -> {
            try {
                return fetchContext(destination);
            } catch (Exception e) {
                log.warn("DestinationContextService: no s'ha pogut obtenir context per '{}': {}",
                        destination, e.getMessage());
                return "";
            }
        });
    }

    // ── implementació interna ────────────────────────────────────────────────

    private String fetchContext(String destination) throws Exception {
        // Normalitzar: "New York City" → "New_York_City"
        String encoded = URLEncoder.encode(destination.trim(), StandardCharsets.UTF_8)
                .replace("+", "_");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(WIKI_URL.formatted(encoded)))
                .header("User-Agent", "TravelAI/1.0 (travelai@example.com)")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            log.debug("DestinationContextService: Wikipedia retorna HTTP {} per '{}'",
                    resp.statusCode(), destination);
            return "";
        }

        JsonNode root = objectMapper.readTree(resp.body());

        // Agafar l'extracte de text pla (camp "extract")
        String extract = root.path("extract").asText("");
        if (extract.isBlank()) return "";

        // Limitar a ~400 caràcters per no inflar el prompt
        if (extract.length() > 400) {
            int cutoff = extract.lastIndexOf(". ", 400);
            extract = cutoff > 100 ? extract.substring(0, cutoff + 1) : extract.substring(0, 400);
        }

        return "Destination overview:\n" + extract + "\n";
    }
}
