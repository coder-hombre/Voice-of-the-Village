package com.foogly.voiceofthevillage.ai;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.data.VillagerData;
import com.foogly.voiceofthevillage.error.ErrorHandler;
import com.foogly.voiceofthevillage.error.FallbackResponseManager;
import com.foogly.voiceofthevillage.error.RetryManager;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

/**
 * Manages different AI service providers and handles request routing and rate limiting.
 * Provides a unified interface for generating villager responses regardless of the underlying AI service.
 */
public class AIServiceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIServiceManager.class);
    
    private static AIServiceManager instance;
    private final Map<String, AIProvider> providers;
    private final Semaphore concurrentRequestSemaphore;
    private AIProvider currentProvider;

    /**
     * Private constructor for singleton pattern.
     */
    private AIServiceManager() {
        this.providers = new HashMap<>();
        this.concurrentRequestSemaphore = new Semaphore(VoiceConfig.MAX_CONCURRENT_CONVERSATIONS.get());
        initializeProviders();
    }

    /**
     * Gets the singleton instance of the AI service manager.
     *
     * @return The AI service manager instance
     */
    public static synchronized AIServiceManager getInstance() {
        if (instance == null) {
            instance = new AIServiceManager();
        }
        return instance;
    }

    /**
     * Initializes available AI providers based on configuration.
     */
    private void initializeProviders() {
        String apiKey = VoiceConfig.AI_API_KEY.get();
        String model = VoiceConfig.AI_MODEL.get();
        int timeout = VoiceConfig.API_REQUEST_TIMEOUT.get();

        // Register OpenAI provider
        OpenAIProvider openAIProvider = new OpenAIProvider(apiKey, model, timeout);
        providers.put("OpenAI", openAIProvider);
        providers.put("openai", openAIProvider);

        // Set current provider based on configuration
        String configuredProvider = VoiceConfig.AI_PROVIDER.get();
        currentProvider = providers.get(configuredProvider);
        
        if (currentProvider == null) {
            LOGGER.warn("Configured AI provider '{}' not found, falling back to OpenAI", configuredProvider);
            currentProvider = openAIProvider;
        }

        LOGGER.info("AI Service Manager initialized with provider: {}", currentProvider.getProviderName());
    }

    /**
     * Generates a villager response using the configured AI provider with error handling and fallbacks.
     *
     * @param playerMessage The message from the player
     * @param villagerData  Data about the villager
     * @param player        The player entity
     * @param gameContext   Game context information
     * @return CompletableFuture containing the AI response
     */
    public CompletableFuture<AIResponse> generateResponse(String playerMessage, VillagerData villagerData,
                                                         Object player, GameContext gameContext) {
        ServerPlayer serverPlayer = player instanceof ServerPlayer ? (ServerPlayer) player : null;
        
        if (!isConfigured()) {
            String fallbackResponse = FallbackResponseManager.getFallbackResponse(villagerData, playerMessage);
            ErrorHandler.handleServiceUnavailable("AI", serverPlayer);
            return CompletableFuture.completedFuture(AIResponse.success(fallbackResponse));
        }

        // Check rate limiting
        if (!concurrentRequestSemaphore.tryAcquire()) {
            String fallbackResponse = FallbackResponseManager.getErrorResponse("network");
            if (serverPlayer != null) {
                ErrorHandler.notifyPlayer(serverPlayer, "Too many conversations active. Please try again in a moment.");
            }
            return CompletableFuture.completedFuture(AIResponse.success(fallbackResponse));
        }

        long startTime = System.currentTimeMillis();

        // Use retry manager with fallback
        return ErrorHandler.handleWithFallback(
            () -> RetryManager.withRetry(
                () -> currentProvider.generateResponse(playerMessage, villagerData, player, gameContext),
                "AI Response Generation"
            ),
            () -> {
                // Fallback response when AI fails
                String fallbackResponse = FallbackResponseManager.getFallbackResponse(villagerData, playerMessage);
                return AIResponse.success(fallbackResponse);
            },
            "AI Service",
            serverPlayer
        ).whenComplete((response, throwable) -> {
            concurrentRequestSemaphore.release();
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (throwable != null) {
                LOGGER.error("AI request failed after {}ms: {}", duration, throwable.getMessage());
            } else if (VoiceConfig.DEBUG_MODE.get()) {
                LOGGER.debug("AI request completed in {}ms, success: {}", duration, response.isSuccess());
            }
        });
    }

    /**
     * Checks if the AI service is properly configured and ready to use.
     *
     * @return true if AI can be used, false otherwise
     */
    public boolean isConfigured() {
        return currentProvider != null && currentProvider.isConfigured();
    }

    /**
     * Gets the name of the currently active AI provider.
     *
     * @return Provider name, or "None" if not configured
     */
    public String getCurrentProviderName() {
        return currentProvider != null ? currentProvider.getProviderName() : "None";
    }

    /**
     * Gets the model being used by the current provider.
     *
     * @return Model name, or "None" if not configured
     */
    public String getCurrentModel() {
        return currentProvider != null ? currentProvider.getModel() : "None";
    }

    /**
     * Registers a new AI provider.
     *
     * @param name     Name of the provider
     * @param provider The provider instance
     */
    public void registerProvider(String name, AIProvider provider) {
        providers.put(name, provider);
        LOGGER.info("Registered AI provider: {}", name);
    }

    /**
     * Switches to a different AI provider.
     *
     * @param providerName Name of the provider to switch to
     * @return true if the switch was successful, false otherwise
     */
    public boolean switchProvider(String providerName) {
        AIProvider provider = providers.get(providerName);
        if (provider != null && provider.isConfigured()) {
            currentProvider = provider;
            LOGGER.info("Switched to AI provider: {}", providerName);
            return true;
        }
        
        LOGGER.warn("Cannot switch to AI provider '{}': not found or not configured", providerName);
        return false;
    }

    /**
     * Reloads the AI service configuration.
     * Should be called when configuration values change.
     */
    public void reloadConfiguration() {
        LOGGER.info("Reloading AI service configuration");
        
        // Update semaphore permits
        int newMaxConcurrent = VoiceConfig.MAX_CONCURRENT_CONVERSATIONS.get();
        int currentPermits = concurrentRequestSemaphore.availablePermits();
        int difference = newMaxConcurrent - (currentPermits + (VoiceConfig.MAX_CONCURRENT_CONVERSATIONS.get() - concurrentRequestSemaphore.availablePermits()));
        
        if (difference > 0) {
            concurrentRequestSemaphore.release(difference);
        } else if (difference < 0) {
            concurrentRequestSemaphore.tryAcquire(-difference);
        }

        // Reinitialize providers
        initializeProviders();
    }

    /**
     * Gets statistics about the AI service usage.
     *
     * @return Statistics string
     */
    public String getStatistics() {
        int availableSlots = concurrentRequestSemaphore.availablePermits();
        int maxSlots = VoiceConfig.MAX_CONCURRENT_CONVERSATIONS.get();
        int activeRequests = maxSlots - availableSlots;

        return String.format("AI Service Statistics: Provider=%s, Model=%s, Active Requests=%d/%d",
                           getCurrentProviderName(), getCurrentModel(), activeRequests, maxSlots);
    }

    /**
     * Shuts down the AI service manager and releases resources.
     */
    public void shutdown() {
        LOGGER.info("Shutting down AI Service Manager");
        providers.clear();
        currentProvider = null;
    }
}