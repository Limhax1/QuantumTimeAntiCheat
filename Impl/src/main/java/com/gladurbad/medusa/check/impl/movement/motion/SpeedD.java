package com.gladurbad.medusa.check.impl.movement.motion;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Speed (D)", description = "Checks for falling too fast")
public class SpeedD extends Check {

    private static final double MAX_FALL_SPEED = -0.377;
    private static final int BUFFER_LIMIT = 3;

    private double lastDeltaY = 0.0;
    private double buffer = 0;
    private boolean wasInAir = false;

    public SpeedD(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isPosition()) {
            double deltaY = data.getPositionProcessor().getDeltaY();
            boolean onGround = data.getPositionProcessor().isOnGround();
            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.VELOCITY, ExemptType.FLYING, ExemptType.LIQUID, ExemptType.SLIME);

            if (!onGround && !exempt) {
                if (wasInAir && deltaY < 0) {
                    double expectedMaxFallSpeed = getExpectedMaxFallSpeed();
                    
                    if (deltaY < expectedMaxFallSpeed && data.getPositionProcessor().getAirTicks() < 5) {
                        if (++buffer > BUFFER_LIMIT) {
                            fail("Falling too quick. DeltaY: " + deltaY + ", Expected max: " + expectedMaxFallSpeed);
                        }
                    } else {
                        buffer = Math.max(0, buffer - 0.25);
                    }

                    debug("DeltaY: " + deltaY + ", Expected max: " + expectedMaxFallSpeed + ", Buffer: " + buffer);
                }
                wasInAir = true;
            } else {
                wasInAir = false;
            }

            lastDeltaY = deltaY;
        }
    }

    private double getExpectedMaxFallSpeed() {
        double maxFallSpeed = MAX_FALL_SPEED;

        return maxFallSpeed;
    }
}
