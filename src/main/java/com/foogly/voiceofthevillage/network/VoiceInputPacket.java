package com.foogly.voiceofthevillage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Packet for sending voice audio data from client to server.
 * Contains the audio data, target villager ID, and player information.
 */
public record VoiceInputPacket(
    UUID villagerId,
    UUID playerId,
    String playerName,
    byte[] audioData,
    int sampleRate,
    long timestamp
) implements CustomPacketPayload {

    public static final Type<VoiceInputPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("voiceofthevillage", "voice_input")
    );

    public static final StreamCodec<FriendlyByteBuf, VoiceInputPacket> STREAM_CODEC = StreamCodec.of(
        VoiceInputPacket::encode,
        VoiceInputPacket::decode
    );

    /**
     * Encodes the packet data to the buffer.
     *
     * @param buf    The buffer to write to
     * @param packet The packet to encode
     */
    public static void encode(FriendlyByteBuf buf, VoiceInputPacket packet) {
        buf.writeUUID(packet.villagerId);
        buf.writeUUID(packet.playerId);
        buf.writeUtf(packet.playerName, 32);
        buf.writeInt(packet.audioData.length);
        buf.writeBytes(packet.audioData);
        buf.writeInt(packet.sampleRate);
        buf.writeLong(packet.timestamp);
    }

    /**
     * Decodes the packet data from the buffer.
     *
     * @param buf The buffer to read from
     * @return The decoded packet
     */
    public static VoiceInputPacket decode(FriendlyByteBuf buf) {
        UUID villagerId = buf.readUUID();
        UUID playerId = buf.readUUID();
        String playerName = buf.readUtf(32);
        int audioDataLength = buf.readInt();
        byte[] audioData = new byte[audioDataLength];
        buf.readBytes(audioData);
        int sampleRate = buf.readInt();
        long timestamp = buf.readLong();

        return new VoiceInputPacket(villagerId, playerId, playerName, audioData, sampleRate, timestamp);
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
               audioData != null && audioData.length > 0 && audioData.length <= 1024 * 1024 && // Max 1MB
               sampleRate > 0 &&
               timestamp > 0;
    }

    /**
     * Gets the size of the audio data in bytes.
     *
     * @return The audio data size
     */
    public int getAudioDataSize() {
        return audioData != null ? audioData.length : 0;
    }
}