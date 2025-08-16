package com.foogly.voiceofthevillage.data;

/**
 * Types of events that can affect player-villager reputation.
 */
public enum ReputationEventType {
    POSITIVE_CONVERSATION(5, "Had a pleasant conversation"),
    NEGATIVE_CONVERSATION(-3, "Had an unpleasant conversation"),
    SUCCESSFUL_TRADE(2, "Completed a trade"),
    CANCELLED_TRADE(-1, "Cancelled a trade"),
    PLAYER_HURT_VILLAGER(-15, "Player hurt the villager"),
    PLAYER_HELPED_VILLAGER(10, "Player helped the villager"),
    RUDE_BEHAVIOR(-5, "Player was rude"),
    POLITE_BEHAVIOR(3, "Player was polite"),
    GIFT_GIVEN(8, "Player gave a gift"),
    THEFT(-20, "Player stole from villager"),
    PROTECTION(15, "Player protected villager from danger");

    private final int defaultScoreChange;
    private final String description;

    ReputationEventType(int defaultScoreChange, String description) {
        this.defaultScoreChange = defaultScoreChange;
        this.description = description;
    }

    public int getDefaultScoreChange() {
        return defaultScoreChange;
    }

    public String getDescription() {
        return description;
    }
}