package com.foogly.voiceofthevillage.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles ReputationUpdatePacket on the client side.
 * Processes reputation changes and manages client-side reputation displays and effects.
 */
public class ReputationUpdatePacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReputationUpdatePacketHandler.class);

    /**
     * Handles reputation update packets received from the server.
     *
     * @param packet  The reputation update packet
     * @param context The payload context
     */
    public static void handleOnClient(ReputationUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            
            if (player == null) {
                LOGGER.warn("Received ReputationUpdatePacket but no local player exists");
                return;
            }

            // Validate packet
            if (!packet.isValid()) {
                LOGGER.warn("Received invalid ReputationUpdatePacket");
                return;
            }

            // Check if the packet is for the current player
            if (!packet.playerId().equals(player.getUUID())) {
                LOGGER.debug("Received ReputationUpdatePacket for different player, ignoring");
                return;
            }

            // Log reputation change
            String changeType = packet.isImprovement() ? "improved" : 
                               packet.isDeterioration() ? "worsened" : "unchanged";
            
            LOGGER.debug("Reputation {} with villager {} ({}): {} -> {} (change: {}{}, threshold: {} -> {})", 
                        changeType,
                        packet.villagerName(),
                        packet.villagerId(),
                        packet.newScore() - packet.scoreChange(),
                        packet.newScore(),
                        packet.scoreChange() >= 0 ? "+" : "",
                        packet.scoreChange(),
                        packet.previousThreshold(),
                        packet.newThreshold());

            // Log threshold changes
            if (packet.hasThresholdChanged()) {
                LOGGER.info("Reputation threshold changed with villager {}: {} -> {}", 
                           packet.villagerName(),
                           packet.previousThreshold(),
                           packet.newThreshold());
            }

            // TODO: Process the reputation update
            // This will be implemented in later tasks:
            // 1. Update client-side reputation displays (if any)
            // 2. Show reputation change notifications to the player
            // 3. Trigger visual effects for significant reputation changes
            // 4. Update villager name tag colors based on reputation threshold
            // 5. Play sound effects for reputation changes
            
            LOGGER.info("Reputation update processing not yet implemented - packet received and validated");
        });
    }
}