package com.afwid.network;

import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AdjustAnimationSpeedC2SPayload(UUID instanceId, double multiplier) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of("animationframework", "adjust_animation_speed");
    public static final CustomPayload.Id<AdjustAnimationSpeedC2SPayload> ID = new CustomPayload.Id<>(ID_RAW);
    public static final PacketCodec<RegistryByteBuf, AdjustAnimationSpeedC2SPayload> CODEC = PacketCodec.of(AdjustAnimationSpeedC2SPayload::encode, AdjustAnimationSpeedC2SPayload::decode);

    private static void encode(AdjustAnimationSpeedC2SPayload payload, RegistryByteBuf buf) {
        buf.writeUuid(payload.instanceId());
        buf.writeDouble(payload.multiplier());
    }

    private static AdjustAnimationSpeedC2SPayload decode(RegistryByteBuf buf) {
        return new AdjustAnimationSpeedC2SPayload(buf.readUuid(), buf.readDouble());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
