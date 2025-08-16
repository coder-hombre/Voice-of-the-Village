package com.foogly.voiceofthevillage.input;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the speaking indicator overlay that appears in the bottom left of the screen
 * when the player is using push-to-talk to communicate with a villager.
 */
@EventBusSubscriber(modid = "voiceofthevillage", value = Dist.CLIENT)
public class SpeakingIndicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeakingIndicator.class);
    
    private static final SpeakingIndicator INSTANCE = new SpeakingIndicator();
    
    // Visual constants
    private static final int INDICATOR_WIDTH = 200;
    private static final int INDICATOR_HEIGHT = 40;
    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 10;
    
    // Colors (ARGB format)
    private static final int BACKGROUND_COLOR = 0x80000000; // Semi-transparent black
    private static final int BORDER_COLOR = 0xFF4CAF50; // Green border
    private static final int TEXT_COLOR = 0xFFFFFFFF; // White text
    private static final int PULSE_COLOR = 0xFF8BC34A; // Light green for pulse effect
    
    // Animation constants
    private static final float PULSE_SPEED = 0.1f;
    private static final float MIN_ALPHA = 0.3f;
    private static final float MAX_ALPHA = 1.0f;
    
    // State
    private boolean isVisible = false;
    private String villagerName = "";
    private long startTime = 0;
    private float pulsePhase = 0.0f;
    
    private SpeakingIndicator() {
        // Private constructor for singleton
    }
    
    public static SpeakingIndicator getInstance() {
        return INSTANCE;
    }
    
    /**
     * Shows the speaking indicator for the specified villager.
     */
    public void showSpeakingIndicator(String villagerName) {
        this.isVisible = true;
        this.villagerName = villagerName != null ? villagerName : "Unknown Villager";
        this.startTime = System.currentTimeMillis();
        this.pulsePhase = 0.0f;
        
        LOGGER.debug("Showing speaking indicator for villager: {}", this.villagerName);
    }
    
    /**
     * Hides the speaking indicator.
     */
    public void hideSpeakingIndicator() {
        this.isVisible = false;
        this.villagerName = "";
        this.startTime = 0;
        this.pulsePhase = 0.0f;
        
        LOGGER.debug("Hiding speaking indicator");
    }
    
    /**
     * Checks if the speaking indicator is currently visible.
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * Gets the name of the villager being spoken to.
     */
    public String getVillagerName() {
        return villagerName;
    }
    
    /**
     * Renders the speaking indicator overlay.
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!INSTANCE.isVisible) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return; // Don't render when GUI is open
        }
        
        INSTANCE.render(event.getGuiGraphics());
    }
    
    /**
     * Renders the speaking indicator.
     */
    private void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // Calculate position (bottom left with margins)
        int x = MARGIN_X;
        int y = screenHeight - INDICATOR_HEIGHT - MARGIN_Y;
        
        // Update pulse animation
        updatePulseAnimation();
        
        // Calculate pulse alpha
        float pulseAlpha = MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * 
                          (0.5f + 0.5f * (float) Math.sin(pulsePhase));
        
        // Render background with pulse effect
        int backgroundColor = interpolateColor(BACKGROUND_COLOR, PULSE_COLOR, pulseAlpha * 0.3f);
        guiGraphics.fill(x, y, x + INDICATOR_WIDTH, y + INDICATOR_HEIGHT, backgroundColor);
        
        // Render border
        renderBorder(guiGraphics, x, y, INDICATOR_WIDTH, INDICATOR_HEIGHT, BORDER_COLOR);
        
        // Render microphone icon (simple rectangle for now)
        renderMicrophoneIcon(guiGraphics, x + 8, y + 8, pulseAlpha);
        
        // Render text
        renderText(guiGraphics, x + 35, y);
    }
    
    /**
     * Updates the pulse animation phase.
     */
    private void updatePulseAnimation() {
        pulsePhase += PULSE_SPEED;
        if (pulsePhase > Math.PI * 2) {
            pulsePhase -= Math.PI * 2;
        }
    }
    
    /**
     * Renders a border around the indicator.
     */
    private void renderBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // Top border
        guiGraphics.fill(x, y, x + width, y + 1, color);
        // Bottom border
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        // Left border
        guiGraphics.fill(x, y, x + 1, y + height, color);
        // Right border
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }
    
    /**
     * Renders a simple microphone icon.
     */
    private void renderMicrophoneIcon(GuiGraphics guiGraphics, int x, int y, float alpha) {
        int iconSize = 24;
        int micColor = interpolateColor(0xFF666666, BORDER_COLOR, alpha);
        
        // Microphone body (rectangle)
        guiGraphics.fill(x + 8, y + 4, x + 16, y + 16, micColor);
        
        // Microphone stand (line)
        guiGraphics.fill(x + 11, y + 16, x + 13, y + 20, micColor);
        
        // Base (small rectangle)
        guiGraphics.fill(x + 6, y + 20, x + 18, y + 22, micColor);
        
        // Sound waves (optional decorative elements)
        if (alpha > 0.7f) {
            int waveColor = interpolateColor(0x00FFFFFF, 0x80FFFFFF, alpha - 0.7f);
            // Small arcs to represent sound waves
            guiGraphics.fill(x + 18, y + 8, x + 20, y + 10, waveColor);
            guiGraphics.fill(x + 20, y + 6, x + 22, y + 8, waveColor);
            guiGraphics.fill(x + 20, y + 10, x + 22, y + 12, waveColor);
        }
    }
    
    /**
     * Renders the text content of the indicator.
     */
    private void renderText(GuiGraphics guiGraphics, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // Main text
        Component speakingText = Component.translatable("gui.voiceofthevillage.speaking_to", villagerName);
        guiGraphics.drawString(minecraft.font, speakingText, x, y + 6, TEXT_COLOR);
        
        // Instruction text
        Component instructionText = Component.translatable("gui.voiceofthevillage.release_to_send");
        guiGraphics.drawString(minecraft.font, instructionText, x, y + 18, 0xFFCCCCCC);
        
        // Duration indicator
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        Component durationText = Component.literal(String.format("%ds", duration));
        int durationWidth = minecraft.font.width(durationText);
        guiGraphics.drawString(minecraft.font, durationText, 
                             x + INDICATOR_WIDTH - durationWidth - 10, y + 6, 0xFFAAAAAA);
    }
    
    /**
     * Interpolates between two colors based on a factor.
     */
    private int interpolateColor(int color1, int color2, float factor) {
        factor = Math.max(0.0f, Math.min(1.0f, factor));
        
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}