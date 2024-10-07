package com.gladurbad.medusa.check.impl.movement.elytra;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Elytra (A)", description = "Checks for ElytraFly", experimental = true, complextype = "High Speed")
public class ElytraA extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    public ElytraA(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isPosLook() || packet.isPosition()) {
            if (isExempt(ExemptType.ELYTRA) && data.getPositionProcessor().getDeltaXZ() > 2.25) {
                if(buffer++ > max_buffer.getDouble()) {
                    if (setback.getBoolean()) {
                        setback();
                    }
                    fail("Going too fast with an elytra: " + data.getPositionProcessor().getDeltaXZ());
                }
            } else {
                buffer = Math.max(0, buffer - buffer_decay.getDouble());
            }
        }
    }
}
