package com.foogly.voiceofthevillage.data;

/**
 * Reputation thresholds that trigger different villager behaviors.
 */
public enum ReputationThreshold {
    HOSTILE(-80, "Villager will spawn iron golem to attack player"),
    UNFRIENDLY(-40, "Villager will attack player once"),
    NEUTRAL(0, "Normal villager behavior"),
    FRIENDLY(40, "Villager is more helpful and talkative"),
    BELOVED(80, "Villager offers special benefits and discounts");

    private final int threshold;
    private final String description;

    ReputationThreshold(int threshold, String description) {
        this.threshold = threshold;
        this.description = description;
    }

    public int getThreshold() {
        return threshold;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines the reputation threshold for a given score.
     *
     * @param score The reputation score
     * @return The appropriate reputation threshold
     */
    public static ReputationThreshold fromScore(int score) {
        if (score <= HOSTILE.threshold) {
            return HOSTILE;
        } else if (score <= UNFRIENDLY.threshold) {
            return UNFRIENDLY;
        } else if (score >= BELOVED.threshold) {
            return BELOVED;
        } else if (score >= FRIENDLY.threshold) {
            return FRIENDLY;
        } else {
            return NEUTRAL;
        }
    }
}