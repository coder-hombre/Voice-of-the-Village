package com.foogly.voiceofthevillage.integration;

import com.foogly.voiceofthevillage.ai.AIServiceManager;
import com.foogly.voiceofthevillage.ai.GameContext;
import com.foogly.voiceofthevillage.audio.AudioCaptureManager;
import com.foogly.voiceofthevillage.audio.SpeechToTextProcessor;
import com.foogly.voiceofthevillage.audio.TextToSpeechProcessor;
import com.foogly.voiceofthevillage.audio.VillagerVoiceManager;
import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.data.VillagerData;
import com.foogly.voiceofthevillage.data.VillagerDataManager;
import com.foogly.voiceofthevillage.error.ErrorHandler;
import com.foogly.voiceofthevillage.error.ServiceNotificationManager;
import com.foogly.voiceofthevillage.memory.ConversationMemoryIntegrator;
import com.foogly.voiceofthevillage.reputation.ReputationManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Central integration system that coordinates all villager communication components.
 * Provides a unified interface for villager interactions across different modes.
 */
public class VillagerCommunicationSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerCommunicationSystem.class);
    
    private static VillagerCommunicationSystem instance;
    
    // Core components
    private final AIServiceManager aiServiceManager;
    private final VillagerDataManager villagerDataManager;
    private final AudioCaptureManager audioCaptureManager;
    private final SpeechToTextProcessor speechToTextProcessor;
    private final TextToSpeechProcessor textToSpeechProcessor;
    private final VillagerVoiceManager villagerVoiceManager;
    private final ConversationMemoryIntegrator memoryIntegrator;
    private final ReputationManager reputationManager;
    
    // Active conversations tracking
    private final ConcurrentMap<String, ActiveConversation> activeConversations;
    
    // Performance metrics
    private final PerformanceMetrics performanceMetrics;
    
    private VillagerCommunicationSystem() {
        LOGGER.info("Initializing Villager Communication System");
        
        // Initialize core components
        this.aiServiceManager = AIServiceManager.getInstance();
        this.villagerDataManager = VillagerDataManager.getInstance();
        this.audioCaptureManager = new AudioCaptureManager();
        this.speechToTextProcessor = new SpeechToTextProcessor();
        this.textToSpeechProcessor = new TextToSpeechProcessor();
        this.villagerVoiceManager = VillagerVoiceManager.getInstance();
        this.memoryIntegrator = new ConversationMemoryIntegrator(
            new com.foogly.voiceofthevillage.data.MemoryManager(new com.foogly.voiceofthevillage.data.DataPersistence()),
            this.aiServiceManager,
            new com.foogly.voiceofthevillage.ai.PromptBuilder()
        );
        this.reputationManager = new ReputationManager();
        
        this.activeConversations = new ConcurrentHashMap<>();
        this.performanceMetrics = new PerformanceMetrics();
        
        // Start notification cleanup scheduler
        ServiceNotificationManager.startCleanupScheduler();
        
        LOGGER.info("Villager Communication System initialized successfully");
    }
    
    /**
     * Gets the singleton instance
     */
    public static synchronized VillagerCommunicationSystem getInstance() {
        if (instance == null) {
            instance = new VillagerCommunicationSystem();
        }
        return instance;
    }
    
    /**
     * Processes a text message from player to villager
     */
    public CompletableFuture<CommunicationResponse> processTextMessage(
            ServerPlayer player, Villager villager, String message) {
        
        long startTime = System.currentTimeMillis();
        String conversationId = generateConversationId(player, villager);
        
        LOGGER.debug("Processing text message from {} to villager {}: {}", 
            player.getName().getString(), villager.getUUID(), message);
        
        return processMessage(player, villager, message, CommunicationType.TEXT)
            .whenComplete((response, throwable) -> {
                long duration = System.currentTimeMillis() - startTime;
                performanceMetrics.recordTextInteraction(duration, throwable == null);
                
                if (throwable != null) {
                    LOGGER.error("Text message processing failed for conversation {}: {}", 
                        conversationId, throwable.getMessage());
                }
            });
    }
    
    /**
     * Processes a voice message from player to villager
     */
    public CompletableFuture<CommunicationResponse> processVoiceMessage(
            ServerPlayer player, Villager villager, byte[] audioData) {
        
        long startTime = System.currentTimeMillis();
        String conversationId = generateConversationId(player, villager);
        
        LOGGER.debug("Processing voice message from {} to villager {}", 
            player.getName().getString(), villager.getUUID());
        
        // Convert speech to text first
        // Create a basic audio format for processing
        com.foogly.voiceofthevillage.audio.AudioFormat audioFormat = 
            com.foogly.voiceofthevillage.audio.AudioFormat.DEFAULT_RECORDING_FORMAT;
        return speechToTextProcessor.processAudio(audioData, audioFormat)
            .thenCompose(result -> {
                if (result == null || !result.isSuccess() || result.getText().trim().isEmpty()) {
                    ServiceNotificationManager.notifyAudioError(player, "processing");
                    return CompletableFuture.completedFuture(
                        CommunicationResponse.error("Could not understand audio input")
                    );
                }
                
                String transcription = result.getText();
                LOGGER.debug("Voice transcription for {}: {}", conversationId, transcription);
                return processMessage(player, villager, transcription, CommunicationType.VOICE);
            })
            .whenComplete((response, throwable) -> {
                long duration = System.currentTimeMillis() - startTime;
                performanceMetrics.recordVoiceInteraction(duration, throwable == null);
                
                if (throwable != null) {
                    LOGGER.error("Voice message processing failed for conversation {}: {}", 
                        conversationId, throwable.getMessage());
                }
            });
    }
    
    /**
     * Core message processing logic
     */
    private CompletableFuture<CommunicationResponse> processMessage(
            ServerPlayer player, Villager villager, String message, CommunicationType type) {
        
        String conversationId = generateConversationId(player, villager);
        
        try {
            // Get or create villager data
            VillagerData villagerData = villagerDataManager.getOrCreateVillagerData(villager);
            
            // Check interaction distance
            if (!isWithinInteractionDistance(player, villager)) {
                return CompletableFuture.completedFuture(
                    CommunicationResponse.error("Too far away to communicate")
                );
            }
            
            // Track active conversation
            ActiveConversation conversation = activeConversations.computeIfAbsent(
                conversationId, 
                k -> new ActiveConversation(player, villager, System.currentTimeMillis())
            );
            conversation.updateLastActivity();
            
            // Build game context
            GameContext gameContext = buildGameContext(player, villager, villagerData);
            
            // Generate AI response
            return aiServiceManager.generateResponse(message, villagerData, player, gameContext)
                .thenCompose(aiResponse -> {
                    if (!aiResponse.isSuccess()) {
                        return CompletableFuture.completedFuture(
                            CommunicationResponse.error(aiResponse.getErrorMessage())
                        );
                    }
                    
                    String responseText = aiResponse.getResponseText();
                    
                    // Store interaction in memory (simplified for now)
                    // memoryIntegrator.processInteraction(villagerData, player, message, responseText);
                    
                    // Update reputation (simplified for now)
                    // reputationManager.processInteraction(villagerData, player, message, responseText);
                    
                    // Generate audio response if voice input was used
                    if (type == CommunicationType.VOICE && VoiceConfig.ENABLE_VOICE_OUTPUT.get()) {
                        return generateVoiceResponse(villagerData, responseText)
                            .thenApply(audioData -> 
                                CommunicationResponse.success(responseText, audioData, type)
                            );
                    } else {
                        return CompletableFuture.completedFuture(
                            CommunicationResponse.success(responseText, null, type)
                        );
                    }
                })
                .exceptionally(throwable -> {
                    ErrorHandler.logAndNotify("Message Processing", throwable, player, 
                        "An error occurred while processing your message");
                    return CommunicationResponse.error("Processing failed: " + throwable.getMessage());
                });
                
        } catch (Exception e) {
            LOGGER.error("Unexpected error in message processing for {}: {}", conversationId, e.getMessage(), e);
            return CompletableFuture.completedFuture(
                CommunicationResponse.error("Unexpected error occurred")
            );
        }
    }
    
    /**
     * Generates voice response for villager
     */
    private CompletableFuture<byte[]> generateVoiceResponse(VillagerData villagerData, String text) {
        // Simplified voice generation - return null for now as method signature needs to be checked
        return CompletableFuture.completedFuture((byte[]) null)
            .exceptionally(throwable -> {
                LOGGER.warn("Voice generation failed, falling back to text only: {}", throwable.getMessage());
                return null; // Return null to indicate voice generation failed
            });
    }
    
    /**
     * Builds game context for AI processing
     */
    private GameContext buildGameContext(ServerPlayer player, Villager villager, VillagerData villagerData) {
        return GameContext.basic(player.level(), villager.blockPosition());
    }
    
    /**
     * Checks if player is within interaction distance of villager
     */
    private boolean isWithinInteractionDistance(ServerPlayer player, Villager villager) {
        double configuredDistance = VoiceConfig.INTERACTION_DISTANCE.get();
        
        // Negative distance means disabled
        if (configuredDistance < 0) {
            return false;
        }
        
        // Zero distance means unlimited
        if (configuredDistance == 0) {
            return true;
        }
        
        double actualDistance = player.distanceTo(villager);
        return actualDistance <= configuredDistance;
    }
    
    /**
     * Generates unique conversation ID
     */
    private String generateConversationId(ServerPlayer player, Villager villager) {
        return player.getUUID() + ":" + villager.getUUID();
    }
    
    /**
     * Cleans up inactive conversations
     */
    public void cleanupInactiveConversations() {
        long currentTime = System.currentTimeMillis();
        long timeoutMs = 300000; // 5 minutes
        
        activeConversations.entrySet().removeIf(entry -> {
            ActiveConversation conversation = entry.getValue();
            boolean isExpired = (currentTime - conversation.getLastActivity()) > timeoutMs;
            
            if (isExpired) {
                LOGGER.debug("Cleaning up inactive conversation: {}", entry.getKey());
            }
            
            return isExpired;
        });
    }
    
    /**
     * Gets system status and statistics
     */
    public SystemStatus getSystemStatus() {
        return SystemStatus.builder()
            .aiServiceConfigured(aiServiceManager.isConfigured())
            .aiProviderName(aiServiceManager.getCurrentProviderName())
            .activeConversations(activeConversations.size())
            .performanceMetrics(performanceMetrics.getSnapshot())
            .build();
    }
    
    /**
     * Shuts down the communication system
     */
    public void shutdown() {
        LOGGER.info("Shutting down Villager Communication System");
        
        try {
            // Cleanup active conversations
            activeConversations.clear();
            
            // Shutdown components
            aiServiceManager.shutdown();
            ServiceNotificationManager.shutdown();
            
            // Reset instance
            instance = null;
            
            LOGGER.info("Villager Communication System shutdown complete");
        } catch (Exception e) {
            LOGGER.error("Error during system shutdown: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Represents an active conversation between player and villager
     */
    private static class ActiveConversation {
        private final ServerPlayer player;
        private final Villager villager;
        private final long startTime;
        private volatile long lastActivity;
        
        public ActiveConversation(ServerPlayer player, Villager villager, long startTime) {
            this.player = player;
            this.villager = villager;
            this.startTime = startTime;
            this.lastActivity = startTime;
        }
        
        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        public long getLastActivity() {
            return lastActivity;
        }
    }
    
    /**
     * Communication type enumeration
     */
    public enum CommunicationType {
        TEXT, VOICE
    }
}