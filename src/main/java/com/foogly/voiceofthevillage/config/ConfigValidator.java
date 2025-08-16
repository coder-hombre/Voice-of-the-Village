package com.foogly.voiceofthevillage.config;

import com.foogly.voiceofthevillage.VoiceOfTheVillage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * Validates configuration values and provides runtime configuration updates.
 */
@EventBusSubscriber(modid = VoiceOfTheVillage.MODID)
public class ConfigValidator {

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        VoiceOfTheVillage.LOGGER.info("Loading Voice of the Village configuration...");
        validateConfiguration();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        VoiceOfTheVillage.LOGGER.info("Reloading Voice of the Village configuration...");
        validateConfiguration();
    }

    /**
     * Validates all configuration values and logs warnings for invalid settings.
     */
    private static void validateConfiguration() {
        validateDistanceSettings();
        validateAISettings();
        validateAudioSettings();
        validatePerformanceSettings();
        
        VoiceOfTheVillage.LOGGER.info("Configuration validation completed.");
    }

    /**
     * Validates distance-related configuration settings.
     */
    private static void validateDistanceSettings() {
        double interactionDistance = VoiceConfig.INTERACTION_DISTANCE.get();
        double nameTagDistance = VoiceConfig.NAME_TAG_DISTANCE.get();

        if (interactionDistance < -1.0) {
            VoiceOfTheVillage.LOGGER.warn("Invalid interaction distance: {}. Using default value.", interactionDistance);
        }

        if (nameTagDistance < 0.0) {
            VoiceOfTheVillage.LOGGER.warn("Invalid name tag distance: {}. Using default value.", nameTagDistance);
        }

        if (interactionDistance > 0 && nameTagDistance > interactionDistance) {
            VoiceOfTheVillage.LOGGER.warn("Name tag distance ({}) is greater than interaction distance ({}). " +
                    "This may cause confusion as names will be visible beyond interaction range.", 
                    nameTagDistance, interactionDistance);
        }
    }

    /**
     * Validates AI-related configuration settings.
     */
    private static void validateAISettings() {
        String apiKey = VoiceConfig.AI_API_KEY.get();
        String provider = VoiceConfig.AI_PROVIDER.get();
        String model = VoiceConfig.AI_MODEL.get();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            VoiceOfTheVillage.LOGGER.warn("AI API key is not configured. Villager responses will not work without a valid API key.");
        } else if (apiKey.length() < 10) {
            VoiceOfTheVillage.LOGGER.warn("AI API key appears to be too short. Please verify your API key is correct.");
        }

        if (!isValidProvider(provider)) {
            VoiceOfTheVillage.LOGGER.warn("Unknown AI provider: {}. Supported providers: OpenAI, Anthropic", provider);
        }

        if (!isValidModel(provider, model)) {
            VoiceOfTheVillage.LOGGER.warn("Unknown AI model '{}' for provider '{}'. Please check your configuration.", model, provider);
        }
    }

    /**
     * Validates audio-related configuration settings.
     */
    private static void validateAudioSettings() {
        String pushToTalkKey = VoiceConfig.PUSH_TO_TALK_KEY.get();
        
        if (pushToTalkKey == null || pushToTalkKey.trim().isEmpty()) {
            VoiceOfTheVillage.LOGGER.warn("Push-to-talk key is not configured. Using default 'V' key.");
        }

        boolean voiceInput = VoiceConfig.ENABLE_VOICE_INPUT.get();
        boolean voiceOutput = VoiceConfig.ENABLE_VOICE_OUTPUT.get();
        boolean advancedMode = VoiceConfig.ADVANCED_MODE.get();

        if (advancedMode && !voiceInput) {
            VoiceOfTheVillage.LOGGER.warn("Advanced mode is enabled but voice input is disabled. " +
                    "Consider enabling voice input for the full advanced mode experience.");
        }

        if (!voiceInput && !voiceOutput) {
            VoiceOfTheVillage.LOGGER.info("Both voice input and output are disabled. Only text communication will be available.");
        }
    }

    /**
     * Validates performance-related configuration settings.
     */
    private static void validatePerformanceSettings() {
        int maxConversations = VoiceConfig.MAX_CONCURRENT_CONVERSATIONS.get();
        int apiTimeout = VoiceConfig.API_REQUEST_TIMEOUT.get();
        int memoryDays = VoiceConfig.MEMORY_RETENTION_DAYS.get();

        if (maxConversations > 5) {
            VoiceOfTheVillage.LOGGER.warn("High concurrent conversation limit ({}). " +
                    "This may cause API rate limiting or performance issues.", maxConversations);
        }

        if (apiTimeout < 10) {
            VoiceOfTheVillage.LOGGER.warn("API timeout is very low ({}s). This may cause frequent timeouts.", apiTimeout);
        }

        if (memoryDays > 100) {
            VoiceOfTheVillage.LOGGER.warn("Memory retention is set to {} days. " +
                    "This may consume significant storage space over time.", memoryDays);
        }
    }

    /**
     * Checks if the specified AI provider is supported.
     */
    private static boolean isValidProvider(String provider) {
        if (provider == null) return false;
        return provider.equalsIgnoreCase("OpenAI") || 
               provider.equalsIgnoreCase("Anthropic");
    }

    /**
     * Checks if the specified model is valid for the given provider.
     */
    private static boolean isValidModel(String provider, String model) {
        if (provider == null || model == null) return false;
        
        switch (provider.toLowerCase()) {
            case "openai":
                return model.startsWith("gpt-3.5") || 
                       model.startsWith("gpt-4") ||
                       model.equals("gpt-3.5-turbo") ||
                       model.equals("gpt-4-turbo");
            case "anthropic":
                return model.startsWith("claude-3") ||
                       model.equals("claude-3-haiku") ||
                       model.equals("claude-3-sonnet") ||
                       model.equals("claude-3-opus");
            default:
                return false;
        }
    }
}