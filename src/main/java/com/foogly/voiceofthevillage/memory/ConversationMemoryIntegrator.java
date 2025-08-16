package com.foogly.voiceofthevillage.memory;

import com.foogly.voiceofthevillage.ai.AIResponse;
import com.foogly.voiceofthevillage.ai.AIServiceManager;
import com.foogly.voiceofthevillage.ai.GameContext;
import com.foogly.voiceofthevillage.ai.PromptBuilder;
import com.foogly.voiceofthevillage.data.InteractionMemory;
import com.foogly.voiceofthevillage.data.InteractionType;
import com.foogly.voiceofthevillage.data.MemoryManager;
import com.foogly.voiceofthevillage.data.VillagerData;
import com.foogly.voiceofthevillage.data.VillagerDataManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Integrates memory system with AI prompt building and conversation flow.
 * Handles memory retrieval for contextual responses and storage of new interactions.
 */
public class ConversationMemoryIntegrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationMemoryIntegrator.class);
    
    // VillagerDataManager uses static methods, so no instance needed
    private final MemoryManager memoryManager;
    private final AIServiceManager aiServiceManager;
    private final PromptBuilder promptBuilder;
    
    // Configuration for memory integration
    private static final int MAX_CONTEXT_MEMORIES = 5;
    private static final int MAX_CONVERSATION_HISTORY = 10;

    /**
     * Creates a new conversation memory integrator.
     *
     * @param memoryManager    The memory manager
     * @param aiServiceManager The AI service manager
     * @param promptBuilder    The prompt builder
     */
    public ConversationMemoryIntegrator(MemoryManager memoryManager,
                                      AIServiceManager aiServiceManager,
                                      PromptBuilder promptBuilder) {
        this.memoryManager = memoryManager;
        this.aiServiceManager = aiServiceManager;
        this.promptBuilder = promptBuilder;
    }

    /**
     * Processes a conversation with memory integration.
     * Retrieves relevant memories, generates AI response, and stores the new interaction.
     *
     * @param villagerId      UUID of the villager
     * @param playerId        UUID of the player
     * @param playerName      Name of the player
     * @param playerMessage   Message from the player
     * @param interactionType Type of interaction (VOICE, TEXT, etc.)
     * @param player          The player entity (for context)
     * @param gameContext     Game context information
     * @return CompletableFuture containing the conversation result
     */
    public CompletableFuture<ConversationResult> processConversation(UUID villagerId,
                                                                   UUID playerId,
                                                                   String playerName,
                                                                   String playerMessage,
                                                                   InteractionType interactionType,
                                                                   Object player,
                                                                   GameContext gameContext) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Load villager data
                VillagerData villagerData = VillagerDataManager.getVillagerData(villagerId);
                if (villagerData == null) {
                    return ConversationResult.failure("Villager not found");
                }

                // Retrieve relevant memories for context
                List<InteractionMemory> contextMemories = retrieveContextualMemories(villagerData, playerId);
                
                // Generate AI response with memory context
                return generateResponseWithMemory(villagerData, playerId, playerName, 
                                                playerMessage, interactionType, player, 
                                                gameContext, contextMemories);
                
            } catch (Exception e) {
                LOGGER.error("Error processing conversation for villager {}: {}", villagerId, e.getMessage(), e);
                return ConversationResult.failure("Failed to process conversation: " + e.getMessage());
            }
        });
    }

    /**
     * Retrieves contextual memories for a conversation.
     * Gets recent memories with the specific player and relevant general memories.
     *
     * @param villagerData The villager data
     * @param playerId     UUID of the player
     * @return List of relevant memories for context
     */
    public List<InteractionMemory> retrieveContextualMemories(VillagerData villagerData, UUID playerId) {
        // Get recent memories with this specific player
        List<InteractionMemory> playerMemories = villagerData.getRecentMemories(playerId, MAX_CONTEXT_MEMORIES);
        
        // If we don't have enough player-specific memories, add some general recent memories
        if (playerMemories.size() < MAX_CONTEXT_MEMORIES) {
            int additionalNeeded = MAX_CONTEXT_MEMORIES - playerMemories.size();
            List<InteractionMemory> generalMemories = villagerData.getRecentMemories(additionalNeeded);
            
            // Add general memories that aren't already included
            for (InteractionMemory memory : generalMemories) {
                if (!playerMemories.contains(memory) && playerMemories.size() < MAX_CONTEXT_MEMORIES) {
                    playerMemories.add(memory);
                }
            }
        }
        
        LOGGER.debug("Retrieved {} contextual memories for villager {} and player {}", 
                    playerMemories.size(), villagerData.getVillagerId(), playerId);
        
        return playerMemories;
    }

    /**
     * Generates an AI response with memory context integration.
     *
     * @param villagerData     The villager data
     * @param playerId         UUID of the player
     * @param playerName       Name of the player
     * @param playerMessage    Message from the player
     * @param interactionType  Type of interaction
     * @param player           The player entity
     * @param gameContext      Game context information
     * @param contextMemories  Relevant memories for context
     * @return Conversation result with AI response
     */
    private ConversationResult generateResponseWithMemory(VillagerData villagerData,
                                                        UUID playerId,
                                                        String playerName,
                                                        String playerMessage,
                                                        InteractionType interactionType,
                                                        Object player,
                                                        GameContext gameContext,
                                                        List<InteractionMemory> contextMemories) {
        try {
            // Generate AI response using the prompt builder (which already includes memory context)
            CompletableFuture<AIResponse> aiResponseFuture = aiServiceManager.generateResponse(
                playerMessage, villagerData, player, gameContext);
            
            AIResponse aiResponse = aiResponseFuture.get();
            
            if (!aiResponse.isSuccess()) {
                return ConversationResult.failure("AI service error: " + aiResponse.getErrorMessage());
            }

            String villagerResponse = aiResponse.getResponseText();
            
            // Store the new interaction in memory
            storeInteractionMemory(villagerData, playerId, playerName, playerMessage, 
                                 villagerResponse, interactionType, gameContext);
            
            // Create conversation result with memory context
            return ConversationResult.success(villagerResponse, contextMemories, 
                                            villagerData.getRecentMemories(playerId, MAX_CONVERSATION_HISTORY));
            
        } catch (Exception e) {
            LOGGER.error("Error generating response with memory for villager {}: {}", 
                        villagerData.getVillagerId(), e.getMessage(), e);
            return ConversationResult.failure("Failed to generate response: " + e.getMessage());
        }
    }

    /**
     * Stores a new interaction memory.
     *
     * @param villagerData     The villager data
     * @param playerId         UUID of the player
     * @param playerName       Name of the player
     * @param playerMessage    Message from the player
     * @param villagerResponse Response from the villager
     * @param interactionType  Type of interaction
     * @param gameContext      Game context information
     */
    public void storeInteractionMemory(VillagerData villagerData,
                                     UUID playerId,
                                     String playerName,
                                     String playerMessage,
                                     String villagerResponse,
                                     InteractionType interactionType,
                                     GameContext gameContext) {
        try {
            long currentGameDay = getCurrentGameDay(gameContext);
            
            InteractionMemory memory = new InteractionMemory(
                playerId,
                playerName,
                playerMessage,
                villagerResponse,
                interactionType,
                currentGameDay
            );
            
            villagerData.addMemory(memory);
            
            // Save the updated villager data
            VillagerDataManager.updateVillagerData(villagerData);
            
            LOGGER.debug("Stored interaction memory for villager {} and player {}: {} -> {}", 
                        villagerData.getVillagerId(), playerId, playerMessage, villagerResponse);
            
        } catch (Exception e) {
            LOGGER.error("Error storing interaction memory for villager {}: {}", 
                        villagerData.getVillagerId(), e.getMessage(), e);
        }
    }

    /**
     * Gets conversation history for a specific player and villager.
     *
     * @param villagerId UUID of the villager
     * @param playerId   UUID of the player
     * @param limit      Maximum number of memories to return
     * @return List of interaction memories
     */
    public List<InteractionMemory> getConversationHistory(UUID villagerId, UUID playerId, int limit) {
        try {
            VillagerData villagerData = VillagerDataManager.getVillagerData(villagerId);
            if (villagerData == null) {
                return List.of();
            }
            
            return villagerData.getRecentMemories(playerId, limit);
            
        } catch (Exception e) {
            LOGGER.error("Error getting conversation history for villager {} and player {}: {}", 
                        villagerId, playerId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Checks if a villager has previous interactions with a player.
     *
     * @param villagerId UUID of the villager
     * @param playerId   UUID of the player
     * @return true if there are previous interactions
     */
    public boolean hasPreviousInteractions(UUID villagerId, UUID playerId) {
        try {
            VillagerData villagerData = VillagerDataManager.getVillagerData(villagerId);
            if (villagerData == null) {
                return false;
            }
            
            return !villagerData.getRecentMemories(playerId, 1).isEmpty();
            
        } catch (Exception e) {
            LOGGER.error("Error checking previous interactions for villager {} and player {}: {}", 
                        villagerId, playerId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets the most recent interaction with a player.
     *
     * @param villagerId UUID of the villager
     * @param playerId   UUID of the player
     * @return The most recent interaction memory, or null if none exists
     */
    public InteractionMemory getLastInteraction(UUID villagerId, UUID playerId) {
        try {
            VillagerData villagerData = VillagerDataManager.getVillagerData(villagerId);
            if (villagerData == null) {
                return null;
            }
            
            List<InteractionMemory> recent = villagerData.getRecentMemories(playerId, 1);
            return recent.isEmpty() ? null : recent.get(0);
            
        } catch (Exception e) {
            LOGGER.error("Error getting last interaction for villager {} and player {}: {}", 
                        villagerId, playerId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Provides conversation continuity by referencing previous interactions.
     *
     * @param villagerId UUID of the villager
     * @param playerId   UUID of the player
     * @return Continuity context string, or empty string if no previous interactions
     */
    public String getConversationContinuity(UUID villagerId, UUID playerId) {
        try {
            InteractionMemory lastInteraction = getLastInteraction(villagerId, playerId);
            if (lastInteraction == null) {
                return "";
            }
            
            // Calculate time since last interaction
            long timeSinceLastInteraction = System.currentTimeMillis() - lastInteraction.getTimestamp();
            long hoursSince = timeSinceLastInteraction / (1000 * 60 * 60);
            
            if (hoursSince < 1) {
                return "We were just talking about: \"" + lastInteraction.getPlayerMessage() + "\"";
            } else if (hoursSince < 24) {
                return "Earlier today you mentioned: \"" + lastInteraction.getPlayerMessage() + "\"";
            } else {
                return "Last time we spoke, you said: \"" + lastInteraction.getPlayerMessage() + "\"";
            }
            
        } catch (Exception e) {
            LOGGER.error("Error getting conversation continuity for villager {} and player {}: {}", 
                        villagerId, playerId, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Gets the current game day from game context or system time.
     *
     * @param gameContext Game context information
     * @return Current game day
     */
    private long getCurrentGameDay(GameContext gameContext) {
        if (gameContext != null && gameContext.getGameDay() > 0) {
            return gameContext.getGameDay();
        }
        
        // Fallback to system time-based calculation
        return System.currentTimeMillis() / (24 * 60 * 60 * 1000);
    }

    /**
     * Result of a conversation processing operation.
     */
    public static class ConversationResult {
        private final boolean success;
        private final String response;
        private final String errorMessage;
        private final List<InteractionMemory> contextMemories;
        private final List<InteractionMemory> conversationHistory;

        private ConversationResult(boolean success, String response, String errorMessage,
                                 List<InteractionMemory> contextMemories, 
                                 List<InteractionMemory> conversationHistory) {
            this.success = success;
            this.response = response;
            this.errorMessage = errorMessage;
            this.contextMemories = contextMemories;
            this.conversationHistory = conversationHistory;
        }

        public static ConversationResult success(String response, List<InteractionMemory> contextMemories,
                                               List<InteractionMemory> conversationHistory) {
            return new ConversationResult(true, response, null, contextMemories, conversationHistory);
        }

        public static ConversationResult failure(String errorMessage) {
            return new ConversationResult(false, null, errorMessage, List.of(), List.of());
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getResponse() { return response; }
        public String getErrorMessage() { return errorMessage; }
        public List<InteractionMemory> getContextMemories() { return contextMemories; }
        public List<InteractionMemory> getConversationHistory() { return conversationHistory; }
    }
}