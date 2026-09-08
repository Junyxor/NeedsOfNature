package com.afwid.network;

import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DebugStopAnimationC2SPayload(UUID instanceId) implements CustomPayload {
    public static final Identifier DEBUG_STOP_ANIMATION_ID = Identifier.of("animationframework", "debug_stop_animation");
    public static final CustomPayload.Id<DebugStopAnimationC2SPayload> ID = new CustomPayload.Id<>(DEBUG_STOP_ANIMATION_ID);
    public static final PacketCodec<RegistryByteBuf, DebugStopAnimationC2SPayload> CODEC = PacketCodec.of(DebugStopAnimationC2SPayload::encode, DebugStopAnimationC2SPayload::decode);
    private static void encode(DebugStopAnimationC2SPayload payload, RegistryByteBuf buf) { buf.writeUuid(payload.instanceId()); }
    private static DebugStopAnimationC2SPayload decode(RegistryByteBuf buf) { return new DebugStopAnimationC2SPayload(buf.readUuid()); }
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
