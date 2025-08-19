package com.foogly.voiceofthevillage;

import com.foogly.voiceofthevillage.input.PushToTalkHandler;
import com.foogly.voiceofthevillage.input.SpeakingIndicator;
import com.foogly.voiceofthevillage.interaction.SimpleModeCommunicationHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = VoiceOfTheVillage.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = VoiceOfTheVillage.MODID, value = Dist.CLIENT)
public class VoiceOfTheVillageClient {
    public VoiceOfTheVillageClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        VoiceOfTheVillage.LOGGER.info("HELLO FROM CLIENT SETUP");
        VoiceOfTheVillage.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        
        // Register simple mode communication handler
        NeoForge.EVENT_BUS.register(SimpleModeCommunicationHandler.class);
        VoiceOfTheVillage.LOGGER.info("Registered SimpleModeCommunicationHandler for client-side villager interactions");
        
        // Note: PushToTalkHandler and SpeakingIndicator are automatically registered via @EventBusSubscriber
        VoiceOfTheVillage.LOGGER.info("Advanced mode input handlers registered automatically via @EventBusSubscriber");
    }
}
