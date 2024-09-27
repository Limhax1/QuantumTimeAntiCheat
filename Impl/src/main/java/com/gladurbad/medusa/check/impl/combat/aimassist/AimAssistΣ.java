package com.gladurbad.medusa.check.impl.combat.aimassist;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "AimAssist (M)", description = "ΣΣ")
public class AimAssistΣ extends Check {
    double MAX_BUFFER = 5;
    double BUFFER;
    public AimAssistΣ(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isRotation() && data.getCombatProcessor().isInCombat()) {
            final float deltaYaw = this.data.getRotationProcessor().getDeltaYaw();
            final float deltaPitch = this.data.getRotationProcessor().getDeltaPitch();
            final int sensitivity = this.data.getRotationProcessor().getSensitivity();
            final boolean cinematic = this.isExempt(ExemptType.CINEMATIC);
            final boolean invalid = sensitivity < -10 && deltaYaw > 1.25f && deltaPitch > 1.25f;
            if (invalid && !cinematic) {
                if (BUFFER++ > this.MAX_BUFFER) {
                    this.fail("sens=" + sensitivity + " deltaYaw=" + deltaYaw + " deltaPitch=" + deltaPitch);
                }
            }
            else {
                BUFFER = Math.max(0, BUFFER - 0.1);
            }
        }
    }
}
