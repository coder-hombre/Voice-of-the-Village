package com.foogly.voiceofthevillage.ai;

import com.foogly.voiceofthevillage.data.VillagerData;


import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for AI service providers.
 * Defines the interface for generating villager responses using external AI services.
 */
public abstract class AIProvider {
    protected final String apiKey;
    protected final String model;
    protected final int timeoutSeconds;

    /**
     * Creates a new AI provider instance.
     *
     * @param apiKey         API key for the service
     * @param model          Model to use for generation
     * @param timeoutSeconds Request timeout in seconds
     */
    protected AIProvider(String apiKey, String model, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Generates a response from the villager based on the player's message and context.
     *
     * @param playerMessage  The message from the player
     * @param villagerData   Data about the villager (personality, memories, etc.)
     * @param player         The player entity (can be null in tests)
     * @param gameContext    Additional game context information (can be null in tests)
     * @return CompletableFuture containing the generated response
     */
    public abstract CompletableFuture<AIResponse> generateResponse(
            String playerMessage,
            VillagerData villagerData,
            Object player,
            GameContext gameContext
    );

    /**
     * Validates if this provider is properly configured and ready to use.
     *
     * @return true if the provider can be used, false otherwise
     */
    public abstract boolean isConfigured();

    /**
     * Gets the name of this AI provider.
     *
     * @return Provider name (e.g., "OpenAI", "Anthropic")
     */
    public abstract String getProviderName();

    /**
     * Gets the model being used by this provider.
     *
     * @return Model name
     */
    public String getModel() {
        return model;
    }

    /**
     * Gets the configured timeout for requests.
     *
     * @return Timeout in seconds
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Validates the API key format for this provider.
     *
     * @param apiKey The API key to validate
     * @return true if the format appears valid
     */
    protected abstract boolean isValidApiKey(String apiKey);

    /**
     * Validates the model name for this provider.
     *
     * @param model The model name to validate
     * @return true if the model is supported
     */
    protected abstract boolean isValidModel(String model);
}