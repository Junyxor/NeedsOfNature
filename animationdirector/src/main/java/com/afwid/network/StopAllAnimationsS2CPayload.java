package com.afwid.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StopAllAnimationsS2CPayload(long stopTick) implements CustomPayload {
    public static final Identifier STOP_ALL_ID = Identifier.of("animationframework", "stop_all_animations");
    public static final CustomPayload.Id<StopAllAnimationsS2CPayload> ID = new CustomPayload.Id<>(STOP_ALL_ID);
    public static final PacketCodec<RegistryByteBuf, StopAllAnimationsS2CPayload> CODEC = PacketCodec.of(StopAllAnimationsS2CPayload::encode, StopAllAnimationsS2CPayload::decode);
    private static void encode(StopAllAnimationsS2CPayload payload, RegistryByteBuf buf) { buf.writeLong(payload.stopTick()); }
    private static StopAllAnimationsS2CPayload decode(RegistryByteBuf buf) { return new StopAllAnimationsS2CPayload(buf.readLong()); }
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
