package com.foogly.voiceofthevillage.error;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages user notifications for service availability and errors
 */
public class ServiceNotificationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceNotificationManager.class);
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
    
    // Track notification cooldowns to prevent spam
    private static final Map<String, Long> NOTIFICATION_COOLDOWNS = new ConcurrentHashMap<>();
    private static final long COOLDOWN_DURATION_MS = 30000; // 30 seconds
    
    /**
     * Notifies player about service unavailability with cooldown
     */
    public static void notifyServiceUnavailable(ServerPlayer player, String serviceName) {
        String key = player.getUUID() + ":" + serviceName;
        long currentTime = System.currentTimeMillis();
        
        Long lastNotification = NOTIFICATION_COOLDOWNS.get(key);
        if (lastNotification != null && (currentTime - lastNotification) < COOLDOWN_DURATION_MS) {
            return; // Skip notification due to cooldown
        }
        
        NOTIFICATION_COOLDOWNS.put(key, currentTime);
        
        String message = String.format("§c[Voice of the Village] %s service is temporarily unavailable. Using fallback response.", serviceName);
        player.sendSystemMessage(Component.literal(message));
        
        LOGGER.info("Notified player {} about {} service unavailability", player.getName().getString(), serviceName);
    }
    
    /**
     * Notifies player about service recovery
     */
    public static void notifyServiceRecovered(ServerPlayer player, String serviceName) {
        String message = String.format("§a[Voice of the Village] %s service has recovered and is now available.", serviceName);
        player.sendSystemMessage(Component.literal(message));
        
        LOGGER.info("Notified player {} about {} service recovery", player.getName().getString(), serviceName);
    }
    
    /**
     * Notifies player about audio processing errors
     */
    public static void notifyAudioError(ServerPlayer player, String errorType) {
        String message;
        switch (errorType.toLowerCase()) {
            case "microphone":
                message = "§e[Voice of the Village] Microphone access failed. Please check your audio settings.";
                break;
            case "recording":
                message = "§e[Voice of the Village] Audio recording failed. Please try again.";
                break;
            case "processing":
                message = "§e[Voice of the Village] Audio processing failed. Falling back to text input.";
                break;
            default:
                message = "§e[Voice of the Village] Audio error occurred. Please try again.";
        }
        
        player.sendSystemMessage(Component.literal(message));
        LOGGER.warn("Audio error for player {}: {}", player.getName().getString(), errorType);
    }
    
    /**
     * Notifies player about network errors
     */
    public static void notifyNetworkError(ServerPlayer player, String operation) {
        String message = String.format("§c[Voice of the Village] Network error during %s. Please check your connection and try again.", operation);
        player.sendSystemMessage(Component.literal(message));
        
        LOGGER.warn("Network error for player {} during {}", player.getName().getString(), operation);
    }
    
    /**
     * Notifies player about configuration issues
     */
    public static void notifyConfigurationError(ServerPlayer player, String issue) {
        String message = String.format("§c[Voice of the Village] Configuration issue: %s. Please contact server administrator.", issue);
        player.sendSystemMessage(Component.literal(message));
        
        LOGGER.error("Configuration error for player {}: {}", player.getName().getString(), issue);
    }
    
    /**
     * Sends a general error notification
     */
    public static void notifyGeneralError(ServerPlayer player, String context) {
        String message = String.format("§e[Voice of the Village] An error occurred during %s. Please try again later.", context);
        player.sendSystemMessage(Component.literal(message));
        
        LOGGER.warn("General error for player {} in context: {}", player.getName().getString(), context);
    }
    
    /**
     * Cleans up old notification cooldowns
     */
    public static void cleanupCooldowns() {
        long currentTime = System.currentTimeMillis();
        NOTIFICATION_COOLDOWNS.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > COOLDOWN_DURATION_MS * 2
        );
    }
    
    /**
     * Starts the cleanup scheduler
     */
    public static void startCleanupScheduler() {
        SCHEDULER.scheduleAtFixedRate(
            ServiceNotificationManager::cleanupCooldowns,
            1, 1, TimeUnit.MINUTES
        );
    }
    
    /**
     * Shuts down the notification manager
     */
    public static void shutdown() {
        SCHEDULER.shutdown();
        try {
            if (!SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
        NOTIFICATION_COOLDOWNS.clear();
    }
}