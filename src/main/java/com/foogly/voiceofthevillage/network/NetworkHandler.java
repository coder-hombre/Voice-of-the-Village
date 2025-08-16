package com.foogly.voiceofthevillage.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Handles registration of all custom network packets for the Voice of the Village mod.
 * This class manages the registration of packet types and their handlers for both
 * client and server sides.
 */
public class NetworkHandler {

    /**
     * Registers all custom packets and their handlers.
     * This method should be called during the mod initialization phase.
     *
     * @param event The payload registration event
     */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Register VoiceInputPacket (Client -> Server)
        registrar.playToServer(
            VoiceInputPacket.TYPE,
            VoiceInputPacket.STREAM_CODEC,
            VoiceInputPacketHandler::handleOnServer
        );

        // Register TextMessagePacket (Client -> Server)
        registrar.playToServer(
            TextMessagePacket.TYPE,
            TextMessagePacket.STREAM_CODEC,
            TextMessagePacketHandler::handleOnServer
        );

        // Register VillagerResponsePacket (Server -> Client)
        registrar.playToClient(
            VillagerResponsePacket.TYPE,
            VillagerResponsePacket.STREAM_CODEC,
            VillagerResponsePacketHandler::handleOnClient
        );

        // Register ReputationUpdatePacket (Server -> Client)
        registrar.playToClient(
            ReputationUpdatePacket.TYPE,
            ReputationUpdatePacket.STREAM_CODEC,
            ReputationUpdatePacketHandler::handleOnClient
        );
    }
}