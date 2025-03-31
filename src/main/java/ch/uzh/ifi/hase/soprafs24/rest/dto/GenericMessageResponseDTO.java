package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GenericMessageResponseDTO {
    private String message;

    public GenericMessageResponseDTO() {}

    public GenericMessageResponseDTO(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

