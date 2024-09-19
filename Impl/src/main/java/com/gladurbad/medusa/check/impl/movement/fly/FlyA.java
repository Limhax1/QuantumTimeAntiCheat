package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Bukkit;

@CheckInfo(name = "Fly (A)", description = "Checks for autistic flight modules.")
public final class FlyA extends Check {

    public FlyA(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {

        if (packet.isPosition() || packet.isPosLook()) {

            final double deltaXZ = data.getPositionProcessor().getDeltaX();
            final double lastdeltadeltaXZ = Math.abs(data.getPositionProcessor().getLastDeltaXZ());
            final double lastDeltaY = data.getPositionProcessor().getLastDeltaY();
            final double Ymotion = data.getPlayer().getVelocity().getY();
            final double DeltaY = data.getPositionProcessor().getDeltaY();
            final double speed = data.getPositionProcessor().getDeltaXZ();
            final double bps = speed * 2;

            boolean exempt = isExempt(ExemptType.FLYING, ExemptType.TELEPORT, ExemptType.VELOCITY, ExemptType.NEAR_VEHICLE);

            if (data.getPositionProcessor().getAirTicks() > 60 && data.getPositionProcessor().isInAir() && !exempt) {
                if (speed > 0.34) {
                    fail("Most likely flying " + speed);
                }
            }

            if (data.getPositionProcessor().isInAir() && data.getPositionProcessor().getAirTicks() > 20 && !exempt) {
                if (DeltaY == 0.0) {
                    fail("Hovering " + DeltaY);
                }
            }
        }
    }
}
