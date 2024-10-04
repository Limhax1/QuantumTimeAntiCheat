package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import com.gladurbad.medusa.util.MathUtil;

@CheckInfo(name = "Aim (K)", description = "Checks for not constant rotations.", complextype = "Constant")
public class AimK extends Check {
    double MAX_BUFFER = 5;
    double BUFFER;
    public AimK(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat() && data.getPositionProcessor().getDeltaXZ() != 0) {
            final float deltaYaw = this.data.getRotationProcessor().getDeltaYaw();
            final float deltaPitch = this.data.getRotationProcessor().getDeltaPitch();
            final float lastDeltaYaw = this.data.getRotationProcessor().getLastDeltaYaw();
            final float lastDeltaPitch = this.data.getRotationProcessor().getLastDeltaPitch();
            final double divisorYaw = (double) MathUtil.getGcd((long)(deltaYaw * MathUtil.EXPANDER), (long)(lastDeltaYaw * MathUtil.EXPANDER));
            final double divisorPitch = (double)MathUtil.getGcd((long)(deltaPitch * MathUtil.EXPANDER), (long)(lastDeltaPitch * MathUtil.EXPANDER));
            final double constantYaw = divisorYaw / MathUtil.EXPANDER;
            final double constantPitch = divisorPitch / MathUtil.EXPANDER;
            final double currentX = deltaYaw / constantYaw;
            final double currentY = deltaPitch / constantPitch;
            final double previousX = lastDeltaYaw / constantYaw;
            final double previousY = lastDeltaPitch / constantPitch;
            if (deltaYaw > 0.1f && deltaPitch > 0.1f && deltaYaw < 20.0f && deltaPitch < 20.0f) {
                final double moduloX = currentX % previousX;
                final double moduloY = currentY % previousY;
                final double floorModuloX = Math.abs(Math.floor(moduloX) - moduloX);
                final double floorModuloY = Math.abs(Math.floor(moduloY) - moduloY);
                final boolean invalidX = moduloX > 60.0 && floorModuloX > 0.1;
                final boolean invalidY = moduloY > 60.0 && floorModuloY > 0.1;
                final double sensitivity = this.data.getRotationProcessor().getSensitivity();
                final boolean tooLowSensitivity = sensitivity < 100.0 && sensitivity > -1.0;
                final boolean cinematic = this.isExempt(ExemptType.CINEMATIC);
                if (invalidX && invalidY && !cinematic && !tooLowSensitivity) {
                    if (BUFFER++ > this.MAX_BUFFER) {
                        this.fail("deltaYaw=" + deltaYaw + " deltaPitch=" + deltaPitch);
                    }
                }
                else {
                    BUFFER = Math.max(0, BUFFER - 0.1);
                }
            }
        }
    }
}
