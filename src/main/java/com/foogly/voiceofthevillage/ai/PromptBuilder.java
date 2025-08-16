package com.foogly.voiceofthevillage.ai;

import com.foogly.voiceofthevillage.data.InteractionMemory;
import com.foogly.voiceofthevillage.data.ReputationData;
import com.foogly.voiceofthevillage.data.VillagerData;


import java.util.List;

/**
 * Builds context-aware prompts for AI services.
 * Incorporates villager personality, memories, reputation, and game context into prompts.
 */
public class PromptBuilder {
    private static final int MAX_MEMORIES_IN_PROMPT = 5;
    private static final int MAX_PROMPT_LENGTH = 2000;

    /**
     * Builds a comprehensive prompt for AI response generation.
     *
     * @param playerMessage The message from the player
     * @param villagerData  Data about the villager
     * @param player        The player entity
     * @param gameContext   Game context information
     * @return Constructed prompt string
     */
    public String buildPrompt(String playerMessage, VillagerData villagerData, Object player, GameContext gameContext) {
        StringBuilder prompt = new StringBuilder();

        // Villager identity and personality
        prompt.append("You are ").append(villagerData.getEffectiveName())
              .append(", a ").append(villagerData.getGender().toString().toLowerCase())
              .append(" villager in Minecraft. ");

        // Personality description
        prompt.append("Your personality is ").append(villagerData.getPersonality().toString().toLowerCase())
              .append(": ").append(villagerData.getPersonality().getDescription()).append(". ");

        // Game context
        appendGameContext(prompt, gameContext);

        // Player relationship and reputation
        appendReputationContext(prompt, villagerData, player);

        // Recent memories
        appendMemoryContext(prompt, villagerData, player);

        // Current interaction
        String playerName = getPlayerName(player);
        prompt.append("\nThe player ").append(playerName)
              .append(" says to you: \"").append(playerMessage).append("\"\n");

        // Response instructions
        prompt.append("\nRespond as ").append(villagerData.getEffectiveName())
              .append(" would, staying in character. Keep your response to 1-2 sentences, ")
              .append("be friendly and appropriate for all ages, and remember your personality and ")
              .append("relationship with this player. Speak naturally as a villager would in Minecraft.");

        // Ensure prompt doesn't exceed maximum length
        String finalPrompt = prompt.toString();
        if (finalPrompt.length() > MAX_PROMPT_LENGTH) {
            finalPrompt = finalPrompt.substring(0, MAX_PROMPT_LENGTH - 50) + "...";
        }

        return finalPrompt;
    }

    /**
     * Appends game context information to the prompt.
     *
     * @param prompt      The prompt builder
     * @param gameContext Game context information
     */
    private void appendGameContext(StringBuilder prompt, GameContext gameContext) {
        if (gameContext == null) {
            prompt.append("It is a pleasant day in the village. ");
            return;
        }
        
        prompt.append("It is currently ").append(gameContext.getTimeDescription())
              .append(" and the weather is ").append(gameContext.getWeatherDescription())
              .append(". ");

        // Add biome context if it's interesting
        String biome = gameContext.getBiome();
        if (biome != null) {
            if (biome.contains("desert")) {
                prompt.append("You live in a desert village. ");
            } else if (biome.contains("snow") || biome.contains("ice")) {
                prompt.append("You live in a snowy village. ");
            } else if (biome.contains("jungle")) {
                prompt.append("You live in a jungle village. ");
            } else if (biome.contains("plains")) {
                prompt.append("You live in a plains village. ");
            }
        }

        // Add dimension context if not overworld
        String dimensionName = gameContext.getDimensionName();
        if (dimensionName != null && !dimensionName.contains("overworld")) {
            prompt.append("You are in the ").append(dimensionName).append(". ");
        }
    }

