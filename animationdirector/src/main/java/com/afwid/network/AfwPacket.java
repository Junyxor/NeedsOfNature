package com.afwid.network;

import net.minecraft.network.packet.CustomPayload;

/**
 * Legacy compatibility marker retained for source compatibility while the
 * 1.20.1 byte-buffer networking layer is removed. New packets are ordinary
 * Minecraft 1.21 CustomPayload implementations.
 */
@Deprecated(forRemoval = true)
public interface AfwPacket extends CustomPayload {
}
