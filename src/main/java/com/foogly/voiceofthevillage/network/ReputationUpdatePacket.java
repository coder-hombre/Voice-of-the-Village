package com.foogly.voiceofthevillage.network;

import com.foogly.voiceofthevillage.data.ReputationThreshold;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Packet for syncing reputation changes from server to client.
 * Used to update client-side reputation displays and trigger client-side effects.
 */
public record ReputationUpdatePacket(
    UUID villagerId,
    UUID playerId,
    String villagerName,
    int newScore,
    int scoreChange,
    ReputationThreshold newThreshold,
    ReputationThreshold previousThreshold,
    String eventDescription,
    long timestamp
) implements CustomPacketPayload {

    public static final Type<ReputationUpdatePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("voiceofthevillage", "reputation_update")
    );

    public static final StreamCodec<FriendlyByteBuf, ReputationUpdatePacket> STREAM_CODEC = StreamCodec.of(
        ReputationUpdatePacket::encode,
        ReputationUpdatePacket::decode
    );

    /**
     * Creates a reputation update packet.
     *
     * @param villagerId         UUID of the villager
     * @param playerId           UUID of the player
     * @param villagerName       Name of the villager
     * @param newScore           New reputation score
     * @param scoreChange        Change in reputation score
     * @param newThreshold       New reputation threshold
     * @param previousThreshold  Previous reputation threshold
     * @param eventDescription   Description of the event that caused the change
     * @return A new reputation update packet
     */
    public static ReputationUpdatePacket create(UUID villagerId, UUID playerId, String villagerName,
                                              int newScore, int scoreChange, ReputationThreshold newThreshold,
                                              ReputationThreshold previousThreshold, String eventDescription) {
        return new ReputationUpdatePacket(villagerId, playerId, villagerName, newScore, scoreChange,
                                        newThreshold, previousThreshold, eventDescription, System.currentTimeMillis());
    }

    /**
     * Encodes the packet data to the buffer.
     *
     * @param buf    The buffer to write to
     * @param packet The packet to encode
     */
    public static void encode(FriendlyByteBuf buf, ReputationUpdatePacket packet) {
        buf.writeUUID(packet.villagerId);
        buf.writeUUID(packet.playerId);
        buf.writeUtf(packet.villagerName, 32);
        buf.writeInt(packet.newScore);
        buf.writeInt(packet.scoreChange);
        buf.writeEnum(packet.newThreshold);
        buf.writeEnum(packet.previousThreshold);
        buf.writeUtf(packet.eventDescription, 256);
        buf.writeLong(packet.timestamp);
    }

    /**
     * Decodes the packet data from the buffer.
     *
     * @param buf The buffer to read from
     * @return The decoded packet
     */
    public static ReputationUpdatePacket decode(FriendlyByteBuf buf) {
        UUID villagerId = buf.readUUID();
        UUID playerId = buf.readUUID();
        String villagerName = buf.readUtf(32);
        int newScore = buf.readInt();
        int scoreChange = buf.readInt();
        ReputationThreshold newThreshold = buf.readEnum(ReputationThreshold.class);
        ReputationThreshold previousThreshold = buf.readEnum(ReputationThreshold.class);
        String eventDescription = buf.readUtf(256);
        long timestamp = buf.readLong();

        return new ReputationUpdatePacket(villagerId, playerId, villagerName, newScore, scoreChange,
                                        newThreshold, previousThreshold, eventDescription, timestamp);
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
               newScore >= -100 && newScore <= 100 &&
               scoreChange >= -100 && scoreChange <= 100 &&
               newThreshold != null &&
               previousThreshold != null &&
               eventDescription != null &&
               timestamp > 0;
    }

    /**
     * Checks if the reputation threshold changed.
     *
     * @return true if the threshold changed
     */
    public boolean hasThresholdChanged() {
        return newThreshold != previousThreshold;
    }

    /**
     * Checks if the reputation improved.
     *
     * @return true if the score increased
     */
    public boolean isImprovement() {
        return scoreChange > 0;
    }

    /**
     * Checks if the reputation worsened.
     *
     * @return true if the score decreased
     */
    public boolean isDeterioration() {
        return scoreChange < 0;
    }

    /**
     * Gets the absolute value of the score change.
     *
     * @return The absolute score change
     */
    public int getAbsoluteScoreChange() {
        return Math.abs(scoreChange);
    }
}