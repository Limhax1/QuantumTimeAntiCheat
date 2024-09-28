package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Bukkit;


@CheckInfo(name = "Fly (C)", description = "Checks for gravity.")
public final class FlyC extends Check {

    private static final ConfigValue max_buffer = new ConfigValue(ConfigValue.ValueType.DOUBLE, "max_buffer");
    private static final ConfigValue buffer_decay = new ConfigValue(ConfigValue.ValueType.DOUBLE, "buffer_decay");
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    public FlyC(final PlayerData data) {
        super(data);
    }

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
                    ExemptType.INSIDE_VEHICLE, ExemptType.VELOCITY
            );

            final boolean invalid = !exempt
                    && difference > 0.001D
                    && !onGround
                    && !(data.getPositionProcessor().getY() % 0.5 == 0 && data.getPositionProcessor().isOnGround() && lastDeltaY < 0);

            debug("posY=" + data.getPositionProcessor().getY() + " dY=" + deltaY + " at=" + data.getPositionProcessor().getAirTicks());

            if (invalid) {
                buffer += buffer < 50 ? 10 : 0;
                if (buffer > max_buffer.getDouble()) {
                    if(setback.getBoolean()) {
                        setback();
                    }

                    fail(String.format("diff=%.4f, buffer=%.2f, at=%o", difference, buffer, data.getPositionProcessor().getAirTicks()));
                }
            } else {
                buffer = Math.max(buffer - buffer_decay.getDouble(), 0);
            }
        }
    }
}