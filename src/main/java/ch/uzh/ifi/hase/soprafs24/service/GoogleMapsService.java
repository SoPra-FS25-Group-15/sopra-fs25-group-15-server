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

@Service
public class GoogleMapsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsService.class);
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
     * Generates random land coordinates and returns as a DTO for the client.
     */
    public LatLngDTO getRandomCoordinatesOnLand() {
        final int maxAttempts = 10;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            double lat = ThreadLocalRandom.current().nextDouble(MIN_LAT, MAX_LAT);
            double lng = ThreadLocalRandom.current().nextDouble(MIN_LNG, MAX_LNG);

            String url = UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("latlng", lat + "," + lng)
                .queryParam("key", apiKey)
                .toUriString();

            try {
                GeocodeResponse response = restTemplate.getForObject(url, GeocodeResponse.class);
                if (response != null && "OK".equalsIgnoreCase(response.status) && !response.results.isEmpty()) {
                    Location loc = response.results.get(0).geometry.location;
                    logger.info("[Attempt {}/{}] API land coords: ({}, {})",
                        attempt, maxAttempts, loc.lat, loc.lng);
                    return new LatLngDTO(loc.lat, loc.lng);
                } else {
                    logger.debug("[Attempt {}/{}] No result for ({}, {})",
                        attempt, maxAttempts, lat, lng);
                }
            } catch (Exception ex) {
                logger.warn("[Attempt {}/{}] Google API error: {}",
                    attempt, maxAttempts, ex.getMessage());
            }
        }
        // Fallback
        LatLngDTO fallback = FALLBACK_LOCATIONS.get(
            ThreadLocalRandom.current().nextInt(FALLBACK_LOCATIONS.size())
        );
        logger.warn("Exceeded {} attempts; using fallback: {}", maxAttempts, fallback);
        return fallback;
    }

    // --- Regular classes for Jackson compatibility ---
    public static class GeocodeResponse {
        @JsonProperty("status")
        public String status;
        
        @JsonProperty("results")
        public List<Result> results;
        
        // Default constructor for Jackson
        public GeocodeResponse() {}
        
        // Constructor for tests
        public GeocodeResponse(String status, List<Result> results) {
            this.status = status;
            this.results = results;
        }
    }
    
    public static class Result {
        @JsonProperty("geometry")
        public Geometry geometry;
        
        // Default constructor for Jackson
        public Result() {}
        
        // Constructor for tests
        public Result(Geometry geometry) {
            this.geometry = geometry;
        }
    }
    
    public static class Geometry {
        @JsonProperty("location")
        public Location location;
        
        // Default constructor for Jackson
        public Geometry() {}
        
        // Constructor for tests
        public Geometry(Location location) {
            this.location = location;
        }
    }
    
    public static class Location {
        @JsonProperty("lat")
        public double lat;
        
        @JsonProperty("lng")
        public double lng;
        
        // Default constructor for Jackson
        public Location() {}
        
        // Constructor for tests
        public Location(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
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
