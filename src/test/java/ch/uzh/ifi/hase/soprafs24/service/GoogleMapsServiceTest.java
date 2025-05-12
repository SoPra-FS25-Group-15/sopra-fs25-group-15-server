package ch.uzh.ifi.hase.soprafs24.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GoogleMapsServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @InjectMocks
    private GoogleMapsService googleMapsService;

    private final String API_KEY = "test-api-key";
    private final long GAME_ID = 123L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        
        googleMapsService = new GoogleMapsService(restTemplateBuilder, API_KEY);

        
        googleMapsService.resetUsedPanoramas(GAME_ID);
    }

    @Test
    void resetUsedPanoramas_clearsExistingPanoramas() {
        
        ConcurrentHashMap<Long, Set<String>> usedPanoramaIds = new ConcurrentHashMap<>();
        Set<String> testSet = ConcurrentHashMap.newKeySet();
        testSet.add("test-pano-id");
        usedPanoramaIds.put(GAME_ID, testSet);

        
        ReflectionTestUtils.setField(googleMapsService, "usedPanoramaIds", usedPanoramaIds);

        
        googleMapsService.resetUsedPanoramas(GAME_ID);

        
        ConcurrentHashMap<Long, Set<String>> resultMap = (ConcurrentHashMap<Long, Set<String>>)
                ReflectionTestUtils.getField(googleMapsService, "usedPanoramaIds");

        assertNotNull(resultMap);
        assertTrue(resultMap.containsKey(GAME_ID));
        assertTrue(resultMap.get(GAME_ID).isEmpty());
    }

    @Test
    void getRandomCoordinatesOnLand_returnsCoverageRegionCoordinates() {
        
        GoogleMapsService.StreetViewMetadata metadata = new GoogleMapsService.StreetViewMetadata();
        metadata.status = "OK";
        metadata.panoId = "test-pano-id";

        GoogleMapsService.Location location = new GoogleMapsService.Location();
        location.lat = 40.7128;
        location.lng = -74.0060;
        metadata.location = location;

        
        when(restTemplate.getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class)))
                .thenReturn(metadata);

        
        GoogleMapsService.LatLngDTO result = googleMapsService.getRandomCoordinatesOnLand(GAME_ID);

        
        assertNotNull(result);
        assertEquals(40.7128, result.getLatitude());
        assertEquals(-74.0060, result.getLongitude());

        
        verify(restTemplate, times(1)).getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class));

        
        ConcurrentHashMap<Long, Set<String>> resultMap = (ConcurrentHashMap<Long, Set<String>>)
                ReflectionTestUtils.getField(googleMapsService, "usedPanoramaIds");
        assertTrue(resultMap.get(GAME_ID).contains("test-pano-id"));
    }

    @Test
    void getRandomCoordinatesOnLand_avoidsDuplicatePanoramas() {
        
        ConcurrentHashMap<Long, Set<String>> usedPanoramaIds = new ConcurrentHashMap<>();
        Set<String> usedSet = ConcurrentHashMap.newKeySet();
        usedSet.add("used-pano-id");
        usedPanoramaIds.put(GAME_ID, usedSet);
        ReflectionTestUtils.setField(googleMapsService, "usedPanoramaIds", usedPanoramaIds);

        
        GoogleMapsService.StreetViewMetadata usedMetadata = new GoogleMapsService.StreetViewMetadata();
        usedMetadata.status = "OK";
        usedMetadata.panoId = "used-pano-id";

        GoogleMapsService.Location usedLocation = new GoogleMapsService.Location();
        usedLocation.lat = 40.0;
        usedLocation.lng = -74.0;
        usedMetadata.location = usedLocation;

        
        GoogleMapsService.StreetViewMetadata newMetadata = new GoogleMapsService.StreetViewMetadata();
        newMetadata.status = "OK";
        newMetadata.panoId = "new-pano-id";

        GoogleMapsService.Location newLocation = new GoogleMapsService.Location();
        newLocation.lat = 41.0;
        newLocation.lng = -75.0;
        newMetadata.location = newLocation;

        
        when(restTemplate.getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class)))
                .thenReturn(usedMetadata, newMetadata);

        
        GoogleMapsService.LatLngDTO result = googleMapsService.getRandomCoordinatesOnLand(GAME_ID);

        
        assertNotNull(result);
        assertEquals(41.0, result.getLatitude());
        assertEquals(-75.0, result.getLongitude());

        
        verify(restTemplate, times(2)).getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class));
    }

    @Test
    void getRandomCoordinatesOnLand_handlesNullMetadata() {
        
        when(restTemplate.getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class)))
                .thenReturn(null)
                .thenReturn(createValidMetadata()); 

        
        GoogleMapsService.LatLngDTO result = googleMapsService.getRandomCoordinatesOnLand(GAME_ID);

        
        assertNotNull(result);

        
        verify(restTemplate, atLeast(2)).getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class));
    }

    @Test
    void getRandomCoordinatesOnLand_handlesErrorStatus() {
        
        GoogleMapsService.StreetViewMetadata errorMetadata = new GoogleMapsService.StreetViewMetadata();
        errorMetadata.status = "NOT_FOUND";

        
        when(restTemplate.getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class)))
                .thenReturn(errorMetadata)
                .thenReturn(createValidMetadata()); 

        
        GoogleMapsService.LatLngDTO result = googleMapsService.getRandomCoordinatesOnLand(GAME_ID);

        
        assertNotNull(result);

        
        verify(restTemplate, atLeast(2)).getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class));
    }

    @Test
    void getRandomCoordinatesOnLand_handlesNullLocation() {
        
        GoogleMapsService.StreetViewMetadata nullLocationMetadata = new GoogleMapsService.StreetViewMetadata();
        nullLocationMetadata.status = "OK";
        nullLocationMetadata.panoId = "test-pano-id";
        nullLocationMetadata.location = null;

        
        when(restTemplate.getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class)))
                .thenReturn(nullLocationMetadata)
                .thenReturn(createValidMetadata()); 

        
        GoogleMapsService.LatLngDTO result = googleMapsService.getRandomCoordinatesOnLand(GAME_ID);

        
        assertNotNull(result);

        
        verify(restTemplate, atLeast(2)).getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class));
    }

    @Test
    void getRandomCoordinatesOnLand_handlesApiException() {
        
        when(restTemplate.getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class)))
                .thenThrow(new RuntimeException("API error"))
                .thenReturn(createValidMetadata()); 

        
        GoogleMapsService.LatLngDTO result = googleMapsService.getRandomCoordinatesOnLand(GAME_ID);

        
        assertNotNull(result);

        
        verify(restTemplate, atLeast(2)).getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class));
    }

    @Test
    void getRandomCoordinatesOnLand_usesFallbackLocations() {
        
        
        when(restTemplate.getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class)))
                .thenThrow(new RuntimeException("API error"))
                .thenThrow(new RuntimeException("API error"))
                .thenThrow(new RuntimeException("API error"))
                .thenThrow(new RuntimeException("API error"))
                .thenThrow(new RuntimeException("API error"))
                .thenThrow(new RuntimeException("API error"))
                
                .thenReturn(createValidMetadataWithCustomLocation(51.5074, -0.1278)); 

        
        GoogleMapsService.LatLngDTO result = googleMapsService.getRandomCoordinatesOnLand(GAME_ID);

        
        assertNotNull(result);

        
        verify(restTemplate, atLeast(6)).getForObject(anyString(), eq(GoogleMapsService.StreetViewMetadata.class));
    }

    @Test
    void latLngDTO_gettersWork() {
        
        GoogleMapsService.LatLngDTO dto = new GoogleMapsService.LatLngDTO(1.0, 2.0);

        assertEquals(1.0, dto.getLatitude());
        assertEquals(2.0, dto.getLongitude());
        assertEquals(1.0, dto.latitude);
        assertEquals(2.0, dto.longitude);
    }

    
    private GoogleMapsService.StreetViewMetadata createValidMetadata() {
        GoogleMapsService.StreetViewMetadata metadata = new GoogleMapsService.StreetViewMetadata();
        metadata.status = "OK";
        metadata.panoId = "test-pano-id-" + System.currentTimeMillis(); 

        GoogleMapsService.Location location = new GoogleMapsService.Location();
        location.lat = 40.7128;
        location.lng = -74.0060;
        metadata.location = location;

        return metadata;
    }

    
    private GoogleMapsService.StreetViewMetadata createValidMetadataWithCustomLocation(double lat, double lng) {
        GoogleMapsService.StreetViewMetadata metadata = new GoogleMapsService.StreetViewMetadata();
        metadata.status = "OK";
        metadata.panoId = "test-pano-id-" + System.currentTimeMillis(); 

        GoogleMapsService.Location location = new GoogleMapsService.Location();
        location.lat = lat;
        location.lng = lng;
        metadata.location = location;

        return metadata;
    }
}