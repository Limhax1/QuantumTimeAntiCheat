package com.gladurbad.medusa.check.impl.movement.speed;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Speed (E)", description = "Checks for invalid Motion")
public class SpeedE extends Check {
    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");
    public SpeedE(PlayerData data) {
        super(data);
    }

    double BUFFER = 0;
    double MAX_BUFFER = 3;

    @Override
    public void handle(Packet packet) {
        if (packet.isPosLook() || packet.isPosition()) {

        }
    }
}
