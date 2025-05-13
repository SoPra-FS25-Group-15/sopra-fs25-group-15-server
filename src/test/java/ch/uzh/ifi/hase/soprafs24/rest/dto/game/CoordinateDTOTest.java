package ch.uzh.ifi.hase.soprafs24.rest.dto.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordinateDTOTest {

    @Test
    void testDefaultConstructor() {
        CoordinateDTO dto = new CoordinateDTO();
        assertEquals(0.0, dto.getLat(), "Default constructor should initialize lat to 0.0.");
        assertEquals(0.0, dto.getLon(), "Default constructor should initialize lon to 0.0.");
    }

    @Test
    void testParameterizedConstructor() {
        double latitude = 47.3769;  // Example: Zurich
        double longitude = 8.5417; // Example: Zurich
        CoordinateDTO dto = new CoordinateDTO(latitude, longitude);

        assertEquals(latitude, dto.getLat(), "Parameterized constructor should set latitude correctly.");
        assertEquals(longitude, dto.getLon(), "Parameterized constructor should set longitude correctly.");
    }

    @Test
    void testSetAndGetLat() {
        CoordinateDTO dto = new CoordinateDTO();
        double latitude = -34.6037; // Example: Buenos Aires
        dto.setLat(latitude);
        assertEquals(latitude, dto.getLat(), "getLat should return the value set by setLat.");
    }

    @Test
    void testSetAndGetLon() {
        CoordinateDTO dto = new CoordinateDTO();
        double longitude = -58.3816; // Example: Buenos Aires
        dto.setLon(longitude);
        assertEquals(longitude, dto.getLon(), "getLon should return the value set by setLon.");
    }

    @Test
    void testSetCoordinatesToZero() {
        CoordinateDTO dto = new CoordinateDTO(10.0, 20.0); // Initialize with non-zero values
        dto.setLat(0.0);
        dto.setLon(0.0);
        assertEquals(0.0, dto.getLat(), "setLat should allow setting latitude to 0.0.");
        assertEquals(0.0, dto.getLon(), "setLon should allow setting longitude to 0.0.");
    }

    @Test
    void testSetCoordinatesToMaxMinValues() {
        CoordinateDTO dto = new CoordinateDTO();

        dto.setLat(90.0); // Max latitude
        assertEquals(90.0, dto.getLat());

        dto.setLat(-90.0); // Min latitude
        assertEquals(-90.0, dto.getLat());

        dto.setLon(180.0); // Max longitude
        assertEquals(180.0, dto.getLon());

        dto.setLon(-180.0); // Min longitude
        assertEquals(-180.0, dto.getLon());
    }

    @Test
    void testCoordinateValuesBeyondTypicalRange() {
        // DTO itself doesn't enforce geographic limits, just stores doubles.
        // Validation for valid geographic coordinates would be a business logic concern.
        CoordinateDTO dto = new CoordinateDTO();
        dto.setLat(100.0);
        assertEquals(100.0, dto.getLat(), "DTO should store latitude value as provided, even if beyond typical range.");
        dto.setLon(-200.0);
        assertEquals(-200.0, dto.getLon(), "DTO should store longitude value as provided, even if beyond typical range.");
    }
}