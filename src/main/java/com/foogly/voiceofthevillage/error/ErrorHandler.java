package com.foogly.voiceofthevillage.error;

import com.foogly.voiceofthevillage.VoiceOfTheVillage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Central error handling system for the Voice of the Village mod.
 * Provides consistent error handling, logging, and user notifications.
 */
public class ErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);
    
    /**
     * Handles errors with fallback responses and user notifications
     */
    public static <T> CompletableFuture<T> handleWithFallback(
            Supplier<CompletableFuture<T>> primaryAction,
            Supplier<T> fallbackAction,
            String errorContext,
            ServerPlayer player) {
        
        return primaryAction.get()
            .exceptionally(throwable -> {
                LOGGER.warn("Error in {}: {}", errorContext, throwable.getMessage(), throwable);
                
                // Notify player of the issue
                if (player != null) {
                    notifyPlayer(player, "Service temporarily unavailable, using fallback response");
                }
                
                // Execute fallback
                try {
                    return fallbackAction.get();
                } catch (Exception fallbackError) {
                    LOGGER.error("Fallback also failed for {}: {}", errorContext, fallbackError.getMessage(), fallbackError);
                    throw new RuntimeException("Both primary and fallback actions failed", fallbackError);
                }
            });
    }
    
    /**
     * Logs error and notifies player
     */
    public static void logAndNotify(String context, Throwable error, ServerPlayer player, String userMessage) {
        LOGGER.error("Error in {}: {}", context, error.getMessage(), error);
        
        if (player != null && userMessage != null) {
            notifyPlayer(player, userMessage);
        }
    }
    
    /**
     * Sends notification to player
     */
    public static void notifyPlayer(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal("[Voice of the Village] " + message));
        }
    }
    
    /**
     * Handles service unavailability
     */
    public static void handleServiceUnavailable(String serviceName, ServerPlayer player) {
        String message = String.format("%s service is currently unavailable", serviceName);
        LOGGER.warn(message);
        
        if (player != null) {
            notifyPlayer(player, message + ". Please try again later.");
        }
    }
}