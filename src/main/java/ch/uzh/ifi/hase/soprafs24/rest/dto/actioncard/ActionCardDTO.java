package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ActionCardDTO – Response payload for an action card.
 * Fields:
 *   id          – identifier (e.g. "7choices" or "badsight")
 *   type        – "powerup" or "punishment"
 *   title       – human‐readable title
 *   description – human‐readable description
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionCardDTO {
    private String id;
    private String type;  // "powerup" or "punishment"
    private String title;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}