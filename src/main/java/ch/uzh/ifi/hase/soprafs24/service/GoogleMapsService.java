package ch.uzh.ifi.hase.soprafs24.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GoogleMapsService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsService.class);
    private static final String STREETVIEW_METADATA_URL =
        "https://maps.googleapis.com/maps/api/streetview/metadata";

    private final RestTemplate restTemplate;
    private final String apiKey;

    // Track which pano_ids we’ve already handed out, per game
    private final ConcurrentHashMap<Long, Set<String>> usedPanoramaIds = new ConcurrentHashMap<>();

    /** Hot-spot metro bounding boxes where Street View coverage is dense. */
    private static final List<BoundingBox> COVERAGE_REGIONS = List.of(
        new BoundingBox(40.4774, 40.9176, -74.2591, -73.7004), // New York City
        new BoundingBox(34.0522, 34.3373, -118.6682, -118.1553), // Los Angeles
        new BoundingBox(51.2868, 51.6919, -0.5103, 0.3340),      // Greater London
        new BoundingBox(48.8156, 48.9022, 2.2241, 2.4699),       // Paris
        new BoundingBox(35.6528, 35.7840, 139.6503, 139.8395)    // Tokyo
        // …add more regions as you like…
    );

    /** Fallback single points if *all* else fails */
    private static final List<LatLngDTO> FALLBACK_LOCATIONS = List.of(
        new LatLngDTO(51.5074, -0.1278),   // London
        new LatLngDTO(40.7128, -74.0060),  // New York
        new LatLngDTO(48.8566, 2.3522),    // Paris
        new LatLngDTO(35.6762, 139.6503),  // Tokyo
        new LatLngDTO(-33.8688, 151.2093)  // Sydney
    );

    public GoogleMapsService(RestTemplateBuilder builder,
                             @Value("${google.maps.api.key}") String apiKey) {
        this.restTemplate = builder.build();
        this.apiKey = apiKey;
    }

    /** Call at game start to clear history. */
    public void resetUsedPanoramas(long gameId) {
        usedPanoramaIds.put(gameId, ConcurrentHashMap.newKeySet());
    }

    /**
     * Returns a random lat/lng that:
     *  - definitely has Street View metadata == OK
     *  - has never been returned before in this game
     *  - biases sampling toward known coverage regions
     */
    public LatLngDTO getRandomCoordinatesOnLand(long gameId) {
        final int maxAttempts = 60;
        Set<String> seen = usedPanoramaIds
            .computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // 1) Decide where to sample from
            double lat, lng;
            if (ThreadLocalRandom.current().nextDouble() < 0.7) {
                // 70%: pick a random coverage box
                BoundingBox box = COVERAGE_REGIONS
                    .get(ThreadLocalRandom.current().nextInt(COVERAGE_REGIONS.size()));
                lat = ThreadLocalRandom.current().nextDouble(box.minLat, box.maxLat);
                lng = ThreadLocalRandom.current().nextDouble(box.minLng, box.maxLng);
            } else {
                // 30%: uniform global sample
                lat = ThreadLocalRandom.current().nextDouble(-85.0, 85.0);
                lng = ThreadLocalRandom.current().nextDouble(-180.0, 180.0);
            }

            // 2) Check Street View metadata
            String url = UriComponentsBuilder
                .fromHttpUrl(STREETVIEW_METADATA_URL)
                .queryParam("location", lat + "," + lng)
                .queryParam("key", apiKey)
                .toUriString();

            try {
                StreetViewMetadata meta = restTemplate
                    .getForObject(url, StreetViewMetadata.class);

                if (meta != null
                    && "OK".equalsIgnoreCase(meta.status)
                    && meta.location != null
                    && meta.panoId != null
                    && !seen.contains(meta.panoId)) {

                    // Found a fresh panorama!
                    seen.add(meta.panoId);
                    logger.info("[{}/{}] Using pano {} @ {},{}",
                                attempt, maxAttempts,
                                meta.panoId,
                                meta.location.lat, meta.location.lng);
                    return new LatLngDTO(meta.location.lat, meta.location.lng);
                }
            } catch (Exception e) {
                logger.warn("Attempt {}/{} metadata error: {}",
                            attempt, maxAttempts, e.getMessage());
            }
        }

        // FINAL FALLBACK: shuffle through single‐point list
        List<LatLngDTO> copy = new ArrayList<>(FALLBACK_LOCATIONS);
        Collections.shuffle(copy);
        for (LatLngDTO pt : copy) {
            try {
                String url = UriComponentsBuilder
                    .fromHttpUrl(STREETVIEW_METADATA_URL)
                    .queryParam("location", pt.latitude + "," + pt.longitude)
                    .queryParam("key", apiKey)
                    .toUriString();

                StreetViewMetadata meta = restTemplate
                    .getForObject(url, StreetViewMetadata.class);

                if (meta != null
                    && "OK".equalsIgnoreCase(meta.status)
                    && meta.panoId != null
                    && !seen.contains(meta.panoId)) {

                    seen.add(meta.panoId);
                    logger.warn("FALLBACK pano {} @ {}", meta.panoId, pt);
                    return pt;
                }
            } catch (Exception ignored) { /* ignore */ }
        }

        // Last-resort: just give the first fallback
        logger.error("All fallback exhausted, returning {}", copy.get(0));
        return copy.get(0);
    }

    // --- DTOs & helper classes ---

    private static class BoundingBox {
        final double minLat, maxLat, minLng, maxLng;
        BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
            this.minLat = minLat; this.maxLat = maxLat;
            this.minLng = minLng; this.maxLng = maxLng;
        }
    }

    public static class LatLngDTO {
        @JsonProperty("latitude")
        public final double latitude;
        @JsonProperty("longitude")
        public final double longitude;
        public LatLngDTO(double lat, double lng) {
            this.latitude = lat;
            this.longitude = lng;
        }
        public double getLatitude() {
            return latitude;
        }
        public double getLongitude() {
            return longitude;
        }
    }

    public static class StreetViewMetadata {
        @JsonProperty("status") public String status;
        @JsonProperty("location") public Location location;
        @JsonProperty("pano_id")  public String panoId;
    }
    public static class Location {
        @JsonProperty("lat") public double lat;
        @JsonProperty("lng") public double lng;
    }
}


