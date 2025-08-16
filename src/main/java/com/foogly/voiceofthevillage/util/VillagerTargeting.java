package com.foogly.voiceofthevillage.util;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Utility class for targeting villagers using raycasting.
 * Handles line-of-sight checks and distance validation for advanced mode communication.
 */
public class VillagerTargeting {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerTargeting.class);
    
    // Maximum raycast distance for targeting
    private static final double MAX_RAYCAST_DISTANCE = 50.0;
    
    // Targeting precision - how close the crosshair needs to be to the villager
    private static final double TARGETING_PRECISION = 1.5;
    
    /**
     * Gets the villager currently being targeted by the player's crosshair.
     * Uses raycasting to find villagers in line of sight within interaction range.
     * 
     * @return the targeted villager, or null if no villager is targeted
     */
    public static Villager getTargetedVillager() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return null;
        }
        
        // Get player's look direction and position
        Vec3 eyePosition = minecraft.player.getEyePosition();
        Vec3 lookDirection = minecraft.player.getViewVector(1.0f);
        
        // Calculate effective interaction distance
        double maxDistance = VoiceConfig.getEffectiveInteractionDistance();
        if (VoiceConfig.isDistanceCheckEnabled()) {
            maxDistance = Math.min(maxDistance, MAX_RAYCAST_DISTANCE);
        } else {
            maxDistance = MAX_RAYCAST_DISTANCE;
        }
        
        Vec3 endPosition = eyePosition.add(lookDirection.scale(maxDistance));
        
        // Find all villagers within range
        AABB searchBox = new AABB(eyePosition, endPosition).inflate(TARGETING_PRECISION);
        List<Villager> nearbyVillagers = minecraft.level.getEntitiesOfClass(
            Villager.class, 
            searchBox,
            villager -> villager.isAlive() && !villager.isBaby()
        );
        
        if (nearbyVillagers.isEmpty()) {
            return null;
        }
        
        // Find the closest villager that intersects with the player's look ray
        Villager closestVillager = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Villager villager : nearbyVillagers) {
            // Check if villager is within interaction distance
            double distance = minecraft.player.distanceTo(villager);
            if (VoiceConfig.isDistanceCheckEnabled() && distance > VoiceConfig.getEffectiveInteractionDistance()) {
                continue;
            }
            
            // Check if the villager intersects with the look ray
            Optional<Vec3> intersection = getEntityIntersection(eyePosition, lookDirection, villager, maxDistance);
            if (intersection.isPresent()) {
                if (distance < closestDistance) {
                    // Verify line of sight (no blocks blocking)
                    if (hasLineOfSight(eyePosition, villager.getEyePosition())) {
                        closestVillager = villager;
                        closestDistance = distance;
                    }
                }
            }
        }
        
        if (closestVillager != null) {
            LOGGER.debug("Targeted villager at distance {:.2f}", closestDistance);
        }
        
        return closestVillager;
    }
    
    /**
     * Checks if there's a clear line of sight between two positions.
     * 
     * @param from the starting position
     * @param to the target position
     * @return true if there's a clear line of sight, false if blocked by blocks
     */
    private static boolean hasLineOfSight(Vec3 from, Vec3 to) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        
        // Perform a block raycast to check for obstructions
        ClipContext clipContext = new ClipContext(
            from,
            to,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            minecraft.player
        );
        
        HitResult hitResult = minecraft.level.clip(clipContext);
        
        // If we hit a block before reaching the target, line of sight is blocked
        return hitResult.getType() == HitResult.Type.MISS;
    }
    
    /**
     * Calculates the intersection point between a ray and an entity's bounding box.
     * 
     * @param rayStart the starting point of the ray
     * @param rayDirection the direction of the ray (normalized)
     * @param entity the entity to check intersection with
     * @param maxDistance the maximum distance to check
     * @return the intersection point if found, empty otherwise
     */
    private static Optional<Vec3> getEntityIntersection(Vec3 rayStart, Vec3 rayDirection, Entity entity, double maxDistance) {
        // Get entity's bounding box
        AABB entityBox = entity.getBoundingBox();
        
        // Expand the bounding box slightly for easier targeting
        entityBox = entityBox.inflate(TARGETING_PRECISION * 0.5);
        
        // Calculate ray-box intersection
        Vec3 rayEnd = rayStart.add(rayDirection.scale(maxDistance));
        
        // Use AABB's clip method to find intersection
        Optional<Vec3> intersection = entityBox.clip(rayStart, rayEnd);
        
        if (intersection.isPresent()) {
            // Verify the intersection is within the maximum distance
            double distance = rayStart.distanceTo(intersection.get());
            if (distance <= maxDistance) {
                return intersection;
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets the distance to the currently targeted villager.
     * 
     * @return the distance to the targeted villager, or -1 if no villager is targeted
     */
    public static double getTargetedVillagerDistance() {
        Villager targetedVillager = getTargetedVillager();
        if (targetedVillager == null) {
            return -1.0;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return -1.0;
        }
        
        return minecraft.player.distanceTo(targetedVillager);
    }
    
    /**
     * Checks if a villager is currently targetable (within range and line of sight).
     * 
     * @param villager the villager to check
     * @return true if the villager can be targeted, false otherwise
     */
    public static boolean isVillagerTargetable(Villager villager) {
        if (villager == null || !villager.isAlive() || villager.isBaby()) {
            return false;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        
        // Check distance
        double distance = minecraft.player.distanceTo(villager);
        if (VoiceConfig.isDistanceCheckEnabled() && distance > VoiceConfig.getEffectiveInteractionDistance()) {
            return false;
        }
        
        // Check line of sight
        Vec3 playerEyes = minecraft.player.getEyePosition();
        Vec3 villagerEyes = villager.getEyePosition();
        
        return hasLineOfSight(playerEyes, villagerEyes);
    }
    
    /**
     * Gets all targetable villagers within range.
     * 
     * @return a list of villagers that can be targeted
     */
    public static List<Villager> getTargetableVillagers() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return List.of();
        }
        
        double maxDistance = VoiceConfig.getEffectiveInteractionDistance();
        if (!VoiceConfig.isDistanceCheckEnabled()) {
            maxDistance = MAX_RAYCAST_DISTANCE;
        }
        
        // Create search area around player
        Vec3 playerPos = minecraft.player.position();
        AABB searchBox = new AABB(playerPos, playerPos).inflate(maxDistance);
        
        // Find all villagers in range
        List<Villager> villagers = minecraft.level.getEntitiesOfClass(
            Villager.class,
            searchBox,
            villager -> isVillagerTargetable(villager)
        );
        
        LOGGER.debug("Found {} targetable villagers within range", villagers.size());
        return villagers;
    }
}