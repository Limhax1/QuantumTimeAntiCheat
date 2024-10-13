package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;

@CheckInfo(name = "Fly (A)", description = "Checks for autistic flight modules.", complextype = "Test")
public final class FlyA extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    private int ignoreTicks = 0;
    private double lastDeltaY = 0.0;

    public FlyA(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isPosition() || packet.isPosLook()) {
            final double DeltaY = data.getPositionProcessor().getDeltaY();
            final double speed = data.getPositionProcessor().getDeltaXZ();

            boolean exempt = isExempt(ExemptType.FLYING, ExemptType.FALLDAMAGE,
                    ExemptType.NEAR_VEHICLE, ExemptType.ELYTRA, ExemptType.HONEY_BLOCK);

            if (DeltaY == 0 && lastDeltaY != 0) {
                ignoreTicks = 1;
                debug("Right-click detected, ignoring next tick");
            }

            if (ignoreTicks > 0) {
                ignoreTicks--;
                debug("Ignoring tick, remaining: " + ignoreTicks);
                lastDeltaY = DeltaY;
                return;
            }

                debug(isExempt(ExemptType.HONEY_BLOCK));

            if (data.getPositionProcessor().getAirTicks() > 20 && data.getPositionProcessor().isInAir() && !exempt) {
                if (speed > 0.34) {
                    if(setback.getBoolean()) {
                        setback();
                    }
                    fail("Most likely flying " + speed);
                }
            }

            if (data.getPositionProcessor().isInAir() && data.getPositionProcessor().getAirTicks() > 20 && !exempt) {
                if (DeltaY == 0.0) {
                    buffer++;
                    if(++buffer > 2) {
                        if (setback.getBoolean()) {
                            setback();
                        }
                        fail("Hovering " + DeltaY);
                    }
                }
            }

            lastDeltaY = DeltaY;
        }
    }
}
