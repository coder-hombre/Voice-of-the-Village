package com.foogly.voiceofthevillage.reputation;

import com.foogly.voiceofthevillage.data.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Manages reputation calculations and updates for player-villager relationships.
 * Handles reputation event tracking and provides methods for reputation-based decisions.
 */
public class ReputationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReputationManager.class);

    public ReputationManager() {
        // Default constructor
    }

    /**
     * Adds a reputation event for a player-villager interaction.
     *
     * @param villager    The villager entity
     * @param player      The player entity
     * @param eventType   Type of reputation event
     * @param description Optional custom description
     */
    public void addReputationEvent(Villager villager, Player player, ReputationEventType eventType, String description) {
        addReputationEvent(villager, player, eventType, eventType.getDefaultScoreChange(), description);
    }

    /**
     * Adds a reputation event with custom score change.
     *
     * @param villager     The villager entity
     * @param player       The player entity
     * @param eventType    Type of reputation event
     * @param scoreChange  Custom score change (overrides default)
     * @param description  Optional custom description
     */
    public void addReputationEvent(Villager villager, Player player, ReputationEventType eventType, 
                                 int scoreChange, String description) {
        try {
            VillagerData villagerData = VillagerDataManager.getOrCreateVillagerData(villager);
            UUID playerId = player.getUUID();
            String playerName = player.getName().getString();

            String eventDescription = description != null ? description : eventType.getDescription();
            ReputationEvent event = new ReputationEvent(eventType, scoreChange, eventDescription);
            
            villagerData.addReputationEvent(playerId, playerName, event);
            
            LOGGER.debug("Added reputation event for player {} with villager {}: {} ({})", 
                        playerName, villagerData.getEffectiveName(), eventType, scoreChange);
            
            // Save the updated data
            VillagerDataManager.updateVillagerData(villagerData);
            
        } catch (Exception e) {
            LOGGER.error("Failed to add reputation event for player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
        }
    }

    /**
     * Gets the current reputation score for a player with a villager.
     *
     * @param villager The villager entity
     * @param player   The player entity
     * @return The reputation score (-100 to 100)
     */
    public int getReputationScore(Villager villager, Player player) {
        try {
            VillagerData villagerData = VillagerDataManager.getVillagerData(villager.getUUID());
            if (villagerData == null) {
                return 0; // Neutral reputation for new villagers
            }

            ReputationData reputation = villagerData.getReputation(player.getUUID());
            return reputation != null ? reputation.getScore() : 0;
            
        } catch (Exception e) {
            LOGGER.error("Failed to get reputation score for player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
            return 0;
        }
    }

    /**
     * Gets the reputation threshold for a player with a villager.
     *
     * @param villager The villager entity
     * @param player   The player entity
     * @return The reputation threshold
     */
    public ReputationThreshold getReputationThreshold(Villager villager, Player player) {
        int score = getReputationScore(villager, player);
        return ReputationThreshold.fromScore(score);
    }

    /**
     * Gets the reputation data for a player with a villager.
     *
     * @param villager The villager entity
     * @param player   The player entity
     * @return The reputation data or null if none exists
     */
    public ReputationData getReputationData(Villager villager, Player player) {
        try {
            VillagerData villagerData = VillagerDataManager.getVillagerData(villager.getUUID());
            if (villagerData == null) {
                return null;
            }

            return villagerData.getReputation(player.getUUID());
            
        } catch (Exception e) {
            LOGGER.error("Failed to get reputation data for player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
            return null;
        }
    }

    /**
     * Checks if a villager should attack a player based on reputation.
     *
     * @param villager The villager entity
     * @param player   The player entity
     * @return true if the villager should attack the player
     */
    public boolean shouldVillagerAttackPlayer(Villager villager, Player player) {
        try {
            ReputationData reputation = getReputationData(villager, player);
            return reputation != null && reputation.shouldAttackPlayer();
            
        } catch (Exception e) {
            LOGGER.error("Failed to check if villager should attack player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
            return false;
        }
    }

    /**
     * Checks if a villager should spawn an iron golem against a player.
     *
     * @param villager The villager entity
     * @param player   The player entity
     * @return true if the villager should spawn an iron golem
     */
    public boolean shouldVillagerSpawnIronGolem(Villager villager, Player player) {
        try {
            ReputationData reputation = getReputationData(villager, player);
            return reputation != null && reputation.shouldSpawnIronGolem();
            
        } catch (Exception e) {
            LOGGER.error("Failed to check if villager should spawn iron golem for player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
            return false;
        }
    }

    /**
     * Marks that a villager has attacked a player (prevents repeated attacks).
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    public void markVillagerAttackedPlayer(Villager villager, Player player) {
        try {
            VillagerData villagerData = VillagerDataManager.getVillagerData(villager.getUUID());
            if (villagerData == null) {
                return;
            }

            ReputationData reputation = villagerData.getReputation(player.getUUID());
            if (reputation != null) {
                reputation.setHasAttackedPlayer(true);
                VillagerDataManager.updateVillagerData(villagerData);
                
                LOGGER.debug("Marked villager {} as having attacked player {}", 
                           villagerData.getEffectiveName(), player.getName().getString());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to mark villager attack for player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
        }
    }

    /**
     * Marks that a villager has spawned an iron golem against a player.
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    public void markVillagerSpawnedIronGolem(Villager villager, Player player) {
        try {
            VillagerData villagerData = VillagerDataManager.getVillagerData(villager.getUUID());
            if (villagerData == null) {
                return;
            }

            ReputationData reputation = villagerData.getReputation(player.getUUID());
            if (reputation != null) {
                reputation.setHasSpawnedIronGolem(true);
                VillagerDataManager.updateVillagerData(villagerData);
                
                LOGGER.debug("Marked villager {} as having spawned iron golem against player {}", 
                           villagerData.getEffectiveName(), player.getName().getString());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to mark iron golem spawn for player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
        }
    }

    /**
     * Calculates reputation modifier for AI responses based on player reputation.
     *
     * @param villager The villager entity
     * @param player   The player entity
     * @return A string describing the villager's attitude toward the player
     */
    public String getReputationModifier(Villager villager, Player player) {
        ReputationThreshold threshold = getReputationThreshold(villager, player);
        
        return switch (threshold) {
            case HOSTILE -> "extremely hostile and angry";
            case UNFRIENDLY -> "unfriendly and suspicious";
            case NEUTRAL -> "neutral";
            case FRIENDLY -> "friendly and helpful";
            case BELOVED -> "very friendly and eager to help";
        };
    }

    /**
     * Processes conversation sentiment and adds appropriate reputation event.
     *
     * @param villager         The villager entity
     * @param player           The player entity
     * @param conversationText The conversation text to analyze
     */
    public void processConversationReputation(Villager villager, Player player, String conversationText) {
        try {
            // Simple sentiment analysis based on keywords
            String lowerText = conversationText.toLowerCase();
            
            // Check for positive indicators
            boolean hasPositive = lowerText.contains("thank") || lowerText.contains("please") || 
                                lowerText.contains("sorry") || lowerText.contains("help") ||
                                lowerText.contains("nice") || lowerText.contains("good");
            
            // Check for negative indicators
            boolean hasNegative = lowerText.contains("stupid") || lowerText.contains("hate") || 
                                lowerText.contains("shut up") || lowerText.contains("idiot") ||
                                lowerText.contains("dumb") || lowerText.contains("useless");
            
            if (hasNegative) {
                addReputationEvent(villager, player, ReputationEventType.RUDE_BEHAVIOR, 
                                 "Player was rude in conversation");
            } else if (hasPositive) {
                addReputationEvent(villager, player, ReputationEventType.POLITE_BEHAVIOR, 
                                 "Player was polite in conversation");
            } else {
                // Neutral conversation - small positive boost for interaction
                addReputationEvent(villager, player, ReputationEventType.POSITIVE_CONVERSATION, 1,
                                 "Had a neutral conversation");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to process conversation reputation for player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
        }
    }

    /**
     * Resets reputation flags (for testing or admin commands).
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    public void resetReputationFlags(Villager villager, Player player) {
        try {
            VillagerData villagerData = VillagerDataManager.getVillagerData(villager.getUUID());
            if (villagerData == null) {
                return;
            }

            ReputationData reputation = villagerData.getReputation(player.getUUID());
            if (reputation != null) {
                reputation.setHasAttackedPlayer(false);
                reputation.setHasSpawnedIronGolem(false);
                VillagerDataManager.updateVillagerData(villagerData);
                
                LOGGER.debug("Reset reputation flags for player {} with villager {}", 
                           player.getName().getString(), villagerData.getEffectiveName());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to reset reputation flags for player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
        }
    }
}