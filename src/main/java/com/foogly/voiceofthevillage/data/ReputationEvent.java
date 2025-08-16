package com.foogly.voiceofthevillage.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * Represents a single event that affects player-villager reputation.
 */
public class ReputationEvent {
    @Expose
    @SerializedName("eventType")
    private ReputationEventType eventType;

    @Expose
    @SerializedName("scoreChange")
    private int scoreChange;

    @Expose
    @SerializedName("timestamp")
    private long timestamp;

    @Expose
    @SerializedName("description")
    private String description;

    /**
     * Default constructor for JSON deserialization.
     */
    public ReputationEvent() {
    }

    /**
     * Creates a new reputation event.
     *
     * @param eventType   Type of the event
     * @param scoreChange Change in reputation score
     * @param description Description of the event
     */
    public ReputationEvent(ReputationEventType eventType, int scoreChange, String description) {
        this.eventType = eventType;
        this.scoreChange = scoreChange;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public ReputationEventType getEventType() {
        return eventType;
    }

    public int getScoreChange() {
        return scoreChange;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    // Setters
    public void setEventType(ReputationEventType eventType) {
        this.eventType = eventType;
    }

    public void setScoreChange(int scoreChange) {
        this.scoreChange = scoreChange;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReputationEvent that = (ReputationEvent) o;
        return scoreChange == that.scoreChange &&
               timestamp == that.timestamp &&
               eventType == that.eventType &&
               Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, scoreChange, timestamp, description);
    }

    @Override
    public String toString() {
        return "ReputationEvent{" +
               "eventType=" + eventType +
               ", scoreChange=" + scoreChange +
               ", timestamp=" + timestamp +
               ", description='" + description + '\'' +
               '}';
    }
}