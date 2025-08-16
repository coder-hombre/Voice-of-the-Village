package com.foogly.voiceofthevillage.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration class for Voice of the Village mod.
 * Manages all mod settings including interaction modes, AI integration, and audio settings.
 */
public class VoiceConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Interaction Mode Settings
    public static final ModConfigSpec.BooleanValue ADVANCED_MODE = BUILDER
            .comment("Enable advanced mode for villager communication.",
                    "Simple mode: Right-click villagers to open communication GUI",
                    "Advanced mode: Use push-to-talk and commands for communication")
            .define("advancedMode", false);

    // Distance Settings
    public static final ModConfigSpec.DoubleValue INTERACTION_DISTANCE = BUILDER
            .comment("Maximum distance for villager communication in blocks.",
                    "Set to -1 to disable distance checking",
                    "Set to 0 for unlimited range",
                    "Default: 10.0 blocks")
            .defineInRange("interactionDistance", 10.0, -1.0, 100.0);

    public static final ModConfigSpec.DoubleValue NAME_TAG_DISTANCE = BUILDER
            .comment("Maximum distance to display villager names above their heads in blocks.",
                    "Default: 5.0 blocks")
            .defineInRange("nameTagDistance", 5.0, 0.0, 50.0);

    // AI Integration Settings
    public static final ModConfigSpec.ConfigValue<String> AI_API_KEY = BUILDER
            .comment("API key for AI service integration.",
                    "Required for villager response generation.",
                    "Keep this secure and do not share publicly!")
            .define("aiApiKey", "");

    public static final ModConfigSpec.ConfigValue<String> AI_PROVIDER = BUILDER
            .comment("AI service provider to use for response generation.",
                    "Supported providers: OpenAI, Anthropic",
                    "Default: OpenAI")
            .define("aiProvider", "OpenAI");

    public static final ModConfigSpec.ConfigValue<String> AI_MODEL = BUILDER
            .comment("AI model to use for response generation.",
                    "For OpenAI: gpt-3.5-turbo, gpt-4, gpt-4-turbo",
                    "For Anthropic: claude-3-haiku, claude-3-sonnet, claude-3-opus",
                    "Default: gpt-3.5-turbo")
            .define("aiModel", "gpt-3.5-turbo");

    // Memory and Reputation Settings
    public static final ModConfigSpec.IntValue MEMORY_RETENTION_DAYS = BUILDER
            .comment("Number of Minecraft days to retain villager memories.",
                    "Older memories will be automatically cleaned up.",
                    "Default: 30 days")
            .defineInRange("memoryRetentionDays", 30, 1, 365);

    // Audio Settings
    public static final ModConfigSpec.BooleanValue ENABLE_VOICE_INPUT = BUILDER
            .comment("Enable voice input for communicating with villagers.",
                    "Requires microphone access and speech-to-text service.",
                    "Default: true")
            .define("enableVoiceInput", true);

    public static final ModConfigSpec.BooleanValue ENABLE_VOICE_OUTPUT = BUILDER
            .comment("Enable voice output for villager responses.",
                    "Villagers will speak their responses aloud.",
                    "Default: true")
            .define("enableVoiceOutput", true);

    public static final ModConfigSpec.ConfigValue<String> PUSH_TO_TALK_KEY = BUILDER
            .comment("Key binding for push-to-talk in advanced mode.",
                    "Use Minecraft key names (e.g., 'key.keyboard.v')",
                    "Default: V key")
            .define("pushToTalkKey", "key.keyboard.v");

    // Performance and Rate Limiting
    public static final ModConfigSpec.IntValue MAX_CONCURRENT_CONVERSATIONS = BUILDER
            .comment("Maximum number of simultaneous villager conversations.",
                    "Helps prevent API rate limiting and performance issues.",
                    "Default: 3")
            .defineInRange("maxConcurrentConversations", 3, 1, 10);

    public static final ModConfigSpec.IntValue API_REQUEST_TIMEOUT = BUILDER
            .comment("Timeout for AI API requests in seconds.",
                    "Default: 30 seconds")
            .defineInRange("apiRequestTimeout", 30, 5, 120);

    // Debug and Logging
    public static final ModConfigSpec.BooleanValue DEBUG_MODE = BUILDER
            .comment("Enable debug logging for troubleshooting.",
                    "Logs detailed information about villager interactions.",
                    "Default: false")
            .define("debugMode", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * Validates if the interaction distance setting is properly configured.
     * @return true if distance checking is enabled, false if disabled
     */
    public static boolean isDistanceCheckEnabled() {
        return INTERACTION_DISTANCE.get() >= 0.0;
    }

    /**
     * Gets the effective interaction distance, handling special values.
     * @return the interaction distance, or Double.MAX_VALUE for unlimited range
     */
    public static double getEffectiveInteractionDistance() {
        double distance = INTERACTION_DISTANCE.get();
        if (distance == 0.0) {
            return Double.MAX_VALUE; // Unlimited range
        }
        return Math.abs(distance);
    }

    /**
     * Validates if the AI configuration is complete and valid.
     * @return true if AI can be used, false otherwise
     */
    public static boolean isAIConfigured() {
        String apiKey = AI_API_KEY.get();
        String provider = AI_PROVIDER.get();
        String model = AI_MODEL.get();
        
        return apiKey != null && !apiKey.trim().isEmpty() &&
               provider != null && !provider.trim().isEmpty() &&
               model != null && !model.trim().isEmpty();
    }

    /**
     * Gets the configured push-to-talk key, with fallback to default.
     * @return the key binding string
     */
    public static String getPushToTalkKey() {
        String key = PUSH_TO_TALK_KEY.get();
        return key != null && !key.trim().isEmpty() ? key : "key.keyboard.v";
    }
}