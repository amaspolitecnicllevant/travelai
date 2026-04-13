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
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WeatherService — obté la previsió meteorològica per a una destinació i dates de viatge.
 *
 * Usa:
 * - Nominatim (OpenStreetMap) per a geocodificació (gratuït, sense API key)
 * - Open-Meteo per a la previsió del temps (gratuït, sense API key, fins a 16 dies)
 *
 * Degrada graciosament: si no es pot obtenir el clima, retorna cadena buida
 * i els agents continuen funcionant sense context meteorològic.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeatherService {

    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1";
    private static final String FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weathercode" +
            "&timezone=auto&start_date=%s&end_date=%s";

    // Caché en memòria per evitar crides repetides durant la mateixa sessió
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Retorna un String formatat amb la previsió del temps per injectar als prompts de la IA.
     * Retorna cadena buida en cas d'error (degradació graciosa).
     *
     * @param destination nom de la destinació (ex: "Barcelona", "Tokyo")
     * @param startDate   data d'inici del viatge
     * @param days        nombre de dies del viatge
     */
    public String getWeatherContext(String destination, LocalDate startDate, int days) {
        if (destination == null || startDate == null || days <= 0) return "";

        // Open-Meteo només ofereix previsió fins a 16 dies vista
        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today.plusDays(15))) {
            log.debug("WeatherService: dates massa llunyanes per a '{}' ({}), sense previsió", destination, startDate);
            return "";
        }

        String cacheKey = destination + "_" + startDate + "_" + days;
        return cache.computeIfAbsent(cacheKey, k -> {
            try {
                return fetchAndFormat(destination, startDate, days);
            } catch (Exception e) {
                log.warn("WeatherService: no s'ha pogut obtenir el clima per '{}': {}", destination, e.getMessage());
                return "";
            }
        });
    }

    // ── implementació interna ────────────────────────────────────────────────

    private String fetchAndFormat(String destination, LocalDate startDate, int days) throws Exception {
        double[] coords = geocode(destination);
        if (coords == null) return "";

        LocalDate today = LocalDate.now();
        LocalDate clampedStart = startDate.isBefore(today) ? today : startDate;
        LocalDate endDate = clampedStart.plusDays(days - 1);
        LocalDate maxEnd = today.plusDays(15);
        if (endDate.isAfter(maxEnd)) endDate = maxEnd;

        String weatherJson = fetchWeather(coords[0], coords[1], clampedStart, endDate);
        if (weatherJson == null) return "";

        return formatContext(weatherJson, clampedStart, days);
    }

    private double[] geocode(String destination) throws Exception {
        String encoded = URLEncoder.encode(destination, StandardCharsets.UTF_8);
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
        return new double[]{ first.get("lat").asDouble(), first.get("lon").asDouble() };
    }

    private String fetchWeather(double lat, double lon, LocalDate start, LocalDate end) throws Exception {
        String url = FORECAST_URL.formatted(lat, lon, start, end);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            log.warn("WeatherService: Open-Meteo retorna HTTP {}", resp.statusCode());
            return null;
        }
        return resp.body();
    }

    private String formatContext(String weatherJson, LocalDate startDate, int days) throws Exception {
        JsonNode root  = objectMapper.readTree(weatherJson);
        JsonNode daily = root.path("daily");

        JsonNode dates   = daily.path("time");
        JsonNode maxT    = daily.path("temperature_2m_max");
        JsonNode minT    = daily.path("temperature_2m_min");
        JsonNode precip  = daily.path("precipitation_sum");
        JsonNode wcodes  = daily.path("weathercode");

        if (dates.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("Weather forecast:\n");
        int available = Math.min(days, dates.size());
        for (int i = 0; i < available; i++) {
            sb.append("- Day ").append(i + 1)
              .append(" (").append(dates.get(i).asText()).append("): ")
              .append(String.format("%.0f–%.0f°C", minT.get(i).asDouble(), maxT.get(i).asDouble()))
              .append(", ").append(describe(wcodes.get(i).asInt(), precip.get(i).asDouble()))
              .append("\n");
        }
        return sb.toString();
    }

    private String describe(int code, double precipMm) {
        return switch (code) {
            case 0       -> "clear sky ☀️";
            case 1, 2    -> "partly cloudy 🌤";
            case 3       -> "overcast ☁️";
            case 45, 48  -> "foggy 🌫";
            case 51, 53, 55 -> "drizzle 🌦";
            case 61, 63  -> "light rain 🌧";
            case 65      -> "heavy rain 🌧";
            case 71, 73  -> "light snow 🌨";
            case 75, 77  -> "heavy snow ❄️";
            case 80, 81  -> "rain showers 🌦";
            case 82      -> "heavy showers ⛈";
            case 95      -> "thunderstorm ⛈";
            case 96, 99  -> "thunderstorm with hail ⛈";
            default      -> precipMm > 5 ? "rainy 🌧" : precipMm > 0 ? "light rain 🌦" : "cloudy ☁️";
        };
    }
}
