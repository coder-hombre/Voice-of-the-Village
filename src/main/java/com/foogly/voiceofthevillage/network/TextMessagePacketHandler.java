package com.foogly.voiceofthevillage.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles TextMessagePacket on the server side.
 * Processes text messages from clients and manages rate limiting.
 */
public class TextMessagePacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextMessagePacketHandler.class);
    
    // Rate limiting: max 5 text messages per second per player
    private static final long RATE_LIMIT_WINDOW_MS = 1000;
    private static final int MAX_PACKETS_PER_WINDOW = 5;
    
    // Track packet counts per player
    private static final ConcurrentHashMap<String, AtomicLong> packetCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> windowStartTimes = new ConcurrentHashMap<>();

    /**
     * Handles text message packets received from clients.
     *
     * @param packet  The text message packet
     * @param context The payload context
     */
    public static void handleOnServer(TextMessagePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            
            if (player == null) {
                LOGGER.warn("Received TextMessagePacket from null player");
                return;
            }

            // Validate packet
            if (!packet.isValid()) {
                LOGGER.warn("Received invalid TextMessagePacket from player {}", player.getName().getString());
                return;
            }

            // Check if the packet is from the correct player
            if (!packet.playerId().equals(player.getUUID())) {
                LOGGER.warn("Player {} sent TextMessagePacket with mismatched UUID", player.getName().getString());
                return;
            }

            // Rate limiting
            if (!isWithinRateLimit(player.getUUID().toString())) {
                LOGGER.warn("Player {} exceeded text message rate limit", player.getName().getString());
                return;
            }

            // Validate message content
            String trimmedMessage = packet.getTrimmedMessage();
            if (trimmedMessage.isEmpty()) {
                LOGGER.warn("Player {} sent empty text message", player.getName().getString());
                return;
            }

            // Log packet reception
            LOGGER.debug("Received text message from player {} for villager {}: \"{}\" (command: {})", 
                        player.getName().getString(), 
                        packet.villagerId(), 
                        trimmedMessage,
                        packet.isCommand());

            // TODO: Process the text message
            // This will be implemented in later tasks:
            // 1. Find the target villager by UUID
            // 2. Check distance between player and villager
            // 3. Generate AI response based on villager personality and memory
            // 4. Optionally convert response to audio using text-to-speech
            // 5. Send VillagerResponsePacket back to client
            
            LOGGER.info("Text message processing not yet implemented - packet received and validated");
        });
    }

    /**
     * Checks if the player is within the rate limit for text message packets.
     *
     * @param playerId The player's UUID as string
     * @return true if within rate limit, false otherwise
     */
    private static boolean isWithinRateLimit(String playerId) {
        long currentTime = System.currentTimeMillis();
        
        // Get or create packet count for this player
        AtomicLong count = packetCounts.computeIfAbsent(playerId, k -> new AtomicLong(0));
        Long windowStart = windowStartTimes.get(playerId);
        
        // Check if we need to reset the window
        if (windowStart == null || (currentTime - windowStart) >= RATE_LIMIT_WINDOW_MS) {
            windowStartTimes.put(playerId, currentTime);
            count.set(1);
            return true;
        }
        
        // Check if we're within the limit
        long currentCount = count.incrementAndGet();
        return currentCount <= MAX_PACKETS_PER_WINDOW;
    }

    /**
     * Cleans up rate limiting data for players who have disconnected.
     * This method should be called periodically to prevent memory leaks.
     *
     * @param playerId The player's UUID as string
     */
    public static void cleanupPlayerData(String playerId) {
        packetCounts.remove(playerId);
        windowStartTimes.remove(playerId);
    }
}