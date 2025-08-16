package com.foogly.voiceofthevillage.reputation;

import com.foogly.voiceofthevillage.data.ReputationThreshold;
import com.foogly.voiceofthevillage.data.VillagerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Handles reputation-based behavior triggers for villagers.
 * Manages villager attacks and iron golem spawning based on reputation thresholds.
 */
public class BehaviorTrigger {
    private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorTrigger.class);
    
    private final ReputationManager reputationManager;
    private final Random random = new Random();
    
    // Humorous messages for iron golem spawning
    private static final String[] IRON_GOLEM_MESSAGES = {
        "That's it! I'm hiring a guy!",
        "You've crossed the line! Time to call in the big guns!",
        "I know a guy who knows a guy... and he's made of iron!",
        "You want trouble? I'll show you trouble!",
        "Time to bring out the heavy artillery!",
        "I'm calling my friend... he's REALLY strong!",
        "You picked the wrong villager to mess with!",
        "Say hello to my metallic friend!",
        "I've had enough of your nonsense! Meet my bodyguard!",
        "You think you're tough? Wait until you meet Iron Mike!"
    };
    
    private static final String[] ATTACK_MESSAGES = {
        "Take that!",
        "I've had enough of you!",
        "That's for being mean to me!",
        "You deserved that!",
        "Don't mess with me!",
        "I'm not as helpless as I look!",
        "That's what you get!",
        "I may be a villager, but I have feelings too!"
    };

    public BehaviorTrigger(ReputationManager reputationManager) {
        this.reputationManager = reputationManager;
    }

    /**
     * Checks and triggers reputation-based behaviors for a villager-player interaction.
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    public void checkAndTriggerBehaviors(Villager villager, Player player) {
        try {
            ReputationThreshold threshold = reputationManager.getReputationThreshold(villager, player);
            
            switch (threshold) {
                case UNFRIENDLY -> handleUnfriendlyBehavior(villager, player);
                case HOSTILE -> handleHostileBehavior(villager, player);
                default -> {
                    // No special behavior for neutral, friendly, or beloved
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to check reputation behaviors for player {} with villager {}", 
                        player.getName().getString(), villager.getUUID(), e);
        }
    }

    /**
     * Handles unfriendly behavior - villager attacks player once.
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    private void handleUnfriendlyBehavior(Villager villager, Player player) {
        if (reputationManager.shouldVillagerAttackPlayer(villager, player)) {
            attackPlayer(villager, player);
            reputationManager.markVillagerAttackedPlayer(villager, player);
            
            LOGGER.debug("Villager {} attacked player {} due to unfriendly reputation", 
                        villager.getUUID(), player.getName().getString());
        }
    }

    /**
     * Handles hostile behavior - villager spawns iron golem.
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    private void handleHostileBehavior(Villager villager, Player player) {
        if (reputationManager.shouldVillagerSpawnIronGolem(villager, player)) {
            spawnIronGolem(villager, player);
            reputationManager.markVillagerSpawnedIronGolem(villager, player);
            
            LOGGER.debug("Villager {} spawned iron golem against player {} due to hostile reputation", 
                        villager.getUUID(), player.getName().getString());
        }
    }

    /**
     * Makes the villager attack the player once.
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    private void attackPlayer(Villager villager, Player player) {
        try {
            // Deal 1 point of damage to the player
            player.hurt(player.damageSources().mobAttack(villager), 1.0f);
            
            // Make the villager say something
            String message = ATTACK_MESSAGES[random.nextInt(ATTACK_MESSAGES.length)];
            villager.sendSystemMessage(Component.literal(message));
            
            // Send message to nearby players
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.getPlayers(serverPlayer -> 
                    serverPlayer.distanceToSqr(villager) <= 256.0 // 16 block radius
                ).forEach(nearbyPlayer -> {
                    nearbyPlayer.sendSystemMessage(
                        Component.literal("§e" + getVillagerName(villager) + " says: " + message)
                    );
                });
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to make villager {} attack player {}", 
                        villager.getUUID(), player.getName().getString(), e);
        }
    }

    /**
     * Spawns an iron golem that is hostile toward the player.
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    private void spawnIronGolem(Villager villager, Player player) {
        try {
            if (!(villager.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            // Find a suitable spawn location near the villager
            BlockPos spawnPos = findIronGolemSpawnLocation(villager);
            if (spawnPos == null) {
                LOGGER.warn("Could not find suitable spawn location for iron golem near villager {}", 
                           villager.getUUID());
                return;
            }

            // Create and spawn the iron golem
            IronGolem ironGolem = EntityType.IRON_GOLEM.create(serverLevel);
            if (ironGolem == null) {
                LOGGER.error("Failed to create iron golem entity");
                return;
            }

            // Position the iron golem
            ironGolem.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            
            // Make the iron golem target the player
            ironGolem.setTarget(player);
            
            // Set the iron golem as player-created (so it's hostile to the player)
            ironGolem.setPlayerCreated(true);
            
            // Spawn the iron golem
            serverLevel.addFreshEntity(ironGolem);

            // Make the villager announce the spawning
            String message = IRON_GOLEM_MESSAGES[random.nextInt(IRON_GOLEM_MESSAGES.length)];
            
            // Send message to the villager (for voice output)
            villager.sendSystemMessage(Component.literal(message));
            
            // Send message to nearby players
            serverLevel.getPlayers(serverPlayer -> 
                serverPlayer.distanceToSqr(villager) <= 256.0 // 16 block radius
            ).forEach(nearbyPlayer -> {
                nearbyPlayer.sendSystemMessage(
                    Component.literal("§c" + getVillagerName(villager) + " shouts: " + message)
                );
            });
            
            LOGGER.info("Iron golem spawned by villager {} to attack player {} at position {}", 
                       villager.getUUID(), player.getName().getString(), spawnPos);
            
        } catch (Exception e) {
            LOGGER.error("Failed to spawn iron golem for villager {} against player {}", 
                        villager.getUUID(), player.getName().getString(), e);
        }
    }

    /**
     * Finds a suitable location to spawn an iron golem near the villager.
     *
     * @param villager The villager entity
     * @return A suitable spawn position, or null if none found
     */
    private BlockPos findIronGolemSpawnLocation(Villager villager) {
        BlockPos villagerPos = villager.blockPosition();
        ServerLevel level = (ServerLevel) villager.level();
        
        // Try positions in a 3x3 area around the villager
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 2; y++) { // Check a few Y levels
                    BlockPos testPos = villagerPos.offset(x, y, z);
                    
                    // Check if the position is suitable for spawning
                    if (isValidSpawnLocation(level, testPos)) {
                        return testPos;
                    }
                }
            }
        }
        
        // If no suitable location found nearby, try a wider area
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -2; y <= 3; y++) {
                    BlockPos testPos = villagerPos.offset(x, y, z);
                    
                    if (isValidSpawnLocation(level, testPos)) {
                        return testPos;
                    }
                }
            }
        }
        
        return null; // No suitable location found
    }

    /**
     * Checks if a position is valid for spawning an iron golem.
     *
     * @param level The server level
     * @param pos   The position to check
     * @return true if the position is valid for spawning
     */
    private boolean isValidSpawnLocation(ServerLevel level, BlockPos pos) {
        // Check if there's enough space (iron golems are 3 blocks tall)
        for (int y = 0; y < 3; y++) {
            BlockPos checkPos = pos.above(y);
            if (!level.getBlockState(checkPos).isAir()) {
                return false;
            }
        }
        
        // Check if there's a solid block below
        BlockPos belowPos = pos.below();
        return level.getBlockState(belowPos).isSolid();
    }

    /**
     * Gets the effective name of a villager for display purposes.
     *
     * @param villager The villager entity
     * @return The villager's name
     */
    private String getVillagerName(Villager villager) {
        try {
            return VillagerDataManager.getVillagerName(villager.getUUID());
        } catch (Exception e) {
            return "Villager";
        }
    }

    /**
     * Triggers reputation-based behaviors when a player interacts with a villager.
     * This should be called from villager interaction events.
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    public void onVillagerInteraction(Villager villager, Player player) {
        checkAndTriggerBehaviors(villager, player);
    }

    /**
     * Triggers reputation-based behaviors when a player approaches a villager.
     * This can be called periodically or from proximity detection.
     *
     * @param villager The villager entity
     * @param player   The player entity
     */
    public void onPlayerApproach(Villager villager, Player player) {
        // Only trigger hostile behaviors on approach, not unfriendly ones
        ReputationThreshold threshold = reputationManager.getReputationThreshold(villager, player);
        
        if (threshold == ReputationThreshold.HOSTILE) {
            handleHostileBehavior(villager, player);
        }
    }

    /**
     * Gets a random attack message for villager attacks.
     *
     * @return A random attack message
     */
    public String getRandomAttackMessage() {
        return ATTACK_MESSAGES[random.nextInt(ATTACK_MESSAGES.length)];
    }

    /**
     * Gets a random iron golem spawn message.
     *
     * @return A random iron golem spawn message
     */
    public String getRandomIronGolemMessage() {
        return IRON_GOLEM_MESSAGES[random.nextInt(IRON_GOLEM_MESSAGES.length)];
    }
}