package com.afwid.network;

import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StopAnimationS2CPayload(UUID instanceId, long stopTick) implements CustomPayload {
    public static final Identifier STOP_ANIMATION_ID = Identifier.of("animationframework", "stop_animation");
    public static final CustomPayload.Id<StopAnimationS2CPayload> ID = new CustomPayload.Id<>(STOP_ANIMATION_ID);
    public static final PacketCodec<RegistryByteBuf, StopAnimationS2CPayload> CODEC = PacketCodec.of(StopAnimationS2CPayload::encode, StopAnimationS2CPayload::decode);
    private static void encode(StopAnimationS2CPayload payload, RegistryByteBuf buf) { buf.writeUuid(payload.instanceId()); buf.writeLong(payload.stopTick()); }
    private static StopAnimationS2CPayload decode(RegistryByteBuf buf) { return new StopAnimationS2CPayload(buf.readUuid(), buf.readLong()); }
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
