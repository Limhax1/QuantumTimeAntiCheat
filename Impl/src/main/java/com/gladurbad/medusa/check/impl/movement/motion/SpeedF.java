package com.gladurbad.medusa.check.impl.movement.motion;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Speed (F)", description = "Checks for invalid Y changes when jumping.")
public class SpeedF extends Check {

    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");
    
    private static final double JUMP_HEIGHT = 0.41999998688697815;
    private static final double JUMP_BOOST_1_HEIGHT = 0.5199999883770943;
    private static final double JUMP_BOOST_2_HEIGHT = 0.6199999898672104;
    
    private double lastY = 0.0;
    private double expectedJumpHeight = 0.0;
    private int jumpTicks = 0;

    public SpeedF(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isFlying()) {
            double deltaY = data.getPositionProcessor().getDeltaY();
            double currentY = data.getPositionProcessor().getY();

            if (isJumpHeight(deltaY)) {
                expectedJumpHeight = deltaY;
                jumpTicks = 0;
                debug("Hump: " + deltaY);
            }

            if (expectedJumpHeight > 0) {
                jumpTicks++;
                double yDifference = currentY - lastY;
                
                if (jumpTicks <= 2) {
                    if (Math.abs(yDifference) < expectedJumpHeight * 0.5) {
                        fail("Abnormal jumping. Expected DeltaY: " + expectedJumpHeight +
                             ", Real DeltaY: " + yDifference +
                             ", Tick: " + jumpTicks);
                        
                        if (setback.getBoolean()) {
                            setback();
                        }
                    }
                } else {
                    expectedJumpHeight = 0;
                    jumpTicks = 0;
                }
            }

            lastY = currentY;
        }
    }

    private boolean isJumpHeight(double deltaY) {
        return Math.abs(deltaY - JUMP_HEIGHT) < 0.001 ||
               Math.abs(deltaY - JUMP_BOOST_1_HEIGHT) < 0.001 ||
               Math.abs(deltaY - JUMP_BOOST_2_HEIGHT) < 0.001;
    }
}