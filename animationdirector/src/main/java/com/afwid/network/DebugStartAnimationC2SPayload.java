package com.afwid.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DebugStartAnimationC2SPayload(List<Integer> actorEntityIds, String damageBehaviorId, boolean ignoreAttackers, int anchorEntityId) implements CustomPayload {
    private static final int MAX_ACTORS = 32;
    public static final Identifier DEBUG_START_ANIMATION_ID = Identifier.of("animationframework", "debug_start_animation");
    public static final CustomPayload.Id<DebugStartAnimationC2SPayload> ID = new CustomPayload.Id<>(DEBUG_START_ANIMATION_ID);
    public static final PacketCodec<RegistryByteBuf, DebugStartAnimationC2SPayload> CODEC = PacketCodec.of(DebugStartAnimationC2SPayload::encode, DebugStartAnimationC2SPayload::decode);

    public DebugStartAnimationC2SPayload {
        actorEntityIds = actorEntityIds == null ? List.of() : List.copyOf(actorEntityIds);
        damageBehaviorId = damageBehaviorId == null ? "" : damageBehaviorId;
    }
    private static void encode(DebugStartAnimationC2SPayload payload, RegistryByteBuf buf) {
        buf.writeVarInt(payload.actorEntityIds().size());
        for (int id : payload.actorEntityIds()) buf.writeVarInt(id);
        buf.writeString(payload.damageBehaviorId(), 64);
        buf.writeBoolean(payload.ignoreAttackers());
        buf.writeVarInt(payload.anchorEntityId());
    }
    private static DebugStartAnimationC2SPayload decode(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ACTORS) throw new IllegalArgumentException("Too many actors: " + count);
        ArrayList<Integer> ids = new ArrayList<>(count);
        for (int i=0;i<count;i++) ids.add(buf.readVarInt());
        return new DebugStartAnimationC2SPayload(ids, buf.readString(64), buf.readBoolean(), buf.readVarInt());
    }
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
