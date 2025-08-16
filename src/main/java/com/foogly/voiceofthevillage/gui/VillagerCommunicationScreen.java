package com.foogly.voiceofthevillage.gui;

import com.foogly.voiceofthevillage.audio.VoiceRecordingManager;
import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.network.TextMessagePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Extended villager trading screen that includes communication functionality.
 * Provides text input, voice recording, and conversation history display.
 */
public class VillagerCommunicationScreen extends MerchantScreen implements VoiceRecordingManager.VoiceRecordingCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerCommunicationScreen.class);
    
    // Communication panel dimensions
    private static final int COMMUNICATION_PANEL_WIDTH = 200;
    private static final int COMMUNICATION_PANEL_HEIGHT = 150;
    private static final int TEXT_INPUT_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 80;
    private static final int MARGIN = 5;
    
    // GUI components
    private EditBox messageInput;
    private Button sendButton;
    private Button voiceButton;
    private ConversationHistoryWidget conversationHistory;
    
    // Communication state
    private final Villager targetVillager;
    private boolean isRecording = false;
    private final List<ConversationEntry> conversationEntries = new ArrayList<>();
    
    public VillagerCommunicationScreen(MerchantMenu menu, Inventory playerInventory, Component title, Villager villager) {
        super(menu, playerInventory, title);
        this.targetVillager = villager;
        
        // Expand the screen width to accommodate communication panel
        this.imageWidth += COMMUNICATION_PANEL_WIDTH;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Calculate communication panel position (right side of trading GUI)
        int panelX = this.leftPos + this.imageWidth - COMMUNICATION_PANEL_WIDTH - MARGIN;
        int panelY = this.topPos + MARGIN;
        
        // Initialize conversation history widget
        conversationHistory = new ConversationHistoryWidget(
            panelX, 
            panelY, 
            COMMUNICATION_PANEL_WIDTH - MARGIN * 2, 
            COMMUNICATION_PANEL_HEIGHT - TEXT_INPUT_HEIGHT - BUTTON_HEIGHT - MARGIN * 3,
            conversationEntries
        );
        addRenderableWidget(conversationHistory);
        
        // Initialize text input field
        int inputY = panelY + conversationHistory.getHeight() + MARGIN;
        messageInput = new EditBox(
            this.font, 
            panelX, 
            inputY, 
            COMMUNICATION_PANEL_WIDTH - MARGIN * 2, 
            TEXT_INPUT_HEIGHT, 
            Component.translatable("gui.voiceofthevillage.message_input")
        );
        messageInput.setMaxLength(500);
        messageInput.setHint(Component.translatable("gui.voiceofthevillage.type_message"));
        addRenderableWidget(messageInput);
        
        // Initialize buttons
        int buttonY = inputY + TEXT_INPUT_HEIGHT + MARGIN;
        
        // Send button
        sendButton = Button.builder(
            Component.translatable("gui.voiceofthevillage.send"),
            button -> sendTextMessage()
        )
        .bounds(panelX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
        .build();
        addRenderableWidget(sendButton);
        
        // Voice button (only if voice input is enabled)
        if (VoiceConfig.ENABLE_VOICE_INPUT.get()) {
            voiceButton = Button.builder(
                Component.translatable("gui.voiceofthevillage.voice"),
                button -> toggleVoiceRecording()
            )
            .bounds(panelX + BUTTON_WIDTH + MARGIN, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
            addRenderableWidget(voiceButton);
        }
        
        LOGGER.debug("Initialized VillagerCommunicationScreen for villager {}", 
                    targetVillager.getUUID());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render communication panel background
        renderCommunicationPanel(guiGraphics);
        
        // Update button states
        updateButtonStates();
    }
    
    /**
     * Renders the communication panel background and title.
     */
    private void renderCommunicationPanel(GuiGraphics guiGraphics) {
        int panelX = this.leftPos + this.imageWidth - COMMUNICATION_PANEL_WIDTH - MARGIN;
        int panelY = this.topPos + MARGIN;
        
        // Draw panel background
        guiGraphics.fill(
            panelX - MARGIN, 
            panelY - MARGIN, 
            panelX + COMMUNICATION_PANEL_WIDTH, 
            panelY + COMMUNICATION_PANEL_HEIGHT + MARGIN, 
            0x88000000
        );
        
        // Draw panel border
        guiGraphics.fill(
            panelX - MARGIN - 1, 
            panelY - MARGIN - 1, 
            panelX + COMMUNICATION_PANEL_WIDTH + 1, 
            panelY + COMMUNICATION_PANEL_HEIGHT + MARGIN + 1, 
            0xFF555555
        );
        
        // Draw title
        Component title = Component.translatable("gui.voiceofthevillage.communication");
        int titleX = panelX + (COMMUNICATION_PANEL_WIDTH - this.font.width(title)) / 2;
        guiGraphics.drawString(this.font, title, titleX, panelY - MARGIN + 2, 0xFFFFFF);
    }
    
    /**
     * Updates the state of GUI buttons based on current conditions.
     */
    private void updateButtonStates() {
        // Enable/disable send button based on message content
        if (sendButton != null) {
            sendButton.active = !messageInput.getValue().trim().isEmpty() && 
                               isVillagerInRange();
        }
        
        // Update voice button appearance if recording
        if (voiceButton != null) {
            if (isRecording) {
                voiceButton.setMessage(Component.translatable("gui.voiceofthevillage.recording"));
            } else {
                voiceButton.setMessage(Component.translatable("gui.voiceofthevillage.voice"));
            }
            voiceButton.active = isVillagerInRange();
        }
    }
    
    /**
     * Sends a text message to the villager.
     */
    private void sendTextMessage() {
        String message = messageInput.getValue().trim();
        if (message.isEmpty()) {
            return;
        }
        
        if (!isVillagerInRange()) {
            addConversationEntry(ConversationEntry.Type.SYSTEM, 
                Component.translatable("gui.voiceofthevillage.villager_too_far"));
            return;
        }
        
        // Add message to conversation history
        addConversationEntry(ConversationEntry.Type.PLAYER, Component.literal(message));
        
        // Create and send packet
        TextMessagePacket packet = new TextMessagePacket(
            targetVillager.getUUID(),
            minecraft.player.getUUID(),
            minecraft.player.getName().getString(),
            message,
            false, // Not a command
            System.currentTimeMillis()
        );
        
        PacketDistributor.sendToServer(packet);
        
        // Clear input field
        messageInput.setValue("");
        
        LOGGER.debug("Sent text message to villager {}: {}", 
                    targetVillager.getUUID(), message);
    }
    
    /**
     * Toggles voice recording state.
     */
    private void toggleVoiceRecording() {
        if (!isVillagerInRange()) {
            addConversationEntry(ConversationEntry.Type.SYSTEM, 
                Component.translatable("gui.voiceofthevillage.villager_too_far"));
            return;
        }
        
        if (isRecording) {
            stopVoiceRecording();
        } else {
            startVoiceRecording();
        }
    }
    
    /**
     * Starts voice recording.
     */
    private void startVoiceRecording() {
        VoiceRecordingManager manager = VoiceRecordingManager.getInstance();
        boolean started = manager.startRecording(targetVillager.getUUID(), this);
        
        if (started) {
            isRecording = true;
            LOGGER.debug("Started voice recording for villager {}", targetVillager.getUUID());
        } else {
            addConversationEntry(ConversationEntry.Type.SYSTEM, 
                Component.translatable("gui.voiceofthevillage.recording_failed"));
        }
    }
    
    /**
     * Stops voice recording and processes the audio.
     */
    private void stopVoiceRecording() {
        VoiceRecordingManager manager = VoiceRecordingManager.getInstance();
        manager.stopRecording();
        
        isRecording = false;
        LOGGER.debug("Stopped voice recording for villager {}", targetVillager.getUUID());
    }
    
    /**
     * Checks if the villager is within interaction range.
     */
    private boolean isVillagerInRange() {
        if (!VoiceConfig.isDistanceCheckEnabled()) {
            return true;
        }
        
        double distance = minecraft.player.distanceTo(targetVillager);
        double maxDistance = VoiceConfig.getEffectiveInteractionDistance();
        
        return distance <= maxDistance;
    }
    
    /**
     * Adds an entry to the conversation history.
     */
    public void addConversationEntry(ConversationEntry.Type type, Component message) {
        conversationEntries.add(new ConversationEntry(type, message, System.currentTimeMillis()));
        if (conversationHistory != null) {
            conversationHistory.scrollToBottom();
        }
    }
    
    /**
     * Handles villager responses received from the server.
     */
    public void handleVillagerResponse(Component response, boolean isVoice) {
        addConversationEntry(ConversationEntry.Type.VILLAGER, response);
        
        if (isVoice && VoiceConfig.ENABLE_VOICE_OUTPUT.get()) {
            // TODO: Play voice audio
            // This will be implemented in task 8.2 (text-to-speech system)
            LOGGER.debug("Would play voice response: {}", response.getString());
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle Enter key in message input
        if (messageInput.isFocused() && keyCode == 257) { // Enter key
            sendTextMessage();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void onClose() {
        // Stop any ongoing voice recording
        if (isRecording) {
            stopVoiceRecording();
        }
        
        super.onClose();
    }
    
    // VoiceRecordingCallback implementation
    
    @Override
    public void onRecordingStarted() {
        addConversationEntry(ConversationEntry.Type.SYSTEM, 
            Component.translatable("gui.voiceofthevillage.recording_started"));
    }
    
    @Override
    public void onRecordingStopped() {
        addConversationEntry(ConversationEntry.Type.SYSTEM, 
            Component.translatable("gui.voiceofthevillage.recording_stopped"));
    }
    
    @Override
    public void onRecordingProgress() {
        // Visual feedback during recording - could update button appearance
        // This is called frequently, so avoid heavy operations
    }
    
    @Override
    public void onProcessingComplete(Component result) {
        addConversationEntry(ConversationEntry.Type.SYSTEM, result);
    }
    
    @Override
    public void onRecordingError(Component error) {
        addConversationEntry(ConversationEntry.Type.SYSTEM, error);
    }
}