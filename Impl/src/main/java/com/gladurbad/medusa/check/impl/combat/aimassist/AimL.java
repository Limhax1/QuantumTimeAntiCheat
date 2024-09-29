package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.MathUtil;

@CheckInfo(name = "Aim (L)", description = "Checks for not constant rotations.", complextype = "Constant")
public class AimL extends Check {
    double MAX_BUFFER = 5;
    double BUFFER;
    public AimL(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat()) {
            final float deltaPitch = this.data.getRotationProcessor().getDeltaPitch();
            final float lastDeltaPitch = this.data.getRotationProcessor().getLastDeltaPitch();
            final float deltaYaw = this.data.getRotationProcessor().getDeltaYaw();
            final long expandedPitch = (long)(MathUtil.EXPANDER * deltaPitch);
            final long expandedLastPitch = (long)(MathUtil.EXPANDER * lastDeltaPitch);
            final boolean cinematic = this.isExempt(ExemptType.CINEMATIC);
            final long gcd = MathUtil.getGcd(expandedPitch, expandedLastPitch);
            final boolean tooLowSensitivity = this.data.getRotationProcessor().hasTooLowSensitivity();
            final boolean validAngles = deltaYaw > 0.25f && deltaPitch > 0.25f && deltaPitch < 20.0f && deltaYaw < 20.0f;
            final boolean invalid = !cinematic && gcd < 131072L;
            if (invalid && validAngles && !tooLowSensitivity) {
                if (BUFFER++ > this.MAX_BUFFER) {
                    this.fail("rotation=" + gcd / 1000L + " deltaPitch=" + deltaPitch + " deltaYaw=" + deltaYaw);
                }
            }
            else {
                BUFFER = Math.max(0, BUFFER - 0.1);
            }
        }
    }
}