    /**
     * Appends reputation and relationship context to the prompt.
     *
     * @param prompt       The prompt builder
     * @param villagerData Villager data
     * @param player       The player
     */
    private void appendReputationContext(StringBuilder prompt, VillagerData villagerData, Object player) {
        if (player == null) {
            prompt.append("You are meeting a new player. ");
            return;
        }
        
        String playerName = getPlayerName(player);
        Object playerId = getPlayerId(player);
        
        if (playerId != null) {
            ReputationData reputation = villagerData.getReputation((java.util.UUID) playerId);
            
            if (reputation != null) {
                int score = reputation.getScore();
                
                if (score >= 80) {
                    prompt.append("You consider ").append(playerName)
                          .append(" a beloved friend. ");
                } else if (score >= 40) {
                    prompt.append("You have a friendly relationship with ")
                          .append(playerName).append(". ");
                } else if (score <= -80) {
                    prompt.append("You strongly dislike ").append(playerName)
                          .append(" and are very wary of them. ");
                } else if (score <= -40) {
                    prompt.append("You are unfriendly towards ").append(playerName)
                          .append(" due to past negative interactions. ");
                } else {
                    prompt.append("You have a neutral relationship with ")
                          .append(playerName).append(". ");
                }
            } else {
                prompt.append("This is your first time meeting ").append(playerName).append(". ");
            }
        } else {
            prompt.append("You are meeting a new player. ");
        }
    }

    /**
     * Appends recent memory context to the prompt.
     *
     * @param prompt       The prompt builder
     * @param villagerData Villager data
     * @param player       The player
     */
    private void appendMemoryContext(StringBuilder prompt, VillagerData villagerData, Object player) {
        if (player == null) {
            return;
        }
        
        String playerName = getPlayerName(player);
        Object playerId = getPlayerId(player);
        
        if (playerId != null) {
            List<InteractionMemory> recentMemories = villagerData.getRecentMemories((java.util.UUID) playerId, MAX_MEMORIES_IN_PROMPT);
            
            if (!recentMemories.isEmpty()) {
                prompt.append("\nRecent conversations with ").append(playerName).append(":\n");
                
                // Sort memories by timestamp to show chronological order
                recentMemories.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                
                for (InteractionMemory memory : recentMemories) {
                    prompt.append("- Player said: \"").append(memory.getPlayerMessage())
                          .append("\" and you replied: \"").append(memory.getVillagerResponse()).append("\"\n");
                }
                
                // Add conversation continuity context
                InteractionMemory lastMemory = recentMemories.get(recentMemories.size() - 1);
                long timeSinceLastInteraction = System.currentTimeMillis() - lastMemory.getTimestamp();
                long hoursSince = timeSinceLastInteraction / (1000 * 60 * 60);
                
                if (hoursSince < 1) {
                    prompt.append("This conversation is continuing from just moments ago. ");
                } else if (hoursSince < 24) {
                    prompt.append("You spoke with this player earlier today. ");
                } else if (hoursSince < 168) { // 1 week
                    prompt.append("You spoke with this player recently. ");
                } else {
                    prompt.append("It has been a while since you last spoke with this player. ");
                }
            }
        }
    }

    /**
     * Builds a memory-enhanced prompt with specific contextual memories.
     *
     * @param playerMessage   The message from the player
     * @param villagerData    Data about the villager
     * @param player          The player entity
     * @param gameContext     Game context information
     * @param contextMemories Specific memories to include in context
     * @return Enhanced prompt string with memory context
     */
    public String buildMemoryEnhancedPrompt(String playerMessage, VillagerData villagerData, 
                                          Object player, GameContext gameContext,
                                          List<InteractionMemory> contextMemories) {
        StringBuilder prompt = new StringBuilder();

        // Villager identity and personality
        prompt.append("You are ").append(villagerData.getEffectiveName())
              .append(", a ").append(villagerData.getGender().toString().toLowerCase())
              .append(" villager in Minecraft. ");

        // Personality description
        prompt.append("Your personality is ").append(villagerData.getPersonality().toString().toLowerCase())
              .append(": ").append(villagerData.getPersonality().getDescription()).append(". ");

        // Game context
        appendGameContext(prompt, gameContext);

        // Player relationship and reputation
        appendReputationContext(prompt, villagerData, player);

        // Enhanced memory context with provided memories
        appendEnhancedMemoryContext(prompt, villagerData, player, contextMemories);

        // Current interaction
        String playerName = getPlayerName(player);
        prompt.append("\nThe player ").append(playerName)
              .append(" says to you: \"").append(playerMessage).append("\"\n");

        // Response instructions with memory awareness
        prompt.append("\nRespond as ").append(villagerData.getEffectiveName())
              .append(" would, staying in character and considering your shared history. ")
              .append("Keep your response to 1-2 sentences, be friendly and appropriate for all ages, ")
              .append("and remember your personality and relationship with this player. ")
              .append("Reference past conversations naturally if relevant. ")
              .append("Speak naturally as a villager would in Minecraft.");

        // Ensure prompt doesn't exceed maximum length
        String finalPrompt = prompt.toString();
        if (finalPrompt.length() > MAX_PROMPT_LENGTH) {
            finalPrompt = finalPrompt.substring(0, MAX_PROMPT_LENGTH - 50) + "...";
        }

        return finalPrompt;
    }

