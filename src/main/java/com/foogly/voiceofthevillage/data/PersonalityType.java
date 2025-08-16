package com.foogly.voiceofthevillage.data;

/**
 * Represents different personality types for villagers.
 * Each personality affects how villagers respond and interact with players.
 */
public enum PersonalityType {
    FRIENDLY("Warm and welcoming, always happy to chat"),
    GRUMPY("Irritable and short-tempered, but not necessarily hostile"),
    WISE("Thoughtful and knowledgeable, speaks with experience"),
    CHEERFUL("Upbeat and optimistic, sees the bright side of things"),
    CAUTIOUS("Careful and reserved, takes time to trust others"),
    CURIOUS("Inquisitive and interested in learning about the player"),
    MERCHANT("Business-focused, always thinking about trades and profit"),
    GOSSIP("Loves to share news and information about the village");

    private final String description;

    PersonalityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}