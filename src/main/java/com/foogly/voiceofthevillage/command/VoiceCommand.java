package com.foogly.voiceofthevillage.command;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.data.VillagerDataManager;
import com.foogly.voiceofthevillage.network.TextMessagePacket;
import com.foogly.voiceofthevillage.util.VillagerTargeting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the "/voice <villagerName> 'message'" command for advanced mode text communication.
 * Provides villager name validation, proximity checking, and message parsing.
 */
public class VoiceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceCommand.class);
    
    // Pattern to match quoted messages: 'message' or "message"
    private static final Pattern QUOTED_MESSAGE_PATTERN = Pattern.compile("^['\"](.+)['\"]$");
    
    // Maximum message length to prevent spam
    private static final int MAX_MESSAGE_LENGTH = 500;
    
    // Maximum villager name length for validation
    private static final int MAX_VILLAGER_NAME_LENGTH = 50;
    
    /**
     * Registers the voice command with the command dispatcher.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("voice")
                .then(Commands.argument("villagerName", StringArgumentType.word())
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(VoiceCommand::executeVoiceCommand)))
        );
        
        LOGGER.debug("Registered /voice command for advanced mode communication");
    }
    
    /**
     * Executes the voice command.
     */
    private static int executeVoiceCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        // Ensure the command is executed by a player
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.voiceofthevillage.voice.player_only"));
            return 0;
        }
        
        // Check if advanced mode is enabled
        if (!VoiceConfig.ADVANCED_MODE.get()) {
            player.sendSystemMessage(Component.translatable("command.voiceofthevillage.voice.advanced_mode_only"));
            return 0;
        }
        
        String villagerName = StringArgumentType.getString(context, "villagerName");
        String rawMessage = StringArgumentType.getString(context, "message");
        
        try {
            // Validate and parse the command
            VoiceCommandData commandData = parseVoiceCommand(villagerName, rawMessage);
            
            // Execute the command
            return executeVoiceMessage(player, commandData);
            
        } catch (VoiceCommandException e) {
            player.sendSystemMessage(Component.translatable(e.getTranslationKey(), e.getArgs()));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected error executing voice command", e);
            player.sendSystemMessage(Component.translatable("command.voiceofthevillage.voice.error"));
            return 0;
        }
    }
    
    /**
     * Parses and validates the voice command arguments.
     */
    private static VoiceCommandData parseVoiceCommand(String villagerName, String rawMessage) throws VoiceCommandException {
        // Validate villager name
        if (villagerName == null || villagerName.trim().isEmpty()) {
            throw new VoiceCommandException("command.voiceofthevillage.voice.empty_villager_name");
        }
        
        if (villagerName.length() > MAX_VILLAGER_NAME_LENGTH) {
            throw new VoiceCommandException("command.voiceofthevillage.voice.villager_name_too_long", 
                                          MAX_VILLAGER_NAME_LENGTH);
        }
        
        // Validate and parse message
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new VoiceCommandException("command.voiceofthevillage.voice.empty_message");
        }
        
        String message = parseQuotedMessage(rawMessage);
        
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new VoiceCommandException("command.voiceofthevillage.voice.message_too_long", 
                                          MAX_MESSAGE_LENGTH);
        }
        
        return new VoiceCommandData(villagerName.trim(), message);
    }
    
    /**
     * Parses a quoted message, removing quotes if present.
     */
    private static String parseQuotedMessage(String rawMessage) throws VoiceCommandException {
        String trimmed = rawMessage.trim();
        
        // Check if message is properly quoted
        Matcher matcher = QUOTED_MESSAGE_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return matcher.group(1); // Return content without quotes
        }
        
        // If not quoted, check if it contains quotes that might indicate malformed input
        if (trimmed.contains("'") || trimmed.contains("\"")) {
            throw new VoiceCommandException("command.voiceofthevillage.voice.malformed_quotes");
        }
        
        // Return as-is if no quotes are involved
        return trimmed;
    }
    
    /**
     * Executes the voice message command.
     */
    private static int executeVoiceMessage(ServerPlayer player, VoiceCommandData commandData) throws VoiceCommandException {
        // Find the target villager
        Villager targetVillager = findTargetVillager(player, commandData.villagerName());
        
        if (targetVillager == null) {
            throw new VoiceCommandException("command.voiceofthevillage.voice.villager_not_found", 
                                          commandData.villagerName());
        }
        
        // Check proximity
        double distance = player.distanceTo(targetVillager);
        double maxDistance = VoiceConfig.getEffectiveInteractionDistance();
        
        if (VoiceConfig.isDistanceCheckEnabled() && distance > maxDistance) {
            throw new VoiceCommandException("command.voiceofthevillage.voice.villager_too_far", 
                                          commandData.villagerName(), 
                                          String.format("%.1f", distance), 
                                          String.format("%.1f", maxDistance));
        }
        
        // Send the message
        sendVoiceMessage(player, targetVillager, commandData.message());
        
        // Confirm to player
        player.sendSystemMessage(Component.translatable("command.voiceofthevillage.voice.message_sent", 
                                                       commandData.villagerName()));
        
        LOGGER.debug("Player {} sent voice message to villager {}: {}", 
                    player.getName().getString(), commandData.villagerName(), commandData.message());
        
        return 1; // Success
    }
    
    /**
     * Finds a villager by name within interaction range.
     */
    private static Villager findTargetVillager(ServerPlayer player, String villagerName) {
        // Get all targetable villagers
        List<Villager> nearbyVillagers = player.level().getEntitiesOfClass(
            Villager.class,
            player.getBoundingBox().inflate(VoiceConfig.getEffectiveInteractionDistance()),
            villager -> villager.isAlive() && !villager.isBaby()
        );
        
        VillagerDataManager dataManager = VillagerDataManager.getInstance();
        
        // Search for villager by name (both original and custom names)
        for (Villager villager : nearbyVillagers) {
            UUID villagerUUID = villager.getUUID();
            
            // Check original generated name
            String originalName = dataManager.getVillagerName(villagerUUID);
            if (villagerName.equalsIgnoreCase(originalName)) {
                return villager;
            }
            
            // Check custom name from name tag
            String customName = dataManager.getCustomName(villagerUUID);
            if (customName != null && villagerName.equalsIgnoreCase(customName)) {
                return villager;
            }
            
            // Also check if the villager has a display name that matches
            if (villager.hasCustomName()) {
                String displayName = villager.getCustomName().getString();
                if (villagerName.equalsIgnoreCase(displayName)) {
                    return villager;
                }
            }
        }
        
        return null; // No matching villager found
    }
    
    /**
     * Sends a voice message to the specified villager.
     */
    private static void sendVoiceMessage(ServerPlayer player, Villager villager, String message) {
        // Create and send text message packet
        TextMessagePacket packet = new TextMessagePacket(
            villager.getUUID(),
            player.getUUID(),
            player.getName().getString(),
            message,
            true, // isCommand = true for voice commands
            System.currentTimeMillis()
        );
        
        // Send to server for processing (this will be handled by the existing TextMessagePacketHandler)
        PacketDistributor.sendToServer(packet);
        
        LOGGER.debug("Sent text message packet for villager {} from player {}", 
                    villager.getUUID(), player.getName().getString());
    }
    
    /**
     * Data class for parsed voice command information.
     */
    private record VoiceCommandData(String villagerName, String message) {}
    
    /**
     * Exception class for voice command parsing and validation errors.
     */
    private static class VoiceCommandException extends Exception {
        private final String translationKey;
        private final Object[] args;
        
        public VoiceCommandException(String translationKey, Object... args) {
            super(translationKey);
            this.translationKey = translationKey;
            this.args = args;
        }
        
        public String getTranslationKey() {
            return translationKey;
        }
        
        public Object[] getArgs() {
            return args;
        }
    }
}