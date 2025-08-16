package com.foogly.voiceofthevillage.logging;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Centralized logging system for the Voice of the Village mod.
 * Provides structured logging with context and performance tracking.
 */
public class VoiceLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceLogger.class);
    
    // Log level constants
    public static final String TRACE = "TRACE";
    public static final String DEBUG = "DEBUG";
    public static final String INFO = "INFO";
    public static final String WARN = "WARN";
    public static final String ERROR = "ERROR";
    
    // Context keys
    private static final String PLAYER_KEY = "player";
    private static final String VILLAGER_KEY = "villager";
    private static final String CONVERSATION_KEY = "conversation";
    private static final String COMPONENT_KEY = "component";
    private static final String OPERATION_KEY = "operation";
    private static final String DURATION_KEY = "duration";
    
    /**
     * Logs a conversation event with context
     */
    public static void logConversation(String level, String playerName, UUID villagerId, 
                                     String operation, String message, Object... args) {
        if (!shouldLog(level)) return;
        
        try {
            MDC.put(PLAYER_KEY, playerName);
            MDC.put(VILLAGER_KEY, villagerId.toString());
            MDC.put(OPERATION_KEY, operation);
            
            logWithLevel(level, "[CONVERSATION] " + message, args);
        } finally {
            clearContext();
        }
    }
    
    /**
     * Logs a component operation with timing
     */
    public static void logComponent(String level, String componentName, String operation, 
                                  long durationMs, String message, Object... args) {
        if (!shouldLog(level)) return;
        
        try {
            MDC.put(COMPONENT_KEY, componentName);
            MDC.put(OPERATION_KEY, operation);
            MDC.put(DURATION_KEY, durationMs + "ms");
            
            logWithLevel(level, "[" + componentName.toUpperCase() + "] " + message, args);
        } finally {
            clearContext();
        }
    }
    
    /**
     * Logs an AI service interaction
     */
    public static void logAI(String level, String provider, String model, String operation, 
                           long durationMs, boolean success, String message, Object... args) {
        if (!shouldLog(level)) return;
        
        try {
            MDC.put(COMPONENT_KEY, "AI");
            MDC.put("provider", provider);
            MDC.put("model", model);
            MDC.put(OPERATION_KEY, operation);
            MDC.put(DURATION_KEY, durationMs + "ms");
            MDC.put("success", String.valueOf(success));
            
            logWithLevel(level, "[AI] " + message, args);
        } finally {
            clearContext();
        }
    }
    
    /**
     * Logs an audio processing event
     */
    public static void logAudio(String level, String audioType, String operation, 
                              long durationMs, int audioSizeBytes, String message, Object... args) {
        if (!shouldLog(level)) return;
        
        try {
            MDC.put(COMPONENT_KEY, "AUDIO");
            MDC.put("audioType", audioType);
            MDC.put(OPERATION_KEY, operation);
            MDC.put(DURATION_KEY, durationMs + "ms");
            MDC.put("audioSize", audioSizeBytes + "bytes");
            
            logWithLevel(level, "[AUDIO] " + message, args);
        } finally {
            clearContext();
        }
    }
    
    /**
     * Logs a network operation
     */
    public static void logNetwork(String level, String packetType, String direction, 
                                String playerName, String message, Object... args) {
        if (!shouldLog(level)) return;
        
        try {
            MDC.put(COMPONENT_KEY, "NETWORK");
            MDC.put("packetType", packetType);
            MDC.put("direction", direction);
            MDC.put(PLAYER_KEY, playerName);
            
            logWithLevel(level, "[NETWORK] " + message, args);
        } finally {
            clearContext();
        }
    }
    
    /**
     * Logs a performance metric
     */
    public static void logPerformance(String operation, long durationMs, boolean success, 
                                    String additionalInfo) {
        if (!VoiceConfig.DEBUG_MODE.get()) return;
        
        try {
            MDC.put(COMPONENT_KEY, "PERFORMANCE");
            MDC.put(OPERATION_KEY, operation);
            MDC.put(DURATION_KEY, durationMs + "ms");
            MDC.put("success", String.valueOf(success));
            
            String message = "Performance: {} completed in {}ms (success: {})";
            if (additionalInfo != null && !additionalInfo.isEmpty()) {
                message += " - {}";
                LOGGER.info(message, operation, durationMs, success, additionalInfo);
            } else {
                LOGGER.info(message, operation, durationMs, success);
            }
        } finally {
            clearContext();
        }
    }
    
    /**
     * Logs an error with full context
     */
    public static void logError(String component, String operation, Throwable error, 
                              String playerName, UUID villagerId, String message, Object... args) {
        try {
            MDC.put(COMPONENT_KEY, component);
            MDC.put(OPERATION_KEY, operation);
            if (playerName != null) MDC.put(PLAYER_KEY, playerName);
            if (villagerId != null) MDC.put(VILLAGER_KEY, villagerId.toString());
            
            LOGGER.error("[ERROR] " + message, args, error);
        } finally {
            clearContext();
        }
    }
    
    /**
     * Logs system startup/shutdown events
     */
    public static void logSystem(String level, String event, String message, Object... args) {
        try {
            MDC.put(COMPONENT_KEY, "SYSTEM");
            MDC.put("event", event);
            
            logWithLevel(level, "[SYSTEM] " + message, args);
        } finally {
            clearContext();
        }
    }
    
    /**
     * Logs configuration changes
     */
    public static void logConfig(String configKey, Object oldValue, Object newValue, String message) {
        if (!VoiceConfig.DEBUG_MODE.get()) return;
        
        try {
            MDC.put(COMPONENT_KEY, "CONFIG");
            MDC.put("configKey", configKey);
            MDC.put("oldValue", String.valueOf(oldValue));
            MDC.put("newValue", String.valueOf(newValue));
            
            LOGGER.info("[CONFIG] " + message);
        } finally {
            clearContext();
        }
    }
    
    /**
     * Creates a conversation context for multiple related log entries
     */
    public static ConversationContext createConversationContext(String playerName, UUID villagerId) {
        String conversationId = playerName + ":" + villagerId;
        return new ConversationContext(conversationId, playerName, villagerId);
    }
    
    /**
     * Determines if logging should occur based on configuration and level
     */
    private static boolean shouldLog(String level) {
        if (!VoiceConfig.DEBUG_MODE.get() && (TRACE.equals(level) || DEBUG.equals(level))) {
            return false;
        }
        return true;
    }
    
    /**
     * Logs with the appropriate level
     */
    private static void logWithLevel(String level, String message, Object... args) {
        switch (level) {
            case TRACE:
                LOGGER.trace(message, args);
                break;
            case DEBUG:
                LOGGER.debug(message, args);
                break;
            case INFO:
                LOGGER.info(message, args);
                break;
            case WARN:
                LOGGER.warn(message, args);
                break;
            case ERROR:
                LOGGER.error(message, args);
                break;
            default:
                LOGGER.info(message, args);
        }
    }
    
    /**
     * Clears MDC context
     */
    private static void clearContext() {
        MDC.clear();
    }
    
    /**
     * Context holder for conversation-related logging
     */
    public static class ConversationContext implements AutoCloseable {
        private final String conversationId;
        
        public ConversationContext(String conversationId, String playerName, UUID villagerId) {
            this.conversationId = conversationId;
            MDC.put(CONVERSATION_KEY, conversationId);
            MDC.put(PLAYER_KEY, playerName);
            MDC.put(VILLAGER_KEY, villagerId.toString());
        }
        
        public void log(String level, String component, String operation, String message, Object... args) {
            MDC.put(COMPONENT_KEY, component);
            MDC.put(OPERATION_KEY, operation);
            logWithLevel(level, message, args);
        }
        
        public void logTiming(String component, String operation, long durationMs, String message, Object... args) {
            MDC.put(COMPONENT_KEY, component);
            MDC.put(OPERATION_KEY, operation);
            MDC.put(DURATION_KEY, durationMs + "ms");
            logWithLevel(INFO, message + " ({}ms)", combineArgs(args, durationMs));
        }
        
        private Object[] combineArgs(Object[] original, Object additional) {
            Object[] combined = new Object[original.length + 1];
            System.arraycopy(original, 0, combined, 0, original.length);
            combined[original.length] = additional;
            return combined;
        }
        
        @Override
        public void close() {
            clearContext();
        }
    }
}