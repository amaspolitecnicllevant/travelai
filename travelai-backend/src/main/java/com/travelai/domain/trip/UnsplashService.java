package com.travelai.domain.trip;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * UnsplashService — obté una foto de portada per a un viatge via Unsplash API.
 *
 * Requereix UNSPLASH_ACCESS_KEY (registre gratuït a unsplash.com/developers).
 * Degrada graciosament: si no hi ha clau o la crida falla, retorna null.
 * Caché en memòria per destinació per evitar crides repetides.
 */
@Service
@Slf4j
public class UnsplashService {

    private static final String UNSPLASH_URL =
            "https://api.unsplash.com/search/photos?query=%s&per_page=1&orientation=landscape";

    private final ObjectMapper objectMapper;
    private final String accessKey;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public UnsplashService(ObjectMapper objectMapper,
                           @Value("${unsplash.access-key:}") String accessKey) {
        this.objectMapper = objectMapper;
        this.accessKey    = accessKey;
    }

    /**
     * Retorna la URL d'una foto de la destinació, o null si no hi ha clau / falla.
     */
    public String getCoverImageUrl(String destination) {
        if (destination == null || destination.isBlank() || accessKey.isBlank()) {
            return null;
        }

        // Simplify query: use only the first part of "Barcelona, Catalonia, Spain" → "Barcelona"
        String query = destination.split(",")[0].trim();
        String cacheKey = query.toLowerCase();

        return cache.computeIfAbsent(cacheKey, k -> {
            try {
                return fetchPhotoUrl(query);
            } catch (Exception e) {
                log.warn("UnsplashService: no s'ha pogut obtenir foto per '{}': {}", query, e.getMessage());
                return null;
            }
        });
    }

    // ── internal ─────────────────────────────────────────────────────────────

    private String fetchPhotoUrl(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url     = UNSPLASH_URL.formatted(encoded);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Client-ID " + accessKey)
                .header("Accept-Version", "v1")
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 401) {
            log.warn("UnsplashService: API key invàlida o expirada");
            return null;
        }
        if (resp.statusCode() == 403) {
            log.warn("UnsplashService: límit de peticions Unsplash superat (50/hora en mode demo)");
            return null;
        }
        if (resp.statusCode() != 200) {
            log.debug("UnsplashService: HTTP {} per '{}'", resp.statusCode(), query);
            return null;
        }

        JsonNode root    = objectMapper.readTree(resp.body());
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) return null;

        // Prefer "regular" size (1080px wide) — good balance quality/size
        String photoUrl = results.get(0).path("urls").path("regular").asText(null);
        if (photoUrl == null || photoUrl.isBlank()) return null;

        log.debug("UnsplashService: foto obtinguda per '{}'", query);
        return photoUrl;
    }
}
