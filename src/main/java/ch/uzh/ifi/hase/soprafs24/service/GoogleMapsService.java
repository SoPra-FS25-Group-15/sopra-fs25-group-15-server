package ch.uzh.ifi.hase.soprafs24.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * GoogleMapsService now ensures that any randomly chosen coordinate
 * not only lies on land, but also has active Street View imagery.
 */
@Service
public class GoogleMapsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsService.class);

    // Metadata endpoint to check for Street View coverage
    private static final String STREETVIEW_METADATA_URL =
        "https://maps.googleapis.com/maps/api/streetview/metadata";

    private final RestTemplate restTemplate;
    private final String apiKey;

    private static final double MIN_LAT = -85.0;
    private static final double MAX_LAT = 85.0;
    private static final double MIN_LNG = -180.0;
    private static final double MAX_LNG = 180.0;

    public static final List<LatLngDTO> FALLBACK_LOCATIONS = List.of(
        new LatLngDTO(51.5074, -0.1278),   // London
        new LatLngDTO(40.7128, -74.0060),  // New York
        new LatLngDTO(48.8566, 2.3522),    // Paris
        new LatLngDTO(35.6762, 139.6503),  // Tokyo
        new LatLngDTO(-33.8688, 151.2093), // Sydney
        new LatLngDTO(55.7558, 37.6173),   // Moscow
        new LatLngDTO(37.7749, -122.4194), // San Francisco
        new LatLngDTO(41.9028, 12.4964),   // Rome
        new LatLngDTO(52.5200, 13.4050),   // Berlin
        new LatLngDTO(25.2048, 55.2708),   // Dubai
        new LatLngDTO(-22.9068, -43.1729), // Rio de Janeiro
        new LatLngDTO(1.3521, 103.8198)    // Singapore
    );

    public GoogleMapsService(RestTemplateBuilder restTemplateBuilder,
                             @Value("${google.maps.api.key}") String apiKey) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
    }

    /**
     * Generates random coordinates that
     * 1. reverse‐geocode to land, and
     * 2. have Street View coverage.
     *
     * Retries up to 10 times, then picks from a fixed fallback list.
     */
    public LatLngDTO getRandomCoordinatesOnLand() {
        final int maxAttempts = 30;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // 1) pick a random lat/lng
            double lat = ThreadLocalRandom.current().nextDouble(MIN_LAT, MAX_LAT);
            double lng = ThreadLocalRandom.current().nextDouble(MIN_LNG, MAX_LNG);

            // 2) reverse‐geocode to find nearest land coordinate
            String geocodeUrl = UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("latlng", lat + "," + lng)
                .queryParam("key", apiKey)
                .toUriString();

            try {
                GeocodeResponse geoResp = restTemplate.getForObject(geocodeUrl, GeocodeResponse.class);
                if (geoResp != null
                    && "OK".equalsIgnoreCase(geoResp.status)
                    && !geoResp.results.isEmpty())
                {
                    Location loc = geoResp.results.get(0).geometry.location;
                    double candidateLat = loc.lat;
                    double candidateLng = loc.lng;
                    logger.info("[Attempt {}/{}] Land at ({}, {})",
                                attempt, maxAttempts, candidateLat, candidateLng);

                    // 3) check Street View metadata for that coordinate
                    String metaUrl = UriComponentsBuilder
                        .fromHttpUrl(STREETVIEW_METADATA_URL)
                        .queryParam("location", candidateLat + "," + candidateLng)
                        .queryParam("key", apiKey)
                        .toUriString();

                    StreetViewMetadata meta = restTemplate
                        .getForObject(metaUrl, StreetViewMetadata.class);

                    if (meta != null && "OK".equalsIgnoreCase(meta.status)) {
                        logger.info("[Attempt {}/{}] Street View OK at ({}, {})",
                                    attempt, maxAttempts, candidateLat, candidateLng);
                        return new LatLngDTO(candidateLat, candidateLng);
                    } else {
                        logger.debug("[Attempt {}/{}] No Street View at ({}, {}): status={}",
                                     attempt, maxAttempts, candidateLat, candidateLng,
                                     meta == null ? "null" : meta.status);
                    }
                } else {
                    logger.debug("[Attempt {}/{}] No land result for ({}, {})",
                                 attempt, maxAttempts, lat, lng);
                }
            } catch (Exception ex) {
                logger.warn("[Attempt {}/{}] Google API error: {}",
                            attempt, maxAttempts, ex.getMessage());
            }
        }

        // 4) fallback if all else fails
        LatLngDTO fallback = FALLBACK_LOCATIONS.get(
            ThreadLocalRandom.current().nextInt(FALLBACK_LOCATIONS.size())
        );
        logger.warn("No suitable Street View found after {} attempts; using fallback: {}",
                    maxAttempts, fallback);
        return fallback;
    }

    // --- Internal DTOs for JSON mapping ---

    public static class GeocodeResponse {
        @JsonProperty("status")
        public String status;

        @JsonProperty("results")
        public List<Result> results;

        public GeocodeResponse() {}
    }

    public static class Result {
        @JsonProperty("geometry")
        public Geometry geometry;

        public Result() {}
    }

    public static class Geometry {
        @JsonProperty("location")
        public Location location;

        public Geometry() {}
    }

    public static class Location {
        @JsonProperty("lat")
        public double lat;

        @JsonProperty("lng")
        public double lng;

        public Location() {}
    }

    public static class StreetViewMetadata {
        @JsonProperty("status")
        public String status;

        public StreetViewMetadata() {}
    }

    /**
     * DTO returned to clients containing latitude & longitude.
     */
    public static class LatLngDTO {
        @JsonProperty("latitude")
        private final double latitude;

        @JsonProperty("longitude")
        private final double longitude;

        public LatLngDTO(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @Override
        public String toString() {
            return "LatLngDTO{" + "latitude=" + latitude + ", longitude=" + longitude + '}';
        }
    }
}
