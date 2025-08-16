package com.foogly.voiceofthevillage.util;

/**
 * Constants used throughout the Voice of the Village mod.
 */
public final class Constants {
    
    // Mod Information
    public static final String MOD_ID = "voiceofthevillage";
    public static final String MOD_NAME = "Voice of the Village";
    
    // Configuration Defaults
    public static final double DEFAULT_INTERACTION_DISTANCE = 10.0;
    public static final double DEFAULT_NAME_TAG_DISTANCE = 5.0;
    public static final int DEFAULT_MEMORY_RETENTION_DAYS = 30;
    public static final int DEFAULT_MAX_CONCURRENT_CONVERSATIONS = 3;
    public static final int DEFAULT_API_REQUEST_TIMEOUT = 30;
    public static final String DEFAULT_PUSH_TO_TALK_KEY = "key.keyboard.v";
    
    // AI Provider Names
    public static final String AI_PROVIDER_OPENAI = "OpenAI";
    public static final String AI_PROVIDER_ANTHROPIC = "Anthropic";
    
    // Default AI Models
    public static final String DEFAULT_OPENAI_MODEL = "gpt-3.5-turbo";
    public static final String DEFAULT_ANTHROPIC_MODEL = "claude-3-haiku";
    
    // Network Constants
    public static final int MAX_MESSAGE_LENGTH = 1000;
    public static final int MAX_AUDIO_DURATION_SECONDS = 30;
    
    // File Extensions
    public static final String VILLAGER_DATA_EXTENSION = ".json";
    public static final String BACKUP_EXTENSION = ".backup";
    
    // Directory Names
    public static final String VILLAGER_DATA_DIR = "villager_data";
    public static final String BACKUP_DIR = "backups";
    
    private Constants() {
        // Utility class - prevent instantiation
    }
}