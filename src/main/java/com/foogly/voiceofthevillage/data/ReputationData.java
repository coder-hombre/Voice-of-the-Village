package com.foogly.voiceofthevillage.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Tracks reputation between a specific player and villager.
 * Reputation affects villager behavior and responses.
 */
public class ReputationData {
    @Expose
    @SerializedName("playerId")
    private UUID playerId;

    @Expose
    @SerializedName("playerName")
    private String playerName;

    @Expose
    @SerializedName("score")
    private int score;

    @Expose
    @SerializedName("events")
    private List<ReputationEvent> events;

    @Expose
    @SerializedName("lastUpdate")
    private long lastUpdate;

    @Expose
    @SerializedName("hasAttackedPlayer")
    private boolean hasAttackedPlayer;

    @Expose
    @SerializedName("hasSpawnedIronGolem")
    private boolean hasSpawnedIronGolem;

    /**
     * Default constructor for JSON deserialization.
     */
    public ReputationData() {
        this.events = new ArrayList<>();
        this.score = 0;
        this.hasAttackedPlayer = false;
        this.hasSpawnedIronGolem = false;
    }

    /**
     * Creates new reputation data for a player.
     *
     * @param playerId   UUID of the player
     * @param playerName Name of the player
     */
    public ReputationData(UUID playerId, String playerName) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
        this.lastUpdate = System.currentTimeMillis();
    }

    /**
     * Adds a reputation event and updates the score.
     *
     * @param event The reputation event to add
     */
    public void addEvent(ReputationEvent event) {
        this.events.add(event);
        this.score += event.getScoreChange();
        this.lastUpdate = System.currentTimeMillis();
        
        // Clamp score to valid range
        this.score = Math.max(-100, Math.min(100, this.score));
    }

    /**
     * Gets the current reputation threshold for this player.
     *
     * @return The reputation threshold
     */
    public ReputationThreshold getThreshold() {
        return ReputationThreshold.fromScore(this.score);
    }

    /**
     * Checks if the villager should attack this player based on reputation.
     *
     * @return true if the villager should attack
     */
    public boolean shouldAttackPlayer() {
        return getThreshold() == ReputationThreshold.UNFRIENDLY && !hasAttackedPlayer;
    }

    /**
     * Checks if the villager should spawn an iron golem for this player.
     *
     * @return true if the villager should spawn an iron golem
     */
    public boolean shouldSpawnIronGolem() {
        return getThreshold() == ReputationThreshold.HOSTILE && !hasSpawnedIronGolem;
    }

    // Getters
    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public List<ReputationEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public boolean hasAttackedPlayer() {
        return hasAttackedPlayer;
    }

    public boolean hasSpawnedIronGolem() {
        return hasSpawnedIronGolem;
    }

    // Setters
    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setScore(int score) {
        this.score = Math.max(-100, Math.min(100, score));
    }

    public void setEvents(List<ReputationEvent> events) {
        this.events = new ArrayList<>(events);
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setHasAttackedPlayer(boolean hasAttackedPlayer) {
        this.hasAttackedPlayer = hasAttackedPlayer;
    }

    public void setHasSpawnedIronGolem(boolean hasSpawnedIronGolem) {
        this.hasSpawnedIronGolem = hasSpawnedIronGolem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReputationData that = (ReputationData) o;
        return score == that.score &&
               Objects.equals(playerId, that.playerId) &&
               Objects.equals(playerName, that.playerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, playerName, score);
    }

    @Override
    public String toString() {
        return "ReputationData{" +
               "playerId=" + playerId +
               ", playerName='" + playerName + '\'' +
               ", score=" + score +
               ", threshold=" + getThreshold() +
               ", events=" + events.size() +
               ", lastUpdate=" + lastUpdate +
               '}';
    }
}