    /**
     * Appends enhanced memory context with specific memories.
     *
     * @param prompt          The prompt builder
     * @param villagerData    Villager data
     * @param player          The player
     * @param contextMemories Specific memories to include
     */
    private void appendEnhancedMemoryContext(StringBuilder prompt, VillagerData villagerData, 
                                           Object player, List<InteractionMemory> contextMemories) {
        if (player == null || contextMemories.isEmpty()) {
            return;
        }
        
        String playerName = getPlayerName(player);
        
        prompt.append("\nYour conversation history with ").append(playerName).append(":\n");
        
        // Sort memories chronologically
        contextMemories.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        
        for (InteractionMemory memory : contextMemories) {
            prompt.append("- ").append(memory.getPlayerName()).append(" said: \"")
                  .append(memory.getPlayerMessage())
                  .append("\" and you replied: \"").append(memory.getVillagerResponse())
                  .append("\"\n");
        }
        
        // Add conversation flow context
        if (!contextMemories.isEmpty()) {
            InteractionMemory lastMemory = contextMemories.get(contextMemories.size() - 1);
            long timeSinceLastInteraction = System.currentTimeMillis() - lastMemory.getTimestamp();
            long hoursSince = timeSinceLastInteraction / (1000 * 60 * 60);
            
            if (hoursSince < 1) {
                prompt.append("This conversation is continuing naturally from your recent exchange. ");
            } else if (hoursSince < 24) {
                prompt.append("You're continuing a conversation from earlier today. ");
            } else {
                prompt.append("You're reconnecting after some time apart. ");
            }
        }
    }

    /**
     * Builds a simplified prompt for testing or fallback scenarios.
     *
     * @param playerMessage The message from the player
     * @param villagerName  Name of the villager
     * @param personality   Villager personality
     * @return Simple prompt string
     */
    public String buildSimplePrompt(String playerMessage, String villagerName, String personality) {
        return String.format(
            "You are %s, a villager in Minecraft with a %s personality. " +
            "The player says: \"%s\". " +
            "Respond in 1-2 sentences as a friendly villager would.",
            villagerName, personality.toLowerCase(), playerMessage
        );
    }

    /**
     * Extracts player name from player object using reflection to avoid Minecraft dependencies in tests.
     *
     * @param player The player object
     * @return Player name or "Player" as fallback
     */
    private String getPlayerName(Object player) {
        if (player == null) {
            return "Player";
        }
        
        try {
            // Try to get name using reflection for Minecraft Player objects
            java.lang.reflect.Method getNameMethod = player.getClass().getMethod("getName");
            Object nameComponent = getNameMethod.invoke(player);
            java.lang.reflect.Method getStringMethod = nameComponent.getClass().getMethod("getString");
            return (String) getStringMethod.invoke(nameComponent);
        } catch (Exception e) {
            // Fallback for tests or other cases
            return "Player";
        }
    }

    /**
     * Extracts player UUID from player object using reflection to avoid Minecraft dependencies in tests.
     *
     * @param player The player object
     * @return Player UUID or null as fallback
     */
    private Object getPlayerId(Object player) {
        if (player == null) {
            return null;
        }
        
        try {
            // Try to get UUID using reflection for Minecraft Player objects
            java.lang.reflect.Method getUUIDMethod = player.getClass().getMethod("getUUID");
            return getUUIDMethod.invoke(player);
        } catch (Exception e) {
            // Fallback for tests or other cases
            return null;
        }
    }
}