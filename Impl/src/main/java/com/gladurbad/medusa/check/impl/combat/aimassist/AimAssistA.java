package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

import java.util.function.Predicate;

/**
 * Created on 10/24/2020 Package com.gladurbad.medusa.check.impl.combat.aim by GladUrBad
 */

@CheckInfo(name = "AimAssist (A)", description = "Checks for killAura flaws.")
public final class AimAssistA extends Check {

    private final Predicate<Float> validRotation = rotation -> rotation > 3F && rotation < 35F;
    private double buffer = 0.0;
    private static final double BUFFER_LIMIT = 8.0;
    private static final double BUFFER_DECAY = 0.25;

    public AimAssistA(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isRotation()) {
            final double deltaYaw = data.getRotationProcessor().getDeltaYaw();
            final double deltaPitch = data.getRotationProcessor().getDeltaPitch();
            final boolean invalid = deltaYaw > 1.5F && deltaPitch < 0.01;
            final boolean invalid2 = deltaYaw < 0.01 && deltaPitch > 2F;

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
