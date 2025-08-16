package com.foogly.voiceofthevillage.render;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.data.VillagerData;
import com.foogly.voiceofthevillage.data.VillagerDataManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Custom renderer for displaying villager names above their heads.
 * Handles distance-based visibility and supports both original and custom names.
 */
public class VillagerNameRenderer {
    
    private static final float NAME_TAG_HEIGHT_OFFSET = 0.5f;
    private static final int NAME_TAG_BACKGROUND_COLOR = 0x40000000; // Semi-transparent black
    private static final int NAME_TAG_TEXT_COLOR = 0xFFFFFFFF; // White text
    private static final float NAME_TAG_SCALE = 0.025f;
    
    /**
     * Renders a villager's name above their head if within configured distance.
     *
     * @param villager The villager entity
     * @param poseStack The pose stack for rendering transformations
     * @param bufferSource The buffer source for rendering
     * @param packedLight The packed light value
     * @param partialTick The partial tick for smooth rendering
     */
    public static void renderVillagerName(Villager villager, PoseStack poseStack, 
                                        MultiBufferSource bufferSource, int packedLight, 
                                        float partialTick) {
        
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null || villager == null) {
            return;
        }
        
        // Check if we should render the name based on distance
        if (!shouldRenderName(villager, player)) {
            return;
        }
        
        // Get the villager's effective name
        String villagerName = getVillagerDisplayName(villager);
        if (villagerName == null || villagerName.trim().isEmpty()) {
            return;
        }
        
        // Render the name tag
        renderNameTag(villagerName, villager, poseStack, bufferSource, packedLight, minecraft.font);
    }
    
    /**
     * Determines if the villager name should be rendered based on distance configuration.
     *
     * @param villager The villager entity
     * @param player The player
     * @return true if the name should be rendered, false otherwise
     */
    public static boolean shouldRenderName(Villager villager, Player player) {
        double nameTagDistance = VoiceConfig.NAME_TAG_DISTANCE.get();
        
        // If distance is 0 or negative, don't render names
        if (nameTagDistance <= 0.0) {
            return false;
        }
        
        // Calculate distance between player and villager
        Vec3 playerPos = player.getEyePosition();
        Vec3 villagerPos = villager.getEyePosition();
        double distance = playerPos.distanceTo(villagerPos);
        
        return distance <= nameTagDistance;
    }
    
    /**
     * Gets the display name for a villager, prioritizing custom names over original names.
     *
     * @param villager The villager entity
     * @return The name to display, or null if no name is available
     */
    public static String getVillagerDisplayName(Villager villager) {
        // First check if the villager has a custom name from a name tag
        if (villager.hasCustomName()) {
            Component customName = villager.getCustomName();
            if (customName != null) {
                return customName.getString();
            }
        }
        
        // Fall back to our mod's villager data
        VillagerData villagerData = VillagerDataManager.getVillagerData(villager.getUUID());
        if (villagerData != null) {
            return villagerData.getEffectiveName();
        }
        
        return null;
    }
    
    /**
     * Renders the actual name tag with background and text.
     *
     * @param name The name to render
     * @param villager The villager entity (for height calculation)
     * @param poseStack The pose stack for transformations
     * @param bufferSource The buffer source for rendering
     * @param packedLight The packed light value
     * @param font The font renderer
     */
    private static void renderNameTag(String name, Villager villager, PoseStack poseStack, 
                                    MultiBufferSource bufferSource, int packedLight, Font font) {
        
        poseStack.pushPose();
        
        // Position the name tag above the villager's head
        poseStack.translate(0.0, villager.getBbHeight() + NAME_TAG_HEIGHT_OFFSET, 0.0);
        
        // Make the name tag face the player
        EntityRenderDispatcher renderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        poseStack.mulPose(renderDispatcher.cameraOrientation());
        
        // Scale the name tag
        poseStack.scale(-NAME_TAG_SCALE, -NAME_TAG_SCALE, NAME_TAG_SCALE);
        
        // Get text dimensions
        int textWidth = font.width(name);
        
        // Render the text with background
        Matrix4f matrix = poseStack.last().pose();
        
        // Draw the name with background
        font.drawInBatch(name, -textWidth / 2.0f, -4.0f, 
                        NAME_TAG_TEXT_COLOR, false, matrix, bufferSource, 
                        Font.DisplayMode.SEE_THROUGH, NAME_TAG_BACKGROUND_COLOR, packedLight);
        
        poseStack.popPose();
    }
    
    /**
     * Calculates the squared distance between a player and villager for performance.
     *
     * @param villager The villager entity
     * @param player The player
     * @return The squared distance between the entities
     */
    public static double getSquaredDistance(Villager villager, Player player) {
        Vec3 playerPos = player.getEyePosition();
        Vec3 villagerPos = villager.getEyePosition();
        return playerPos.distanceToSqr(villagerPos);
    }
    
    /**
     * Checks if a villager is within the configured name tag distance using squared distance for performance.
     *
     * @param villager The villager entity
     * @param player The player
     * @return true if within distance, false otherwise
     */
    public static boolean isWithinNameTagDistance(Villager villager, Player player) {
        double nameTagDistance = VoiceConfig.NAME_TAG_DISTANCE.get();
        
        if (nameTagDistance <= 0.0) {
            return false;
        }
        
        double squaredDistance = getSquaredDistance(villager, player);
        double squaredNameTagDistance = nameTagDistance * nameTagDistance;
        
        return squaredDistance <= squaredNameTagDistance;
    }
}