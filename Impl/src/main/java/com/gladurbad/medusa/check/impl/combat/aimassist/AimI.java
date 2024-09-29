package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Aim (I)", description = "Checks for invalid yaw changes.", complextype = "InvalidYaw")
public class AimI extends Check {
    public AimI(PlayerData data) {
        super(data);
    }
    double MAX_BUFFER = 5;
    double BUFFER;

    @Override
    public void handle(final Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat()) {
            final float deltaYaw = this.data.getRotationProcessor().getDeltaYaw();
            final float deltaPitch = this.data.getRotationProcessor().getDeltaPitch();
            final float pitch = this.data.getRotationProcessor().getPitch();
            final boolean invalid = deltaYaw > 15.0f && deltaPitch < 0.1 && Math.abs(pitch) < 65.0f;
            if (invalid) {
                if (BUFFER++ > MAX_BUFFER) {
                    this.fail("deltaYaw=" + deltaYaw + " deltaPitch=" + deltaPitch);
                }
            }
            else {
                BUFFER = Math.max(0, BUFFER - 0.05);
            }
        }
    }
}
