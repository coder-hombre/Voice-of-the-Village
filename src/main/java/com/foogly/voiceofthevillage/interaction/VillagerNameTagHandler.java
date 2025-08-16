package com.foogly.voiceofthevillage.interaction;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.data.Gender;
import com.foogly.voiceofthevillage.data.NameGenerator;
import com.foogly.voiceofthevillage.data.VillagerData;
import com.foogly.voiceofthevillage.data.VillagerDataManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.NameTagItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles name tag interactions with villagers.
 * Updates villager data when a name tag is used to rename a villager.
 */
@EventBusSubscriber(modid = "voiceofthevillage")
public class VillagerNameTagHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerNameTagHandler.class);
    
    /**
     * Handles entity interaction events to detect name tag usage on villagers.
     *
     * @param event The player interact event
     */
    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        // Only handle villager entities
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack itemStack = player.getItemInHand(hand);
        
        // Check if player is using a name tag
        if (itemStack.getItem() != Items.NAME_TAG) {
            return;
        }
        
        // Get the name from the name tag
        String newName = itemStack.getDisplayName().getString();
        
        // Check if the name tag has a meaningful name (not just "Name Tag")
        if (newName == null || newName.equals("Name Tag") || newName.trim().isEmpty()) {
            return;
        }
        
        if (VoiceConfig.DEBUG_MODE.get()) {
            LOGGER.debug("Player {} is renaming villager {} to '{}'", 
                        player.getName().getString(), villager.getUUID(), newName);
        }
        
        // Handle the name tag interaction
        handleNameTagInteraction(villager, newName, player);
    }
    
    /**
     * Handles the actual name tag interaction logic.
     *
     * @param villager The villager being renamed
     * @param newName The new name from the name tag
     * @param player The player using the name tag
     */
    private static void handleNameTagInteraction(Villager villager, String newName, Player player) {
        try {
            // Get or create villager data
            VillagerData villagerData = VillagerDataManager.getOrCreateVillagerData(villager);
            
            // Store the old name for logging
            String oldName = villagerData.getEffectiveName();
            
            // Update the custom name
            villagerData.setCustomName(newName);
            
            // Update gender based on the new name if it's recognizable
            Gender detectedGender = NameGenerator.detectGender(newName);
            if (detectedGender != Gender.UNKNOWN) {
                Gender oldGender = villagerData.getGender();
                villagerData.setGender(detectedGender);
                
                if (VoiceConfig.DEBUG_MODE.get() && oldGender != detectedGender) {
                    LOGGER.debug("Updated villager {} gender from {} to {} based on new name '{}'", 
                                villager.getUUID(), oldGender, detectedGender, newName);
                }
            }
            
            // Save the updated data
            VillagerDataManager.updateVillagerData(villagerData);
            
            if (VoiceConfig.DEBUG_MODE.get()) {
                LOGGER.debug("Successfully renamed villager {} from '{}' to '{}'", 
                            villager.getUUID(), oldName, newName);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to handle name tag interaction for villager {}", villager.getUUID(), e);
        }
    }
    
    /**
     * Checks if a villager can be renamed by the given player.
     * This method can be extended to add permission checks or other restrictions.
     *
     * @param villager The villager to be renamed
     * @param player The player attempting to rename
     * @return true if the villager can be renamed, false otherwise
     */
    public static boolean canRenameVillager(Villager villager, Player player) {
        // Basic checks
        if (villager == null || player == null) {
            return false;
        }
        
        // Check if the villager is alive
        if (!villager.isAlive()) {
            return false;
        }
        
        // Check if the player is in creative or has appropriate permissions
        // For now, allow all players to rename villagers
        return true;
    }
    
    /**
     * Validates a name tag name for use with villagers.
     *
     * @param name The name to validate
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidVillagerName(String name) {
        if (name == null) {
            return false;
        }
        
        String trimmedName = name.trim();
        
        // Check if name is empty
        if (trimmedName.isEmpty()) {
            return false;
        }
        
        // Check name length (reasonable limits)
        if (trimmedName.length() > 32) {
            return false;
        }
        
        // Check for invalid characters (basic validation)
        // Allow letters, numbers, spaces, and common punctuation
        if (!trimmedName.matches("^[a-zA-Z0-9\\s\\-_'.]+$")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the display name that should be shown for a villager.
     * This prioritizes custom names over original generated names.
     *
     * @param villager The villager entity
     * @return The display name, or null if no name is available
     */
    public static String getVillagerDisplayName(Villager villager) {
        if (villager == null) {
            return null;
        }
        
        // First check if the villager has a vanilla custom name
        if (villager.hasCustomName()) {
            return villager.getCustomName().getString();
        }
        
        // Fall back to our mod's villager data
        VillagerData villagerData = VillagerDataManager.getVillagerData(villager.getUUID());
        if (villagerData != null) {
            return villagerData.getEffectiveName();
        }
        
        return null;
    }
    
    /**
     * Synchronizes a villager's vanilla custom name with our mod's data.
     * This ensures consistency between Minecraft's name system and our data.
     *
     * @param villager The villager to synchronize
     */
    public static void synchronizeVillagerName(Villager villager) {
        if (villager == null) {
            return;
        }
        
        try {
            VillagerData villagerData = VillagerDataManager.getVillagerData(villager.getUUID());
            if (villagerData == null) {
                return;
            }
            
            // Check if vanilla name and our data are out of sync
            String vanillaName = villager.hasCustomName() ? villager.getCustomName().getString() : null;
            String ourName = villagerData.getCustomName();
            
            // If they differ, update our data to match vanilla
            if (vanillaName != null && !vanillaName.equals(ourName)) {
                villagerData.setCustomName(vanillaName);
                
                // Update gender if the new name is recognizable
                Gender detectedGender = NameGenerator.detectGender(vanillaName);
                if (detectedGender != Gender.UNKNOWN) {
                    villagerData.setGender(detectedGender);
                }
                
                VillagerDataManager.updateVillagerData(villagerData);
                
                if (VoiceConfig.DEBUG_MODE.get()) {
                    LOGGER.debug("Synchronized villager {} name to '{}'", villager.getUUID(), vanillaName);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to synchronize villager name for {}", villager.getUUID(), e);
        }
    }
}