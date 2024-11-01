package com.gladurbad.medusa.check.impl.player.protocol;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;


@CheckInfo(name = "Protocol (A)", description = "Checks for invalid pitch rotation.", complextype = "Invalid Pitch")
public final class ProtocolA extends Check {

    public ProtocolA(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isRotation()) {
            final float pitch = data.getRotationProcessor().getPitch();

            if (Math.abs(pitch) > 90) fail(String.format("pitch=%.2f", pitch));
        }
    }
}
