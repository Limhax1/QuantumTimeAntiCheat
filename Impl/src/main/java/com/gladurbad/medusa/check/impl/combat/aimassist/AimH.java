package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Aim (H)", description = "Generic rotation analysis heuristic.", complextype = "Analysis")
public class AimH extends Check {
    public AimH(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat() && data.getPositionProcessor().getDeltaXZ() != 0) {
            final float lastFuckedYaw = this.data.getRotationProcessor().getLastFuckedPredictedYaw();
            final float fuckedYaw = this.data.getRotationProcessor().getFuckedPredictedYaw();
            final float difference = Math.abs(fuckedYaw - lastFuckedYaw);
            final double distance = this.data.getCombatProcessor().getDistance();
            final boolean exempt = this.isExempt(ExemptType.TELEPORT);
            if (exempt) {
                return;
            }

            debug(difference + " " + distance);

            if (distance > 0.6 && difference > 20.0f && distance < 10.0) {
                this.fail("diff=" + difference + " dist=" + distance);
            }
        }
    }
}
