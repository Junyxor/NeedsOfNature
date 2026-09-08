package com.afwid.network;

import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AnimationSpeedUpdateS2CPayload(UUID instanceId, double speed) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of("animationframework", "animation_speed_update");
    public static final CustomPayload.Id<AnimationSpeedUpdateS2CPayload> ID = new CustomPayload.Id<>(ID_RAW);
    public static final PacketCodec<RegistryByteBuf, AnimationSpeedUpdateS2CPayload> CODEC = PacketCodec.of(AnimationSpeedUpdateS2CPayload::encode, AnimationSpeedUpdateS2CPayload::decode);

    private static void encode(AnimationSpeedUpdateS2CPayload payload, RegistryByteBuf buf) {
        buf.writeUuid(payload.instanceId());
        buf.writeDouble(payload.speed());
    }
    private static AnimationSpeedUpdateS2CPayload decode(RegistryByteBuf buf) {
        return new AnimationSpeedUpdateS2CPayload(buf.readUuid(), buf.readDouble());
    }
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
