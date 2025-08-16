package com.foogly.voiceofthevillage.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles VillagerResponsePacket on the client side.
 * Processes responses from villagers and manages audio playback and UI updates.
 */
public class VillagerResponsePacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerResponsePacketHandler.class);

    /**
     * Handles villager response packets received from the server.
     *
     * @param packet  The villager response packet
     * @param context The payload context
     */
    public static void handleOnClient(VillagerResponsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            
            if (player == null) {
                LOGGER.warn("Received VillagerResponsePacket but no local player exists");
                return;
            }

            // Validate packet
            if (!packet.isValid()) {
                LOGGER.warn("Received invalid VillagerResponsePacket");
                return;
            }

            // Check if the packet is for the current player
            if (!packet.playerId().equals(player.getUUID())) {
                LOGGER.debug("Received VillagerResponsePacket for different player, ignoring");
                return;
            }

            // Log packet reception
            LOGGER.debug("Received response from villager {} ({}): \"{}\" (hasAudio: {}, audioSize: {}KB)", 
                        packet.villagerName(),
                        packet.villagerId(), 
                        packet.getTrimmedTextResponse(),
                        packet.hasAudio(),
                        packet.getAudioDataSize() / 1024);

            // TODO: Process the villager response
            // This will be implemented in later tasks:
            // 1. Display the text response in the appropriate UI (chat, GUI, or overlay)
            // 2. If hasAudio is true, play the audio data using the audio system
            // 3. Update any conversation history displays
            // 4. Trigger any visual effects or animations for the speaking villager
            
            LOGGER.info("Villager response processing not yet implemented - packet received and validated");
        });
    }
}