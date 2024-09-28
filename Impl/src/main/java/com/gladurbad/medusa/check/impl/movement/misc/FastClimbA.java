package com.gladurbad.medusa.check.impl.movement.misc;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "FastClimb (A)", description = "Checks for fast-climb", experimental = true)
public class FastClimbA extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    public FastClimbA(PlayerData data) {
        super(data);
    }

    private double BUFFER;

    @Override
    public void handle(Packet packet) {
        if (packet.isPosition() || packet.isPosLook()) {
            if(data.getPositionProcessor().isOnClimbable() && data.getPositionProcessor().getDeltaY() > 0.118) {
                BUFFER++;
                if(BUFFER > max_buffer.getDouble() / 2) {
                    setback();
                }

                if(BUFFER > max_buffer.getDouble()) {
                    fail("Climbing too quick DY: " + data.getPositionProcessor().getDeltaY());
                    setback();
                    BUFFER = 0;
                }
            } else {
                BUFFER = Math.max(0, buffer_decay.getDouble() - 0.75);
            }
        }
    }
}
