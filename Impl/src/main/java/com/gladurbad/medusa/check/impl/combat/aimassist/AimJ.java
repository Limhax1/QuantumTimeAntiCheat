package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Aim (J)", description = "Checks for Switching directions too quickly", complextype = "Switch")
public class AimJ extends Check {
    private float lastDeltaPitch;
    private int ticksSinceSwitchedDirection;
    double MAX_BUFFER = 5;
    double BUFFER;
    public AimJ(PlayerData data) {
        super(data);
    }
    @Override
    public void handle(final Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat()) {
            final float pitch = this.data.getRotationProcessor().getPitch();
            final float lastPitch = this.data.getRotationProcessor().getLastPitch();
            final float deltaPitch = pitch - lastPitch;
            if ((deltaPitch < 0.0f && this.lastDeltaPitch > 0.0f) || (deltaPitch > 0.0f && this.lastDeltaPitch < 0.0f)) {
                this.ticksSinceSwitchedDirection = 0;
            }
            else {
                ++this.ticksSinceSwitchedDirection;
            }
            final boolean invalid = this.ticksSinceSwitchedDirection == 0 && Math.abs(deltaPitch) > 5.0f;
            if (invalid) {
                if (BUFFER++ > this.MAX_BUFFER) {
                    this.fail("deltaPitch=" + deltaPitch);
                }
            }
            else {
                BUFFER = 0;
            }
            this.lastDeltaPitch = deltaPitch;
        }
    }
}
