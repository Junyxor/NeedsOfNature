package com.afwid.network;

import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DebugAdvanceStageC2SPayload(UUID instanceId) implements CustomPayload {
    public static final Identifier DEBUG_ADVANCE_STAGE_ID = Identifier.of("animationframework", "debug_advance_stage");
    public static final CustomPayload.Id<DebugAdvanceStageC2SPayload> ID = new CustomPayload.Id<>(DEBUG_ADVANCE_STAGE_ID);
    public static final PacketCodec<RegistryByteBuf, DebugAdvanceStageC2SPayload> CODEC = PacketCodec.of(DebugAdvanceStageC2SPayload::encode, DebugAdvanceStageC2SPayload::decode);
    private static void encode(DebugAdvanceStageC2SPayload payload, RegistryByteBuf buf) { buf.writeUuid(payload.instanceId()); }
    private static DebugAdvanceStageC2SPayload decode(RegistryByteBuf buf) { return new DebugAdvanceStageC2SPayload(buf.readUuid()); }
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
