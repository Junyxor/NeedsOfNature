package com.afwid.network;

import net.minecraft.network.packet.CustomPayload;

/** Legacy compatibility marker while the old Fabric byte-buffer layer is removed. */
@Deprecated(forRemoval = true)
public interface AfwPacket extends CustomPayload {
}
