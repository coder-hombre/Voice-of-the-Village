package com.foogly.voiceofthevillage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Packet for sending text messages from client to server.
 * Used for both simple mode GUI input and advanced mode commands.
 */
public record TextMessagePacket(
    UUID villagerId,
    UUID playerId,
    String playerName,
    String message,
    boolean isCommand,
    long timestamp
) implements CustomPacketPayload {

    public static final Type<TextMessagePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("voiceofthevillage", "text_message")
    );

    public static final StreamCodec<FriendlyByteBuf, TextMessagePacket> STREAM_CODEC = StreamCodec.of(
        TextMessagePacket::encode,
        TextMessagePacket::decode
    );

    /**
     * Encodes the packet data to the buffer.
     *
     * @param buf    The buffer to write to
     * @param packet The packet to encode
     */
    public static void encode(FriendlyByteBuf buf, TextMessagePacket packet) {
        buf.writeUUID(packet.villagerId);
        buf.writeUUID(packet.playerId);
        buf.writeUtf(packet.playerName, 32);
        buf.writeUtf(packet.message, 512);
        buf.writeBoolean(packet.isCommand);
        buf.writeLong(packet.timestamp);
    }

    /**
     * Decodes the packet data from the buffer.
     *
     * @param buf The buffer to read from
     * @return The decoded packet
     */
    public static TextMessagePacket decode(FriendlyByteBuf buf) {
        UUID villagerId = buf.readUUID();
        UUID playerId = buf.readUUID();
        String playerName = buf.readUtf(32);
        String message = buf.readUtf(512);
        boolean isCommand = buf.readBoolean();
        long timestamp = buf.readLong();

        return new TextMessagePacket(villagerId, playerId, playerName, message, isCommand, timestamp);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Validates the packet data.
     *
     * @return true if the packet is valid
     */
    public boolean isValid() {
        return villagerId != null &&
               playerId != null &&
               playerName != null && !playerName.trim().isEmpty() &&
               message != null && !message.trim().isEmpty() && message.length() <= 500 &&
               timestamp > 0;
    }

    /**
     * Gets the trimmed message content.
     *
     * @return The trimmed message
     */
    public String getTrimmedMessage() {
        return message != null ? message.trim() : "";
    }
}