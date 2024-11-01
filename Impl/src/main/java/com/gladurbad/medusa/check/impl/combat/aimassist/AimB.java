package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

import java.util.function.Predicate;

@CheckInfo(name = "Aim (B)", description = "Checks for killAura flaws.", experimental = true, complextype = "Snap")
public final class AimB extends Check {

    private double buffer = 0.0;
    private static final double BUFFER_LIMIT = 2.0;
    private static final double BUFFER_DECAY = 0.25;

    public AimB(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat() && data.getPositionProcessor().getDeltaXZ() != 0) {
            final double deltaYaw = data.getRotationProcessor().getDeltaYaw();
            final double deltaPitch = data.getRotationProcessor().getDeltaPitch();
            final boolean invalid = deltaYaw >= 160 && deltaPitch <= 2;
            final boolean invalid2 =deltaPitch > 85 && deltaYaw <= 2;

            if (invalid || invalid2) {
                buffer += 1.0;

                if (buffer > BUFFER_LIMIT) {
                    fail("deltaYaw=" + deltaYaw + " deltaPitch=" + deltaPitch);
                    buffer = 0;
                }
            } else {
                buffer = Math.max(0, buffer - BUFFER_DECAY);
            }
        }
    }
}
