package com.afwid.network;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoForge 1.21.1 server-side payload sender. */
public final class AfwServerNetworking {
    private AfwServerNetworking() { }

    public static void send(ServerPlayerEntity player, CustomPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
