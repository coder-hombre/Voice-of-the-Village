package com.foogly.voiceofthevillage.data;

/**
 * Represents the type of interaction between a player and villager.
 */
public enum InteractionType {
    VOICE("Voice communication"),
    TEXT("Text communication"),
    TRADE("Trading interaction"),
    NAME_TAG("Villager renamed with name tag"),
    REPUTATION_EVENT("Reputation-affecting event");

    private final String description;

    InteractionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}