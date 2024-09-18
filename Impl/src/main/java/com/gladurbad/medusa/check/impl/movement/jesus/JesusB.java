package com.gladurbad.medusa.check.impl.movement.jesus;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Jesus (B)", description = "Checks for going too quick in water / lava")
public class JesusB extends Check {

    public JesusB(final PlayerData data) {
        super(data);
    }

    double BUFFER;
    double MAX_BUFFER = 3;

    @Override
    public void handle(Packet packet) {
        if(packet.isPosition() || packet.isPosLook()) {
            final double speed = data.getPositionProcessor().getDeltaXZ();
            boolean posAir = data.getPlayer().getLocation().getBlock().isEmpty();
            boolean isExempt = isExempt(ExemptType.VELOCITY);

            debug("Speed: " + speed);

            if(data.getPlayer().getLocation().getBlock().isLiquid() && speed > 0.2 && !isExempt) {
                if(BUFFER++ > MAX_BUFFER) {
                    fail("Going too quick in liquids; " + speed);
                    BUFFER = 0;
                }
            } else {
                BUFFER = Math.max(0, BUFFER - 1);
            }

            if(data.getPlayer().getLocation().add(0, -1, 0).getBlock().isLiquid() && posAir && speed > 0.2 && !isExempt) {
                if(BUFFER++ > MAX_BUFFER) {
                    fail("Going too quick on liquids; " + speed);
                    BUFFER = 0;
                } else {
                    BUFFER = Math.max(0, BUFFER - 8);
                }
            }
        }
    }
}
