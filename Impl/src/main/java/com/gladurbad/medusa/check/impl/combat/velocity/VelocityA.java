package com.gladurbad.medusa.check.impl.combat.velocity;

import com.gladurbad.medusa.check.Check;
import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.data.processor.PositionProcessor;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import io.github.retrooper.packetevents.PacketEvents;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftBlock;



@CheckInfo(name = "Velocity (A)", experimental = true, description = "Checks for vertical velocity.", complextype = "Vertical")
public final class VelocityA extends Check {

    private static final ConfigValue minVelPct = new ConfigValue(
            ConfigValue.ValueType.INTEGER, "minimum-velocity-percentage"
    );
    private static final ConfigValue maxVelPct = new ConfigValue(
            ConfigValue.ValueType.INTEGER, "maximum-velocity-percentage"
    );

    public VelocityA(final PlayerData data) {
        super(data);
    }

    @Override
    public void handle(final Packet packet) {
        if (packet.isFlying()) {
            if (data.getVelocityProcessor().getTicksSinceVelocity() < 5) {
                final double deltaY = data.getPositionProcessor().getDeltaY();
                final double velocityY = data.getVelocityProcessor().getVelocityY();
                double diff = Math.abs(velocityY - deltaY);
                debug("dy=" + deltaY + " vy=" + velocityY);

                if(data.getVelocityProcessor().getTicksSinceVelocity() == 1) {
                    if(diff > 0.15) {
                        fail(diff);
                    }
                }
            }
        }
    }
}
