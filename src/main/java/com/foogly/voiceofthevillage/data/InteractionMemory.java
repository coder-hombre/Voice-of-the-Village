package com.foogly.voiceofthevillage.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single interaction memory between a player and villager.
 * Stores conversation history and context for AI-powered responses.
 */
public class InteractionMemory {
    @Expose
    @SerializedName("playerId")
    private UUID playerId;

    @Expose
    @SerializedName("playerName")
    private String playerName;

    @Expose
    @SerializedName("playerMessage")
    private String playerMessage;

    @Expose
    @SerializedName("villagerResponse")
    private String villagerResponse;

    @Expose
    @SerializedName("timestamp")
    private long timestamp;

    @Expose
    @SerializedName("interactionType")
    private InteractionType interactionType;

    @Expose
    @SerializedName("gameDay")
    private long gameDay;

    /**
     * Default constructor for JSON deserialization.
     */
    public InteractionMemory() {
        this.timestamp = 0L;
    }

    /**
     * Creates a new interaction memory.
     *
     * @param playerId         UUID of the player
     * @param playerName       Name of the player
     * @param playerMessage    Message sent by the player
     * @param villagerResponse Response from the villager
     * @param interactionType  Type of interaction
     * @param gameDay          Current game day
     */
    public InteractionMemory(UUID playerId, String playerName, String playerMessage, 
                           String villagerResponse, InteractionType interactionType, long gameDay) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playerMessage = playerMessage;
        this.villagerResponse = villagerResponse;
        this.interactionType = interactionType;
        this.gameDay = gameDay;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerMessage() {
        return playerMessage;
    }

    public String getVillagerResponse() {
        return villagerResponse;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public long getGameDay() {
        return gameDay;
    }

    // Setters
    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setPlayerMessage(String playerMessage) {
        this.playerMessage = playerMessage;
    }

    public void setVillagerResponse(String villagerResponse) {
        this.villagerResponse = villagerResponse;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    public void setGameDay(long gameDay) {
        this.gameDay = gameDay;
    }

    /**
     * Checks if this memory is older than the specified number of days.
     *
     * @param currentGameDay Current game day
     * @param retentionDays  Number of days to retain memories
     * @return true if the memory should be cleaned up
     */
    public boolean isExpired(long currentGameDay, int retentionDays) {
        return (currentGameDay - this.gameDay) > retentionDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InteractionMemory that = (InteractionMemory) o;
        return timestamp == that.timestamp &&
               gameDay == that.gameDay &&
               Objects.equals(playerId, that.playerId) &&
               Objects.equals(playerMessage, that.playerMessage) &&
               Objects.equals(villagerResponse, that.villagerResponse) &&
               interactionType == that.interactionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, playerMessage, villagerResponse, timestamp, interactionType, gameDay);
    }

    @Override
    public String toString() {
        return "InteractionMemory{" +
               "playerId=" + playerId +
               ", playerName='" + playerName + '\'' +
               ", playerMessage='" + playerMessage + '\'' +
               ", villagerResponse='" + villagerResponse + '\'' +
               ", timestamp=" + timestamp +
               ", interactionType=" + interactionType +
               ", gameDay=" + gameDay +
               '}';
    }
}