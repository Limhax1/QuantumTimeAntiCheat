package com.gladurbad.medusa.check.impl.player.protocol;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Bukkit;

/**
    * I came
 **/

@CheckInfo(name = "Protocol (K)", description = "Checks for goofy deceleration.")
public final class ProtocolK extends Check {

    public ProtocolK(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (!packet.isRotation()) {
            return;
        }

        final float deltaYaw = Math.abs(data.getRotationProcessor().getDeltaYaw());
        final double deltaXZ = Math.abs(data.getPositionProcessor().getDeltaX());
        final double lastdeltadeltaXZ =  Math.abs(data.getPositionProcessor().getLastDeltaXZ());

        final double accel = Math.abs(deltaXZ - lastdeltadeltaXZ);

        final double squaredAccel =accel * 100;

        if(deltaYaw > 1.5F && deltaXZ > 0.150 && squaredAccel < 1.0E-5) {
            fail("Invalid Deceleration " + squaredAccel);
        }
    }
}
