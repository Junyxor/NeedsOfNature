package com.afwid.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DebugJoinAnimationC2SPayload(UUID instanceId, List<Integer> actorEntityIds) implements CustomPayload {
    private static final int MAX_ACTORS = 32;
    public static final Identifier DEBUG_JOIN_ANIMATION_ID = Identifier.of("animationframework", "debug_join_animation");
    public static final CustomPayload.Id<DebugJoinAnimationC2SPayload> ID = new CustomPayload.Id<>(DEBUG_JOIN_ANIMATION_ID);
    public static final PacketCodec<RegistryByteBuf, DebugJoinAnimationC2SPayload> CODEC = PacketCodec.of(DebugJoinAnimationC2SPayload::encode, DebugJoinAnimationC2SPayload::decode);

    public DebugJoinAnimationC2SPayload { actorEntityIds = actorEntityIds == null ? List.of() : List.copyOf(actorEntityIds); }
    private static void encode(DebugJoinAnimationC2SPayload payload, RegistryByteBuf buf) {
        buf.writeUuid(payload.instanceId());
        buf.writeVarInt(payload.actorEntityIds().size());
        for (int id : payload.actorEntityIds()) buf.writeVarInt(id);
    }
    private static DebugJoinAnimationC2SPayload decode(RegistryByteBuf buf) {
        UUID instance = buf.readUuid();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ACTORS) throw new IllegalArgumentException("Too many actors: " + count);
        ArrayList<Integer> ids = new ArrayList<>(count);
        for (int i=0;i<count;i++) ids.add(buf.readVarInt());
        return new DebugJoinAnimationC2SPayload(instance, ids);
    }
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
