package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

import java.util.function.Predicate;

@CheckInfo(name = "Aim (A)", description = "Checks for killAura flaws.", experimental = true, complextype = "GCD")
public final class AimA extends Check {

    private final Predicate<Float> validRotation = rotation -> rotation > 3F && rotation < 35F;
    private double buffer = 0.0;
    private static final double BUFFER_LIMIT = 10;
    private static final double BUFFER_DECAY = 0.25;

    public AimA(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat() && data.getPositionProcessor().getDeltaXZ() != 0) {
            final double deltaYaw = data.getRotationProcessor().getDeltaYaw();
            final double deltaPitch = data.getRotationProcessor().getDeltaPitch();
            final boolean invalid = deltaYaw > 3F && deltaPitch < 0.01 && Math.abs(data.getRotationProcessor().getPitch()) != 90  ;
            final boolean invalid2 = deltaYaw < 0.01 && deltaPitch > 4F;
            final boolean invalid3 = deltaYaw < 6F && deltaPitch < 0.15;

            if (invalid || invalid2 || invalid3) {
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
