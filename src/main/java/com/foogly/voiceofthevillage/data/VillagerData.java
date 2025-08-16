package com.foogly.voiceofthevillage.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.*;

/**
 * Stores all data associated with a villager including name, personality, memories, and reputation.
 * This class is serializable to JSON for persistence.
 */
public class VillagerData {
    @Expose
    @SerializedName("villagerId")
    private UUID villagerId;

    @Expose
    @SerializedName("originalName")
    private String originalName;

    @Expose
    @SerializedName("customName")
    private String customName;

    @Expose
    @SerializedName("gender")
    private Gender gender;

    @Expose
    @SerializedName("personality")
    private PersonalityType personality;

    @Expose
    @SerializedName("memories")
    private List<InteractionMemory> memories;

    @Expose
    @SerializedName("playerReputations")
    private Map<UUID, ReputationData> playerReputations;

    @Expose
    @SerializedName("lastInteractionTime")
    private long lastInteractionTime;

    @Expose
    @SerializedName("creationTime")
    private long creationTime;

    @Expose
    @SerializedName("totalInteractions")
    private int totalInteractions;

    /**
     * Default constructor for JSON deserialization.
     */
    public VillagerData() {
        this.memories = new ArrayList<>();
        this.playerReputations = new HashMap<>();
        this.totalInteractions = 0;
    }

    /**
     * Creates new villager data with generated name and personality.
     *
     * @param villagerId UUID of the villager entity
     * @param name       Generated name for the villager
     * @param gender     Gender based on the name
     * @param personality Random personality type
     */
    public VillagerData(UUID villagerId, String name, Gender gender, PersonalityType personality) {
        this();
        this.villagerId = villagerId;
        this.originalName = name;
        this.gender = gender;
        this.personality = personality;
        this.creationTime = System.currentTimeMillis();
        this.lastInteractionTime = this.creationTime;
    }

    /**
     * Gets the effective name of the villager (custom name if set, otherwise original name).
     *
     * @return The name to display and use for commands
     */
    public String getEffectiveName() {
        return customName != null && !customName.trim().isEmpty() ? customName : originalName;
    }

    /**
     * Sets a custom name for the villager (from name tag).
     *
     * @param customName The new custom name
     */
    public void setCustomName(String customName) {
        this.customName = customName;
    }

    /**
     * Adds a new interaction memory.
     *
     * @param memory The interaction memory to add
     */
    public void addMemory(InteractionMemory memory) {
        this.memories.add(memory);
        this.lastInteractionTime = System.currentTimeMillis();
        this.totalInteractions++;
    }

    /**
     * Gets recent memories for a specific player.
     *
     * @param playerId UUID of the player
     * @param limit    Maximum number of memories to return
     * @return List of recent memories with the player
     */
    public List<InteractionMemory> getRecentMemories(UUID playerId, int limit) {
        return memories.stream()
                .filter(memory -> memory.getPlayerId().equals(playerId))
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit)
                .toList();
    }

    /**
     * Gets all recent memories regardless of player.
     *
     * @param limit Maximum number of memories to return
     * @return List of recent memories
     */
    public List<InteractionMemory> getRecentMemories(int limit) {
        return memories.stream()
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit)
                .toList();
    }

    /**
     * Removes expired memories based on the retention period.
     *
     * @param currentGameDay Current game day
     * @param retentionDays  Number of days to retain memories
     * @return Number of memories removed
     */
    public int cleanupExpiredMemories(long currentGameDay, int retentionDays) {
        int initialSize = memories.size();
        memories.removeIf(memory -> memory.isExpired(currentGameDay, retentionDays));
        return initialSize - memories.size();
    }

    /**
     * Gets or creates reputation data for a player.
     *
     * @param playerId   UUID of the player
     * @param playerName Name of the player
     * @return Reputation data for the player
     */
    public ReputationData getOrCreateReputation(UUID playerId, String playerName) {
        return playerReputations.computeIfAbsent(playerId, id -> new ReputationData(id, playerName));
    }

    /**
     * Gets reputation data for a player if it exists.
     *
     * @param playerId UUID of the player
     * @return Reputation data or null if not found
     */
    public ReputationData getReputation(UUID playerId) {
        return playerReputations.get(playerId);
    }

    /**
     * Adds a reputation event for a player.
     *
     * @param playerId   UUID of the player
     * @param playerName Name of the player
     * @param event      The reputation event
     */
    public void addReputationEvent(UUID playerId, String playerName, ReputationEvent event) {
        ReputationData reputation = getOrCreateReputation(playerId, playerName);
        reputation.addEvent(event);
    }

    // Getters
    public UUID getVillagerId() {
        return villagerId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getCustomName() {
        return customName;
    }

    public Gender getGender() {
        return gender;
    }

    public PersonalityType getPersonality() {
        return personality;
    }

    public List<InteractionMemory> getMemories() {
        return new ArrayList<>(memories);
    }

    public Map<UUID, ReputationData> getPlayerReputations() {
        return new HashMap<>(playerReputations);
    }

    public long getLastInteractionTime() {
        return lastInteractionTime;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public int getTotalInteractions() {
        return totalInteractions;
    }

    // Setters
    public void setVillagerId(UUID villagerId) {
        this.villagerId = villagerId;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setPersonality(PersonalityType personality) {
        this.personality = personality;
    }

    public void setMemories(List<InteractionMemory> memories) {
        this.memories = new ArrayList<>(memories);
    }

    public void setPlayerReputations(Map<UUID, ReputationData> playerReputations) {
        this.playerReputations = new HashMap<>(playerReputations);
    }

    public void setLastInteractionTime(long lastInteractionTime) {
        this.lastInteractionTime = lastInteractionTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setTotalInteractions(int totalInteractions) {
        this.totalInteractions = totalInteractions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VillagerData that = (VillagerData) o;
        return Objects.equals(villagerId, that.villagerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(villagerId);
    }

    @Override
    public String toString() {
        return "VillagerData{" +
               "villagerId=" + villagerId +
               ", effectiveName='" + getEffectiveName() + '\'' +
               ", gender=" + gender +
               ", personality=" + personality +
               ", memories=" + memories.size() +
               ", reputations=" + playerReputations.size() +
               ", totalInteractions=" + totalInteractions +
               '}';
    }
}