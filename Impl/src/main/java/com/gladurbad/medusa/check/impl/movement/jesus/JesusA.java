package com.gladurbad.medusa.check.impl.movement.jesus;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

@CheckInfo(name = "Jesus (A)", description = "Checks for Jesus hacks", experimental = true)
public class JesusA extends Check {

    public JesusA(final PlayerData data) {
        super(data);
    }
    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");
    private double buffer;
    private static final double MAX_BUFFER = 2;
    private static final double BUFFER_DECREMENT = 0.75;

    @Override
    public void handle(Packet packet) {
        if (packet.isPosition() || packet.isPosLook()) {
            Location location = data.getPlayer().getLocation();
            boolean posAir = location.getBlock().isEmpty();
            boolean onLiquid = location.clone().subtract(0, 0.3, 0).getBlock().isLiquid();

            if (onLiquid && posAir && !isNearSolidBlock(location)) {
                double deltaY = data.getPositionProcessor().getDeltaY();
                if (deltaY == 0 || deltaY == 0.33319999363422426) {
                    if ((buffer += 1) > MAX_BUFFER) {
                        if(setback.getBoolean()) {
                            setback();
                        }
                        fail("Hovering over water: " + deltaY);
                        buffer = 0;
                    }
                } else {
                    buffer = Math.max(0, buffer - BUFFER_DECREMENT);
                }
            }

            debug("DY: " + data.getPositionProcessor().getDeltaY() + " Buffer: " + buffer);
        }
    }

    private boolean isNearSolidBlock(Location location) {
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                Block block = location.clone().add(x, -0.1, z).getBlock();
                if (block.getType().isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }
}
