package com.gladurbad.medusa.check.impl.movement.motion;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "FastClimb (A)", description = "Checks for fast-climb", experimental = true)
public class FastClimbA extends Check {
    public FastClimbA(PlayerData data) {
        super(data);
    }

    private double BUFFER;
    private double MAX_BUFFER = 10;

    @Override
    public void handle(Packet packet) {
        if (packet.isPosition() || packet.isPosLook()) {
            if(data.getPositionProcessor().isOnClimbable() && data.getPositionProcessor().getDeltaY() > 0.118) {
                BUFFER++;
                if(BUFFER > MAX_BUFFER / 2) {
                    setback();
                }

                if(BUFFER > MAX_BUFFER) {
                    fail("Climbing too quick DY: " + data.getPositionProcessor().getDeltaY());
                    setback();
                    BUFFER = 0;
                }
            } else {
                BUFFER = Math.max(0, BUFFER - 0.75);
            }
        }
    }
}
