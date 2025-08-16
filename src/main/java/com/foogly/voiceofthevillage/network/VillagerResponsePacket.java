package com.foogly.voiceofthevillage.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Packet for sending villager responses from server to client.
 * Contains both text and optional audio data for the response.
 */
public record VillagerResponsePacket(
    UUID villagerId,
    UUID playerId,
    String villagerName,
    String textResponse,
    byte[] audioData,
    boolean hasAudio,
    long timestamp
) implements CustomPacketPayload {

    public static final Type<VillagerResponsePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("voiceofthevillage", "villager_response")
    );

    public static final StreamCodec<FriendlyByteBuf, VillagerResponsePacket> STREAM_CODEC = StreamCodec.of(
        VillagerResponsePacket::encode,
        VillagerResponsePacket::decode
    );

    /**
     * Creates a text-only response packet.
     *
     * @param villagerId   UUID of the responding villager
     * @param playerId     UUID of the target player
     * @param villagerName Name of the villager
     * @param textResponse The text response
     * @return A new response packet without audio
     */
    public static VillagerResponsePacket textOnly(UUID villagerId, UUID playerId, String villagerName, String textResponse) {
        return new VillagerResponsePacket(villagerId, playerId, villagerName, textResponse, new byte[0], false, System.currentTimeMillis());
    }

    /**
     * Creates a response packet with both text and audio.
     *
     * @param villagerId   UUID of the responding villager
     * @param playerId     UUID of the target player
     * @param villagerName Name of the villager
     * @param textResponse The text response
     * @param audioData    The audio data
     * @return A new response packet with audio
     */
    public static VillagerResponsePacket withAudio(UUID villagerId, UUID playerId, String villagerName, String textResponse, byte[] audioData) {
        return new VillagerResponsePacket(villagerId, playerId, villagerName, textResponse, audioData, true, System.currentTimeMillis());
    }

    /**
     * Encodes the packet data to the buffer.
     *
     * @param buf    The buffer to write to
     * @param packet The packet to encode
     */
    public static void encode(FriendlyByteBuf buf, VillagerResponsePacket packet) {
        buf.writeUUID(packet.villagerId);
        buf.writeUUID(packet.playerId);
        buf.writeUtf(packet.villagerName, 32);
        buf.writeUtf(packet.textResponse, 1024);
        buf.writeBoolean(packet.hasAudio);
        if (packet.hasAudio && packet.audioData != null) {
            buf.writeInt(packet.audioData.length);
            buf.writeBytes(packet.audioData);
        } else {
            buf.writeInt(0);
        }
        buf.writeLong(packet.timestamp);
    }

    /**
     * Decodes the packet data from the buffer.
     *
     * @param buf The buffer to read from
     * @return The decoded packet
     */
    public static VillagerResponsePacket decode(FriendlyByteBuf buf) {
        UUID villagerId = buf.readUUID();
        UUID playerId = buf.readUUID();
        String villagerName = buf.readUtf(32);
        String textResponse = buf.readUtf(1024);
        boolean hasAudio = buf.readBoolean();
        
        byte[] audioData = new byte[0];
        int audioDataLength = buf.readInt();
        if (hasAudio && audioDataLength > 0) {
            audioData = new byte[audioDataLength];
            buf.readBytes(audioData);
        }
        
        long timestamp = buf.readLong();

        return new VillagerResponsePacket(villagerId, playerId, villagerName, textResponse, audioData, hasAudio, timestamp);
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
               villagerName != null && !villagerName.trim().isEmpty() &&
               textResponse != null && !textResponse.trim().isEmpty() &&
               (!hasAudio || (audioData != null && audioData.length <= 2 * 1024 * 1024)) && // Max 2MB for audio
               timestamp > 0;
    }

    /**
     * Gets the size of the audio data in bytes.
     *
     * @return The audio data size
     */
    public int getAudioDataSize() {
        return hasAudio && audioData != null ? audioData.length : 0;
    }

    /**
     * Gets the trimmed text response.
     *
     * @return The trimmed text response
     */
    public String getTrimmedTextResponse() {
        return textResponse != null ? textResponse.trim() : "";
    }
}