package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;


@CheckInfo(name = "Fly (C)", description = "Checks for gravity.")
public final class FlyC extends Check {

    public FlyC(final PlayerData data) {
        super(data);
    }
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    @Override
    public void handle(final Packet packet) {
        if (packet.isPosition()) {
            final double deltaY = data.getPositionProcessor().getDeltaY();
            final double lastDeltaY = data.getPositionProcessor().getLastDeltaY();

            final boolean onGround = data.getPositionProcessor().getAirTicks() <= 5;

            final double prediction = Math.abs((lastDeltaY - 0.08) * 0.98F) < 0.005 ? -0.08 * 0.98F : (lastDeltaY - 0.08) * 0.98F;
            final double difference = Math.abs(deltaY - prediction);

            final boolean exempt = isExempt(
                    ExemptType.TELEPORT, ExemptType.NEAR_VEHICLE, ExemptType.FLYING,
                    ExemptType.INSIDE_VEHICLE, ExemptType.VELOCITY, ExemptType.PISTON
            );

            final boolean invalid = !exempt
                    && difference > 0.001D
                    && !onGround
                    && !(data.getPositionProcessor().getY() % 0.5 == 0 && data.getPositionProcessor().isOnGround() && lastDeltaY < 0);

            debug("posY=" + data.getPositionProcessor().getY() + " dY=" + deltaY + " at=" + data.getPositionProcessor().getAirTicks());

            if (invalid) {
                buffer += buffer < 50 ? 10 : 0;
                if (buffer % 10 == 0) {
                    if(setback.getBoolean()) {
                        setback();
                    }
                    if(buffer > 50) {
                        fail(String.format("diff=%.4f, buffer=%.2f, at=%o", difference, buffer, data.getPositionProcessor().getAirTicks()));
                        buffer = 0;
                    }
                }
            } else {
                buffer = Math.max(buffer - 0.75, 0);
            }
        }
    }
}