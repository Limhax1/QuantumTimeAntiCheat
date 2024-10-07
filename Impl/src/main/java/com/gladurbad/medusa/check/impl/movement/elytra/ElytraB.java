package com.gladurbad.medusa.check.impl.movement.elytra;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Elytra (B)", description = "Checks for constant DeltaY while flying", experimental = true, complextype = "Invalid Y")
public class ElytraB extends Check {
    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private int constantYTicks = 0;
    private static final int MAX_CONSTANT_Y_TICKS = 20;
    private double lastDeltaY = 0.0;

    public ElytraB(PlayerData data) {
        super(data);
    }

    @Override
    public void handle(Packet packet) {
        if (packet.isPosLook() || packet.isPosition()) {
            if (isExempt(ExemptType.ELYTRA)) {
                double currentDeltaY = data.getPositionProcessor().getDeltaY();
                debug(constantYTicks + " " + currentDeltaY + " " + lastDeltaY);

                if (Math.abs(currentDeltaY - lastDeltaY) < 1E-7) {
                    constantYTicks++;
                    if (constantYTicks >= MAX_CONSTANT_Y_TICKS) {
                        if(setback.getBoolean()) {
                            setback();
                        }
                        fail("Constant DeltaY for " + MAX_CONSTANT_Y_TICKS + " ticks");
                        constantYTicks = 0;
                    }
                } else {
                    constantYTicks = 0;
                }

                lastDeltaY = currentDeltaY;
            } else {
                constantYTicks = 0;
                lastDeltaY = 0.0;
            }
        }
    }
}
