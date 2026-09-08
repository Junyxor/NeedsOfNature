package com.afwid.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DebugStopAllAnimationsC2SPayload() implements CustomPayload {
    public static final Identifier DEBUG_STOP_ALL_ID = Identifier.of("animationframework", "debug_stop_all_animations");
    public static final CustomPayload.Id<DebugStopAllAnimationsC2SPayload> ID = new CustomPayload.Id<>(DEBUG_STOP_ALL_ID);
    public static final PacketCodec<RegistryByteBuf, DebugStopAllAnimationsC2SPayload> CODEC = PacketCodec.of((payload, buf) -> {}, buf -> new DebugStopAllAnimationsC2SPayload());
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
