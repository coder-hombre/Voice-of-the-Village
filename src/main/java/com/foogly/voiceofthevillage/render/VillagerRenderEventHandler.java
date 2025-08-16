package com.foogly.voiceofthevillage.render;

import net.minecraft.world.entity.npc.Villager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * Event handler for villager rendering events.
 * Handles custom name tag rendering for villagers.
 */
@EventBusSubscriber(modid = "voiceofthevillage", value = Dist.CLIENT)
public class VillagerRenderEventHandler {
    
    /**
     * Handles the render name tag event for villagers.
     * Renders custom name tags for villagers when appropriate.
     *
     * @param event The render name tag event
     */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        // Only handle villager entities
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        
        // Use our custom name renderer
        VillagerNameRenderer.renderVillagerName(
            villager,
            event.getPoseStack(),
            event.getMultiBufferSource(),
            event.getPackedLight(),
            event.getPartialTick()
        );
    }
}