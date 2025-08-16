package com.foogly.voiceofthevillage.interaction;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.gui.VillagerCommunicationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles simple mode communication interactions with villagers.
 * Manages right-click interactions to open the communication-enabled trading GUI.
 */
public class SimpleModeCommunicationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleModeCommunicationHandler.class);
    
    /**
     * Handles right-click interactions with villagers in simple mode.
     * Opens the communication-enabled trading GUI instead of the default trading screen.
     */
    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        // Only handle client-side interactions
        if (event.getLevel().isClientSide()) {
            handleClientSideInteraction(event);
        }
    }
    
    /**
     * Handles client-side villager interactions for simple mode.
     */
    private static void handleClientSideInteraction(PlayerInteractEvent.EntityInteract event) {
        // Check if we're in simple mode
        if (VoiceConfig.ADVANCED_MODE.get()) {
            return; // Advanced mode uses different interaction method
        }
        
        // Check if the target is a villager
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        
        // Check if player is using main hand
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        
        Player player = event.getEntity();
        
        // Validate distance
        if (!isVillagerInRange(player, villager)) {
            player.displayClientMessage(
                Component.translatable("message.voiceofthevillage.villager_too_far"), 
                true
            );
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        
        // Check if villager has trades available
        if (!villager.getOffers().isEmpty()) {
            // Open communication-enabled trading GUI
            openCommunicationTradingGUI(villager, player);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            
            LOGGER.debug("Opened communication trading GUI for villager {} and player {}", 
                        villager.getUUID(), player.getUUID());
        } else {
            // Villager has no trades, just show communication interface
            // This could be expanded to show a communication-only GUI in the future
            player.displayClientMessage(
                Component.translatable("message.voiceofthevillage.villager_no_trades"), 
                false
            );
        }
    }
    
    /**
     * Opens the communication-enabled trading GUI.
     */
    private static void openCommunicationTradingGUI(Villager villager, Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // Create a merchant menu for the villager
        MerchantMenu merchantMenu = new MerchantMenu(
            minecraft.player.containerMenu.containerId + 1,
            minecraft.player.getInventory(),
            villager
        );
        
        // Create and open the communication screen
        VillagerCommunicationScreen screen = new VillagerCommunicationScreen(
            merchantMenu,
            minecraft.player.getInventory(),
            villager.getDisplayName(),
            villager
        );
        
        minecraft.setScreen(screen);
    }
    
    /**
     * Checks if the villager is within interaction range.
     */
    private static boolean isVillagerInRange(Player player, Villager villager) {
        if (!VoiceConfig.isDistanceCheckEnabled()) {
            return true;
        }
        
        double distance = player.distanceTo(villager);
        double maxDistance = VoiceConfig.getEffectiveInteractionDistance();
        
        boolean inRange = distance <= maxDistance;
        
        if (!inRange) {
            LOGGER.debug("Villager {} is out of range: {} > {}", 
                        villager.getUUID(), distance, maxDistance);
        }
        
        return inRange;
    }
    
    /**
     * Validates if simple mode communication is available.
     */
    public static boolean isSimpleModeAvailable() {
        return !VoiceConfig.ADVANCED_MODE.get();
    }
    
    /**
     * Gets the effective interaction distance for simple mode.
     */
    public static double getInteractionDistance() {
        return VoiceConfig.getEffectiveInteractionDistance();
    }
}