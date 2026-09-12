package com.carbonlink.service;

import com.carbonlink.dto.DistanceResult;
import com.carbonlink.entity.DistanceSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DistanceService {

    private static final Logger log = LoggerFactory.getLogger(DistanceService.class);

    private static final double HAVERSINE_ROAD_FACTOR = 1.3;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int CACHE_KEY_DECIMAL_PLACES = 4;

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;

    private final Map<String, DistanceResult> cache = new ConcurrentHashMap<>();

    public DistanceService(RestTemplate restTemplate,
                            @Value("${openrouteservice.api.key}") String apiKey,
                            @Value("${openrouteservice.api.url}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    @PostConstruct
    void warnIfApiKeyMissing() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenRouteService API key not set — DistanceService will use Haversine fallback for all requests");
        }
    }

    public DistanceResult getDistance(double lat1, double lng1, double lat2, double lng2) {
        String cacheKey = buildCacheKey(lat1, lng1, lat2, lng2);
        DistanceResult cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        DistanceResult result = fetchFromOpenRouteService(lat1, lng1, lat2, lng2)
                .orElseGet(() -> haversineFallback(lat1, lng1, lat2, lng2));

        cache.put(cacheKey, result);
        return result;
    }

    private Optional<DistanceResult> fetchFromOpenRouteService(double lat1, double lng1, double lat2, double lng2) {
        try {
            Map<String, Object> body = Map.of(
                    "locations", List.of(List.of(lng1, lat1), List.of(lng2, lat2)),
                    "metrics", List.of("distance")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<OrsMatrixResponse> response =
                    restTemplate.postForEntity(apiUrl, entity, OrsMatrixResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("OpenRouteService returned non-success status: {}", response.getStatusCode());
                return Optional.empty();
            }

            double distanceMeters = response.getBody().distances().get(0).get(1);
            double distanceKm = distanceMeters / 1000.0;

            log.info("Distance resolved via OPENROUTESERVICE: {} km (({}, {}) -> ({}, {}))",
                    String.format(Locale.ROOT, "%.2f", distanceKm), lat1, lng1, lat2, lng2);

            return Optional.of(new DistanceResult(distanceKm, DistanceSource.OPENROUTESERVICE));
        } catch (Exception e) {
            log.warn("OpenRouteService call failed, falling back to Haversine: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private DistanceResult haversineFallback(double lat1, double lng1, double lat2, double lng2) {
        double straightLineKm = haversine(lat1, lng1, lat2, lng2);
        double roadDistanceKm = straightLineKm * HAVERSINE_ROAD_FACTOR;

        log.info("Distance resolved via HAVERSINE_FALLBACK: {} km (({}, {}) -> ({}, {}))",
                String.format(Locale.ROOT, "%.2f", roadDistanceKm), lat1, lng1, lat2, lng2);

        return new DistanceResult(roadDistanceKm, DistanceSource.HAVERSINE_FALLBACK);
    }

    double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private String buildCacheKey(double lat1, double lng1, double lat2, double lng2) {
        return round(lat1) + "," + round(lng1) + ":" + round(lat2) + "," + round(lng2);
    }

    private double round(double value) {
        double scale = Math.pow(10, CACHE_KEY_DECIMAL_PLACES);
        return Math.round(value * scale) / scale;
    }

    // Only the fields of the ORS Matrix API response we actually use.
    // Package-private (not private) so tests can construct one directly.
    record OrsMatrixResponse(List<List<Double>> distances) {
    }
}
