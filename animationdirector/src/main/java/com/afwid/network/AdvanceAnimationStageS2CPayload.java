package com.afwid.network;

import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AdvanceAnimationStageS2CPayload(UUID instanceId, long advanceTick, int stageIndex) implements CustomPayload {
    public static final Identifier ADVANCE_STAGE_ID = Identifier.of("animationframework", "advance_stage");
    public static final CustomPayload.Id<AdvanceAnimationStageS2CPayload> ID = new CustomPayload.Id<>(ADVANCE_STAGE_ID);
    public static final PacketCodec<RegistryByteBuf, AdvanceAnimationStageS2CPayload> CODEC = PacketCodec.of(AdvanceAnimationStageS2CPayload::encode, AdvanceAnimationStageS2CPayload::decode);

    private static void encode(AdvanceAnimationStageS2CPayload payload, RegistryByteBuf buf) {
        buf.writeUuid(payload.instanceId());
        buf.writeLong(payload.advanceTick());
        buf.writeVarInt(payload.stageIndex());
    }

    private static AdvanceAnimationStageS2CPayload decode(RegistryByteBuf buf) {
        return new AdvanceAnimationStageS2CPayload(buf.readUuid(), buf.readLong(), buf.readVarInt());
    }

    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
