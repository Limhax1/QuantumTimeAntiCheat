package com.gladurbad.medusa.check.impl.movement.fly;

import com.gladurbad.api.check.CheckInfo;
import com.gladurbad.medusa.check.Check;
import com.gladurbad.medusa.config.ConfigValue;
import com.gladurbad.medusa.data.PlayerData;
import com.gladurbad.medusa.exempt.type.ExemptType;
import com.gladurbad.medusa.packet.Packet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import static java.awt.SystemColor.info;

@CheckInfo(name = "Fly (E)", description = "Checks for Continuous ascension.", experimental = true)
public class FlyE extends Check {

    private static final double DELTA_Y_THRESHOLD = 0.0;
    private static final int MAX_CONSECUTIVE_SAME_DELTA_Y = 3;

    private double lastDeltaY = 0.0;
    private int consecutiveSameDeltaYCount = 0;

    public FlyE(final PlayerData data) {
        super(data);
    }

    private static final ConfigValue setback = new ConfigValue(ConfigValue.ValueType.BOOLEAN, "setback");

    @Override
    public void handle(Packet packet) {
        if (packet.isFlying()) {
            final double deltaY = data.getPositionProcessor().getDeltaY();
            boolean exempt = isExempt(ExemptType.TELEPORT, ExemptType.FLYING, ExemptType.STEPPED, ExemptType.STAIRS, ExemptType.PISTON);

            if (deltaY > DELTA_Y_THRESHOLD && deltaY == lastDeltaY && !nearClimbable(data.getPlayer().getLocation()) && !exempt) {
                consecutiveSameDeltaYCount++;
                if (consecutiveSameDeltaYCount > MAX_CONSECUTIVE_SAME_DELTA_Y) {
                    if(setback.getBoolean()) {
                        setback();
                    }
                    fail(info);
                }
            } else {
                consecutiveSameDeltaYCount = 0;
            }

            String info = "DY: " + deltaY + "Consecutive deltaY: " + consecutiveSameDeltaYCount;

            debug(info);
            lastDeltaY = deltaY;
        }
    }

    private boolean nearClimbable(Location location) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block block = location.getBlock().getRelative(x, y, z);
                    Material type = block.getType();
                    if (type == Material.LADDER || type == Material.VINE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
