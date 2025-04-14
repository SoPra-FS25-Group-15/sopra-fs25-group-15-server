package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;

public class ActionCardDTO {
    private Long id;
    private String name;
    private ActionCardType type;
    private String effect;
    private boolean isActive;
    private Long ownerId;

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ActionCardType getType() {
        return type;
    }

    public String getEffect() {
        return effect;
    }

    public boolean isActive() {
        return isActive;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(ActionCardType type) {
        this.type = type;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
}