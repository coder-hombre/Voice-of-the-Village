package com.foogly.voiceofthevillage.input;

import com.foogly.voiceofthevillage.audio.VoiceRecordingManager;
import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.data.VillagerDataManager;
import com.foogly.voiceofthevillage.util.VillagerTargeting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Handles push-to-talk input for advanced mode villager communication.
 * Manages key binding, villager targeting, and voice recording coordination.
 */
@EventBusSubscriber(modid = "voiceofthevillage", value = Dist.CLIENT)
public class PushToTalkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushToTalkHandler.class);
    
    private static KeyMapping pushToTalkKey;
    private static boolean isPushToTalkActive = false;
    private static Villager targetedVillager = null;
    private static UUID recordingVillagerUUID = null;
    
    /**
     * Registers the push-to-talk key mapping.
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // Create key mapping from config
        String keyName = VoiceConfig.getPushToTalkKey();
        int keyCode = parseKeyName(keyName);
        
        pushToTalkKey = new KeyMapping(
            "key.voiceofthevillage.push_to_talk",
            keyCode,
            "key.categories.voiceofthevillage"
        );
        
        event.register(pushToTalkKey);
        LOGGER.debug("Registered push-to-talk key mapping: {}", keyName);
    }
    
    /**
     * Handles key input events for push-to-talk functionality.
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!VoiceConfig.ADVANCED_MODE.get() || !VoiceConfig.ENABLE_VOICE_INPUT.get()) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return; // Don't handle input when GUI is open
        }
        
        if (pushToTalkKey != null && event.getKey() == pushToTalkKey.getKey().getValue()) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                handlePushToTalkPressed();
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                handlePushToTalkReleased();
            }
        }
    }
    
    /**
     * Handles push-to-talk key press.
     */
    private static void handlePushToTalkPressed() {
        if (isPushToTalkActive) {
            return; // Already active
        }
        
        // Find targeted villager
        targetedVillager = VillagerTargeting.getTargetedVillager();
        if (targetedVillager == null) {
            // No villager in range or line of sight
            showMessage(Component.translatable("message.voiceofthevillage.no_villager_targeted"));
            return;
        }
        
        // Check distance
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        
        double distance = minecraft.player.distanceTo(targetedVillager);
        double maxDistance = VoiceConfig.getEffectiveInteractionDistance();
        
        if (VoiceConfig.isDistanceCheckEnabled() && distance > maxDistance) {
            showMessage(Component.translatable("message.voiceofthevillage.villager_too_far", 
                String.format("%.1f", distance), String.format("%.1f", maxDistance)));
            return;
        }
        
        // Get villager data
        UUID villagerUUID = targetedVillager.getUUID();
        String villagerName = VillagerDataManager.getInstance().getVillagerName(villagerUUID);
        
        // Start voice recording
        VoiceRecordingManager.VoiceRecordingCallback callback = new PushToTalkRecordingCallback(villagerName);
        boolean recordingStarted = VoiceRecordingManager.getInstance().startRecording(villagerUUID, callback);
        
        if (recordingStarted) {
            isPushToTalkActive = true;
            recordingVillagerUUID = villagerUUID;
            
            // Show speaking indicator
            SpeakingIndicator.getInstance().showSpeakingIndicator(villagerName);
            
            LOGGER.debug("Started push-to-talk recording for villager {} ({})", villagerName, villagerUUID);
        } else {
            showMessage(Component.translatable("message.voiceofthevillage.recording_unavailable"));
        }
    }
    
    /**
     * Handles push-to-talk key release.
     */
    private static void handlePushToTalkReleased() {
        if (!isPushToTalkActive) {
            return; // Not currently active
        }
        
        // Stop voice recording
        VoiceRecordingManager.getInstance().stopRecording();
        
        // Hide speaking indicator
        SpeakingIndicator.getInstance().hideSpeakingIndicator();
        
        isPushToTalkActive = false;
        targetedVillager = null;
        recordingVillagerUUID = null;
        
        LOGGER.debug("Stopped push-to-talk recording");
    }
    
    /**
     * Parses a key name string to GLFW key code.
     */
    private static int parseKeyName(String keyName) {
        // Handle common key formats
        if (keyName.startsWith("key.keyboard.")) {
            String keyChar = keyName.substring("key.keyboard.".length()).toLowerCase();
            
            // Handle single character keys
            if (keyChar.length() == 1) {
                char c = keyChar.charAt(0);
                if (c >= 'a' && c <= 'z') {
                    return GLFW.GLFW_KEY_A + (c - 'a');
                }
                if (c >= '0' && c <= '9') {
                    return GLFW.GLFW_KEY_0 + (c - '0');
                }
            }
            
            // Handle special keys
            switch (keyChar) {
                case "space": return GLFW.GLFW_KEY_SPACE;
                case "enter": return GLFW.GLFW_KEY_ENTER;
                case "tab": return GLFW.GLFW_KEY_TAB;
                case "left.shift": return GLFW.GLFW_KEY_LEFT_SHIFT;
                case "right.shift": return GLFW.GLFW_KEY_RIGHT_SHIFT;
                case "left.control": return GLFW.GLFW_KEY_LEFT_CONTROL;
                case "right.control": return GLFW.GLFW_KEY_RIGHT_CONTROL;
                case "left.alt": return GLFW.GLFW_KEY_LEFT_ALT;
                case "right.alt": return GLFW.GLFW_KEY_RIGHT_ALT;
                default:
                    LOGGER.warn("Unknown key name: {}, defaulting to V", keyName);
                    return GLFW.GLFW_KEY_V;
            }
        }
        
        // Default to V key if parsing fails
        LOGGER.warn("Could not parse key name: {}, defaulting to V", keyName);
        return GLFW.GLFW_KEY_V;
    }
    
    /**
     * Shows a message to the player.
     */
    private static void showMessage(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }
    
    /**
     * Gets the currently targeted villager.
     */
    public static Villager getTargetedVillager() {
        return targetedVillager;
    }
    
    /**
     * Checks if push-to-talk is currently active.
     */
    public static boolean isPushToTalkActive() {
        return isPushToTalkActive;
    }
    
    /**
     * Gets the UUID of the villager currently being recorded.
     */
    public static UUID getRecordingVillagerUUID() {
        return recordingVillagerUUID;
    }
    
    /**
     * Callback implementation for push-to-talk voice recording.
     */
    private static class PushToTalkRecordingCallback implements VoiceRecordingManager.VoiceRecordingCallback {
        private final String villagerName;
        
        public PushToTalkRecordingCallback(String villagerName) {
            this.villagerName = villagerName;
        }
        
        @Override
        public void onRecordingStarted() {
            showMessage(Component.translatable("message.voiceofthevillage.recording_started", villagerName));
        }
        
        @Override
        public void onRecordingStopped() {
            showMessage(Component.translatable("message.voiceofthevillage.recording_stopped"));
        }
        
        @Override
        public void onRecordingProgress() {
            // Visual feedback is handled by SpeakingIndicator
        }
        
        @Override
        public void onProcessingComplete(Component result) {
            showMessage(Component.translatable("message.voiceofthevillage.message_sent", villagerName));
        }
        
        @Override
        public void onRecordingError(Component error) {
            showMessage(error);
            SpeakingIndicator.getInstance().hideSpeakingIndicator();
        }
    }
}