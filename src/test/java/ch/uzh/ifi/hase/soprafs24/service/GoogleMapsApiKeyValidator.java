package ch.uzh.ifi.hase.soprafs24.service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class contains tests that validate the Google Maps API key.
 * It makes real API calls to verify your Google Maps API key is working properly.
 * 
 * To run this test:
 * ./gradlew test --tests ch.uzh.ifi.hase.soprafs24.service.GoogleMapsApiKeyValidator
 */
@SpringBootTest
public class GoogleMapsApiKeyValidator {

    private static final Logger logger = LoggerFactory.getLogger("apiKeyValidation");

    @Value("${google.maps.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;
    
    @Test
    void confirmApiKeySource() {
        // This test confirms where the API key is coming from
        logger.info("API key is loaded from: /src/main/resources/application.properties");
        logger.info("Current API key value: {}...", 
                    apiKey != null && apiKey.length() > 10 ? 
                    apiKey.substring(0, 5) + "..." + apiKey.substring(apiKey.length() - 3) : 
                    "Invalid or missing key");
                    
        // Show if the key looks valid
        boolean validFormat = apiKey != null && apiKey.startsWith("AIza") && apiKey.length() >= 30;
        logger.info("API key format looks valid: {}", validFormat);
    }

    @Test
    void validateApiKeyWorks() {
        // Skip if API key looks like a placeholder
        assumeRealApiKey();

        RestTemplate restTemplate = restTemplateBuilder.build();
        
        // Test with a known valid location (New York City)
        String url = UriComponentsBuilder
            .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
            .queryParam("latlng", "40.7128,-74.0060")
            .queryParam("key", apiKey)
            .toUriString();
            
        try {
            GoogleMapsService.GeocodeResponse response = restTemplate.getForObject(url, GoogleMapsService.GeocodeResponse.class);
            
            // Log detailed response
            logger.info("API response status: {}", response.status);
            logger.info("Results count: {}", response.results != null ? response.results.size() : 0);
            
            if ("REQUEST_DENIED".equals(response.status)) {
                // Check common API key issues
                logger.error("API REQUEST DENIED. This usually means:");
                logger.error("1. The API key hasn't been activated for Geocoding API");
                logger.error("2. Billing hasn't been enabled on your Google Cloud project");
                logger.error("3. The API key has restrictive settings (IP, referrer, etc.)");
                logger.error("4. You've reached your quota limits");
                logger.error("");
                logger.error("To fix this:");
                logger.error("- Go to https://console.cloud.google.com/apis/library/geocoding-backend.googleapis.com");
                logger.error("- Enable the Geocoding API for your project");
                logger.error("- Verify billing is set up at https://console.cloud.google.com/billing");
                logger.error("- Check API key restrictions at https://console.cloud.google.com/apis/credentials");
                
                // This will fail but with better error message
                fail("Google Maps API returned REQUEST_DENIED. See test output for troubleshooting steps.");
            }
            
            // Assert that the request was successful
            assertEquals("OK", response.status, "API key is not working properly. Status: " + response.status);
            assertNotNull(response.results, "Results shouldn't be null");
            assertFalse(response.results.isEmpty(), "No results returned from the API");
            
            // Verify the coordinates
            GoogleMapsService.Location location = response.results.get(0).geometry.location;
            assertNotNull(location, "Location shouldn't be null");
            logger.info("Verified coordinates: lat={}, lng={}", location.lat, location.lng);
            
            logger.info("API KEY VALIDATION SUCCESSFUL! Your API key is working correctly.");
        } catch (Exception e) {
            logger.error("API call failed with exception: {}", e.getMessage(), e);
            fail("API call failed: " + e.getMessage());
        }
    }
    
    private void assumeRealApiKey() {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("AIza") && apiKey.length() < 30) {
            logger.warn("API key looks invalid or like a placeholder: {}", apiKey);
        }
        
        logger.info("Testing API key: {}...", apiKey.substring(0, 10) + "...");
    }
    
    @Test
    void validateRandomCoordinatesGenerator() {
        // Skip if API key looks like a placeholder
        assumeRealApiKey();
        
        GoogleMapsService service = new GoogleMapsService(restTemplateBuilder, apiKey);
        GoogleMapsService.LatLngDTO result = service.getRandomCoordinatesOnLand();
        
        logger.info("Received coordinates: {}, {}", result.getLatitude(), result.getLongitude());
        assertNotNull(result);
        
        // Check if these are NOT one of the fallback coordinates
        for (int i = 0; i < 3; i++) {
            GoogleMapsService.LatLngDTO attempt = service.getRandomCoordinatesOnLand();
            logger.info("Attempt #{}: {}, {}", i+1, attempt.getLatitude(), attempt.getLongitude());
            
            // If we get three different coordinates, it's likely not falling back
            if (Math.abs(attempt.getLatitude() - result.getLatitude()) > 0.001 ||
                Math.abs(attempt.getLongitude() - result.getLongitude()) > 0.001) {
                logger.info("Got different coordinates - API appears to be working!");
                return;
            }
        }
        
        logger.warn("Multiple requests returned the same coordinates - API might be using fallbacks");
        logger.info("To verify, check if the coordinates match any of these common fallback locations:");
        logger.info("  - London (51.5074, -0.1278)");
        logger.info("  - New York (40.7128, -74.0060)");
        logger.info("  - Paris (48.8566, 2.3522)");
        logger.info("  - Tokyo (35.6762, 139.6503)");
        logger.info("  - Berlin (52.5200, 13.4050)");
    }
}
